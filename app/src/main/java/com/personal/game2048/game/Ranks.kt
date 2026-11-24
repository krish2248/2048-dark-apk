package com.personal.game2048.game

enum class RankFamily { PEBBLE, IRON, STEEL, EMERALD, OBSIDIAN, APEX, TITAN, INFINITE }

/** Skill tier, decided purely by best Classic score, so anyone's tier can be derived from the board. */
enum class Rank(val family: RankFamily, val division: Int, val title: String, val minScore: Long) {
    PEBBLE_1(RankFamily.PEBBLE, 1, "Pebble I", 0),
    PEBBLE_2(RankFamily.PEBBLE, 2, "Pebble II", 500),
    PEBBLE_3(RankFamily.PEBBLE, 3, "Pebble III", 1_000),
    IRON_1(RankFamily.IRON, 1, "Iron I", 2_000),
    IRON_2(RankFamily.IRON, 2, "Iron II", 3_500),
    IRON_3(RankFamily.IRON, 3, "Iron III", 5_000),
    STEEL_1(RankFamily.STEEL, 1, "Steel I", 7_500),
    STEEL_2(RankFamily.STEEL, 2, "Steel II", 10_000),
    STEEL_3(RankFamily.STEEL, 3, "Steel III", 15_000),
    EMERALD_1(RankFamily.EMERALD, 1, "Emerald I", 20_000),
    EMERALD_2(RankFamily.EMERALD, 2, "Emerald II", 27_500),
    EMERALD_3(RankFamily.EMERALD, 3, "Emerald III", 35_000),
    OBSIDIAN_1(RankFamily.OBSIDIAN, 1, "Obsidian I", 45_000),
    OBSIDIAN_2(RankFamily.OBSIDIAN, 2, "Obsidian II", 60_000),
    OBSIDIAN_3(RankFamily.OBSIDIAN, 3, "Obsidian III", 80_000),
    APEX(RankFamily.APEX, 0, "Apex", 120_000),
    TITAN(RankFamily.TITAN, 0, "Titan", 200_000),
    INFINITE(RankFamily.INFINITE, 0, "Infinite", 400_000);

    val next: Rank? get() = entries.getOrNull(ordinal + 1)

    companion object {
        fun of(score: Long): Rank = entries.last { score >= it.minScore }

        /** 0..1 progress from this score's tier towards the next one. */
        fun progress(score: Long): Float {
            val r = of(score)
            val n = r.next ?: return 1f
            return ((score - r.minScore).toFloat() / (n.minScore - r.minScore)).coerceIn(0f, 1f)
        }
    }
}
