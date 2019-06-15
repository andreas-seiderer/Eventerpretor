package de.seiderer.eventerpretor.plugins.base

/**
 * @author Andreas Seiderer
 */
enum class NodeType constructor(val value : Int) {
    Input(0),
    Transformer(1),
    Output(2)
}