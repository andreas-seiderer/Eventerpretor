package de.seiderer.eventerpretor.plugins.transform

import de.seiderer.eventerpretor.core.Engine
import de.seiderer.eventerpretor.plugins.base.TransformNode
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

/**
 *  @author Andreas Seiderer
 *  Allow message to pass to next node
 */
class TransformAllowMsgRegex(engine: Engine, name:String) : TransformNode(engine,name,0) {

    var regex : Regex by Delegates.notNull()

    override fun setConfig() {
        val path : String = engine.workingdir + "/options_" + this.nodename + ".json"

        //read options from file if it exists; if not create new with default values
        if (File(path).exists())
            opts.fromJsonFile(path)
        else {
            opts.add("regex", "", "regular expression that has to match the message string")
            opts.add("replaceString" ,false , "replace found string?")
            opts.add("replace" ,"" , "replace found string with this one if enabled")

            opts.toJsonFile(path)
        }
    }

    override fun start() {
        regex = opts.getStringVal("regex").toRegex()
    }

    override fun stop() {

    }

    override fun kill() {

    }

    override fun threadedTask() {
        val value = indata.poll(1000, TimeUnit.MILLISECONDS)
        if (value != null) {
            if (value.value is String && regex.containsMatchIn(value.value)) {
                var invalue = value.value

                if (opts.getBoolVal("replaceString"))
                    invalue = regex.replace(invalue, opts.getStringVal("replace"))

                dataOut(invalue)
            } else if (value.value is HashMap<*,*> ) {
                val hashvalue = value.value as HashMap<String,Any?>
                if (hashvalue.containsKey("message")) {
                    val hashvalueStr = hashvalue["message"]
                    if (hashvalueStr is String )
                        if (regex.containsMatchIn(hashvalueStr)) {
                            var invalue = hashvalueStr

                            if (opts.getBoolVal("replaceString"))
                                invalue = regex.replace(invalue, opts.getStringVal("replace"))

                            dataOut(invalue)
                        }

                }

            }
        }
    }

}