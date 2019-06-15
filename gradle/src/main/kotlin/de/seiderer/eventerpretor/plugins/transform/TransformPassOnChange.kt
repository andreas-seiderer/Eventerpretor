package de.seiderer.eventerpretor.plugins.transform

import de.seiderer.eventerpretor.core.Engine
import de.seiderer.eventerpretor.plugins.base.TransformNode
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

/**
 *  @author Andreas Seiderer
 *  Just allow new values to pass
 */
class TransformPassOnChange(engine: Engine, name:String) : TransformNode(engine,name,0) {

    private var oldval: Any by Delegates.notNull()

    override fun setConfig() {
    }

    override fun start() {
        oldval = 0
    }

    override fun stop() {

    }

    override fun kill() {

    }

    override fun threadedTask() {
        val value = indata.poll(1000, TimeUnit.MILLISECONDS)
        if (value != null) {
            if (value.value != oldval) {
                dataOut(value.value)
                oldval = value.value
            }
        }
    }
}