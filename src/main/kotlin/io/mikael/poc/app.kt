package io.mikael.poc

import io.mikael.poc.services.PresentationService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Controller
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Mono

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
        POST("/upload/image", handler::uploadImage)
    }

}

@Controller
class PresentationHandler(val svc: PresentationService) {

    companion object {
        private val log = LoggerFactory.getLogger(PresentationHandler::class.java)
    }

    fun indexPage(req: ServerRequest): Mono<ServerResponse> {
        return ok().contentType(TEXT_HTML_UTF8).render("index")
    }

    fun showImage(req: ServerRequest): Mono<ServerResponse> {
        val id = req.pathVariable("id").toLong()
        return ok().contentType(MediaType.IMAGE_PNG).syncBody(svc.showImage(id))
    }

    fun showMask(req: ServerRequest): Mono<ServerResponse> {
        val id = req.pathVariable("id").toLong()
        return ok().contentType(MediaType.IMAGE_PNG).syncBody(svc.showMask(id))
    }

    fun uploadImage(req: ServerRequest): Mono<ServerResponse> {
        return req.body(BodyExtractors.toMultipartData())
                .flatMap {
                    val filePart = it.getFirst("file") as FilePart
                    ok().syncBody(svc.processImage(filePart))
                }
    }

}