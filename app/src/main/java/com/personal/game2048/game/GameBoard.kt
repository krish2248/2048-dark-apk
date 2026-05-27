package com.personal.game2048.game

import kotlin.random.Random

enum class Direction { UP, DOWN, LEFT, RIGHT }

data class BoardState(
    val grid: List<List<Int>>,
    val score: Int
)

class GameBoard {
    var grid = Array(4) { IntArray(4) }
        private set
    var score = 0
        private set
    var hasWon = false
        private set
    var winAcknowledged = false

    fun reset() {
        grid = Array(4) { IntArray(4) }
        score = 0
        hasWon = false
        winAcknowledged = false
        spawnTile()
        spawnTile()
    }

    fun loadState(flatGrid: List<Int>, savedScore: Int) {
        grid = Array(4) { r -> IntArray(4) { c -> flatGrid[r * 4 + c] } }
        score = savedScore
        hasWon = flatGrid.any { it >= 2048 }
        if (hasWon) winAcknowledged = true
    }

    fun toFlatList(): List<Int> = grid.flatMap { it.toList() }

    fun captureState(): BoardState =
        BoardState(grid.map { it.toList() }, score)

    fun restoreState(state: BoardState) {
        grid = Array(4) { r -> IntArray(4) { c -> state.grid[r][c] } }
        score = state.score
    }

    fun move(direction: Direction): Boolean {
        val before = Array(4) { grid[it].copyOf() }

        when (direction) {
            Direction.LEFT -> moveLeft()
            Direction.RIGHT -> moveRight()
            Direction.UP -> moveUp()
            Direction.DOWN -> moveDown()
        }

        val changed = !gridsEqual(before, grid)
        if (changed) {
            spawnTile()
            if (!hasWon && grid.any { row -> row.any { it == 2048 } }) {
                hasWon = true
            }
        }
        return changed
    }

    private fun moveLeft() {
        for (r in 0 until 4) {
            val row = grid[r].filter { it != 0 }.toMutableList()
            val merged = mutableListOf<Int>()
            var i = 0
            while (i < row.size) {
                if (i + 1 < row.size && row[i] == row[i + 1]) {
                    val mergedVal = row[i] * 2
                    merged.add(mergedVal)
                    score += mergedVal
                    i += 2
                } else {
                    merged.add(row[i])
                    i++
                }
            }
            while (merged.size < 4) merged.add(0)
            grid[r] = merged.toIntArray()
        }
    }

    private fun moveRight() {
        for (r in 0 until 4) {
            val row = grid[r].filter { it != 0 }.toMutableList()
            val merged = mutableListOf<Int>()
            var i = row.size - 1
            while (i >= 0) {
                if (i - 1 >= 0 && row[i] == row[i - 1]) {
                    val mergedVal = row[i] * 2
                    merged.add(0, mergedVal)
                    score += mergedVal
                    i -= 2
                } else {
                    merged.add(0, row[i])
                    i--
                }
            }
            while (merged.size < 4) merged.add(0, 0)
            grid[r] = merged.toIntArray()
        }
    }

    private fun moveUp() {
        for (c in 0 until 4) {
            val col = (0 until 4).map { grid[it][c] }.filter { it != 0 }.toMutableList()
            val merged = mutableListOf<Int>()
            var i = 0
            while (i < col.size) {
                if (i + 1 < col.size && col[i] == col[i + 1]) {
                    val mergedVal = col[i] * 2
                    merged.add(mergedVal)
                    score += mergedVal
                    i += 2
                } else {
                    merged.add(col[i])
                    i++
                }
            }
            while (merged.size < 4) merged.add(0)
            for (r in 0 until 4) grid[r][c] = merged[r]
        }
    }

    private fun moveDown() {
        for (c in 0 until 4) {
            val col = (0 until 4).map { grid[it][c] }.filter { it != 0 }.toMutableList()
            val merged = mutableListOf<Int>()
            var i = col.size - 1
            while (i >= 0) {
                if (i - 1 >= 0 && col[i] == col[i - 1]) {
                    val mergedVal = col[i] * 2
                    merged.add(0, mergedVal)
                    score += mergedVal
                    i -= 2
                } else {
                    merged.add(0, col[i])
                    i--
                }
            }
            while (merged.size < 4) merged.add(0, 0)
            for (r in 0 until 4) grid[r][c] = merged[r]
        }
    }

    fun spawnTile() {
        val empty = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until 4) {
            for (c in 0 until 4) {
                if (grid[r][c] == 0) empty.add(r to c)
            }
        }
        if (empty.isEmpty()) return
        val (r, c) = empty[Random.nextInt(empty.size)]
        grid[r][c] = if (Random.nextFloat() < 0.9f) 2 else 4
    }

    fun isGameOver(): Boolean {
        for (r in 0 until 4) {
            for (c in 0 until 4) {
                if (grid[r][c] == 0) return false
                if (c + 1 < 4 && grid[r][c] == grid[r][c + 1]) return false
                if (r + 1 < 4 && grid[r][c] == grid[r + 1][c]) return false
            }
        }
        return true
    }

    fun highestTile(): Int = grid.maxOf { it.max() }

    private fun gridsEqual(a: Array<IntArray>, b: Array<IntArray>): Boolean {
        for (r in 0 until 4) {
            for (c in 0 until 4) {
                if (a[r][c] != b[r][c]) return false
            }
        }
        return true
    }
}
