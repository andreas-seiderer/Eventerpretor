package de.seiderer.eventerpretor.plugins.input

import de.seiderer.eventerpretor.core.Engine
import de.seiderer.eventerpretor.plugins.base.InNode
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.net.ConnectException
import java.net.Socket
import java.net.SocketTimeoutException

/**
 * @author Andreas Seiderer
 * receive data from TCP server
 */
class InTCP(engine: Engine, name:String) : InNode(engine,name,0) {

    private var sock : Socket? = null
    private var bufferedReader: BufferedReader? = null

    override fun setConfig() {
        val path : String = engine.workingdir + "/options_" + this.nodename + ".json"

        //read options from file if it exists; if not create new with default values
        if (File(path).exists())
            opts.fromJsonFile(path)
        else {
            opts.add("ip", "localhost", "ip address of server")
            opts.add("port", 7072, "port of server")
            opts.add("password", "password", "password, leave empty if there is none")
            opts.add("sendOnBegin", "", "command to send at start; e.g. 'inform timer' for FHEM")

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


        bufferedReader = sock?.inputStream?.bufferedReader()

    }

    override fun start() {
        reconnect()
    }


    override fun threadedTask() {
        if (sock != null && bufferedReader != null ) {
            try {
                while (true) {
                    val msg = bufferedReader?.readLine()
                    //println(msg)

                    if (msg != null) {
                        val msgstr = msg.toString()
                        dataOut(hashMapOf("message" to msgstr))
                    } else
                        break
                }
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

    override fun stop() {
        //sock?.close()
    }

    override fun kill() {
        //sock?.close()
    }
}