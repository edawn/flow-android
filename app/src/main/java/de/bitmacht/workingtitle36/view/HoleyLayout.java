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

package de.bitmacht.workingtitle36.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import de.bitmacht.workingtitle36.R;

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
public class HoleyLayout extends RelativeLayout {

    private final Paint holePaint = new Paint();
    private final ColorMatrixColorFilter inverseAlphaColorFilter =
            new ColorMatrixColorFilter(new float[]{1,0,0,0,0, 0,1,0,0,0, 0,0,1,0,0, 0,0,0,-1,255});
    private final Paint inverseAlphaPaint = new Paint();
    private int screenColor;
    private int inverseAlphaScreenColor;
    private Integer restoreCount = null;

    public HoleyLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public HoleyLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public HoleyLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        holePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));
        inverseAlphaPaint.setColorFilter(inverseAlphaColorFilter);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.HoleyLayout);
        final int N = a.getIndexCount();
        for (int i = 0; i < N; i++) {
            int attr = a.getIndex(i);
            switch (attr) {
                case R.styleable.HoleyLayout_screenColor:
                    screenColor = a.getColor(attr, Color.argb(100, 0, 0, 0));
                    break;
            }
        }
        a.recycle();

        inverseAlphaScreenColor = (screenColor & 0x00ffffff) | (0xff000000 ^ (screenColor & 0xff000000));
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        int index = indexOfChild(child);

        boolean childIsPunching = ((LayoutParams) child.getLayoutParams()).punch;

        boolean result = false;
        if (!childIsPunching) {
            if (restoreCount != null) {
                canvas.restoreToCount(restoreCount);
                restoreCount = null;
            }
            result = super.drawChild(canvas, child, drawingTime);
        } else if (restoreCount != null) {
            result = super.drawChild(canvas, child, drawingTime);
        }

        return result;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        restoreCount = canvas.saveLayer(null, inverseAlphaPaint, Canvas.ALL_SAVE_FLAG);
        canvas.drawColor(inverseAlphaScreenColor);

        super.dispatchDraw(canvas);

        if (restoreCount != null) {
            canvas.restoreToCount(restoreCount);
        }
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        return super.generateLayoutParams(lp);
    }

    public static class LayoutParams extends RelativeLayout.LayoutParams {
        public boolean punch = false;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.HoleyLayout_Layout);
            final int N = a.getIndexCount();
            for (int i = 0; i < N; i++) {
                int attr = a.getIndex(i);
                switch (attr) {
                    case R.styleable.HoleyLayout_Layout_layout_punch:
                        punch = a.getBoolean(attr, false);
                        break;
                }
            }
            a.recycle();
        }

        public LayoutParams(int w, int h) {
            super(w, h);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        @TargetApi(Build.VERSION_CODES.KITKAT)
        public LayoutParams(RelativeLayout.LayoutParams source) {
            super(source);
        }
    }
}
