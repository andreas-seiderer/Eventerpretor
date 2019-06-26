package de.seiderer.eventerpretor.plugins.transform

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import de.seiderer.eventerpretor.core.Engine
import de.seiderer.eventerpretor.drools.Command
import de.seiderer.eventerpretor.plugins.base.TransformNode
import java.io.File
import java.util.HashMap
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

/**
 *  @author Andreas Seiderer
 *  Convert String in JSON format to hashmap
 */
class TransformJSONtoMAP(engine: Engine, name:String) : TransformNode(engine,name,0) {
    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val issueAdapter = moshi.adapter(Map::class.java)

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
            var message = ""
            if (value.value is String) {
                message = value.value
            } else if (value.value is Command) {
                message = value.value.value
            }
            val json = issueAdapter.fromJson(message)
            val jsonhm = HashMap<Any, Any>(json)

            dataOut(jsonhm)
        }
    }
}