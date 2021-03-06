package io.mikael.poc.test

import io.mikael.poc.services.SegmentationService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE
import org.springframework.core.io.Resource
import org.springframework.test.context.junit4.SpringRunner
import javax.imageio.ImageIO

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = NONE)
class SegmentationTest {

    @Autowired
    lateinit var segmentationService: SegmentationService

    @Value("classpath:bonding-daylight-enjoying-708440.jpg")
    lateinit var imageFile: Resource

    @Test
    fun init() {
        imageFile.inputStream.use {
            val mask = segmentationService.transform(ImageIO.read(it))
            val targetSize = SegmentationService.INPUT_IMAGE_SIZE.toInt()
            Assertions.assertTrue(mask.width == targetSize || mask.height == targetSize, "size")
        }
    }

}
