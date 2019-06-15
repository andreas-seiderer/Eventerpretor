package de.seiderer.eventerpretor.drools

class Sensor {
    var value: Any? = null

    var timestamp: Long = 0

    var sensortype: String? = null
    var sensorname: String? = null

    var positiontype: String? = null
    var positionname: String? = null


    constructor(value: Any?, timestamp: Long, sensortype: String?, sensorname: String?, positionname: String?, positiontype: String?) {
        this.value = value
        this.timestamp = timestamp
        this.sensortype = sensortype
        this.sensorname = sensorname
        this.positionname = positionname
        this.positiontype = positiontype
    }

}