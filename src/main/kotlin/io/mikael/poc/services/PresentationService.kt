package io.mikael.poc.services

import io.mikael.poc.ProcessResponse
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.imageio.ImageIO

@Service
class PresentationService(val fileManager: FileManagerService, val segmentation: SegmentationService) {

    private val counter = AtomicLong()

    private val images = ConcurrentHashMap<Long, String>()

    private val masks = ConcurrentHashMap<Long, String>()

    fun processImage(filePart: FilePart): ProcessResponse {
        val imageId = persist(filePart)
        val mask = segmentation.segmented(ImageIO.read(File(images[imageId])))
        val maskId = persist(mask)
        return ProcessResponse(imageId, maskId)
    }

    fun showMask(id: Long): ByteArray {
        return Files.readAllBytes(Paths.get(masks[id]))
    }

    fun showImage(id: Long): ByteArray {
        return Files.readAllBytes(Paths.get(images[id]))
    }

    private fun persist(filePart: FilePart): Long {
        val id = counter.getAndIncrement()
        images.put(id, fileManager.saveImage(filePart))
        return id
    }

    private fun persist(bufferedImage: BufferedImage): Long {
        val id = counter.getAndIncrement()
        masks.put(id, fileManager.saveMask(bufferedImage))
        return id
    }

}