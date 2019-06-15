package de.seiderer.eventerpretor.plugins.input

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import de.seiderer.eventerpretor.core.Engine
import de.seiderer.eventerpretor.plugins.base.DataValue
import de.seiderer.eventerpretor.plugins.base.InNode
import de.seiderer.eventerpretor.plugins.base.Options
import io.vertx.core.AbstractVerticle
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
import kotlin.properties.Delegates
import io.vertx.core.Handler
import java.nio.file.Paths
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.HashMap





/**
 * @author Andreas Seiderer
 * Plugin to provide a web interface for data input
 */
class InWebInput(engine: Engine, name:String) : InNode(engine,name,0) {
    var deploymentID = ""
    var websocketval : LinkedBlockingQueue<DataValue> = LinkedBlockingQueue()

    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val issueAdapter = moshi.adapter(Map::class.java)

    override fun setConfig() {
        val path : String = engine.workingdir + "/options_" + this.nodename + ".json"

        //read options from file if it exists; if not create new with default values
        if (File(path).exists())
            opts.fromJsonFile(path)
        else {
            opts.add("webroot", "webroot", "name of the web root directory; relative to working directory")
            opts.add("isJSON", false, "received message should be parsed as JSON")
            opts.add("port", 9999, "port of the web server")
            opts.toJsonFile(path)
        }
    }

    override fun start() {
        engine.vertx.deployVerticle(WebInputUI(engine,opts,this.nodename)) { ar ->
            if (ar.succeeded()) {
                deploymentID = ar.result()
                println("Web In UI started: http://localhost:"+ opts.getIntVal("port").toString())
            } else {
                println("Could not start Web In UI")
                ar.cause().printStackTrace()
            }
        }

        //receive data from websocket
        val eb = engine.vertx.eventBus()

        if (opts.getBoolVal("isJSON"))
            eb.consumer<String>(this.nodename) { message ->
               run {
                    val json = issueAdapter.fromJson(message.body())
                    val jsonhm = HashMap<Any, Any>(json)
                    if (json != null)
                        websocketval.put(
                            DataValue(
                                engine.getCurrentDate(),
                                jsonhm,
                                this.nodename
                            )
                        )
                }
            }
        else
            eb.consumer<String>(this.nodename) { message ->
                websocketval.put(DataValue(engine.getCurrentDate(), hashMapOf("message" to message.body()), this.nodename))
            }
    }

    override fun stop() {
        if (deploymentID != "")
            engine.vertx.undeploy(deploymentID) { res2 ->
                if (res2.succeeded()) {
                    println("Web In UI undeployed!")
                } else {
                    res2.cause().printStackTrace()
                }
            }
    }

    override fun kill() {
    }

    override fun threadedTask() {
        val value = websocketval.poll(1000, TimeUnit.MILLISECONDS)
        if (value != null) {
            dataOut(value)
        }
    }

}


/**
 * @author Andreas Seiderer
 * Vertx.io based web interface
 *
 */
class WebInputUI(engine: Engine, opts: Options, nodename: String): AbstractVerticle() {
    var engine: Engine by Delegates.notNull()
    var opts: Options by Delegates.notNull()
    var nodename: String

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
                .addOutboundPermitted(PermittedOptions().setAddressRegex("auction\\.[0-9]+"))       //TODO

        return SockJSHandler.create(vertx).bridge(options) { event ->

            if (event.type() == BridgeEventType.SOCKET_CREATED) {
                println("Web In UI Socket created")
            }
            event.complete(true)
        }
    }

    private fun uiApiRouter(): Router {
        val handler = WebInputUIHandler(engine, nodename)

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



class WebInputUIHandler(engine: Engine, nodename: String) {
    var engine : Engine by Delegates.notNull()
    var nodename: String by Delegates.notNull()

    init {
        this.engine = engine
        this.nodename = nodename
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
        val clientCmd = context.bodyAsJson.getString("msg")

        //println("Web In client cmd:\t" + clientCmd)

        val eb = engine.vertx.eventBus()
        eb.publish(nodename, clientCmd)

        if (true) {
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
        //val clientCmd = context.getBodyAsJson().getString("msg")

        //println("Web In client cmd:\t" + clientCmd)

        context.next()
    }

}