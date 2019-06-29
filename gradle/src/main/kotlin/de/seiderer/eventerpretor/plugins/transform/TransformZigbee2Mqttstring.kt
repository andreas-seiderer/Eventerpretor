package de.seiderer.eventerpretor.plugins.transform

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import de.seiderer.eventerpretor.core.Engine
import de.seiderer.eventerpretor.plugins.base.TransformNode
import java.io.File
import java.util.HashMap
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

/**
 *  @author Andreas Seiderer
 *  Converts Zigee2Mqtt event strings to be usable for TransformSensorModel
 *
 */
class TransformZigbee2Mqttstring(engine: Engine, name:String) : TransformNode(engine,name,0) {
    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val issueAdapter = moshi.adapter(Map::class.java)


    override fun threadedTask() {
        val value = indata.poll(1000, TimeUnit.MILLISECONDS)
        if (value != null) {

            //is from mqtt; && value.value["topic"] is String
            if (value.value is HashMap<*,*>) {
                val valc = value.value as HashMap<String,Any?>

                if (valc["message"] is String && valc["topic"] is String) {
                    var msg = value.value["message"]
                    var topic = value.value["topic"]

                    if (topic is String)
                        topic = topic.split("/").last()

                    if (msg is String) {
                        val json = issueAdapter.fromJson(msg)
                        val jsonhm = HashMap<Any, Any>(json)
                        if (json != null) {

                            for ((key, v) in jsonhm) {
                                dataOut(
                                    hashMapOf(
                                        "provider" to "zigbee2mqtt",
                                        "sensorname" to topic,
                                        "readingname" to key,
                                        "reading" to v,
                                        "readingunit" to ""
                                    )
                                )
                            }
                        }

                    }
                }
            }
        }
    }

    override fun setConfig() {
        /*val path : String = engine.workingdir + "/options_" + this.nodename + ".json"

        //read options from file if it exists; if not create new with default values
        if (File(path).exists())
            opts.fromJsonFile(path)
        else {
            opts.add("telnetmode", false, "switch on if using the InTCP node for telnet connection to FHEM")
            opts.toJsonFile(path)
        }*/
    }

    override fun start() {
    }

    override fun stop() {
    }

    override fun kill() {
    }

}