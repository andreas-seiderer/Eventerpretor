package de.seiderer.eventerpretor.plugins.input

import com.impossibl.postgres.api.jdbc.PGConnection
import de.seiderer.eventerpretor.core.Engine
import de.seiderer.eventerpretor.plugins.base.InNode
import java.io.File
import kotlin.properties.Delegates
import com.impossibl.postgres.jdbc.PGDataSource
import com.impossibl.postgres.api.jdbc.PGNotificationListener
import com.impossibl.postgres.jdbc.PGSQLSimpleException
import java.sql.ResultSet



/**
 * @author Andreas Seiderer
 * create triggers and listen on notifications of sensors
 */
class InPgsqlSensorListen(engine: Engine, name:String) : InNode(engine,name,0) {

    var dbConnection: PGConnection by Delegates.notNull()

    data class SensorListenEntry(val dbSensorName: String, val dbSensorReading: String, val dbSensorReadingIsNum: Boolean, val dbSensorReadingAggFunc: String, val dbSensorReadingAggSec: Int)
    var sensorHashmap : HashMap<String,SensorListenEntry> = HashMap()

    override fun setConfig() {

        val path: String = engine.workingdir + "/options_" + this.nodename + ".json"

        //read options from file if it exists; if not create new with default values
        if (File(path).exists()) {
            opts.fromJsonFile(path)

            val optsensormap = opts.getVal("sensors")
            if (optsensormap is Map<*,*>) {
                for ((key, value) in optsensormap) {
                    if (key is String && value is Map<*,*>) {
                        var dbSensorName = ""
                        var dbSensorReading = ""
                        var dbSensorReadingIsNum = false
                        var dbSensorReadingAggFunc = ""
                        var dbSensorReadingAggSec = 0

                        val valuec = value as Map<String, Any?>

                        var v = valuec["dbSensorName"]
                        if (v is String)
                            dbSensorName = v

                        v = valuec["dbSensorReading"]
                        if (v is String)
                            dbSensorReading = v

                        v = valuec["dbSensorReadingIsNum"]
                        if (v is Boolean)
                            dbSensorReadingIsNum = v

                        v = valuec["dbSensorReadingAggFunc"]
                        if (v is String)
                            dbSensorReadingAggFunc = v

                        v = valuec["dbSensorReadingAggSec"]
                        if (v is Double)
                            dbSensorReadingAggSec = v.toInt()


                        sensorHashmap.put(key, SensorListenEntry(dbSensorName, dbSensorReading, dbSensorReadingIsNum, dbSensorReadingAggFunc, dbSensorReadingAggSec ))
                    }
                }
            }
        }
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

            opts.add("getdataonstartup", true, "output data of all sensors on startup")

            // Example
            sensorHashmap.put("mqtt_co2_avg", SensorListenEntry("mqtt_co2","co2", true, "AVG", 300))

            opts.add("sensors", sensorHashmap, "hash map of sensor names")

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


        for ((key, value) in sensorHashmap) {

            //create trigger functions

            var func : String

            if (value.dbSensorReadingIsNum) {
                /*
                CREATE OR REPLACE FUNCTION trigger_MRoom_co20_func()
                RETURNS trigger AS
                $$
                BEGIN
                    PERFORM pg_notify('MRoom_co20_measured-co2', (SELECT cast(AVG(reading_num) as VARCHAR) FROM public."MRoom_co20" WHERE timestamp >= NOW() - INTERVAL '5 minutes' and readingname = 'voc'));
                    RETURN NEW;
                END
                $$
                LANGUAGE 'plpgsql'
             */

                func = "CREATE OR REPLACE FUNCTION trigger_${key}_func()\n" +
                        "RETURNS trigger AS\n" +
                        "\$\$\n" +
                        "BEGIN\n" +
                        "\tPERFORM pg_notify('$key', (SELECT cast(${value.dbSensorReadingAggFunc}(reading_num) as VARCHAR) FROM ${value.dbSensorName} WHERE timestamp >= NOW() - INTERVAL '${value.dbSensorReadingAggSec} seconds' and readingname = '${value.dbSensorReading}'));\n" +
                        "\tRETURN NEW;\n" +
                        "END \n" +
                        "\$\$\n" +
                        "LANGUAGE 'plpgsql'"
            } else {
                func = "CREATE OR REPLACE FUNCTION trigger_${key}_func()\n" +
                        "RETURNS trigger AS\n" +
                        "\$\$\n" +
                        "BEGIN\n" +
                        "\tPERFORM pg_notify('$key', NEW.reading_str);\n" +
                        "\tRETURN NEW;\n" +
                        "END \n" +
                        "\$\$\n" +
                        "LANGUAGE 'plpgsql'"
            }

            var statement = dbConnection.createStatement()

            try {
                statement.execute(func)
            } catch (e : PGSQLSimpleException) {
                engine.warning("Could not create trigger function: " + e.message, this.nodename)
            }


            //remove OLD triggers

            func = "DROP TRIGGER IF EXISTS trigger_$key ON ${value.dbSensorName}"

            try {
                statement.execute(func)
            } catch (e : PGSQLSimpleException) {
                engine.warning("Could not drop trigger: " + e.message, this.nodename)
            }

            //create triggers

            /*
                CREATE TRIGGER trigger_MRoom_co20
                AFTER INSERT ON public."MRoom_co20"
                FOR EACH ROW
                WHEN (NEW.readingname = 'voc')
                EXECUTE PROCEDURE trigger_MRoom_co20_func()
             */

            func = "CREATE TRIGGER trigger_$key\n" +
                    "AFTER INSERT ON ${value.dbSensorName}\n" +
                    "FOR EACH ROW\n" +
                    "WHEN (NEW.readingname = '${value.dbSensorReading}')\n" +
                    "EXECUTE PROCEDURE trigger_${key}_func()"

            try {
                statement.execute(func)
            } catch (e : PGSQLSimpleException) {
                engine.warning("Could not create trigger: " + e.message, this.nodename)
            }


            func = "LISTEN $key"
            statement.execute(func)

            statement.close()


            if (opts.getBoolVal("getdataonstartup")) {

                if (value.dbSensorReadingIsNum) {   //numeric reading -> aggregation

                    // data count of last dbSensorReadingAggSec seconds
                    var datacount = 0
                    statement = dbConnection.createStatement()
                    func = "SELECT COUNT(reading_num) FROM ${value.dbSensorName} WHERE timestamp >= NOW() - INTERVAL '${value.dbSensorReadingAggSec} seconds' and readingname = '${value.dbSensorReading}'"

                    var rs : ResultSet? = null
                    try {
                        rs = statement.executeQuery(func)
                        while (rs.next()) {
                            datacount = rs.getInt(1)
                        }

                    } catch (e: PGSQLSimpleException) {
                        engine.warning("Could not check for data: " + e.message, this.nodename)
                    }

                    rs?.close()
                    statement.close()


                    if (datacount > 0) // if value count > 0
                        func = "SELECT cast(${value.dbSensorReadingAggFunc}(reading_num) as VARCHAR) FROM ${value.dbSensorName} WHERE timestamp >= NOW() - INTERVAL '${value.dbSensorReadingAggSec} seconds' and readingname = '${value.dbSensorReading}'"
                    else               // else get last value
                        func = "SELECT cast(reading_num as VARCHAR) FROM ${value.dbSensorName} WHERE readingname = '${value.dbSensorReading}' ORDER BY timestamp DESC LIMIT 1"


                    statement = dbConnection.createStatement()

                    try {
                        rs = statement.executeQuery(func)

                        while (rs.next()) {
                            val strmsg = rs.getString(1)
                            dataOut(hashMapOf("channelName" to key, "message" to strmsg))
                        }
                    } catch (e: PGSQLSimpleException) {
                        engine.warning("Could not get data: " + e.message, this.nodename)
                    }

                    rs?.close()
                    statement.close()
                }
                else {  // string reading
                    func = "SELECT reading_str FROM ${value.dbSensorName} WHERE readingname = '${value.dbSensorReading}' ORDER BY timestamp DESC LIMIT 1"

                    var rs : ResultSet? = null
                    try {
                        statement = dbConnection.createStatement()
                        rs = statement.executeQuery(func)

                        while (rs.next()) {
                            val strmsg = rs.getString(1)

                            dataOut(hashMapOf("channelName" to key, "message" to strmsg))
                        }
                    }  catch (e: PGSQLSimpleException) {
                        engine.warning("Could not get data: " + e.message, this.nodename)
                    }

                    rs?.close()
                    statement.close()
                }
            }
        }
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
