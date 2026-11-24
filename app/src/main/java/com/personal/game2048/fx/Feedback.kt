package com.personal.game2048.fx

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import com.personal.game2048.R
import kotlin.math.pow

/** Sound and haptics. Low-latency SoundPool; merges climb a pentatonic scale with the tile. */
class Feedback(context: Context) {
    var soundOn = true
    var hapticsOn = true
    var view: View? = null

    private val pool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val ready = HashSet<Int>()
    private val slide: Int
    private val merge: Int
    private val milestone: Int
    private val over: Int
    private val unlock: Int

    init {
        pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) synchronized(ready) { ready.add(id) } }
        slide = pool.load(context, R.raw.sfx_slide, 1)
        merge = pool.load(context, R.raw.sfx_merge, 1)
        milestone = pool.load(context, R.raw.sfx_milestone, 1)
        over = pool.load(context, R.raw.sfx_over, 1)
        unlock = pool.load(context, R.raw.sfx_unlock, 1)
    }

    fun slide() {
        play(slide, 0.5f, 1f)
        haptic(if (Build.VERSION.SDK_INT >= 27) HapticFeedbackConstants.TEXT_HANDLE_MOVE else HapticFeedbackConstants.CLOCK_TICK)
    }

    fun merge(exp: Int) {
        val step = (exp - 2).coerceIn(0, 15)
        val semitones = PENTATONIC[step % 5] + 12 * (step / 5) - 8
        play(merge, 0.8f, 2f.pow(semitones / 12f).coerceIn(0.5f, 2f))
        haptic(if (exp >= 8) HapticFeedbackConstants.VIRTUAL_KEY else HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun milestone() {
        play(milestone, 1f, 1f)
        haptic(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS)
    }

    fun unlock() = play(unlock, 0.9f, 1f)

    fun over() {
        play(over, 0.9f, 1f)
        haptic(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS)
    }

    fun tap() = haptic(HapticFeedbackConstants.KEYBOARD_TAP)

    fun release() = pool.release()

    private fun play(id: Int, volume: Float, rate: Float) {
        if (soundOn && synchronized(ready) { id in ready }) pool.play(id, volume, volume, 1, 0, rate)
    }

    private fun haptic(kind: Int) {
        if (hapticsOn) view?.performHapticFeedback(kind)
    }

    private companion object {
        val PENTATONIC = intArrayOf(0, 2, 4, 7, 9)
    }
}
