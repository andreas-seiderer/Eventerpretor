package de.seiderer.eventerpretor.plugins.input

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import de.seiderer.eventerpretor.core.Engine
import de.seiderer.eventerpretor.plugins.base.DataValue
import de.seiderer.eventerpretor.plugins.base.InNode
import java.io.BufferedReader
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

/**
 * @author Andreas Seiderer
 */
class InFile(engine: Engine, name:String) : InNode(engine,name,0) {

    private var f : File by Delegates.notNull()
    private var r : BufferedReader by Delegates.notNull()

    private var line : String? = null
    private var lineahead : String? = null

    private var issueAdapter : JsonAdapter<Map<*,*>> by Delegates.notNull()

    private var timelefttosleep : Long = 0

    private val maxsleep : Long = 1000 //ms

    override fun setConfig() {
        val path : String = engine.workingdir + "/options_" + this.nodename + ".json"

        //read options from file if it exists; if not create new with default values
        if (File(path).exists())
            opts.fromJsonFile(path)
        else {
            opts.add("filename", this.nodename+".data", "file name of input file")
            opts.add("buffersize", 8*1024, "size of the buffer")
            opts.add("delayfromfile", true, "use relative delay from timestamps in file")
            opts.add("fixeddelay", 1000, "use this as delay in ms; requires delayfromfile to be false")
            opts.toJsonFile(path)
        }
    }

    override fun start() {
        f = File(opts.getStringVal("filename"))
        r = f.bufferedReader(Charsets.UTF_8, opts.getIntVal("buffersize"))

        val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

        issueAdapter = moshi.adapter(Map::class.java)
    }

    override fun stop() {
        r.close()
    }

    override fun kill() {
    }

    override fun threadedTask() {
        //prevent thread hanging for long time
        while (timelefttosleep > 0 || this.getStopFlag()) {

            if (timelefttosleep > maxsleep) {
                Thread.sleep(maxsleep)
                timelefttosleep-=maxsleep

            } else {
                Thread.sleep(timelefttosleep)
                timelefttosleep = 0
            }
        }

        if (!this.getStopFlag()) {

            line = lineahead
            lineahead = r.readLine()

            if (lineahead == null) {
                //restart from beginning
                r = f.bufferedReader(Charsets.UTF_8, opts.getIntVal("buffersize"))
                lineahead = r.readLine()
            }

            if (line != null) {

                val vals = line.toString().split("\t")
                val curtimestamp = vals[0].toLong()

                //try to convert JSON string to Map
                val message = issueAdapter.fromJson(vals[1])?.toMap()

                if (message != null)
                    dataOut(DataValue(engine.getCurrentDate(), message, this.nodename))
                else
                //this was not a JSON string; just publish the string it
                    dataOut(DataValue(engine.getCurrentDate(), vals[1], this.nodename))


                val valsahead = lineahead.toString().split("\t")
                val nextimestamp = valsahead[0].toLong()

                val deltaTime = nextimestamp - curtimestamp

                if (opts.getBoolVal("delayfromfile") && deltaTime > 0)
                    timelefttosleep = deltaTime
                else
                    timelefttosleep = opts.getIntVal("fixeddelay").toLong()

            }
        }
    }

}