package de.seiderer.eventerpretor.drools

class ContextElement {

        var value: String? = null
        var name: String? = null

        var timestamp: Long = 0

        var sensorvalue: Any? = null

        var sensortype: String? = null
        var sensorname: String? = null

        var positionname: String? = null
        var positiontype: String? = null


    constructor(value: String?, name: String?, timestamp: Long, sensorvalue: Any?, sensortype: String?, sensorname: String?, positionname: String?, positiontype: String?) {
        this.value = value
        this.name = name
        this.timestamp = timestamp
        this.sensorvalue = sensorvalue
        this.sensortype = sensortype
        this.sensorname = sensorname
        this.positionname = positionname
        this.positiontype = positiontype
    }


    constructor(value: String?, name: String?, sensor: Sensor) : this(value, name, sensor.timestamp, sensor.value, sensor.sensortype, sensor.sensorname, sensor.positionname, sensor.positiontype) {
    }
}