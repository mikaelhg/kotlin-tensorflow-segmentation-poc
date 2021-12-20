package io.mikael.poc.test

import io.mikael.poc.services.SegmentationService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE
import org.springframework.core.io.Resource
import javax.imageio.ImageIO
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = NONE)
class SegmentationTest {

    private val gras = SegmentationTest::class.java.classLoader::getResourceAsStream

    @Autowired
    lateinit var segmentationService: SegmentationService

    @Value("classpath:bonding-daylight-enjoying-708440.jpg")
    lateinit var imageFile: Resource

    @Test
    fun processMask() {
        val truth = gras("mask.png").use {
            ImageIO.read(it)
        }
        val mask = imageFile.inputStream.use {
            segmentationService.transform(ImageIO.read(it))
        }
        assertEquals(truth.width, mask.width)
        assertEquals(truth.height, mask.height)
        var correct = 0
        for (x in 0 until truth.width) {
            for (y in 0 until truth.height) {
                if (mask.getRGB(x, y) == truth.getRGB(x, y)) {
                    correct++
                }
            }
        }
        assertTrue { correct >= (truth.width * truth.height * 0.99) }
    }

}
