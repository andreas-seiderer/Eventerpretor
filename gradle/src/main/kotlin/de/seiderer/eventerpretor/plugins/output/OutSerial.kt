package de.seiderer.eventerpretor.plugins.output

import com.fazecast.jSerialComm.SerialPort
import de.seiderer.eventerpretor.core.Engine
import de.seiderer.eventerpretor.plugins.base.OutNode
import de.seiderer.eventerpretor.plugins.manager.SerialDevices
import java.io.File
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

/**
 * @author Andreas Seiderer
 * Sends data to serial port
 */
class OutSerial(engine: Engine, name:String) : OutNode(engine,name,0) {

    private var port : SerialPort by Delegates.notNull()
    private var newlinechars : String by Delegates.notNull()

    override fun setConfig() {
        val path : String = engine.workingdir + "/options_" + this.nodename + ".json"

        //read options from file if it exists; if not create new with default values
        if (File(path).exists())
            opts.fromJsonFile(path)
        else {
            opts.add("port", "COM1", "serial port name")
            opts.add("baudrate", 115200, "serial port baudrate")

            opts.add("newline", "\\r\\n", "new line char(s) added at end")

            opts.add("isBinary", false, "binary or ascii data?")                //not implemented

            opts.add("sendOnBegin", "", "send string to device at beginning")
            opts.add("sendOnEnd", "", "send string to device at end")

            opts.toJsonFile(path)
        }
    }

    override fun start() {
        newlinechars = opts.getStringVal("newline").replace("\\r","\r"). replace("\\n", "\n")

        if (SerialDevices.deviceExists(opts.getStringVal("port"))) {
            val entry = SerialDevices.getDevice(opts.getStringVal("port"))
            if (entry != null)
                port = entry
        } else {
            port = SerialPort.getCommPort(opts.getStringVal("port"))
            SerialDevices.addDevice(opts.getStringVal("port"), port)
        }

        if (!port.isOpen) {
            port.baudRate = opts.getIntVal("baudrate")
            port.openPort()
        }

        if (!port.isOpen) {
            engine.warning("Not able to open serial port: " + opts.getStringVal("port"), this.nodename)
            return
        }

        //send on serial begin
        val bytesOnBegin = opts.getStringVal("sendOnBegin")
                .replace("\\r","\r")
                .replace("\\n", "\n")
                .toByteArray(Charset.forName("US-ASCII"))

        if (bytesOnBegin.isNotEmpty())
            port.writeBytes(bytesOnBegin, bytesOnBegin.size.toLong())
    }

    override fun stop() {
        //send on serial end
        val bytesOnEnd = opts.getStringVal("sendOnEnd")
                .replace("\\r","\r")
                .replace("\\n", "\n")
                .toByteArray(Charset.forName("US-ASCII"))

        if (bytesOnEnd.isNotEmpty())
            port.writeBytes(bytesOnEnd, bytesOnEnd.size.toLong())

        port.removeDataListener()

        SerialDevices.removeDevice(opts.getStringVal("port"))
    }

    override fun kill() {
    }

    override fun threadedTask() {
        val value = indata.poll(1000, TimeUnit.MILLISECONDS)

        if (value != null) {
            if (value.value is String) {

                var bytesToSend = value.value

                if (newlinechars.isNotEmpty())
                    bytesToSend = value.value + newlinechars

                bytesToSend = bytesToSend.toByteArray()

                port.writeBytes(bytesToSend, bytesToSend.size.toLong())

            } else if (value.value is HashMap<*,*>) {

                val valc = value.value as HashMap<String,Any?>

                if (valc.containsKey("message")) {
                    val msg = valc["message"]
                    if (msg is String) {
                        var bytesToSend = msg

                        if (newlinechars.isNotEmpty())
                            bytesToSend = msg + newlinechars

                        bytesToSend = bytesToSend.toByteArray()

                        port.writeBytes(bytesToSend, bytesToSend.size.toLong())
                    }
                }
            }
        }
    }

}