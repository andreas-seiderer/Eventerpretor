package de.seiderer.eventerpretor.plugins.base

import de.seiderer.eventerpretor.core.Engine
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue

/**
 * @author Andreas Seiderer
 */
abstract class TransformNode(engine: Engine,name:String,interval:Long) : BaseNode(engine,name,NodeType.Transformer,interval) {

    var indata : LinkedBlockingQueue<DataValue> = LinkedBlockingQueue()

    fun dataInput(data : DataValue?) {
        indata.put(data)
    }



    var subscribers: ConcurrentHashMap<String, BaseNode> = ConcurrentHashMap()

    /**
     * send data to all subscribers
     */
    protected fun dataOut(data : DataValue) {
        for (s in subscribers) {
            val node = s.value
            if (node is OutNode)
                node.dataInput(data)
            if (node is TransformNode)
                node.dataInput(data)

        }
    }

    /**
     * send data to all subscribers
     */
    protected fun dataOut(value : Any) {
        for (s in subscribers) {
            val node = s.value
            if (node is OutNode)
                node.dataInput(DataValue(engine.getCurrentDate(), value, this.nodename))
            if (node is TransformNode)
                node.dataInput(DataValue(engine.getCurrentDate(), value, this.nodename))
        }
    }

    /**
     * add OutNode as subscriber
     */
    fun publishTo( node : OutNode) {
        subscribers.put(node.nodename, node)
    }

    /**
     * add TransformNode as subscriber
     */
    fun publishTo( node : TransformNode) {
        subscribers.put(node.nodename, node)
    }
}