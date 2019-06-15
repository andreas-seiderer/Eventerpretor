package de.seiderer.eventerpretor.plugins.transform

import de.seiderer.eventerpretor.core.Engine
import de.seiderer.eventerpretor.plugins.base.TransformNode
import java.io.File
import java.util.concurrent.TimeUnit

/**
 *  @author Andreas Seiderer
 *  Select index from Array
 */
class TransformArraySelect(engine: Engine, name:String) : TransformNode(engine,name,0) {

    override fun setConfig() {
        val path : String = engine.workingdir + "/options_" + this.nodename + ".json"

        //read options from file if it exists; if not create new with default values
        if (File(path).exists())
            opts.fromJsonFile(path)
        else {
            opts.add("index", 0, "index of value of array to pass")

            opts.toJsonFile(path)
        }
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
            if (value.value is DoubleArray)
                dataOut(value.value[opts.getIntVal("index")])

            else if (value.value is Array<*>) {
                val arrval = value.value[opts.getIntVal("index")]
                if (arrval != null)
                    dataOut(arrval)
            }
            else {
                engine.warning("Received not an Array / DoubleArray as input!", this.nodename)
            }
        }
    }

}