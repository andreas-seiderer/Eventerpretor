package de.seiderer.eventerpretor.core

import io.vertx.core.AbstractVerticle
import io.vertx.core.Handler
import io.vertx.core.http.HttpServerRequest
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.ErrorHandler
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.ext.web.handler.sockjs.BridgeOptions
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import io.vertx.ext.bridge.BridgeEventType
import io.vertx.ext.bridge.PermittedOptions
import kotlin.properties.Delegates

/**
 * @author Andreas Seiderer
 * Vertx.io based web interface
 *
 * https://github.com/vert-x3/vertx-examples/tree/master/kotlin-examples/web
 * vertx.io/blog/real-time-bidding-with-websockets-and-vert-x/
 * http://maballesteros.com/articles/vertx3-kotlin-rest-jdbc-tutorial/
 */
class WebUI(engine:Engine): AbstractVerticle() {
    var engine : Engine by Delegates.notNull()

    init {
        this.engine = engine
    }

    override fun start() {
        val router = Router.router(vertx)

        router.route("/eventbus/*").handler(eventBusHandler())
        router.mountSubRouter("/api", uiApiRouter())
        router.route().failureHandler(errorHandler())
        router.route().handler(staticHandler())
        vertx.createHttpServer().requestHandler(router).listen(8080)
    }

    private fun eventBusHandler(): SockJSHandler {

        val options = BridgeOptions()
                .addOutboundPermitted(PermittedOptions().setAddressRegex("auction\\.[0-9]+"))

        return SockJSHandler.create(vertx).bridge(options) { event ->

            if (event.type() == BridgeEventType.SOCKET_CREATED) {
                println("Web UI Socket created")
            }
            event.complete(true)
        }
    }

    private fun uiApiRouter(): Router {
        val handler = WebUIhandler(engine)

        val router = Router.router(vertx)
        router.route().handler(BodyHandler.create())

        router.route().consumes("application/json")
        router.route().produces("application/json")

        router.route("/ui/:id").handler (handler::iniCmdInSharedData )
        router.get("/ui/").handler(handler::handleGetData)
        router.patch("/ui/:id").handler(handler::handleChangeCmd)

        return router
    }

    private fun errorHandler(): ErrorHandler {
        return ErrorHandler.create(true)
    }

    private fun staticHandler(): StaticHandler {
        return StaticHandler.create()
                .setCachingEnabled(false)
    }

}