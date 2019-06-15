package de.seiderer.eventerpretor.plugins.base

import de.seiderer.eventerpretor.core.Engine
import java.util.*

import kotlin.concurrent.thread
import kotlin.concurrent.timer
import kotlin.properties.Delegates
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.concurrent.locks.ReadWriteLock



/**
 * @author Andreas Seiderer
 * interval = 0 means thread with while loop -> stop allows to leave it
 */
abstract class BaseNode constructor(engine:Engine, name:String, nodetype:NodeType, interval:Long) {
    var engine : Engine by Delegates.notNull()
    var nodename : String by Delegates.notNull()
    var nodetype : NodeType by Delegates.notNull()

    var thr : Thread by Delegates.notNull()
    var tim : Timer by Delegates.notNull()
    private var stop : Boolean  by Delegates.notNull()
    private var lock_stop: ReadWriteLock = ReentrantReadWriteLock()

    private var running : Boolean by Delegates.notNull()
    private var lock_running: ReadWriteLock = ReentrantReadWriteLock()

    var interval : Long by Delegates.notNull()

    var lastWDreset : Date by Delegates.notNull()

    var opts : Options = Options()

    init {
        this.engine = engine
        this.nodename = name
        this.nodetype = nodetype
        this.stop = false
        this.running = false
        this.interval = interval
    }

    /**
     * atomic set of stop flag via write lock
     */
    fun setStopFlag(value: Boolean) {
        lock_stop.writeLock().lock()
        this.stop = value
        lock_stop.writeLock().unlock()
    }

    /**
     * atomic get of stop flag via read lock
     */
    fun getStopFlag():Boolean {
        lock_stop.readLock().lock()
        val value = this.stop
        lock_stop.readLock().unlock()

        return value
    }


    /**
     * atomic set of running flag via write lock
     */
    fun setRunningFlag(value: Boolean) {
        lock_running.writeLock().lock()
        this.running = value
        lock_running.writeLock().unlock()
    }

    /**
     * atomic get of running flag via read lock
     */
    fun getRunningFlag():Boolean {
        lock_running.readLock().lock()
        val value = this.running
        lock_running.readLock().unlock()

        return value
    }



    fun startThread() {
        setStopFlag(false)

        if (interval == 0L) {
            thr = thread(start = true) {
                while(!this.getStopFlag())
                    threadedTask()
            }
        } else {
            tim = timer(period = interval) {
                threadedTask()
            }
        }

        lastWDreset = Date()

        setRunningFlag(true)
    }

    /**
     * reset timer interval
     */
    fun resetTimer(interval : Long) {
        if (getRunningFlag()) {
            stopThread()
            killThread()
        }

        this.interval = interval

        if (getRunningFlag()) {
            setStopFlag(false)

            if (interval == 0L) {
                thr = thread(start = true) {
                    while (!getStopFlag()) {
                        watchdogReset()
                        threadedTask()
                    }
                }
            } else {
                tim = timer(period = interval) {
                    watchdogReset()
                    threadedTask()
                }
            }
        }
    }

    /**
     * setting stop variable to true
     * if thread used waiting to join for 1 second
     * if timer used cancel is called
     */
    fun stopThread() {
        setStopFlag(true)
        if (interval == 0L)
            this.thr.join(1000)
        else
            this.tim.cancel()
    }

    /**
     * doing the same like stop but in case of thread interrupt is called
     */
    fun killThread() {
        setStopFlag(true)

        if (interval == 0L)
            thr.interrupt()
        else
            tim.cancel()

        setRunningFlag(false)
    }


    /**
     * reset watchdog time so that the "engine" can determine whether the node hangs
     */
    fun watchdogReset() {
        lastWDreset = Date()
    }

    /**
     * implemented by plugin; in this function all work should be done;
     */
    abstract fun threadedTask()

    /**
     * called in addNode function of engine
     * can be implemented by plugin to load configurations
     */
    abstract fun setConfig()

    /**
     * called before startThread
     * can be implemented by plugin to react before startThread is called
     */
    abstract fun start()

    /**
     * called before stopThread
     * can be implemented by plugin to react before stopThread is called
     */
    abstract fun stop()

    /**
     * called before killThread
     * can be implemented by plugin to react before killThread is called
     */
    abstract fun kill()
}