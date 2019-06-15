package de.seiderer.eventerpretor.plugins.base

import de.seiderer.eventerpretor.core.Engine
import java.util.concurrent.LinkedBlockingQueue

/**
 * @author Andreas Seiderer
 */
abstract class OutNode(engine:Engine, name:String, interval:Long) : BaseNode(engine,name, NodeType.Output,interval) {

    var indata : LinkedBlockingQueue<DataValue> = LinkedBlockingQueue()

    fun dataInput(data : DataValue?) {
        indata.put(data)
    }

}