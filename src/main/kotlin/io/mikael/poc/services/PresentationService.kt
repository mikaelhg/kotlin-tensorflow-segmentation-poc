package io.mikael.poc.services

import io.mikael.poc.ProcessResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.imageio.ImageIO
import jakarta.servlet.http.Part

@Service
class PresentationService(private val fileManager: FileManagerService,
                          private val segmentation: SegmentationService) {

    companion object {
        private val log = LoggerFactory.getLogger(PresentationService::class.java)
    }

    private val counter = AtomicLong()

    private val images = ConcurrentHashMap<Long, String>()

    private val masks = ConcurrentHashMap<Long, String>()

    fun processImage(filePart: Part): ProcessResponse {
        val imageId = persistImage(filePart)
        val mask = segmentation.transform(ImageIO.read(File(images[imageId]!!)))
        val maskId = persistMask(mask)
        return ProcessResponse(imageId, maskId)
    }

    fun showMask(id: Long): ByteArray = Files.readAllBytes(Paths.get(masks[id]!!))

    fun showImage(id: Long): ByteArray = Files.readAllBytes(Paths.get(images[id]!!))

    /**
     * Cut the contents of the input image onto a white background,
     * but only those pixels which are labeled HUMAN on the prediction mask.
     *
     * Hacky and unoptimal as hell, but who cares in a POC.
     */
    fun showCombined(imageId: Long, maskId: Long): ByteArray {
        val image = ImageIO.read(File(images[imageId]!!))
        val mask = ImageIO.read(File(masks[maskId]!!))
        val result = BufferedImage(image.width, image.height, BufferedImage.TYPE_3BYTE_BGR)

        result.createGraphics().apply {
            color = Color.WHITE
            fillRect(0, 0, image.width, image.height)
            dispose()
        }

        val fx = mask.width.toFloat() / image.width.toFloat()
        val fy = mask.height.toFloat() / image.height.toFloat()

        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                val m = mask.getRGB((x * fx).toInt(), (y * fy).toInt())
                if (m < -1) {
                    result.setRGB(x, y, image.getRGB(x, y))
                }
            }
        }

        return ByteArrayOutputStream().use { baos ->
            ImageIO.write(result, "PNG", baos)
            baos.toByteArray()
        }
    }

    private fun persistImage(filePart: Part): Long {
        val id = counter.getAndIncrement()
        images[id] = fileManager.saveImage(filePart)
        return id
    }

    private fun persistMask(bufferedImage: BufferedImage): Long {
        val id = counter.getAndIncrement()
        masks[id] = fileManager.saveMask(bufferedImage)
        return id
    }

}
