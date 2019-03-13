package com.shiorin.iroiromemorial

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.collections.ArrayList

class CanvasView @JvmOverloads constructor(
        context: Context, atttrs: AttributeSet? = null, defStyleAttr: Int = 0, private val path: Path
) : View(context,atttrs,defStyleAttr) {


    private val pathList: ArrayList<Path>? = ArrayList<Path>()
    private var paint: Paint = Paint()



    init {
        paint.setARGB(100, 255, 0, 0)
        paint.style
        paint.isAntiAlias
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.drawPath(path,paint)

        invalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val x = x
        val y = y

        if (event != null) {
            when(event.action){
                MotionEvent.ACTION_DOWN -> {
                    path.moveTo(x,y)
                    invalidate()
                }
                MotionEvent.ACTION_MOVE -> {
                    path.lineTo(x,y)
                    invalidate()
                }
                MotionEvent.ACTION_UP ->{
                    path.lineTo(x,y)
                    invalidate()
                }
            }
        }
        return true
    }





}
