package pw.modder.answernator.commands.utils

import kotlin.random.Random

/* function createGamesLists( _filename )
  if fs.existsSync(_filename) then
    local file = io.open(_filename)
    local gameNameLists = {}
    local curList = {}

    for str in file:lines() do
      if str == "----" then
        table.insert(gameNameLists, curList)
        curList = {}
      else
        table.insert(curList, str)
      end
    end
    if #curList > 0 then table.insert(gameNameLists, curList) end
    return gameNameLists
  else
    printColored("No file exists: ".._filename, "ERR")
    return {{"error"},{"loading"},{_filename}}
  end
end

function getGameName( nameLists, wordsCount )
  wordsCount = wordsCount or math.random(2, 3)

  local badMatchList, words = {}, {}
  local Tnum = wordsCount == 3 and 1 or math.random(1, 2)
  repeat
    local word = nameLists[#words+Tnum][math.random(#nameLists[#words+Tnum])]
    if string.find(word, "^") then
      local Tword, badWords = string.match(word, "([^%^]+)(.*)")
      word = Tword or word
      if badWords then
        for badWord in string.gmatch(badWords ,"[^%|]+") do
          table.insert(badMatchList, badWord)
        end
      end
    end
    if not table.find(badMatchList, word) and not table.find(words, word) then table.insert(words, word) end
  until #words == wordsCount
  return table.concat(words, " ")
end
*/
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