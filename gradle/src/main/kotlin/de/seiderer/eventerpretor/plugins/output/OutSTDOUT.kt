package de.seiderer.eventerpretor.plugins.output

import de.seiderer.eventerpretor.core.Engine
import de.seiderer.eventerpretor.drools.Command
import de.seiderer.eventerpretor.plugins.base.OutNode
import java.util.concurrent.TimeUnit

/**
 * @author Andreas Seiderer
 */
class OutSTDOUT(engine: Engine, name:String) : OutNode(engine,name,0) {
    override fun setConfig() {
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
                println("timestamp: ${value.timestamp}; sourcename: ${value.sourcename}; value: ${value.value.joinToString(";")}")
            else if (value.value is Command)
                println("timestamp: ${value.timestamp}; sourcename: ${value.sourcename}; value: ${value.value.value}")
            else
                println("timestamp: ${value.timestamp}; sourcename: ${value.sourcename}; value: ${value.value}")
        }
    }

}
