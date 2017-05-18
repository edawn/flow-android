/*
 * Copyright 2016 Kamil Sartys
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.bitmacht.workingtitle36.view

import android.annotation.TargetApi
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout

import de.bitmacht.workingtitle36.R

/**
 * An extended RelativeLayout that is completely filled with a (usually partially transparent) color.
 * The attribute screenColor defines the color of that background, which is unrelated to the View's
 * background/backgroundDrawable.
 * This background can be made more transparent by designated children having set the attribute
 * layout_punch to true.
 * In that case the alpha of that child will reduce the alpha of the background. For instance a
 * pixel with an alpha of 1 will make the background completely transparent.
 * Conversely an alpha value of 0 will leave the background's alpha unchanged.
 */
class HoleyLayout : RelativeLayout {

    private val holePaint = Paint()
    private val inverseAlphaColorFilter = ColorMatrixColorFilter(floatArrayOf(1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, -1f, 255f))
    private val inverseAlphaPaint = Paint()
    private var screenColor: Int = 0
    private var inverseAlphaScreenColor: Int = 0
    private var restoreCount: Int? = null

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet) {
        holePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
        inverseAlphaPaint.colorFilter = inverseAlphaColorFilter

        context.obtainStyledAttributes(attrs, R.styleable.HoleyLayout).also {
            screenColor = it.getColor(R.styleable.HoleyLayout_screenColor, Color.argb(100, 0, 0, 0))
        }.recycle()

        inverseAlphaScreenColor = screenColor and 0x00ffffff or (0xff000000.toInt() xor (screenColor and 0xff000000.toInt()))
    }

    override fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
        val index = indexOfChild(child)

        val childIsPunching = (child.layoutParams as LayoutParams).punch

        var result = false
        if (!childIsPunching) {
            if (restoreCount != null) {
                canvas.restoreToCount(restoreCount!!)
                restoreCount = null
            }
            result = super.drawChild(canvas, child, drawingTime)
        } else if (restoreCount != null) {
            result = super.drawChild(canvas, child, drawingTime)
        }

        return result
    }

    override fun dispatchDraw(canvas: Canvas) {
        restoreCount = canvas.saveLayer(null, inverseAlphaPaint, Canvas.ALL_SAVE_FLAG)
        canvas.drawColor(inverseAlphaScreenColor)

        super.dispatchDraw(canvas)

        if (restoreCount != null) {
            canvas.restoreToCount(restoreCount!!)
        }
    }

    override fun generateLayoutParams(attrs: AttributeSet): LayoutParams {
        return LayoutParams(context, attrs)
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams): Boolean {
        return p is LayoutParams
    }

    override fun generateLayoutParams(lp: ViewGroup.LayoutParams): ViewGroup.LayoutParams {
        return super.generateLayoutParams(lp)
    }

    class LayoutParams : RelativeLayout.LayoutParams {
        var punch = false

        constructor(c: Context, attrs: AttributeSet) : super(c, attrs) {
            c.obtainStyledAttributes(attrs, R.styleable.HoleyLayout_Layout).also {
                punch = it.getBoolean(R.styleable.HoleyLayout_Layout_layout_punch, false)
            }.recycle()
        }

        constructor(w: Int, h: Int) : super(w, h)

        constructor(source: ViewGroup.LayoutParams) : super(source)

        constructor(source: ViewGroup.MarginLayoutParams) : super(source)

        @TargetApi(Build.VERSION_CODES.KITKAT)
        constructor(source: RelativeLayout.LayoutParams) : super(source)
    }
}
