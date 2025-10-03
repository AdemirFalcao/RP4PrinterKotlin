package com.honeywell.rp4printer.signature

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class SignatureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val path = Path()
    private val paint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 5f
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    private val canvasPaint = Paint(Paint.DITHER_FLAG)
    private var canvasBitmap: Bitmap? = null
    private var canvas: Canvas? = null

    private var lastTouchX = 0f
    private var lastTouchY = 0f

    init {
        setBackgroundColor(Color.WHITE)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(canvasBitmap!!)
        canvas?.drawColor(Color.WHITE)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvasBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, canvasPaint)
        }
        canvas.drawPath(path, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                path.moveTo(x, y)
                lastTouchX = x
                lastTouchY = y
            }
            MotionEvent.ACTION_MOVE -> {
                path.quadTo(lastTouchX, lastTouchY, (x + lastTouchX) / 2, (y + lastTouchY) / 2)
                lastTouchX = x
                lastTouchY = y
            }
            MotionEvent.ACTION_UP -> {
                canvas?.drawPath(path, paint)
                path.reset()
            }
        }

        invalidate()
        return true
    }

    fun clear() {
        path.reset()
        canvas?.drawColor(Color.WHITE)
        invalidate()
    }

    fun getSignatureBitmap(): Bitmap? {
        return canvasBitmap?.copy(Bitmap.Config.ARGB_8888, false)
    }

    fun isEmpty(): Boolean {
        val bitmap = canvasBitmap ?: return true
        val emptyBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(emptyBitmap)
        canvas.drawColor(Color.WHITE)
        
        return bitmap.sameAs(emptyBitmap)
    }
}



