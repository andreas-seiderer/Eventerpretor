import de.seiderer.eventerpretor.core.Engine

import de.seiderer.eventerpretor.plugins.input.InSerial
import de.seiderer.eventerpretor.plugins.output.OutSTDOUT
import de.seiderer.eventerpretor.plugins.transform.TransformAllowDatatype
import de.seiderer.eventerpretor.plugins.transform.TransformSampleRate
import de.seiderer.eventerpretor.plugins.transform.TransformSplitToArray


static Boolean init(Engine engine, String[] args) {

    InSerial nodeSerialIn = new InSerial(engine,"serialInNode");
    engine.addNode(nodeSerialIn);

    TransformSampleRate nodeSR = new TransformSampleRate(engine,"srNode");
    engine.addNode(nodeSR);

    TransformSplitToArray nodeSplit = new TransformSplitToArray(engine, "splitNode");
    engine.addNode(nodeSplit);

    TransformAllowDatatype nodeAllow = new TransformAllowDatatype(engine, "allowNode");
    engine.addNode(nodeAllow);

    OutSTDOUT nodeStdoutOut = new OutSTDOUT(engine, "stdoutNode");
    engine.addNode(nodeStdoutOut);

    OutSTDOUT nodeStdoutOut2 = new OutSTDOUT(engine, "stdoutNode2");
    engine.addNode(nodeStdoutOut2);

    nodeSerialIn.publishTo(nodeSplit);
    nodeSplit.publishTo(nodeAllow);
    nodeAllow.publishTo(nodeStdoutOut);

    nodeSerialIn.publishTo(nodeSR);
    nodeSR.publishTo(nodeStdoutOut2);

    return true;
}