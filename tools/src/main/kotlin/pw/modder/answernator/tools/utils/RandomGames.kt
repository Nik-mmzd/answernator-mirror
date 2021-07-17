package pw.modder.answernator.tools.utils

import kotlin.random.Random

class RandomGames {
    private val words: List<List<String>>
    private val random = Random(System.currentTimeMillis())

    init {
        val reader = javaClass.classLoader.getResourceAsStream("gameslist.txt").reader()
        val lines = reader.use {
            it.readLines()
        }
        val firstSep = lines.indexOf("----")
        val secondSep = lines.lastIndexOf("----")

        words = listOf(
            lines.subList(0, firstSep - 1),
            lines.subList(firstSep + 1, secondSep - 1),
            lines.subList(secondSep + 1, lines.lastIndex)
        )
    }

    fun getRandomGame(): String {
        val goodWords = mutableListOf<String>()
        val badWords = mutableListOf<String>()
        val wordsCount = random.nextInt(2, 4)
        var i = if (wordsCount == 3) 0 else random.nextInt(0, 2) // first
        do {
            val word = words[i].random(random).split('^', limit = 2)
            if (word[0] in badWords) continue

            word.getOrNull(1)?.run {
                badWords.addAll(split('|'))
            }
            goodWords.add(word[0])
            i++
        } while (goodWords.size < wordsCount)

        return goodWords.joinToString(" ")
    }
}