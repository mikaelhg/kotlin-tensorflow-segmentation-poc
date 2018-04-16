package io.mikael.poc.services

import org.slf4j.LoggerFactory
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

@Service
class FileManagerService {

    companion object {
        private val log = LoggerFactory.getLogger(FileManagerService::class.java)
    }

    fun saveImage(filePart: FilePart): String {
        val temp = File.createTempFile("image-", ".png")
        filePart.transferTo(temp)
        return temp.absolutePath
    }

    fun saveMask(bufferedImage: BufferedImage): String {
        val temp = File.createTempFile("mask-", ".png")
        ImageIO.write(bufferedImage, "PNG", temp)
        return temp.absolutePath
    }

}