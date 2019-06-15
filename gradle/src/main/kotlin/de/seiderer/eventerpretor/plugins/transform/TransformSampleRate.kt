package de.seiderer.eventerpretor.plugins.transform

import de.seiderer.eventerpretor.core.Engine
import de.seiderer.eventerpretor.plugins.base.TransformNode
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 *  @author Andreas Seiderer
 *  Publish samplerate of input data
 */
class TransformSampleRate(engine: Engine, name:String) : TransformNode(engine,name,0) {
    private var lastCall: Instant? = null

    override fun threadedTask() {
        val value = indata.poll(1000, TimeUnit.MILLISECONDS)
        if (value != null) {
            if (lastCall == null)
                lastCall = Instant.now()
            else {
                val now = Instant.now()
                val timeElapsed = Duration.between(lastCall, now)

                if (timeElapsed.toMillis() > 0) {
                    val sr = 1000.0 / timeElapsed.toMillis()
                    dataOut(sr)
                }

                lastCall = now
            }
        }
    }

    override fun setConfig() {

    }

    override fun start() {

    }

    override fun stop() {

    }

    override fun kill() {

    }

}