import de.seiderer.eventerpretor.core.Engine

import de.seiderer.eventerpretor.plugins.input.InMQTT
import de.seiderer.eventerpretor.plugins.transform.TransformZigbee2Mqttstring
import de.seiderer.eventerpretor.plugins.transform.TransformSensorModel
import de.seiderer.eventerpretor.plugins.transform.TransformDrools
import de.seiderer.eventerpretor.plugins.transform.TransformAllowDatatype
import de.seiderer.eventerpretor.plugins.transform.TransformJSONtoMAP
import de.seiderer.eventerpretor.plugins.output.OutMQTT
import de.seiderer.eventerpretor.plugins.output.OutSTDOUT


static Boolean init(Engine engine, String[] args) {

    def mqttIn = new InMQTT(engine, "mqttInNode")
    engine.addNode(mqttIn)

    def zigbee2mqttTransform = new TransformZigbee2Mqttstring(engine, "zigbee2mqttTransform")
    engine.addNode(zigbee2mqttTransform)

    def sensorModel = new TransformSensorModel(engine, "sensorModel")
    engine.addNode(sensorModel)

    def transDrools = new TransformDrools(engine, "transDrools")
    engine.addNode(transDrools)

    def transDataType = new TransformAllowDatatype(engine, "transDataType")
    engine.addNode(transDataType)

    def transjsontomap = new TransformJSONtoMAP(engine, "transJSON2MAP")
    engine.addNode(transjsontomap)

    def nodeStdoutOut = new OutSTDOUT(engine, "stdoutNode")
    engine.addNode(nodeStdoutOut)

    def mqttOut = new OutMQTT(engine, "mqttOutNode")
    engine.addNode(mqttOut)


    mqttIn.publishTo(zigbee2mqttTransform)

    zigbee2mqttTransform.publishTo(sensorModel)

    sensorModel.publishTo(transDrools)

    transDrools.publishTo(transDataType)

    transDataType.publishTo(transjsontomap)

    transjsontomap.publishTo(nodeStdoutOut)
    transjsontomap.publishTo(mqttOut)

    return true
}