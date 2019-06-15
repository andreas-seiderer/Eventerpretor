package de.seiderer.eventerpretor.plugins.input

import de.seiderer.eventerpretor.core.Engine
import de.seiderer.eventerpretor.plugins.base.InNode
import net.fortuna.ical4j.connector.dav.CalDavCalendarStore
import net.fortuna.ical4j.connector.dav.PathResolver
import java.io.File
import java.net.URL
import net.fortuna.ical4j.connector.dav.CalDavCalendarCollection
import kotlin.properties.Delegates
import net.fortuna.ical4j.model.*
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.util.CompatibilityHints


/**
 * @author Andreas Seiderer
 *
 * Google CalDav events of own calendars can be read out;
 * due to problems preselecting the events with iCal4j all events are downloaded and selected afterwards;
 */
class InCalDavEvent(engine: Engine, name:String) : InNode(engine,name,100) {

    private var calCollections: List<CalDavCalendarCollection> by Delegates.notNull()

    override fun setConfig() {
        val path : String = engine.workingdir + "/options_" + this.nodename + ".json"

        //read options from file if it exists; if not create new with default values
        if (File(path).exists())
            opts.fromJsonFile(path)
        else {
            opts.add("interval", 60000, "interval in milliseconds")
            opts.add("username", "username@gmail.com", "username for authentication")
            opts.add("password", "password", "password for authentication")
            opts.add("host", "www.google.com", "host")
            opts.add("port", 443, "port")
            opts.add("calendars", arrayOf("username@gmail.com"), "specific calendars that should be considered")
            opts.toJsonFile(path)
        }
    }

    override fun start() {
        resetTimer(this.opts.getIntVal("interval").toLong())

        CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_PARSING, true)
        CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_VALIDATION, true)
        CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_UNFOLDING, true)

        val PRODID = "-//Ben Fortuna//iCal4j Connector 1.0//EN"
        val host = opts.getStringVal("host")
        val port = opts.getIntVal("port")
        val pathResolver = PathResolver.GenericPathResolver.GCAL
        //val pathResolver = GenericCalDavPathResolver()

        //val url = URL("https", host, port, "")

        val url = URL("https", host, port, "")

        val store = CalDavCalendarStore(PRODID, url, pathResolver)
        store.connect(opts.getStringVal("username"), opts.getStringVal("password").toCharArray())
        calCollections = store.collections


    }

    override fun stop() {
    }

    override fun kill() {
    }

    var counter : Int = 0

    override fun threadedTask() {
        for(cal in calCollections) {
            val calArr = opts.getArrVal("calendars")

            if (calArr != null && calArr.size > 1 && calArr.contains(cal.displayName))
                engine.warning("Calendar not found!", "InCalDavEvent")


            if ((calArr != null && calArr.contains(cal.displayName)) || (calArr != null && calArr.size == 0)) {

                for (event in cal.events) {
                    val events = event.getComponents<VEvent>(Component.VEVENT)
                    for (e in events) {
                        val startdate = e.startDate.date
                        val endDate = e.endDate.date

                        var time = -1L

                        //current event (still active)
                        if (startdate <= engine.getCurrentDate() && endDate >= engine.getCurrentDate())
                            time = 0L
                        //events that come up during the next hour
                        else if (startdate > engine.getCurrentDate() && ((startdate.time - engine.getCurrentDate().time)) / 1000 <= 60 * 60)
                            time = (startdate.time - engine.getCurrentDate().time) / 1000


                        if (time != -1L) {
                            val calname = cal.displayName
                            val summary = e.summary.value
                            dataOut(mapOf("calname" to calname, "summary" to summary, "startdate" to startdate, "enddate" to endDate, "secinfuture" to time))
                        }
                    }
                }
            }
        }

    }
}

/*
internal class GenericCalDavPathResolver : PathResolver() {

    override fun getUserPath(username: String?): String {

        return "/cloud/remote.php/dav/calendars"
    }

    override fun getPrincipalPath(username: String?): String {

        return "/cloud/remote.php/dav/calendars"
    }

}*/
