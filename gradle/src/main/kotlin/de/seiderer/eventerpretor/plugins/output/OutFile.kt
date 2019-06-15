package de.seiderer.eventerpretor.plugins.output

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import de.seiderer.eventerpretor.core.Engine
import de.seiderer.eventerpretor.plugins.base.OutNode
import java.io.BufferedWriter
import java.io.File
import java.util.HashMap
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

/**
 * @author Andreas Seiderer
 *
 * writes values as strings; hashmaps as JSON strings
 */
class OutFile(engine: Engine, name:String) : OutNode(engine,name,0) {

    private var f : File by Delegates.notNull()
    private var w : BufferedWriter by Delegates.notNull()

    private var issueAdapter : JsonAdapter<Map<*, *>> by Delegates.notNull()

    override fun setConfig() {
        val path : String = engine.workingdir + "/options_" + this.nodename + ".json"

        //read options from file if it exists; if not create new with default values
        if (File(path).exists())
            opts.fromJsonFile(path)
        else {
            opts.add("filename", this.nodename+".data", "file name of output file")
            opts.add("buffersize", 8*1024, "size of the buffer")
            opts.add("flushEveryVal", false, "flush after every new value")
            opts.toJsonFile(path)
        }
    }

    override fun start() {
        f = File(opts.getStringVal("filename"))
        w = f.bufferedWriter(Charsets.UTF_8, opts.getIntVal("buffersize"))

        val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

        issueAdapter = moshi.adapter(Map::class.java)
    }

    override fun stop() {
        w.close()
    }

    override fun kill() {
    }

    override fun threadedTask() {
        val value = indata.poll(1000, TimeUnit.MILLISECONDS)
        if (value != null) {
            if (value.value is String)
                w.write(value.timestamp.time.toString() + "\t" + value.value)
            else if (value.value is HashMap<*, *>) {
                //convert hashmaps to JSON string
                w.write(value.timestamp.time.toString() + "\t" + issueAdapter.toJson(value.value))
            }

            w.newLine()
            if (opts.getBoolVal("flushEveryVal"))
                w.flush()
        }
    }

}