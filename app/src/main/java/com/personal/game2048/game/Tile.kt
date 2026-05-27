package com.personal.game2048.game

data class Tile(
    val value: Int,
    val row: Int,
    val col: Int,
    val id: Long = System.nanoTime(),
    val isNew: Boolean = false,
    val isMerged: Boolean = false
)
