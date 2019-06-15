package de.seiderer.eventerpretor.plugins.transform

import de.seiderer.eventerpretor.core.Engine
import de.seiderer.eventerpretor.plugins.base.TransformNode
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates
import kotlin.math.*


/**
 * @author Andreas Seiderer
 * Publish result of function applied to moving window of specific sample count
 */
class TransformMvgFunc(engine: Engine, name:String) : TransformNode(engine,name,0) {
    private var buffer: DoubleArray by Delegates.notNull()
    private var pos = 0

    private fun median(l: DoubleArray) = l.sorted().let { (it[it.size / 2] + it[(it.size - 1) / 2]) / 2 }

    private fun std(numArray: DoubleArray): Double {
        val mean = numArray.average()
        var std = 0.0

        for (num in numArray)
            std += Math.pow(num - mean, 2.0)

        return Math.sqrt(std / numArray.size)
    }

    override fun setConfig() {
        val path : String = engine.workingdir + "/options_" + this.nodename + ".json"

        //read options from file if it exists; if not create new with default values
        if (File(path).exists())
            opts.fromJsonFile(path)
        else {
            opts.add("buffersize", 5, "circular buffer size over which function is applied")
            opts.add("function", "avg", "function applied on buffer: avg (average), sum, max, min, med (median), std (standard deviation)")
            opts.add("round", true, "round result")
            opts.toJsonFile(path)
        }
    }

    override fun start() {
        buffer = DoubleArray(opts.getIntVal("buffersize"))

    }


    override fun stop() {
    }

    override fun kill() {
    }

    override fun threadedTask() {
        val value = indata.poll(1000, TimeUnit.MILLISECONDS)
        if (value != null) {
            if (value.value is Double) {
                buffer[pos] = value.value
                pos++

                var calc : Double = value.value

                when (opts.getStringVal("function")) {
                    "avg" -> calc = buffer.average()

                    "sum" -> calc = buffer.sum()

                    "max" -> {
                        val maxval = buffer.max()
                        if (maxval != null)
                            calc = maxval
                    }

                    "min" -> {
                        val minval = buffer.min()
                        if (minval != null)
                            calc = minval
                    }

                    "med" -> calc = median(buffer)

                    "std" -> calc = std(buffer)
                }

                if (opts.getBoolVal("round"))
                    calc = Math.round(calc).toDouble()

                dataOut(calc)

                pos %= opts.getIntVal("buffersize")
            }
        }
    }

}