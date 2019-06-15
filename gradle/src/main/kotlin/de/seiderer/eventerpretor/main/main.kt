package de.seiderer.eventerpretor.main

import de.seiderer.eventerpretor.core.Engine
import de.seiderer.eventerpretor.core.WebUI
import groovy.util.GroovyScriptEngine
import java.io.File
import java.lang.reflect.Method

/**
 * @author Andreas Seiderer
 *
 * command line arguments:
 * arg 1: node structure groovy file (default: "main.groovy")
 * arg 2: working directory (default: ".")
 * arg 3: 0: normal startup; 1: just init plugins to create all missing option files
 */
fun main(args: Array<String>) {
    println("Eventerpretor Alpha 0.1")
    println("(c)2017-2019 Andreas Seiderer")
    println("========================================================================")

    var initfile = "main.groovy"
    var workingdir = "."
    var createconfigonly = false

    if (args.isNotEmpty())
        initfile = args[0]

    if (args.size > 1)
        workingdir = args[1]

    if (args.size > 2)
        createconfigonly = args[2] == "1"

    if (createconfigonly)
        println("Creating only config files ...")
    else
        println("Starting up ...")


    val engine = Engine(workingdir)
    val f = File(workingdir+ "/" +initfile)

    if (!f.isFile) {
        engine.warning("File describing pipeline \"" + workingdir+ "/" + initfile + "\" doesn't exist!","MAIN")
        return
    }

    val groovyEngine = GroovyScriptEngine( workingdir )
    val scriptObject : Any
    val scriptMethodTransform : Method?

    val script : Class<Any> = groovyEngine.loadScriptByName(initfile)
    scriptObject = script.newInstance()
    scriptMethodTransform = scriptObject.javaClass.getDeclaredMethod("init", Engine::class.java, Array<String>::class.java)

    val result : Any? = scriptMethodTransform?.invoke(scriptObject, engine, args)

    if (result is Boolean && result && !createconfigonly) {
        engine.start()

        engine.vertx.deployVerticle(WebUI(engine)) { ar ->
            if (ar.succeeded()) {
                println("Web UI started: http://localhost:8080")
                println("========================================================================")
            } else {
                println("Could not start Web UI")
                ar.cause().printStackTrace()
                println("========================================================================")
            }
        }


        //Thread.sleep(36000000)
        //engine.stop()
        //Thread.sleep(100)
        //engine.kill()
    }
}
