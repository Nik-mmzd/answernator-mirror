package pw.modder.answernator4.dice

/** A group of `count` identical dice with the same number of `faces`, e.g. `2d6`. */
data class DiceGroup(val count: Int, val faces: Int) {
    init {
        if (count < 1) throw DiceException("Dice count must be >= 1 (got $count)")
        // `faces` invariants are enforced by [Dice] itself when the group is rolled,
        // but we want fail-fast at parse time as well.
        Dice(faces)
    }
}