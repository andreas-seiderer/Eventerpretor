package de.seiderer.eventerpretor.plugins.input

import com.impossibl.postgres.api.jdbc.PGConnection
import de.seiderer.eventerpretor.core.Engine
import de.seiderer.eventerpretor.plugins.base.InNode
import java.io.File
import kotlin.properties.Delegates
import com.impossibl.postgres.jdbc.PGDataSource
import com.impossibl.postgres.api.jdbc.PGNotificationListener

/**
 * @author Andreas Seiderer
 * listen on notifications
 */
class InPgsqlListen(engine: Engine, name:String) : InNode(engine,name,0) {

    var dbConnection: PGConnection by Delegates.notNull()

    override fun setConfig() {

        val path : String = engine.workingdir + "/options_" + this.nodename + ".json"

        //read options from file if it exists; if not create new with default values
        if (File(path).exists())
            opts.fromJsonFile(path)
        else {
            opts.add("host", "localhost", "database server")
            opts.add("port", 5432, "database server port")
            opts.add("databasename", "postgres", "database name")
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
        val listener = PGNotificationListener { processId, channelName, payload ->
            dataOut(hashMapOf("channelName" to channelName, "message" to payload))
        }

        val dataSource = PGDataSource()
        dataSource.host = opts.getStringVal("host")
        dataSource.port = opts.getIntVal("port")
        dataSource.database = opts.getStringVal("databasename")
        dataSource.user = opts.getStringVal("user")
        dataSource.password = opts.getStringVal("password")

        if (opts.getBoolVal("ssl")) {
            dataSource.sslMode = "require"
            dataSource.sslRootCertificateFile = opts.getStringVal("sslrootcert")
            dataSource.sslCertificateFile = opts.getStringVal("sslcert")
            dataSource.sslKeyFile = opts.getStringVal("sslkey")
        }

        try {
            dbConnection = dataSource.connection as PGConnection
        } catch (e: Exception) {
            println(e.printStackTrace())
            return
        }


        // add the callback listener created earlier to the connection
        dbConnection.addNotificationListener(listener)

        val statement = dbConnection.createStatement()
        statement.execute("LISTEN " + opts.getStringVal("notificationname"))
        statement.close()
    }

    override fun stop() {
        dbConnection.close()
    }

    override fun kill() {
    }

    override fun threadedTask() {
        Thread.sleep(1000)
    }
}