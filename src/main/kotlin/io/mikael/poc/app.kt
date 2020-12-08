package io.mikael.poc

import io.mikael.poc.services.PresentationService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import org.springframework.web.servlet.function.ServerResponse.ok
import org.springframework.web.servlet.function.router

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        runApplication<Application>(*args)
    }
}

@SpringBootApplication
class Application(val handler: PresentationHandler) {

    companion object {
        private val log = LoggerFactory.getLogger(Application::class.java)
    }

    @Bean
    fun mainRouter() = router {
        GET("/", handler::indexPage)
        GET("/show/image/{id}", handler::showImage)
        GET("/show/mask/{id}", handler::showMask)
        GET("/show/combined/{imageId}/{maskId}", handler::showCombined)
        POST("/upload/image", handler::uploadImage)
    }

}

@Component
@ConfigurationProperties(prefix="app")
class AppConfiguration {

    /**
     * The DeepLab model TensorFlow model protocol buffer file.
     *
     * app.model: 'classpath:deeplabv3_mnv2_pascal_trainval.pb'
     */
    lateinit var model: Resource

    /**
     * How much of the GPU memory we'll reserve maximum.
     *
     * Starts with around 100 MB, and grows as needed up to this limit.
     *
     * app.gpu-memory-fraction: 0.25
     */
    var gpuMemoryFraction: Double = 0.25

    /**
     * Doesn't work yet, don't know if I can get this to work.
     *
     * app.gpu-enabled: true
     */
    var gpuEnabled: Boolean = true

}

@Component
class PresentationHandler(val svc: PresentationService) {

    companion object {
        private val log = LoggerFactory.getLogger(PresentationHandler::class.java)
        private val TEXT_HTML_UTF8 = MediaType.parseMediaType("text/html; charset=utf-8")
    }

    fun indexPage(req: ServerRequest): ServerResponse {
        return html().render("index")
    }

    fun showImage(req: ServerRequest): ServerResponse {
        val id = req.pathVariable("id").toLong()
        return png().body(svc.showImage(id))
    }

    fun showMask(req: ServerRequest): ServerResponse {
        val id = req.pathVariable("id").toLong()
        return png().body(svc.showMask(id))
    }

    fun showCombined(req: ServerRequest): ServerResponse {
        val imageId = req.pathVariable("imageId").toLong()
        val maskId = req.pathVariable("maskId").toLong()
        return png().body(svc.showCombined(imageId, maskId))
    }

    fun uploadImage(req: ServerRequest): ServerResponse {
        val filePart = req.multipartData().getFirst("file")!!
        return ok().body(svc.processImage(filePart))
    }

    private fun png() = ServerResponse.ok().contentType(MediaType.IMAGE_PNG)

    private fun html() = ServerResponse.ok().contentType(TEXT_HTML_UTF8)

}
