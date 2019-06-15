package de.seiderer.eventerpretor.plugins.transform

import de.seiderer.eventerpretor.core.Engine
import de.seiderer.eventerpretor.plugins.base.TransformNode
import java.io.File
import java.util.concurrent.TimeUnit

/**
 *  @author Andreas Seiderer
 *  Split input string to Array
 */
class TransformSplitToArray(engine: Engine, name:String) : TransformNode(engine,name,0) {

    override fun setConfig() {
        val path : String = engine.workingdir + "/options_" + this.nodename + ".json"

        //read options from file if it exists; if not create new with default values
        if (File(path).exists())
            opts.fromJsonFile(path)
        else {
            opts.add("delimiter", ";", "split input string with this delimiter")

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
            if (value.value is String) {
                val stringvalues = value.value.split(opts.getStringVal("delimiter"))

                if (stringvalues.isNotEmpty()) {
                    val isDouble = stringvalues[0].toDoubleOrNull() != null

                    if (isDouble) {
                        val doublevalues = DoubleArray(stringvalues.size)
                        for (i in 0 until stringvalues.size)
                            doublevalues[i] = stringvalues[i].toDouble()

                        dataOut(doublevalues)
                    } else {
                        dataOut(stringvalues)
                    }
                }
            } else {
                engine.warning("Received not a String as input!", this.nodename)
            }
        }
    }

}