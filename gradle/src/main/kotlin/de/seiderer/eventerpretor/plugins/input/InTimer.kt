package de.seiderer.eventerpretor.plugins.input

import de.seiderer.eventerpretor.core.Engine
import de.seiderer.eventerpretor.plugins.base.InNode
import java.io.File

/**
 * @author Andreas Seiderer
 */
class InTimer(engine: Engine, name:String) : InNode(engine,name,100) {

    override fun setConfig() {
        val path : String = engine.workingdir + "/options_" + this.nodename + ".json"

        //read options from file if it exists; if not create new with default values
        if (File(path).exists())
            opts.fromJsonFile(path)
        else {
            opts.add("interval", 100, "interval in milliseconds")
            opts.toJsonFile(path)
        }
    }

    override fun start() {
        resetTimer(this.opts.getIntVal("interval").toLong())
    }

    override fun stop() {
    }

    override fun kill() {
    }

    var counter : Int = 0

    override fun threadedTask() {
        counter++
        dataOut(counter)
    }
}