package de.seiderer.eventerpretor.plugins.output

import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import de.seiderer.eventerpretor.core.Engine
import de.seiderer.eventerpretor.plugins.base.OutNode
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

/**
 * @author Andreas Seiderer
 */
class OutPgsqlNotify(engine: Engine, name:String) : OutNode(engine,name,0) {
    var dbConnection: Connection by Delegates.notNull()


    override fun setConfig() {

        val path : String = engine.workingdir + "/options_" + this.nodename + ".json"

        //read options from file if it exists; if not create new with default values
        if (File(path).exists())
            opts.fromJsonFile(path)
        else {
            opts.add("connection", "jdbc:postgresql://localhost/postgres", "database connection string")
            opts.add("user", "postgres", "user name")
            opts.add("password", "", "password")
            opts.add("ssl", true, "use SSL connection; required for ssl related options")
            opts.add("sslcert", "postgresql.crt", "client certificate for SSL connection")
            opts.add("sslkey", "postgresql.pk8", "client key in PK8 format for SSL connection")
            opts.add("sslrootcert", "root.crt", "server certificate for SSL connection")
            opts.add("notificationname", "mynotify", "name of the notification")

            opts.toJsonFile(path)
        }
    }

    override fun start() {
        val props = Properties()
        props.setProperty("user", opts.getStringVal("user"))
        props.setProperty("password", opts.getStringVal("password"))

        //https://jdbc.postgresql.org/documentation/head/connect.html
        if (opts.getBoolVal("ssl")) {
            props.setProperty("ssl", "true")
            props.setProperty("sslcert", engine.workingdir + "/" + opts.getStringVal("sslcert"))
            props.setProperty("sslkey", engine.workingdir + "/" + opts.getStringVal("sslkey"))
            props.setProperty("sslrootcert", engine.workingdir + "/" + opts.getStringVal("sslrootcert"))
            props.setProperty("sslfactoryarg", engine.workingdir + "/" + opts.getStringVal("sslrootcert"))
            props.setProperty("sslmode", "require")
        }

        dbConnection = DriverManager.getConnection(opts.getStringVal("connection"), props)
        dbConnection.autoCommit = true
    }

    override fun stop() {
        dbConnection.close()
    }

    override fun kill() {
    }

    override fun threadedTask() {
        val value = indata.poll(1000, TimeUnit.MILLISECONDS)
        if (value != null) {
            //create JSON
            val msgdata = hashMapOf("timestamp" to value.timestamp.time, "value" to value.value, "sourcename" to value.sourcename)
            val moshi = Moshi.Builder()
                    .add(KotlinJsonAdapterFactory())
                    .build()
            val issueAdapter = moshi.adapter(Map::class.java)

            //send notify with JSON payload
            val stmt = dbConnection.createStatement()
            stmt.execute("NOTIFY " + opts.getStringVal("notificationname") + ",'" + issueAdapter.toJson(msgdata)+"'")
            stmt.close()
        }
    }

}
