package de.seiderer.eventerpretor.plugins.output

import de.seiderer.eventerpretor.core.Engine
import de.seiderer.eventerpretor.drools.Command
import de.seiderer.eventerpretor.plugins.base.OutNode
import java.io.File
import java.io.InputStream
import java.net.ConnectException
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * @author Andreas Seiderer
 * send data to TCP server; accepts String and Command as input
 */
class OutTCP(engine: Engine, name:String) : OutNode(engine,name,0) {

    private var sock : Socket? = null

    override fun setConfig() {
        val path : String = engine.workingdir + "/options_" + this.nodename + ".json"

        //read options from file if it exists; if not create new with default values
        if (File(path).exists())
            opts.fromJsonFile(path)
        else {
            opts.add("ip", "localhost", "ip address of server")
            opts.add("port", 7072, "port of server")
            opts.add("password", "password", "password, leave empty if there is none")
            opts.add("sendOnBegin", "", "command to send at start;")

            opts.toJsonFile(path)
        }
    }


    private fun reconnect() {
        try {
            sock = Socket(opts.getStringVal("ip"), opts.getIntVal("port"))
        }
        catch(e: ConnectException) {
            engine.warning("Not able to connect to TCP server: " + opts.getStringVal("ip") + ":" + opts.getIntVal("port"), this.nodename)
            return
        }

        if (sock != null) {

            sock?.keepAlive = true
            sock?.soTimeout = 500

            try {
                val ins : InputStream? = sock?.getInputStream()
                var read = 0
                val buffer = ByteArray(1024)

                if (ins != null) {
                    while (read != -1) {
                        read = ins.read(buffer)
                        //val output = String(buffer, 0, read)
                        //print(output)
                        //System.out.flush()
                    }
                }
            } catch (e: SocketTimeoutException) {

            }

            sock?.soTimeout = 1000

            try {
                if (opts.getStringVal("password") != "") {

                    sock?.outputStream?.write(opts.getStringVal("password").toByteArray(Charsets.US_ASCII))
                    sock?.outputStream?.write("\r\n".toByteArray(Charsets.US_ASCII))
                }

                if (opts.getStringVal("sendOnBegin") != "") {
                    sock?.outputStream?.write(opts.getStringVal("sendOnBegin").toByteArray())
                }

            } catch (e: SocketTimeoutException) {
                engine.warning("Not able to connect to TCP server: " + opts.getStringVal("ip") + ":" + opts.getIntVal("port") + " "+ e.message, this.nodename)
            }


            sock?.soTimeout = 500

            //remove first data from server
            try {
                val ins : InputStream? = sock?.getInputStream()
                var read = 0
                val buffer = ByteArray(1024)

                if (ins != null) {
                    while (read != -1) {
                        read = ins.read(buffer)
                    }
                }
            } catch (e: SocketTimeoutException) {

            }

            sock?.soTimeout = 500
        }

    }

    override fun start() {
        reconnect()
    }


    override fun threadedTask() {
        val value = indata.poll(1000, TimeUnit.MILLISECONDS)
        if (value != null) {
            val cmd : String

            if (value.value is String)
                cmd = value.value
            else if (value.value is Command)
                cmd = value.value.value
            else {
                engine.warning("Incompatible data type as input found!", this.nodename)
                return
            }
            if (sock != null) {
                try {
                    sock?.outputStream?.write(cmd.toByteArray(Charsets.US_ASCII))
                } catch (e: SocketTimeoutException) {
                    if (sock != null) {
                        val connected = sock?.isConnected
                        if (connected != null && !connected) {
                            engine.warning("Not able to connect to TCP server: " + opts.getStringVal("ip") + ":" + opts.getIntVal("port") + " " + e.message, this.nodename)

                            engine.warning("Reconnecting to TCP server: " + opts.getStringVal("ip") + ":" + opts.getIntVal("port"), this.nodename)
                            reconnect()
                        }
                    }
                }
            } else {
                engine.warning("Reconnecting to TCP server: " + opts.getStringVal("ip") + ":" + opts.getIntVal("port"), this.nodename)
                reconnect()
            }
        }
    }

    override fun stop() {
        //sock?.close()
    }

    override fun kill() {
        //sock?.close()
    }
}