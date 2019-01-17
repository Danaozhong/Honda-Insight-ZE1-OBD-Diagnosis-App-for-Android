package com.texelography.hybridinsight;

/**
 * Created by Clemens on 12.12.2017.
 */

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.support.v4.content.ContextCompat;
import android.content.res.TypedArray;
import android.util.TypedValue;

import com.texelography.hybridinsight.datapool.DataPoolOBDData;
import com.texelography.hybridinsight.datapool.data_pool;

public class DataView extends View
{
    /* Properties */
    private float mLeft = 0;
    private float mTop = 0;
    private float mWidth = 0.0f;
    private float mHeight = 0.0f;
    private float mMin = 0.0f;
    private float mMax = 0.0f;
    private float mZero = 0.0f;
    private float mValue = 0.0f;
    private String mCaption = "";
    private String mUnit = "";

    private float mPosX = 0;
    private float mPosY = 0;

    private int mBarColor2;
    private Paint mBarColor;
    private Paint mFrameColor;
    private Paint mFontColor;

    Context context;


    public DataView(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public DataView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        // Read values from attributes
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.DataView,
                0, 0);

        try {
            mMin = a.getFloat(R.styleable.DataView_data_view_min, 0.0f);
            mMax = a.getFloat(R.styleable.DataView_data_view_max, 1.0f);
            mZero = a.getFloat(R.styleable.DataView_zero, 0.0f);
            mValue = a.getFloat(R.styleable.DataView_data_view_value, 0.3f);
            mCaption = a.getString(R.styleable.DataView_caption);
            if (null == mCaption) { mCaption = "data"; }
            mUnit = a.getString(R.styleable.DataView_data_view_unit);
            if (null == mUnit) { mUnit = "hp"; }

            //mBarColor2 = a.getColor(R.styleable.DataView_barColor, 0x00FF00);
            ColorStateList barColorList = a.getColorStateList(R.styleable.DataView_barColor);
            mBarColor2 = android.R.color.holo_green_light; //0x00FF00; // Default value
            if (null != barColorList)
            {
               // mBarColor2 = barColorList.getDefaultColor();
            }
        } finally {
            a.recycle();
        }

        init();
    }

    public void init(){

        this.mBarColor = new Paint();
        this.mBarColor.setColor(ContextCompat.getColor(context, android.R.color.holo_green_light)); //getResources().getColor(R.color.id));
        this.mBarColor.setAntiAlias(true);
        this.mBarColor.setStrokeWidth(3);

        this.mFrameColor = new Paint();
        this.mFrameColor.setColor(ContextCompat.getColor(context, android.R.color.black)); //getResources().getColor(R.color.id));
        this.mFrameColor.setAntiAlias(true);
        this.mFrameColor.setStrokeWidth(3);

        this.mFontColor = new Paint();
        this.mFontColor.setColor(ContextCompat.getColor(context, android.R.color.black)); //getResources().getColor(R.color.id));
        this.mFontColor.setStrokeWidth(3);
    }

    @Override
    public void onMeasure(int widthMeasureSpec,int heightMeasureSpec)
    {
        this.mWidth = (float)widthMeasureSpec;
        this.mHeight = (float)heightMeasureSpec;
        setMeasuredDimension((int)this.mWidth, (int)this.mHeight);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();
        //canvas.translate(mPosX, mPosY);

        float mPadding = 5.0f;
        this.drawHorizontalProgressBar(
                canvas, mPadding, mPadding, this.getWidth() - 2.0f*mPadding, this.getHeight() - 2.0f*mPadding,
                mMin, mMax, mZero, mValue);

        canvas.restore();
    }


    public void setMin(float value) { this.mMin = value; }
    public void setMax(float value) { this.mMax = value; }
    public void setZero(float value) { this.mZero = value; }
    public void setValue(float value) { this.mValue = value; this.invalidate(); }
    public void setCaption(String value) { this.mCaption = value; }
    public String getCaption() { return this.mCaption; }


    public void setUnit(String value) { this.mUnit = value; }
    public String getUnit() { return this.mUnit; }

    /**
     * Sets the text size for a Paint object so a given string of text will be a
     * given width.
     *
     * @param paint
     *            the Paint to set the text size for
     * @param desiredWidth
     *            the desired width
     * @param text
     *            the text that should be that width
     */
    private static void setTextSizeForWidth(Paint paint, float desiredWidth, String text)
    {

        // Pick a reasonably large value for the test. Larger values produce
        // more accurate results, but may cause problems with hardware
        // acceleration. But there are workarounds for that, too; refer to
        // http://stackoverflow.com/questions/6253528/font-size-too-large-to-fit-in-cache
        final float testTextSize = 48f;

        // Get the bounds of the text, using our testTextSize.
        paint.setTextSize(testTextSize);
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);

        // Calculate the desired size as a proportion of our testTextSize.
        float desiredTextSize = testTextSize * desiredWidth / bounds.width();

        // Set the paint for that size.
        paint.setTextSize(desiredTextSize);
    }

    public void drawHorizontalProgressBar(
            Canvas canvas, float left, float top, float width, float height,
            float min, float max, float zero, float value) {
        canvas.translate(left, top);

        float range = max - min;

        // Translate values so that min is absolutely left, max absolutely right and zero somewhere
        // in between.
        zero = (zero - min) / range * width;
        value = (value - min) / range * width;

        // Draw the bar
        if (zero > value) {
            canvas.drawRect(value, 0.0f, zero, height, mBarColor);
        } else {
            canvas.drawRect(zero, 0.0f, value, height, mBarColor);
        }

        // Draw the frame.
        canvas.drawLine(0.0f, 0.0f, width, 0.0f, mFrameColor);
        canvas.drawLine(width, 0.0f, width, height, mFrameColor);
        canvas.drawLine(0.0f, height, width, height, mFrameColor);
        canvas.drawLine(0.0f, height, 0.0f, 0.0f, mFrameColor);

        //setTextSizeForWidth(mFontColor, bottom / 3, mCaption);
        float flFontSize = 48f;


/*
 */
        mFontColor.setTextSize(48f);
        // Get the text height in px
        Paint.FontMetrics fm = mFontColor.getFontMetrics();
        float flFontHeight = fm.bottom - fm.top + fm.leading;


        float flTextY1Pos = top + height / 3.0f; // - flFontHeight / 2.0f;
        float flTextY2Pos = top + height * 2.0f / 3.0f; // - flFontHeight / 2.0f;

        /*
        if (height < 100f)
        {
            // Convert the dps to pixels
            final float scale = getContext().getResources().getDisplayMetrics().density;
            /* Convert the vertical size from pixel to dp

            int size_in_pixel = (int)(height * 0.8f) + 1;
            flFontSize = size_in_pixel / scale;
            flTextYPos = top + height / 2;
        }
*/

        canvas.drawText(mCaption, left + 5, flTextY1Pos, mFontColor);


        String valueLabel = String.format("%.2f", this.mValue) + mUnit;
        canvas.drawText(valueLabel, left + 5, flTextY2Pos, mFontColor);
    }
}