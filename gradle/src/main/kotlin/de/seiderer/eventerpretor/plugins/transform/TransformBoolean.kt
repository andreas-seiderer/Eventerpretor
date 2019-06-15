package de.seiderer.eventerpretor.plugins.transform

import de.seiderer.eventerpretor.core.Engine
import de.seiderer.eventerpretor.plugins.base.TransformNode
import java.io.File
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

/**
 *  @author Andreas Seiderer
 *  Just allow values to pass if boolean function is true
 */
class TransformBoolean(engine: Engine, name:String) : TransformNode(engine,name,0) {

    private var lastCall: Instant? = null
    private var laststate = false
    private var lastvalue: Double by Delegates.notNull()
    private var lastoutstate = false

    override fun setConfig() {
        val path : String = engine.workingdir + "/options_" + this.nodename + ".json"

        //read options from file if it exists; if not create new with default values
        if (File(path).exists())
            opts.fromJsonFile(path)
        else {
            opts.add("operator",                "",     "<;>;<=;>=;==;!=")
            opts.add("value",                   0.0,    "value to compare to message value with operator: <invalue> OPERATOR <value>")

            opts.add("delayedStateSwitch",      false,  "enable or disable delayed state switch")
            opts.add("switchToTrueAfterMS" ,    1000,   "switch to True if the state was True for this time period")
            opts.add("switchToFalseAfterMS" ,   1000,   "switch to False if the state was False for this time period")
            opts.add("outputValue",             false,  "output the value instead of boolean")
            opts.toJsonFile(path)
        }
    }

    override fun start() {
        lastCall = Instant.now()

    }

    override fun stop() {

    }

    override fun kill() {

    }

    override fun threadedTask() {
        val value = indata.poll(1, TimeUnit.MILLISECONDS)   // tick with one millisecond

        if (value != null) {
            if (value.value is Double) {

                lastvalue = value.value

                var result = false

                when (opts.getStringVal("operator")) {
                    "<" ->  result = value.value < opts.getDoubleVal("value")
                    ">" ->  result = value.value > opts.getDoubleVal("value")
                    "<=" -> result = value.value <= opts.getDoubleVal("value")
                    ">=" -> result = value.value >= opts.getDoubleVal("value")
                    "==" -> result = value.value == opts.getDoubleVal("value")
                    "!=" -> result = value.value != opts.getDoubleVal("value")
                }

                if (!opts.getBoolVal("delayedStateSwitch")) {
                    if (opts.getBoolVal("outputValue"))
                        dataOut(value.value)
                    else
                        dataOut(result)
                }

                if (result != laststate)
                    lastCall = Instant.now()

                laststate = result
            }
        }

        if (opts.getBoolVal("delayedStateSwitch") ) {
            if (laststate && Duration.between(lastCall, Instant.now()).toMillis() >= opts.getIntVal("switchToTrueAfterMS")) {
                if (!lastoutstate) {
                    if (opts.getBoolVal("outputValue"))
                        dataOut(lastvalue)
                    else
                        dataOut(true)
                }
                lastoutstate = true
            }
            else if (!laststate && Duration.between(lastCall, Instant.now()).toMillis() >= opts.getIntVal("switchToFalseAfterMS")) {
                if (lastoutstate)
                    if (opts.getBoolVal("outputValue"))
                        dataOut(lastvalue)
                    else
                        dataOut(false)
                lastoutstate = false
            }
        }
    }
}