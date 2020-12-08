package io.mikael.poc.services

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import javax.imageio.ImageIO
import javax.servlet.http.Part

@Service
class FileManagerService {

    companion object {
        private val log = LoggerFactory.getLogger(FileManagerService::class.java)
    }

    fun saveImage(filePart: Part): String {
        val temp = File.createTempFile("image-", ".png")
        filePart.inputStream.use { inputStream ->
            Files.copy(inputStream, temp.toPath(), REPLACE_EXISTING)
        }
        return temp.absolutePath
    }

    fun saveMask(bufferedImage: BufferedImage): String {
        val temp = File.createTempFile("mask-", ".png")
        ImageIO.write(bufferedImage, "PNG", temp)
        return temp.absolutePath
    }

}