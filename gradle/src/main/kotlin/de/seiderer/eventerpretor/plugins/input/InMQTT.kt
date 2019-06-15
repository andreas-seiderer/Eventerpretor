package de.seiderer.eventerpretor.plugins.input

import de.seiderer.eventerpretor.core.Engine
import de.seiderer.eventerpretor.plugins.base.InNode
import org.eclipse.paho.client.mqttv3.*
import java.io.File
import java.util.*
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory


/**
 * @author Andreas Seiderer
 */
class InMQTT(engine:Engine, name:String) : InNode(engine,name,0), MqttCallback {

    var mqttClient : MqttClient by Delegates.notNull()
    var mqttindata : SynchronousQueue<HashMap<String, String>> = SynchronousQueue()


    override fun setConfig() {
        val path : String = engine.workingdir + "/options_" + this.nodename + ".json"

        //read options from file if it exists; if not create new with default values
        if (File(path).exists())
            opts.fromJsonFile(path)
        else {
            opts.add("broker", "tcp://nuc.lan:1883", "broker string")
            opts.add("clientId", "Eventerpretor", "id of the client")
            opts.add("topics", arrayOf("/CO2mon/CO2", "fhem/temp/set"), "topics to subscribe to")
            opts.add("username", "username", "username for user authentication; disabled with empty string")
            opts.add("password", "password", "password if username is set")
            opts.add("certificate", "", "path to certificate file; disabled if set to empty string")

            opts.toJsonFile(path)
        }
    }

    override fun start() {
        val connOpts = MqttConnectOptions()
        connOpts.isCleanSession = true
        connOpts.isAutomaticReconnect = true

        if (opts.getStringVal("username").isNotEmpty()) {
            connOpts.userName = opts.getStringVal("username")
            connOpts.password = opts.getStringVal("password").toCharArray()
        }

        if (opts.getStringVal("certificate").isNotEmpty()) {
            connOpts.socketFactory = getSocketFactory(engine.workingdir + "/" + opts.getStringVal("certificate"))
        }

        mqttClient = MqttClient(opts.getStringVal("broker"), opts.getStringVal("clientId"))
        mqttClient.connect(connOpts)
        mqttClient.setCallback(this)

        val topics = opts.getArrVal("topics")
        if (topics != null)
            for (t in topics)
                if (t is String)
                    mqttClient.subscribe(t)
    }

    override fun threadedTask() {
        val value : HashMap<String, String>? = mqttindata.poll(1000, TimeUnit.MILLISECONDS)
        if (value != null)
            dataOut(value)
    }


    override fun stop() {
        mqttClient.disconnect(1000)
    }

    override fun kill() {

    }

    override fun messageArrived(topic: String?, msg: MqttMessage?) {
        if (msg != null && topic != null)
            mqttindata.put(hashMapOf("topic" to topic, "message" to msg.toString()))
    }

    override fun connectionLost(p0: Throwable?) {
    }

    override fun deliveryComplete(p0: IMqttDeliveryToken?) {
    }


    @Throws(Exception::class)
    private fun getSocketFactory(
            crtFile: String): SSLSocketFactory {

        var caCert: X509Certificate? = null

        val fis = FileInputStream(crtFile)
        val bis = BufferedInputStream(fis)
        val cf = CertificateFactory.getInstance("X.509")

        while (bis.available() > 0) {
            caCert = cf.generateCertificate(bis) as X509Certificate
        }

        val caKs = KeyStore.getInstance(KeyStore.getDefaultType())
        caKs.load(null, null)
        caKs.setCertificateEntry("ca-certificate", caCert)
        val tmf = TrustManagerFactory.getInstance("X509")
        tmf.init(caKs)

        val context = SSLContext.getInstance("TLSv1.2")
        context.init(null, tmf.trustManagers, null)

        return context.socketFactory
    }

}