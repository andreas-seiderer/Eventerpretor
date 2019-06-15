package de.seiderer.eventerpretor.plugins.manager

import com.fazecast.jSerialComm.SerialPort

/**
 * @author Andreas Seiderer
 * Manage all serial devices so that InSerial and OutSerial can share devices with the same port
 */
class SerialDevices {

    companion object {
        private var sensorHashmap : HashMap<String,SerialPort> = HashMap()

        fun addDevice(name:String, port:SerialPort) {
            if (!sensorHashmap.containsKey(name))
                sensorHashmap[name] = port
        }

        fun deviceExists(name:String):Boolean {
            return sensorHashmap.containsKey(name)
        }

        fun getDevice(name:String):SerialPort? {
            return sensorHashmap[name]
        }

        fun getDeviceList():Array<SerialPort> {
            return SerialPort.getCommPorts()
        }

        fun removeDevice(name:String): Boolean {
            val entry = sensorHashmap[name]
            if (entry != null) {
                if (entry.isOpen)
                    entry.closePort()
                sensorHashmap.remove(name)

                return true
            }
            return false
        }

        fun closeDevice(name:String):Boolean {
            val entry = sensorHashmap[name]
            if (entry != null) {
                if (entry.isOpen)
                    entry.closePort()

                return true
            }
            return false
        }
    }
}