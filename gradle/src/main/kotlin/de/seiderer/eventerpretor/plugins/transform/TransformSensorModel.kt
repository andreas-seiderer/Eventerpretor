package de.seiderer.eventerpretor.plugins.transform

import de.seiderer.eventerpretor.core.Engine
import de.seiderer.eventerpretor.plugins.base.TransformNode
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

/**
 * @author Andreas Seiderer
 * complement sensor data with sensor information
 */
class TransformSensorModel(engine: Engine, name:String) : TransformNode(engine,name,0) {

    /**
     * readingreplace:
     *  - <original>: use original readingname
     *
     */
    data class SensorEntry(val readingname:List<String>, val readingreplace:List<String>, val sensorname:String, val sensortype:String, val positionname:String, val positiontype:String, val provider:String)

    var sensorHashmap : HashMap<String,SensorEntry> = HashMap()


    fun isNumeric(input: String): Boolean =
            try {
                input.toDouble()
                true
            } catch(e: NumberFormatException) {
                false
            }


    override fun threadedTask() {
        val value = indata.poll(1000, TimeUnit.MILLISECONDS)
        if (value != null) {

            if (value.value is java.util.HashMap<*, *>) {

                var sensorname = ""
                var reading : Any = ""

                var v = value.value["sensorname"]

                if (v is String)
                    sensorname = v


                v = value.value["reading"]
                if (v is String)
                    if (isNumeric(v))
                        reading = v.toDouble()
                    else
                        reading = v

                val entry = sensorHashmap[sensorname]

                if (entry != null) {

                    var pos = 0

                    //search for readingname in entry list
                    while(entry.readingname[pos] != value.value["readingname"] && pos < entry.readingname.size-1) {
                        pos++
                    }

                    //if readingname not found skip
                    if (entry.readingname[pos] != value.value["readingname"])
                        return

                    var readingname = entry.readingreplace[pos]

                    //if readingname is left blank then use original readingname
                    if (entry.readingreplace[pos] == "<original>") {
                        val valreadingname = value.value["readingname"]
                        if (valreadingname is String)
                            readingname = entry.readingname[pos]
                    }

                    val outputmap = hashMapOf("readingname" to readingname,
                            "sensorname" to entry.sensorname,
                            "sensortype" to entry.sensortype,
                            "positionname" to entry.positionname,
                            "positiontype" to entry.positiontype,
                            "provider" to entry.provider,
                            "reading" to reading)

                    dataOut(outputmap)
                }
                else {
                    engine.warning("Unknown sensorname: " + sensorname, this.nodename)
                }

            } else {
                engine.warning("Unknown input data! ", this.nodename)
            }
        }
    }

    override fun setConfig() {
        val path : String = engine.workingdir + "/options_" + this.nodename + ".json"

        //read options from file if it exists; if not create new with default values
        if (File(path).exists()) {
            opts.fromJsonFile(path)

            val optsensormap = opts.getVal("sensors")
            if (optsensormap is Map<*,*>) {
                for ((key, value) in optsensormap) {
                    if (key is String && value is Map<*,*>) {
                        var readingname = listOf("")
                        var readingreplace = listOf("")
                        var sensorname = ""
                        var sensortype = ""
                        var positionname = ""
                        var positiontype = ""
                        var provider = ""

                        var v = value["readingname"]
                        if (v is ArrayList<*>)
                            readingname = v.filterIsInstance<String>()

                        v = value["readingreplace"]
                        if (v is ArrayList<*>)
                            readingreplace = v.filterIsInstance<String>()

                        v = value["sensorname"]
                        if (v is String)
                            sensorname = v

                        v = value["sensortype"]
                        if (v is String)
                            sensortype = v

                        v = value["positionname"]
                        if (v is String)
                            positionname = v

                        v = value["positiontype"]
                        if (v is String)
                            positiontype = v

                        v = value["provider"]
                        if (v is String)
                            provider = v

                        sensorHashmap.put(key, SensorEntry(readingname, readingreplace, sensorname, sensortype, positionname, positiontype, provider ))
                    }
                }
            }
        }
        else {
            sensorHashmap.put("mqtt_co2", SensorEntry(listOf("CO2"), listOf("replace"), "AirCO2ntrol","gas sensor", "living room", "room", "fhem"))

            opts.add("sensors", sensorHashmap, "hash map of sensor names as key that should be adapted with data given in value")

            opts.toJsonFile(path)
        }
    }

    override fun start() {

    }

    override fun stop() {

    }

    override fun kill() {

    }

}