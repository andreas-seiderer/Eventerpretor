package de.seiderer.eventerpretor.plugins.transform

import de.seiderer.eventerpretor.core.Engine
import de.seiderer.eventerpretor.plugins.base.DataValue
import de.seiderer.eventerpretor.plugins.base.TransformNode
import groovy.lang.MetaClassImpl
import groovy.util.GroovyScriptEngine
import java.io.File
import java.lang.reflect.Method
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType

/**
 * @author Andreas Seiderer
 * Call Groovy script with input data and publish output
 */
class TransformGroovy(engine: Engine, name:String) : TransformNode(engine,name,0) {

    var groovyEngine : GroovyScriptEngine = GroovyScriptEngine( engine.workingdir )
    var scriptObject : Any = Delegates.notNull<Any>()
    var scriptMethodTransform : Method? = null


    override fun setConfig() {
        val path : String = engine.workingdir + "/options_" + this.nodename + ".json"

        //read options from file if it exists; if not create new with default values
        if (File(path).exists())
            opts.fromJsonFile(path)
        else {
            opts.add("filename", this.nodename + ".groovy", "file name of groovy script")
            opts.add("path", "groovy_scripts/", "path to groovy script")

            opts.toJsonFile(path)
        }

        val script : Class<Any> = groovyEngine.loadScriptByName(opts.getStringVal("path") + opts.getStringVal("filename"))
        scriptObject = script.newInstance()
        scriptMethodTransform = scriptObject.javaClass.getDeclaredMethod("transform", TransformGroovy::class.java, DataValue::class.java)

    }

    override fun start() {

    }

    override fun stop() {
    }

    override fun kill() {
    }

    override fun threadedTask() {
        val value = indata.poll(1000, TimeUnit.MILLISECONDS)
        if (value != null) {
            val result : Any? = scriptMethodTransform?.invoke(scriptObject, this, value)

            if (result != null)
                dataOut(result)
        }
    }

}