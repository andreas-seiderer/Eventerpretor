package de.seiderer.eventerpretor.plugins.output

import de.seiderer.eventerpretor.core.Engine
import de.seiderer.eventerpretor.plugins.base.OutNode
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.sql.Timestamp
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

/**
 * @author Andreas Seiderer
 *
 * DataValue.value should arrive in format:
 * sensorname
 * sensortype
 * positionname
 * positiontype
 * readingname
 * reading
 * provider
 *
 * A table sensors and for each sensorname is created.
 */
class OutSqliteAutoDataInsert(engine: Engine, name:String) : OutNode(engine,name,0) {
    var dbConnection: Connection by Delegates.notNull()

    override fun threadedTask() {
        val value = indata.poll(1000, TimeUnit.MILLISECONDS)
        if (value != null) {
            if (value.value is HashMap<*, *>) {

                var query : String

                //check if sensor table exists -> if not create

                val valc = value.value as HashMap<String,Any?>

                val sensorname = valc["sensorname"]
                val sensortype = valc["sensortype"]
                val positionname = valc["positionname"]
                val positiontype = valc["positiontype"]
                val provider = valc["provider"]

                var _id_sensor : Long = -1

                if (sensorname is String && sensortype is String && positionname is String && positiontype is String && provider is String) {
                    //check if sensor is in sensor table
                    query = String.format("SELECT _id FROM sensors WHERE " +
                            "sensorname = ? AND " +
                            "positionname = ? AND " +
                            "provider = ? LIMIT 1")
                    var prep = dbConnection.prepareStatement(query)


                    prep.setString(1, sensorname)
                    prep.setString(2, positionname)
                    prep.setString(3, provider)

                    val rs = prep.executeQuery()

                    if (rs.next()) {
                        _id_sensor = rs.getLong("_id")
                    }

                    rs.close()
                    prep.close()

                    //did not find sensor -> insert, create sensor data table
                    if (_id_sensor == -1L) {
                        query = String.format("INSERT INTO \"%s\"" + "(" +
                                "sensorname, " +
                                "sensortype, " +
                                "positionname, " +
                                "positiontype, " +
                                "provider) values" + "(?,?,?,?,?)", "sensors")
                        prep = dbConnection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)

                        prep.setString(1, sensorname)
                        prep.setString(2, sensortype)
                        prep.setString(3, positionname)
                        prep.setString(4, positiontype)
                        prep.setString(5, provider)

                        prep.executeUpdate()

                        val generatedKeys = prep.generatedKeys
                        if (generatedKeys.next()) {
                            _id_sensor = generatedKeys.getLong(1)
                        }
                        prep.close()


                        //create table for sensor data
                        query = String.format("CREATE TABLE IF NOT EXISTS \"%s\"(" +
                                "_id INTEGER PRIMARY KEY, " +
                                "timestamp INTEGER, " +
                                "readingname TEXT, " +
                                "reading_str TEXT, " +
                                "reading_num REAL, " +
                                "_id_sensor INTEGER, " +
                                "foreign key(_id_sensor) REFERENCES sensors(_id))", valc["sensorname"])
                        prep = dbConnection.prepareStatement(query)
                        prep.executeUpdate()

                        query = String.format("CREATE INDEX IF NOT EXISTS \"IDX_%s\" ON \"%s\"(timestamp);", valc["sensorname"], valc["sensorname"])
                        prep = dbConnection.prepareStatement(query)
                        prep.executeUpdate()

                        prep.close()
                    }
                }

                query = String.format("INSERT INTO \"%s\"" + "(" +
                        "timestamp, " +
                        "readingname, " +
                        "reading_str, " +
                        "reading_num, " +
                        "_id_sensor) values" + "(?,?,?,?,?)", valc["sensorname"])

                val preparedStatement = dbConnection.prepareStatement(query)

                preparedStatement.setTimestamp(1, Timestamp(value.timestamp.time))

                val readingname = valc["readingname"]
                if (readingname is String)
                    preparedStatement.setString(2, readingname)

                //reading can not just be of type String!
                val reading = valc["reading"]
                if (reading is String)
                    preparedStatement.setString(3, reading)
                else
                    preparedStatement.setNull(3, java.sql.Types.VARCHAR)


                var convReading = 0f

                if (reading is Int)
                    convReading = reading.toFloat()

                if (reading is Long)
                    convReading = reading.toFloat()

                if (reading is Double)
                    convReading = reading.toFloat()


                if (reading is Int || reading is Double || reading is Float) {
                    preparedStatement.setFloat(4, convReading)
                }
                else
                    preparedStatement.setNull(4, java.sql.Types.REAL)

                preparedStatement.setLong(5, _id_sensor)

                preparedStatement.executeUpdate()
                preparedStatement.close()

                dbConnection.commit()

                println(value.toString())
            }
        }
    }

    override fun setConfig() {

        val path : String = engine.workingdir + "/options_" + this.nodename + ".json"

        //read options from file if it exists; if not create new with default values
        if (File(path).exists())
            opts.fromJsonFile(path)
        else {
            opts.add("filename", "data.db", "database filename")

            opts.toJsonFile(path)
        }
    }

    override fun start() {
        dbConnection = DriverManager.getConnection("jdbc:sqlite:" + opts.getStringVal("filename"))
        dbConnection.autoCommit = false

        val query = String.format("CREATE TABLE IF NOT EXISTS \"%s\"(" +
                "_id INTEGER primary key, " +
                "sensorname TEXT, " +
                "sensortype TEXT, " +
                "positionname TEXT, " +
                "positiontype TEXT, " +
                "provider TEXT)", "sensors")

        val preparedStatement = dbConnection.prepareStatement(query)
        preparedStatement.executeUpdate()
        preparedStatement.close()

        dbConnection.commit()
    }

    override fun stop() {
        dbConnection.close()
    }

    override fun kill() {

    }

}