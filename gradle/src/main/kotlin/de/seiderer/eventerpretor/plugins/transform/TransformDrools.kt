package de.seiderer.eventerpretor.plugins.transform

import de.seiderer.eventerpretor.core.Engine
import de.seiderer.eventerpretor.drools.Command
import de.seiderer.eventerpretor.drools.ContextElement
import de.seiderer.eventerpretor.drools.Sensor
import de.seiderer.eventerpretor.plugins.base.TransformNode
import org.kie.api.KieServices
import org.kie.api.runtime.KieSession
import org.kie.api.runtime.rule.FactHandle
import org.kie.internal.conf.MultithreadEvaluationOption
import org.kie.internal.io.ResourceFactory
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates
import org.apache.commons.lang3.event.EventUtils.addEventListener
import org.drools.core.event.DebugAgendaEventListener
import org.drools.core.event.DebugRuleRuntimeEventListener


/**
 * @author Andreas Seiderer
 *
 * use Drools rule engine for context elements
 */
class TransformDrools(engine: Engine, name:String) : TransformNode(engine,name,0) {

    data class SensorEntry(val readingname:String, val sensorname:String, val sensortype:String, val positionname:String, val positiontype:String, val provider:String, val isNumeric:Boolean)
    data class SensorFactEntry(val sensor:Sensor, val fact:FactHandle)

    var sensorHashmap : HashMap<String,SensorEntry> = HashMap()
    var sensorFactHashmap : HashMap<String,SensorFactEntry> = HashMap()

    private var ksession : KieSession by Delegates.notNull()

    //var s_temp: Sensor by Delegates.notNull()
    //var fh: FactHandle by Delegates.notNull()


    override fun setConfig() {
        val path : String = engine.workingdir + "/options_" + this.nodename + ".json"

        //read options from file if it exists; if not create new with default values
        if (File(path).exists()) {
            opts.fromJsonFile(path)

            val optsensormap = opts.getVal("sensors")
            if (optsensormap is Map<*,*>) {
                for ((key, value) in optsensormap) {
                    if (key is String && value is Map<*,*>) {
                        var readingname = ""
                        var sensorname = ""
                        var sensortype = ""
                        var positionname = ""
                        var positiontype = ""
                        var provider = ""
                        var isNumeric = true

                        var v = value["readingname"]
                        if (v is String)
                            readingname = v

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

                        v = value["isNumeric"]
                        if (v is Boolean)
                            isNumeric = v

                        sensorHashmap.put(key, SensorEntry(readingname, sensorname, sensortype, positionname, positiontype, provider, isNumeric ))
                    }
                }
            }
        }
        else {
            sensorHashmap.put("co2_avg", SensorEntry("CO2", "AirCO2ntrol","gas sensor", "living room", "room", "fhem", true))

            opts.add("sensors", sensorHashmap, "hash map of sensor names as key that should be adapted with data given in value")

            opts.add("filenamerules", "rules.drl", "file name of drools rule file relative to working directory")
            opts.add("debug", false, "enable SLF4J debug messages")
            opts.toJsonFile(path)
        }
    }

    override fun start() {
        val kieServices = KieServices.Factory.get()
        val kfs = kieServices.newKieFileSystem()

        kfs.write(ResourceFactory.newFileResource(File(engine.workingdir+"/"+opts.getStringVal("filenamerules"))))

        val kieBuilder = kieServices.newKieBuilder(kfs).buildAll()
        val kieContainer = kieServices.newKieContainer(kieServices.repository.defaultReleaseId)

        val kieBaseConf = kieServices.newKieBaseConfiguration()
        //kieBaseConf.setOption(MultithreadEvaluationOption.YES)

        val kieBase = kieContainer.newKieBase(kieBaseConf)
        ksession = kieBase.newKieSession()

        if (opts.getBoolVal("debug")) {
            ksession.addEventListener(DebugAgendaEventListener())
            ksession.addEventListener(DebugRuleRuntimeEventListener())
        }

        for ((key, value) in sensorHashmap) {
            val s = Sensor(null, 0, value.sensortype, value.sensorname, value.positionname, value.positiontype)
            val f = ksession.insert(s)
            sensorFactHashmap.put(key, SensorFactEntry(s, f))
        }


        //s_temp = Sensor(25.0f, 123, "temperature", "SHT75_1", "kitchen01", "kitchen")
        //fh = ksession.insert(s_temp)

        //val s_month = Sensor(12, 123, "month", "month", "month", "global")
        //val fh1 = ksession.insert(s_month)
    }

    override fun stop() {
        ksession.dispose()
    }

    override fun kill() {
    }

    override fun threadedTask() {
        val value = indata.poll(1000, TimeUnit.MILLISECONDS)
        if (value != null) {

            //s_temp.value = (value.value as Int).toFloat()
            //ksession.update(fh, s_temp)

            if (value.value is HashMap<*, *>) {

                var key = ""
                if (value.value.containsKey("channelName")) {
                    val str = value.value["channelName"]
                    if (str is String)
                        key = str
                } else if (value.value.containsKey("sensorname")) {
                    val str = value.value["sensorname"]
                    if (str is String)
                        key = str
                }

                if (sensorHashmap.containsKey(key)) {

                    var msg : Any by Delegates.notNull()
                    if (value.value.containsKey("message")) {
                        val valmsg = value.value["message"]
                        if (valmsg != null)
                            msg = valmsg
                    } else if (value.value.containsKey("reading")) {
                        val valmsg = value.value["reading"]
                            if (valmsg != null)
                                msg = valmsg
                    }


                    val isNumeric = sensorHashmap[key]?.isNumeric
                    if (isNumeric != null && isNumeric) {
                        if (msg is String) {
                            sensorFactHashmap[key]?.sensor?.value = (msg as String).toDouble()
                        }
                        else
                            sensorFactHashmap[key]?.sensor?.value = msg
                    }
                    else
                        sensorFactHashmap[key]?.sensor?.value = msg as String

                    sensorFactHashmap[key]?.sensor?.timestamp = value.timestamp.time


                    ksession.update(sensorFactHashmap[key]?.fact, sensorFactHashmap[key]?.sensor)

                    ksession.fireAllRules()

                    for (f in ksession.getFactHandles<FactHandle>()) {
                        val sessionObject = ksession.getObject(f)
                        if (sessionObject.javaClass.name == "de.seiderer.eventerpretor.drools.ContextElement") {
                            //println("Final output (ContextElement): " + (sessionObject as ContextElement).name + ": " + (sessionObject).value + ": " + (sessionObject).sensorvalue)
                            dataOut(sessionObject)
                        }
                        if (sessionObject.javaClass.name == "de.seiderer.eventerpretor.drools.Command") {
                            //println("Final output (Command): " + (sessionObject as Command).value)
                            dataOut(sessionObject)
                        }
                        if (sessionObject.javaClass.name == "de.seiderer.eventerpretor.drools.Sensor") {
                            //println("Final output (Sensor): " + (sessionObject as Sensor).value)
                            dataOut(sessionObject)
                        }
                    }

                } else {
                    engine.warning("Key \"$key\" not found!", this.nodename)
                }

            } else {
                engine.warning("Unknown data type!", this.nodename)
            }

        }
    }

}