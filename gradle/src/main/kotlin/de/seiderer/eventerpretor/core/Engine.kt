package de.seiderer.eventerpretor.core

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import de.seiderer.eventerpretor.plugins.base.BaseNode
import de.seiderer.eventerpretor.plugins.base.InNode
import de.seiderer.eventerpretor.plugins.base.NodeType
import de.seiderer.eventerpretor.plugins.base.TransformNode
import io.vertx.core.Vertx
import org.fusesource.jansi.AnsiConsole
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.properties.Delegates
import org.fusesource.jansi.Ansi.*
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.FutureTask
import kotlin.collections.ArrayList

/**
 * @author Andreas Seiderer
 */
class Engine(workingdir:String) {
    /**
     * stores all nodes with their name
     */
    private var map: ConcurrentHashMap<String, BaseNode> by Delegates.notNull()

    private var executor = Executors.newFixedThreadPool(1)

    var isShuttingDown = false

    /**
     * global vertx instance; use for all plugins that require a web interface
     */
    var vertx: Vertx by Delegates.notNull()

    /**
     * workingdir
     */
    var workingdir: String by Delegates.notNull()

    init {
        map = ConcurrentHashMap()
        this.workingdir = workingdir

        vertx = Vertx.vertx()
        AnsiConsole.systemInstall()
    }

    /**
     * add a node and call setConfig of the node
     */
    fun addNode(node:BaseNode) {
        map.put(node.nodename, node)
        node.setConfig()
    }

    /**
     * get node
     */
    fun getNode(nodename : String) : BaseNode? = map[nodename]


    /**
     * return node structure
     */
    fun getNodeStructureAsJSON() : String {
        val nodemap = HashMap<String,Any>()

        for (node in map) {
            val publishtolist = ArrayList<String>()

            val n = node.value
            if (n is InNode)
                for (key in n.subscribers)
                    publishtolist.add(key.key)

            if (n is TransformNode)
                for (key in n.subscribers)
                    publishtolist.add(key.key)

            val nodedata = mapOf("basetype" to node.value.nodetype.name, "classname" to node.value.javaClass.name, "publishto" to publishtolist)
            nodemap.put(node.value.nodename, nodedata)
        }

        val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

        val issueAdapter = moshi.adapter(Map::class.java)
        return issueAdapter.toJson(nodemap)
    }


    /**
     * stop, reload config and start node
     */
    fun restartNode(nodename : String) {
        val node = getNode(nodename)
        if (node != null) {
            println("Restarting node: " + node.nodename + "; type: " + node.nodetype)

            node.stop()
            node.stopThread()
            Thread.sleep(100)
            node.kill()
            node.killThread()

            node.setConfig()
            node.start()
            node.startThread()
        }
    }

    /**
     * call start function of all nodes
     * at first all output nodes are started, then transformers and finally inputs follow
     */
    fun start() {
        //start all outputs
        for ((key, value) in map) {
            if (value.nodetype == NodeType.Output) {
                println("Starting node: " + key + "; type: " + value.nodetype)
                value.start()
                value.startThread()
            }
        }

        //start all filters, transformers
        for ((key, value) in map) {
            if (value.nodetype == NodeType.Transformer) {
                println("Starting node: " + key + "; type: " + value.nodetype)
                value.start()
                value.startThread()
            }
        }

        //start all inputs
        for ((key, value) in map) {
            if (value.nodetype == NodeType.Input) {
                println("Starting node: " + key + "; type: " + value.nodetype)
                value.start()
                value.startThread()
            }
        }
    }

    /**
     * call stop function of all nodes
     * at first all input nodes are stopped, then transformers and finally outputs follow
     */
    fun stop() {
        //stop all inputs
        for ((key, value) in map) {
            if (value.nodetype == NodeType.Input) {
                println("Stopping node: " + key + "; type: " + value.nodetype)
                value.stop()
                value.stopThread()
            }
        }
        //stop all filters, transformers
        for ((key, value) in map) {
            if (value.nodetype == NodeType.Transformer) {
                println("Stopping node: " + key + "; type: " + value.nodetype)
                value.stop()
                value.stopThread()
            }
        }

        //stop all outputs
        for ((key, value) in map) {
            if (value.nodetype == NodeType.Output) {
                println("Stopping node: " + key + "; type: " + value.nodetype)
                value.stop()
                value.stopThread()
            }
        }
    }

    /**
     * call kill function of all nodes
     */
    fun kill() {
        for ((key, value) in map) {
            println("Killing node: " + key + "; type: " + value.nodetype)
            value.kill()
            value.killThread()
        }

        AnsiConsole.systemUninstall()
    }

    /**
     * use thread to asynchronously restart node (e.g. used by webui)
     */
    fun asyncrestartnode(nodename: String) {
        if (!isShuttingDown) {
            val callable = AsyncEngineNodeRestartTask(this, nodename)
            val futureTask = FutureTask(callable)

            executor.execute(futureTask)
        }
    }

    /**
     * use thread to asynchronously shutdown (e.g. used by webui)
     */
    fun asyncshutdown() {
        if (!isShuttingDown) {
            val callable = AsyncEngineShutdownTask(this)
            val futureTask = FutureTask(callable)

            executor.execute(futureTask)
        }
    }

    /**
     * call stop function and then kill function of all nodes and shutdown web UI
     */
    fun shutdown() {
        if (!isShuttingDown) {
            isShuttingDown = true

            stop()
            Thread.sleep(100)
            kill()

            // stop all vertx web interfaces
            vertx.close({ result ->
                if (result.succeeded()) {
                    println("Vertx stopped")
                }
            })

            executor.shutdown()
            isShuttingDown = false
        }
    }


    /**
     * get current engine date -> useful for plugin tests
     */
    fun getCurrentDate() = Date()

    fun warning(msg: String, name: String) {
        println(ansi().fg(Color.YELLOW).a("WARNING: " + name + ": " + msg).reset())
    }
}


class AsyncEngineShutdownTask(engine: Engine) : Callable<String> {
    var engine: Engine by Delegates.notNull()

    init {
        this.engine = engine
    }

    @Throws(Exception::class)
    override fun call(): String {
        engine.shutdown()
        return Thread.currentThread().name
    }

}

class AsyncEngineNodeRestartTask(engine: Engine, nodename: String) : Callable<String> {
    var engine: Engine by Delegates.notNull()
    var nodename: String by Delegates.notNull()

    init {
        this.engine = engine
        this.nodename = nodename
    }

    @Throws(Exception::class)
    override fun call(): String {
        engine.restartNode(nodename)
        return Thread.currentThread().name
    }

}