package de.seiderer.eventerpretor.plugins.transform

import de.seiderer.eventerpretor.core.Engine
import de.seiderer.eventerpretor.plugins.base.TransformNode
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

/**
 *  @author Andreas Seiderer
 *  Just allow specific datatypes as values to pass
 */
class TransformAllowDatatype(engine: Engine, name:String) : TransformNode(engine,name,0) {

    private var allowedclassnames : List<String> by Delegates.notNull()

    override fun setConfig() {
        val path : String = engine.workingdir + "/options_" + this.nodename + ".json"

        //read options from file if it exists; if not create new with default values
        if (File(path).exists())
            opts.fromJsonFile(path)
        else {
            opts.add("classname", "kotlin.DoubleArray", "just output values of this datatype(s); separate with ;")
            opts.add("printDropped", true, "print dropped values to console as warning")

            opts.toJsonFile(path)
        }
    }

    override fun start() {
        allowedclassnames = opts.getStringVal("classname").split(";")

    }

    override fun stop() {

    }

    override fun kill() {

    }

    override fun threadedTask() {
        val value = indata.poll(1000, TimeUnit.MILLISECONDS)
        if (value != null) {
            if (allowedclassnames.contains(value.value.javaClass.kotlin.qualifiedName)) {
                dataOut(value.value)
            } else if (opts.getBoolVal("printDropped"))
                engine.warning("Dropped following value: " + value.value, this.nodename )
        }
    }

}