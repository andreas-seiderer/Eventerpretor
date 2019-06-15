package de.seiderer.eventerpretor.plugins.base

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

/**
 *  @author Andreas Seiderer
 */
class Options {

    private var options : HashMap<String, HashMap<String, Any>> = HashMap()

    fun add(name : String, value : Any, help : String) {
        options.put(name, hashMapOf("value" to value, "help" to help, "class" to value.javaClass.toString()))
    }

    fun getVal(name : String) : Any? {
        return options[name]?.get("value")
    }

    fun getStringVal(name : String) : String {
        val v = options[name]?.get("value")
        if(v is String)
            return v
        else
            return ""
    }

    fun getBoolVal(name : String) : Boolean {
        val v = options[name]?.get("value")
        if(v is Boolean)
            return v
        else
            return false
    }

    fun getIntVal(name : String) : Int {
        var v = options[name]?.get("value")

        if (v is Double)
            v = v.toInt()

        if(v is Int)
            return v
        else
            return 0
    }

    fun getDoubleVal(name : String) : Double {
        val v = options[name]?.get("value")

        if (v is Double)
            return v
        else
            return 0.0
    }

    fun getArrVal(name : String) : ArrayList<*>? {
        val v = options[name]?.get("value")
        if(v is ArrayList<*>) {
            return v
        }
        else
            return null
    }

    fun toJsonFile(path : String) {
        val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

        val issueAdapter = moshi.adapter(Map::class.java)
        File(path).writeText(issueAdapter.indent("  ").toJson(options))
    }

    fun fromJsonFile (path : String) {
        val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

        val issueAdapter = moshi.adapter(Map::class.java)
        val opts = issueAdapter.fromJson(File(path).readText())

        if (opts != null)
            for ((key,value) in opts) {
                if (key is String)
                    if (value is Map<*,*>) {

                        val prop_value = value["value"]
                        val prop_help = value["help"]
                        val prop_class = value["class"]

                        if (prop_value != null && prop_help != null && prop_class != null)
                            options.put(key, hashMapOf("value" to prop_value, "help" to prop_help, "class" to prop_class))
                    }
            }
    }

}