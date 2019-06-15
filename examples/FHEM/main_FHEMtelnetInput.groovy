import de.seiderer.eventerpretor.core.Engine

import de.seiderer.eventerpretor.plugins.input.InTCP
import de.seiderer.eventerpretor.plugins.output.OutMQTT
import de.seiderer.eventerpretor.plugins.output.OutSTDOUT
import de.seiderer.eventerpretor.plugins.transform.TransformFHEMstring


static Boolean init(Engine engine, String[] args) {

    InTCP tcpIn = new InTCP(engine, "tcpInNode")
    engine.addNode(tcpIn)


    TransformFHEMstring transFHEMstr = new TransformFHEMstring(engine, "transFHEMstr")
    engine.addNode(transFHEMstr)

    OutSTDOUT nodeStdoutOut = new OutSTDOUT(engine, "stdoutNode")
    engine.addNode(nodeStdoutOut)

    OutMQTT mqttOut = new OutMQTT(engine, "mqttOutNode")
    engine.addNode(mqttOut)


    tcpIn.publishTo(transFHEMstr)
    tcpIn.publishTo(mqttOut)
    tcpIn.publishTo(nodeStdoutOut)

    transFHEMstr.publishTo(nodeStdoutOut)

    return true
}