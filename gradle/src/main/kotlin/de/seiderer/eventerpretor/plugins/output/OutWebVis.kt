package de.seiderer.eventerpretor.plugins.output

import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import de.seiderer.eventerpretor.core.Engine
import de.seiderer.eventerpretor.drools.Command
import de.seiderer.eventerpretor.plugins.base.Options
import de.seiderer.eventerpretor.plugins.base.OutNode
import io.vertx.core.AbstractVerticle
import io.vertx.core.Handler
import io.vertx.core.http.HttpServerRequest
import io.vertx.ext.bridge.BridgeEventType
import io.vertx.ext.bridge.PermittedOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.ErrorHandler
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.ext.web.handler.sockjs.BridgeOptions
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates


/**
 * @author Andreas Seiderer
 * Plugin to provide a web interface for data visualizations
 */
class OutWebVis(engine: Engine, name:String) : OutNode(engine,name,0) {
    var deploymentID = ""

    override fun setConfig() {
        val path : String = engine.workingdir + "/options_" + this.nodename + ".json"

        //read options from file if it exists; if not create new with default values
        if (File(path).exists())
            opts.fromJsonFile(path)
        else {
            opts.add("webroot", "webroot", "name of the web root directory; relative to working directory")
            opts.add("port", 8888, "port of the web server")
            opts.toJsonFile(path)
        }
    }

    override fun start() {
        engine.vertx.deployVerticle(WebVisUI(engine,opts,this.nodename)) { ar ->
            if (ar.succeeded()) {
                deploymentID = ar.result()
                println("Web Vis UI started: http://localhost:"+ opts.getIntVal("port").toString())
            } else {
                println("Could not start Web Vis UI")
                ar.cause().printStackTrace()
            }
        }
    }

    override fun stop() {

        if (deploymentID != "")
            engine.vertx.undeploy(deploymentID) { res2 ->
                if (res2.succeeded()) {
                    println("Web Vis UI undeployed!")
                } else {
                    res2.cause().printStackTrace()
                }
            }
    }

    override fun kill() {
    }

    override fun threadedTask() {
        val value = indata.poll(1000, TimeUnit.MILLISECONDS)
        if (value != null) {
            //println("TODO OutWebVis: " + value.toString())  //TODO

            // convert multiple input data types to string
            val valout : String
            if (value.value is DoubleArray)
                valout = value.value.joinToString(";")
            else if (value.value is Command)
                valout = value.value.value
            else
                valout = value.value.toString()


            val msgdata = hashMapOf("timestamp" to value.timestamp.time, "value" to valout, "sourcename" to value.sourcename)

            val moshi = Moshi.Builder()
                    .add(KotlinJsonAdapterFactory())
                    .build()

            val issueAdapter = moshi.adapter(Map::class.java)

            engine.vertx.eventBus().publish(this.nodename + "_web", issueAdapter.toJson(msgdata))

        }
    }

}


/**
 * @author Andreas Seiderer
 * Vertx.io based web interface
 *
 */
class WebVisUI(engine:Engine, opts: Options, nodename: String): AbstractVerticle() {
    var engine: Engine by Delegates.notNull()
    var opts: Options by Delegates.notNull()
    var nodename: String by Delegates.notNull()

    init {
        this.engine = engine
        this.opts = opts
        this.nodename = nodename
    }

    override fun start() {
        val router = Router.router(vertx)

        router.route("/eventbus/*").handler(eventBusHandler())
        router.mountSubRouter("/api", uiApiRouter())
        router.route().failureHandler(errorHandler())
        router.route().handler(staticHandler())
        vertx.createHttpServer().requestHandler(router).listen(opts.getIntVal("port"))
    }

    private fun eventBusHandler(): SockJSHandler {

        val options = BridgeOptions()
                .addOutboundPermitted(PermittedOptions().setAddressRegex(this.nodename+"_web"))       //TODO

        return SockJSHandler.create(vertx).bridge(options) { event ->

            if (event.type() == BridgeEventType.SOCKET_CREATED) {
                println("Web Vis UI Socket created")
            }
            event.complete(true)
        }
    }

    private fun uiApiRouter(): Router {
        val handler = WebVisUIHandler(engine)

        val router = Router.router(vertx)
        router.route().handler(BodyHandler.create())

        router.route().consumes("application/json")
        router.route().produces("application/json")

        router.route("/ui/:id").handler(handler::iniCmdInSharedData)
        router.get("/ui/:id").handler(handler::handleGetData)
        router.patch("/ui/:id").handler(handler::handleChangeCmd)

        return router
    }

    private fun errorHandler(): ErrorHandler {
        return ErrorHandler.create(true)
    }

    private fun staticHandler(): StaticHandler {
        val executingdir =  System.getProperty("user.dir")
        val sourceFile = Paths.get(executingdir)
        val targetFile = Paths.get(engine.workingdir+"/"+opts.getStringVal("webroot"))
        val relpath = sourceFile.relativize(targetFile).toString()

        return StaticHandler.create()
                .setCachingEnabled(false)
                .setWebRoot(relpath)
    }
}



class WebVisUIHandler(engine:Engine) {
    var engine : Engine by Delegates.notNull()

    init {
        this.engine = engine
    }

    fun handleGetData(context: RoutingContext) {
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

        println("Web UI client cmd:\t" + clientCmd)

        if (clientCmd.equals("stop")) {
            //known command
            context.response()
                    .setStatusCode(200)
                    .end()
        } else {
            context.response()
                    .setStatusCode(422)
                    .end()
        }
    }

    fun iniCmdInSharedData(context: RoutingContext) {
        //val clientId = context.request().getParam("id")
        //val clientCmd = context.getBodyAsJson().getString("cmd")

        //println("Web UI client cmd:\t" + clientCmd)

        context.next()
    }

}