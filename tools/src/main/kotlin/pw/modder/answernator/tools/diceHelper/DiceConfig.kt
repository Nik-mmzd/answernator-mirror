package pw.modder.answernator.tools.diceHelper

import java.util.*

object DiceConfig {
    private val props = Properties()
    init {
        props.load(javaClass.classLoader.getResourceAsStream("properties/dice.properties"))
    }

    val dicesLimit = props.getProperty("dice.dicesLimit").toInt()
    val throwsLimit = props.getProperty("dice.throwsLimit").toInt()
    val dotsLimit = props.getProperty("dice.dotsLimit").toInt()
    val modLimit = props.getProperty("dice.modLimit").toInt()
    val triesLimit = props.getProperty("dice.triesLimit").toInt()

    val defautDicePips = props.getProperty("dice.default.pips").toInt()
    val defaultDices = props.getProperty("dice.default.dices").toInt()
    val defaultTries = props.getProperty("dice.default.tries").toInt()
    val defaultModifier = props.getProperty("dice.default.mod").toInt()
}