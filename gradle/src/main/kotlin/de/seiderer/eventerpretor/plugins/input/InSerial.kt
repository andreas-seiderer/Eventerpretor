package de.seiderer.eventerpretor.plugins.input

import com.fazecast.jSerialComm.SerialPort
import de.seiderer.eventerpretor.core.Engine
import de.seiderer.eventerpretor.plugins.base.InNode
import de.seiderer.eventerpretor.plugins.manager.SerialDevices
import java.io.File
import kotlin.properties.Delegates
import java.io.UnsupportedEncodingException
import com.fazecast.jSerialComm.SerialPortEvent
import com.fazecast.jSerialComm.SerialPortDataListener
import java.nio.charset.Charset
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.TimeUnit


/**
 * @author Andreas Seiderer
 * Receives data from serial port
 */
class InSerial(engine: Engine, name:String) : InNode(engine,name,0) {

    private var port : SerialPort by Delegates.notNull()
    private val buf = StringBuffer()
    private var serialindata : SynchronousQueue<Any> = SynchronousQueue()

    override fun setConfig() {
        val path : String = engine.workingdir + "/options_" + this.nodename + ".json"

        //read options from file if it exists; if not create new with default values
        if (File(path).exists())
            opts.fromJsonFile(path)
        else {
            opts.add("port", "COM1", "serial port name")                                                                    //ok
            opts.add("baudrate", 115200, "serial port baudrate")                                                            //ok

            opts.add("newline", "\\r\\n", "new line char(s)")                                                               //ok
            opts.add("bytesToRead", 0, "how many bytes should be read at once in binaryMode?")                              //not implemented

            opts.add("isBinary", false, "binary or ascii data?")                                                            //not implemented

            opts.add("sendOnBegin", "", "send string to device at beginning")                                               //ok (TODO test)
            opts.add("sendOnEnd", "", "send string to device at end")                                                       //ok (TODO test)

            opts.toJsonFile(path)
        }
    }

    override fun start() {

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


        //clear buffer
        val avBytes = port.bytesAvailable()

        if (avBytes > 0) {
            val temp: ByteArray? = ByteArray(avBytes)
            port.readBytes(temp, avBytes.toLong())
        }

        //send on serial begin
        val bytesOnBegin = opts.getStringVal("sendOnBegin")
                .replace("\\r","\r")
                .replace("\\n", "\n")
                .toByteArray(Charset.forName("US-ASCII"))

        if (bytesOnBegin.isNotEmpty())
            port.writeBytes(bytesOnBegin, bytesOnBegin.size.toLong())


        val newlinechars = opts.getStringVal("newline").replace("\\r","\r"). replace("\\n", "\n")


        port.addDataListener(object : SerialPortDataListener {
            override fun getListeningEvents(): Int {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE
            }

            override fun serialEvent(event: SerialPortEvent) {
                if (event.eventType != SerialPort.LISTENING_EVENT_DATA_AVAILABLE)
                    return
                val newData = ByteArray(port.bytesAvailable())
                port.readBytes(newData, newData.size.toLong())

                try {
                    val string = String(newData, Charset.forName("US-ASCII"))

                    for (i in 0 until string.length) {
                        buf.append(string[i])

                        if (buf.length > newlinechars.length && buf.substring(buf.length - newlinechars.length) == newlinechars) {
                            serialindata.put(buf.substring(0,buf.length-newlinechars.length))

                            buf.setLength(0)
                        }
                    }
                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()
                }

            }
        })
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
        val value :Any? = serialindata.poll(1000, TimeUnit.MILLISECONDS)
        if (value != null) {
            dataOut(value)
        }
    }
}