package de.seiderer.eventerpretor.plugins.base

import de.seiderer.eventerpretor.core.Engine
import java.util.concurrent.ConcurrentHashMap

abstract class InNode(engine:Engine, name:String,interval:Long) : BaseNode(engine,name,NodeType.Input,interval) {

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
     * send data to all running subscribers
     */
    protected fun dataOut(value : Any) {
        for (s in subscribers) {
            val node = s.value
            if (node is OutNode && node.getRunningFlag())
                node.dataInput(DataValue(engine.getCurrentDate(), value, this.nodename))
            if (node is TransformNode && node.getRunningFlag())
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