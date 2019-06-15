package de.seiderer.eventerpretor.core

import io.vertx.ext.web.RoutingContext
import kotlin.properties.Delegates
import io.vertx.core.impl.VertxImpl.context
import java.util.concurrent.*
import java.util.concurrent.FutureTask
import java.util.concurrent.Callable


class WebUIhandler(engine:Engine) {
    var engine : Engine by Delegates.notNull()

    init {
        this.engine = engine
    }

    fun handleGetData(context: RoutingContext) {
        context.response()
                .putHeader("content-type", "application/json")
                .setStatusCode(200)
                .end(engine.getNodeStructureAsJSON())


        //val clientId = context.request().getParam("id")
        /*val auction = this.repository.getById(auctionId)

        if (auction.isPresent()) {
            context.response()
                    .putHeader("content-type", "application/json")
                    .setStatusCode(200)
                    .end(Json.encodePrettily(auction.get()))
        } else {
            context.response()
                    .putHeader("content-type", "application/json")
                    .setStatusCode(404)
                    .end()
        }*/
    }

    fun handleChangeCmd(context: RoutingContext) {
        //val clientId = context.request().getParam("id")
        val clientCmd = context.bodyAsJson.getString("cmd")

        //println("Web UI client cmd:\t" + clientCmd)

        if (clientCmd == "stop" || (clientCmd.length > 8 && clientCmd.substring(0, 7) == "restart")) {
            //known command
            context.response()
                    .setStatusCode(200)
                    .end()
        } else {
            context.response()
                    .setStatusCode(422)
                    .end()
        }

        /*
        if (validator.validate(auctionRequest)) {
            this.repository.save(auctionRequest)
            context.vertx().eventBus().publish("auction." + auctionId, context.getBodyAsString())

            context.response()
                    .setStatusCode(200)
                    .end()
        } else {
            context.response()
                    .setStatusCode(422)
                    .end()
        }*/
    }

    fun iniCmdInSharedData(context: RoutingContext) {
        //val clientId = context.request().getParam("id")
        val clientCmd = context.bodyAsJson.getString("cmd")

        println("Web UI client cmd:\t" + clientCmd)

        if (clientCmd == "stop") {
            //prevent warnings because of blocking the thread
            engine.asyncshutdown()

            /*context.vertx().close({ result ->
                if (result.succeeded()) {
                    println("Web UI stopped")
                }
            })*/
        } else if (clientCmd.length > 8 && clientCmd.substring(0, 7) == "restart") {
            val nodename = clientCmd.substring(8)
            engine.asyncrestartnode(nodename)
        }

        context.next()
    }

}


