import de.seiderer.eventerpretor.core.Engine

import de.seiderer.eventerpretor.plugins.input.InTimer
import de.seiderer.eventerpretor.plugins.output.OutSTDOUT


static Boolean init(Engine engine, String[] args) {

    InTimer nodeTimerIn = new InTimer(engine,"timerNode")
    engine.addNode(nodeTimerIn);

    InTimer nodeTimerIn2 = new InTimer(engine,"timerNode2")
    engine.addNode(nodeTimerIn2);


    OutSTDOUT nodeStdoutOut = new OutSTDOUT(engine, "stdoutNode");
    engine.addNode(nodeStdoutOut);

    nodeTimerIn.publishTo(nodeStdoutOut)
    nodeTimerIn2.publishTo(nodeStdoutOut)

    return true;
}