package io.mikael.poc.services

import io.mikael.poc.AppConfiguration
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.tensorflow.*
import org.tensorflow.ndarray.Shape
import org.tensorflow.ndarray.buffer.DataBuffers
import org.tensorflow.proto.framework.ConfigProto
import org.tensorflow.proto.framework.GPUOptions
import org.tensorflow.proto.framework.GraphDef
import org.tensorflow.types.TInt64
import org.tensorflow.types.TUint8
import java.awt.Image
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.nio.ByteBuffer
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import kotlin.math.max

@Service
class SegmentationService(val app: AppConfiguration) {

    companion object {
        private val log = LoggerFactory.getLogger(SegmentationService::class.java)
        const val INPUT_TENSOR_NAME = "ImageTensor:0"
        const val OUTPUT_TENSOR_NAME = "SemanticPredictions:0"
        const val BATCH_SIZE = 1L
        const val CHANNELS = 3L
        const val INPUT_IMAGE_SIZE = 513f
        const val LABEL_PERSON = 15L
    }

    private lateinit var graph: Graph

    private lateinit var session: Session

    @PostConstruct
    fun start() {
        log.info("TensorFlow: ${TensorFlow.version()}")

        graph = Graph().apply {
            app.model.inputStream.use {
                importGraphDef(GraphDef.parseFrom(it))
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
        config.gpuOptions = gpuOptions.build()
        session = Session(graph, config.build())
    }

    @PreDestroy
    fun stop() {
        session.close()
        graph.close()
    }

    /**
     * The calls to `use {}` will close the tensors and should free all of the resources.
     */
    fun transform(inputImage: BufferedImage) = makeImageTensor(inputImage).use {
        session.runner()
            .feed(INPUT_TENSOR_NAME, it)
            .fetch(OUTPUT_TENSOR_NAME)
            .run()
            .map { x -> x as TInt64 }
            .first()
            .use(::maskTensorToImage)
    }

    private fun labelToColour(label: Long) =
            if (label == LABEL_PERSON) Int.MIN_VALUE else Int.MAX_VALUE

    /**
     * Contract: Closes the input tensor.
     */
    private fun maskTensorToImage(result: TInt64): BufferedImage {
        val (_, height, width) = result.shape().asArray()
        val maskImage = BufferedImage(width.toInt(), height.toInt(), BufferedImage.TYPE_BYTE_BINARY)
        for (x in 0 until width) {
            for (y in 0 until height) {
                val i = ((y * width) + x)
                maskImage.setRGB(x.toInt(), y.toInt(), labelToColour(result.getLong(0, y, x)))
            }
        }
        return maskImage
    }

    private fun makeImageTensor(input: BufferedImage): TUint8 {
        val resizeRatio = INPUT_IMAGE_SIZE / max(input.width, input.height)
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

        val shape = Shape.of(BATCH_SIZE, img.height.toLong(), img.width.toLong(), CHANNELS)
        return Tensor.of(TUint8::class.java, shape, DataBuffers.of(ByteBuffer.wrap(data)))
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
