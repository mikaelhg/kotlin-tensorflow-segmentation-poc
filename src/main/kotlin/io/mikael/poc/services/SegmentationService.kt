package io.mikael.poc.services

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.util.StreamUtils
import org.tensorflow.Graph
import org.tensorflow.Session
import org.tensorflow.Tensor
import org.tensorflow.TensorFlow
import org.tensorflow.framework.ConfigProto
import org.tensorflow.framework.GPUOptions
import org.tensorflow.types.UInt8
import java.awt.Image
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.nio.ByteBuffer
import java.nio.LongBuffer
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

@Component
@ConfigurationProperties(prefix="app")
class AppConfiguration {

    lateinit var model: Resource

    var gpuMemoryFraction: Double = 0.25

    var gpuEnabled: Boolean = true

}

@Service
class SegmentationService {

    companion object {
        private val log = LoggerFactory.getLogger(SegmentationService::class.java)
        const val INPUT_TENSOR_NAME = "ImageTensor:0"
        const val OUTPUT_TENSOR_NAME = "SemanticPredictions:0"
        const val BATCH_SIZE = 1L
        const val CHANNELS = 3L
        const val INPUT_IMAGE_SIZE = 513f
        const val LABEL_PERSON = 15L
    }

    @Autowired
    lateinit var app: AppConfiguration

    private lateinit var graph: Graph

    private lateinit var session: Session

    @PostConstruct
    fun start() {
        log.info("TensorFlow: ${TensorFlow.version()}")

        graph = Graph().apply {
            app.model.inputStream.use {
                importGraphDef(StreamUtils.copyToByteArray(it))
            }
        }

        val gpuOptions = GPUOptions.newBuilder()
        val config = ConfigProto.newBuilder()
        if (app.gpuEnabled) {
            log.debug("GPU enabled")
            gpuOptions
                    .setPerProcessGpuMemoryFraction(app.gpuMemoryFraction)
                    .setAllowGrowth(true)
        } else {
            log.debug("GPU disabled")
            config.putDeviceCount("GPU", 0)
        }
        config.setGpuOptions(gpuOptions.build())
        session = Session(graph, config.build().toByteArray())
    }

    @PreDestroy
    fun stop() {
        session.close()
        graph.close()
    }

    fun segmented(inputImage: BufferedImage): BufferedImage {
        makeImageTensor(inputImage).use {
            return session.runner()
                    .feed(INPUT_TENSOR_NAME, it)
                    .fetch(OUTPUT_TENSOR_NAME)
                    .run().get(0).expect(java.lang.Long::class.java)
                    .let(::maskTensorToImage)
        }
    }

    private fun labelToColour(label: Long) =
            if (label == LABEL_PERSON) Int.MIN_VALUE else Int.MAX_VALUE

    private fun maskTensorToImage(result: Tensor<java.lang.Long>): BufferedImage {
        val maskBuffer = LongBuffer.allocate(result.numElements())
        result.writeTo(maskBuffer)

        val (_, height, width) = result.shape()
        val maskImage = BufferedImage(width.toInt(), height.toInt(), BufferedImage.TYPE_BYTE_BINARY)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val i = ((y * width) + x).toInt()
                maskImage.setRGB(x.toInt(), y.toInt(), labelToColour(maskBuffer[i]))
            }
        }

        return maskImage
    }

    private fun makeImageTensor(input: BufferedImage): Tensor<UInt8> {
        val resizeRatio = INPUT_IMAGE_SIZE / Math.max(input.width, input.height)
        val rw = (input.width * resizeRatio).toInt()
        val rh = (input.height * resizeRatio).toInt()

        val tmp = input.getScaledInstance(rw, rh, Image.SCALE_SMOOTH)
        val img = BufferedImage(rw, rh, BufferedImage.TYPE_3BYTE_BGR)

        img.createGraphics().apply {
            drawImage(tmp, 0, 0, null)
            dispose()
        }

        val data = (img.data.dataBuffer as DataBufferByte).data
        bgr2rgb(data)

        val shape = longArrayOf(BATCH_SIZE, img.height.toLong(), img.width.toLong(), CHANNELS)
        return Tensor.create(UInt8::class.java, shape, ByteBuffer.wrap(data))
    }

    private fun bgr2rgb(data: ByteArray) {
        var i = 0
        while (i < data.size) {
            val tmp = data[i]
            data[i] = data[i + 2]
            data[i + 2] = tmp
            i += 3
        }
    }

}
