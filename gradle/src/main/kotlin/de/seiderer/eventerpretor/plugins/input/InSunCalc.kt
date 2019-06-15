package de.seiderer.eventerpretor.plugins.input

import de.seiderer.eventerpretor.core.Engine
import de.seiderer.eventerpretor.plugins.base.InNode
import org.shredzone.commons.suncalc.SunPosition
import org.shredzone.commons.suncalc.SunTimes
import java.io.File

/**
 * @author Andreas Seiderer
 * Calculate sun data with a specific sr and location
 */
class InSunCalc(engine: Engine, name:String) : InNode(engine,name,1000) {

    override fun setConfig() {
        val path : String = engine.workingdir + "/options_" + this.nodename + ".json"

        //read options from file if it exists; if not create new with default values
        if (File(path).exists())
            opts.fromJsonFile(path)
        else {
            opts.add("interval", 1000, "interval in milliseconds")
            opts.add("latitude", 48.366512, "geographic latitude")
            opts.add("longitude", 10.894446, "geographic longitude")
            opts.toJsonFile(path)
        }
    }

    override fun start() {
        resetTimer(this.opts.getIntVal("interval").toLong())
    }

    override fun stop() {
    }

    override fun kill() {
    }

    override fun threadedTask() {
        val sunposition = SunPosition.compute()
                .on(engine.getCurrentDate())       // set a date
                .at(opts.getDoubleVal("latitude"), opts.getDoubleVal("longitude"))   // set a location
                .execute()     // get the results

        val suntimes = SunTimes.compute()
                .on(engine.getCurrentDate())
                .at(opts.getDoubleVal("latitude"), opts.getDoubleVal("longitude"))
                .fullCycle()
                .execute()

        dataOut(hashMapOf("altitude" to sunposition.altitude, "azimuth" to sunposition.azimuth, "sunrise" to suntimes.rise, "sunset" to suntimes.set))
    }
}