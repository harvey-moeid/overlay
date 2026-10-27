package com.byga.smartoverlay.overlay

/** Commit a touch hold on release, after every delivered movement has been inspected.
 * A Handler long-press timer can overtake queued MOVE events on a busy device and
 * stop the service in the middle of a drag. Accessibility long-click is separate.
 */
internal class BubbleGesture(private val slop: Int, private val holdMillis: Long) {
    enum class Release { TAP, HOLD, DRAG, NONE }
    private var active = false
    private var startX = 0f
    private var startY = 0f
    private var downTime = 0L
    var dragged = false
        private set

    fun down(x: Float, y: Float, time: Long) {
        active = true; dragged = false; startX = x; startY = y; downTime = time
    }
    fun move(x: Float, y: Float) {
        if (active && kotlin.math.abs(x-startX)+kotlin.math.abs(y-startY)>slop) dragged = true
    }
    fun up(x: Float, y: Float, time: Long): Release {
        if (!active) return Release.NONE
        move(x,y)
        active = false
        return when {
            dragged -> Release.DRAG
            time-downTime >= holdMillis -> Release.HOLD
            else -> Release.TAP
        }
    }
    fun cancel() { active = false; dragged = false }
}
