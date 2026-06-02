package pw.modder.answernator4.dice

import kotlin.random.Random

data class Dice(val faces: Int) {
    init {
        if (faces < 2) throw DiceException("Dice must have at least 2 faces (got $faces)")
        if (DiceConfig.facesLimit > 0 && faces > DiceConfig.facesLimit) {
            throw DiceLimitExceededException(
                "Faces count $faces exceeds limit ${DiceConfig.facesLimit}",
                "faces", faces, DiceConfig.facesLimit,
            )
        }
    }

    fun roll(random: Random = Random.Default, explode: Boolean = false, explodeLimit: Int = 0): Int {
        var sum = 0
        var explosions = 0
        while (true) {
            val r = random.nextInt(1, faces + 1)
            sum += r
            if (!explode || r != faces) break
            explosions++
            if (explodeLimit > 0 && explosions >= explodeLimit) break
        }
        return sum
    }
}