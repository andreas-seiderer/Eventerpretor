package de.seiderer.eventerpretor.plugins.transform

import de.seiderer.eventerpretor.core.Engine
import de.seiderer.eventerpretor.plugins.base.TransformNode
import java.io.File
import java.util.HashMap
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

/**
 *  @author Andreas Seiderer
 *  Converts FHEM event strings in form of <DEVICENAME> <EVENTSTRING> ("$NAME $EVENT") to be usable for TransformSensorModel
 *
    my_Utils:  use Net::MQTT::Simple "localhost";

    .* {
    publish "fhem" => "$TYPE;$NAME;$EVENT";
    return;
    }
 */
class TransformFHEMstring(engine: Engine, name:String) : TransformNode(engine,name,0) {
    override fun threadedTask() {
        val value = indata.poll(1000, TimeUnit.MILLISECONDS)
        if (value != null) {

            //is from mqtt; && value.value["topic"] is String
            if (value.value is HashMap<*,*>) {

                val valc = value.value as HashMap<String,Any?>

                if(valc["message"] is String) {
                    var msg = valc["message"]

                    if (msg is String) {
                        //remove double space
                        msg = msg.replace("  ", " ")

                        var stringEles: List<String>

                        if (opts.getBoolVal("telnetmode")) {
                            //example : 2019-01-15 23:02:07 MQTT mqttClient connection: active
                            stringEles = msg.split(" ")
                            stringEles = listOf(
                                stringEles[2],
                                stringEles[3],
                                stringEles.subList(4, stringEles.size).joinToString(" ")
                            )
                        } else {
                            //example from MQTT: CUL_HM;PlugM03_Pwr;power: 16.75
                            stringEles = msg.split(";")
                        }

                        if (stringEles.size < 3) {
                            engine.warning("Skipped msg: " + msg, this.nodename)
                            return
                        }

                        //val sensordevice = stringEles[0]
                        val sensorname = stringEles[1]

                        val sensorreadings = stringEles[2].split(" ")

                        var readingname: String
                        var reading: String
                        var readingUnit = ""

                        var i = 0


                        if (sensorreadings.size == 1)
                            dataOut(
                                hashMapOf(
                                    "provider" to "fhem",
                                    "sensorname" to sensorname,
                                    "readingname" to "state",
                                    "reading" to sensorreadings[0],
                                    "readingunit" to ""
                                )
                            )
                        else {

                            while (i < sensorreadings.size) {
                                if (sensorreadings[i].last() == ':')
                                    readingname = sensorreadings[i].dropLast(1)
                                else {
                                    engine.warning("Skipped msg: " + msg, this.nodename)
                                    break
                                }

                                i++
                                if (i < sensorreadings.size)
                                    reading = sensorreadings[i]
                                else {
                                    engine.warning("Skipped msg: " + msg, this.nodename)
                                    break
                                }
                                i++

                                if (i < sensorreadings.size && sensorreadings[i].last() != ':') {
                                    readingUnit = sensorreadings[i]
                                    i++
                                }

                                dataOut(
                                    hashMapOf(
                                        "provider" to "fhem",
                                        "sensorname" to sensorname,
                                        "readingname" to readingname,
                                        "reading" to reading,
                                        "readingunit" to readingUnit
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
        val path : String = engine.workingdir + "/options_" + this.nodename + ".json"

        //read options from file if it exists; if not create new with default values
        if (File(path).exists())
            opts.fromJsonFile(path)
        else {
            opts.add("telnetmode", false, "switch on if using the InTCP node for telnet connection to FHEM")
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