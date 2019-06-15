import de.seiderer.eventerpretor.core.Engine

import de.seiderer.eventerpretor.plugins.input.InTimer
import de.seiderer.eventerpretor.plugins.transform.TransformGroovy
import de.seiderer.eventerpretor.plugins.output.OutSTDOUT


static Boolean init(Engine engine, String[] args) {

    InTimer nodeTimerIn = new InTimer(engine,"timerNode");
    engine.addNode(nodeTimerIn);

    TransformGroovy groovyTrans = new TransformGroovy(engine, "groovyTrans1");
    engine.addNode(groovyTrans);


    OutSTDOUT nodeStdoutOut = new OutSTDOUT(engine, "stdoutNode");
    engine.addNode(nodeStdoutOut);

    nodeTimerIn.publishTo(groovyTrans);
    groovyTrans.publishTo(nodeStdoutOut);

    return true;
}