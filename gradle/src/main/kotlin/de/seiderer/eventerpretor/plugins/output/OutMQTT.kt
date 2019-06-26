package de.seiderer.eventerpretor.plugins.output

import de.seiderer.eventerpretor.core.Engine
import de.seiderer.eventerpretor.plugins.base.OutNode
import org.eclipse.paho.client.mqttv3.*
import java.io.BufferedInputStream
import kotlin.properties.Delegates
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import java.io.File
import java.io.FileInputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.util.concurrent.TimeUnit
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.util.HashMap


/**
 * @author Andreas Seiderer
 */
class OutMQTT(engine: Engine, name:String) : OutNode(engine,name,0) {

    private var mqttClient : MqttClient by Delegates.notNull()

    override fun setConfig() {
        val path : String = engine.workingdir + "/options_" + this.nodename + ".json"

        //read options from file if it exists; if not create new with default values
        if (File(path).exists())
            opts.fromJsonFile(path)
        else {
            opts.add("broker", "tcp://nuc.lan:1883", "broker string")
            opts.add("clientId", "Eventerpretor", "id of the client; has to be unique to prevent strange behavior!")
            opts.add("qos", 0 , "qos: 0, 1 or 2")
            opts.add("topic", "/CO2mon/CO2", "topic to publish to")
            opts.add("username", "username", "username for user authentication; disabled with empty string")
            opts.add("password", "password", "password if username is set")
            opts.add("certificate", "", "path to certificate file; disabled if set to empty string")
            opts.add("topicFromData", false, "true: provide the topic in the data input via \"topic\"; false: use the option \"topic\" for all messages")

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
    }


    override fun stop() {
        mqttClient.disconnect(1000)
    }

    override fun kill() {

    }

    override fun threadedTask() {
        val value = indata.poll(1000, TimeUnit.MILLISECONDS)
        if (value != null) {
            var messageStr : String by Delegates.notNull()
            var topicStr: String? = null

            if (value.value is String)
                messageStr = value.value

            if (value.value is HashMap<*,*>) {
                val valstr = value.value["message"]
                if (valstr is String)
                    messageStr = valstr

                if (value.value.containsKey("topic") && opts.getBoolVal("topicFromData")) {
                    val topic = value.value["topic"]
                    if (topic is String)
                        topicStr = topic
                }
            }

            if (value.value is Number)
                messageStr = value.value.toString()

            if (mqttClient.isConnected) {
                val message = MqttMessage(messageStr.toByteArray())
                message.qos = opts.getIntVal("qos")


                if (topicStr == null) {
                    topicStr = opts.getStringVal("topic")
                }

                mqttClient.publish(topicStr, message)
            }

        }
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