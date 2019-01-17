package com.texelography.hybridinsight;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.text.Layout;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Switch;

/**
 * Created by Clemens on 20.03.2018.
 */

public class OBDValueListEntry extends LinearLayout
{
    /** The byte of the OBD data value */
    private char m_u8Byte;

    /** The label of the entry */
    private String m_strCaption;

    /** The unit displayed in the progress bar field */
    private String m_strUnit;

    /** The label for the byte */
    private TextView mLblOBDDataByte;

    /* The label for the description */
    private TextView mLblOBDDataDescription;

    /* The switch to enable it for the main view */
    private Switch m_SwhShowInMainView;

    /* The element to display the value */
    private DataView mPrbDataView;

    public void setCaption(String caption)
    {
        this.m_strCaption = caption;
        mLblOBDDataDescription.setText(caption);
    }

    public void setUnit(String unit)
    {
        this.m_strUnit = unit;
        this.mPrbDataView.setUnit(unit);
    }

    public void setChecked(boolean checked) { this.m_SwhShowInMainView.setChecked(checked); }
    public void setMin(float min) { this.mPrbDataView.setMin(min); }
    public void setMax(float max) { this.mPrbDataView.setMax(max); }
    public void setZero(float zero) { this.mPrbDataView.setZero(zero); }
    public void setValue(float value) { this.mPrbDataView.setValue(value); }

    public boolean getChecked() { return this.m_SwhShowInMainView.isChecked(); }




    public OBDValueListEntry(Context context)
    {
        super(context);
        initializeViews(context);
    }

    public OBDValueListEntry(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        initializeViews(context);

        TypedArray typedArray;
        typedArray = context.obtainStyledAttributes(attrs, R.styleable.OBDValueListEntry);

        /* Load the default values */
        this.setCaption(typedArray.getString(R.styleable.OBDValueListEntry_obd_value_list_caption));
        this.setUnit(typedArray.getString(R.styleable.OBDValueListEntry_obd_value_list_unit));
        this.setValue(typedArray.getFloat(R.styleable.OBDValueListEntry_obd_value_list_value, 0.5f));
        this.setMin(typedArray.getFloat(R.styleable.OBDValueListEntry_obd_value_list_min, 0.0f));
        this.setMax(typedArray.getFloat(R.styleable.OBDValueListEntry_obd_value_list_max, 1.0f));
        this.setZero(typedArray.getFloat(R.styleable.OBDValueListEntry_obd_value_list_zero, 0.0f));
        this.setChecked(typedArray.getBoolean(R.styleable.OBDValueListEntry_obd_value_list_checked, false));

        typedArray.recycle();
    }


    /**
     * Inflates the views in the layout.
     *
     * @param context
     *           the current context for the view.
     */
    private void initializeViews(Context context)
    {
        this.setOrientation(HORIZONTAL);
        this.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        /* Add the GUI components, first the label */
        mLblOBDDataDescription = new TextView(context);
        mLblOBDDataDescription.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f));
        this.addView(mLblOBDDataDescription);


        mLblOBDDataByte = new TextView(context);
        mLblOBDDataByte.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f));
        this.addView(mLblOBDDataByte);

        /* Switch to enable / disable the entry in the main view */
        m_SwhShowInMainView = new Switch(context);
        m_SwhShowInMainView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f));
        this.addView(m_SwhShowInMainView);

        /* And the bar showing the current value */
        mPrbDataView = new DataView(context);
        mPrbDataView.setLayoutParams(new LayoutParams(300, LayoutParams.WRAP_CONTENT, 0f));
        this.addView(mPrbDataView);

        /* Do Layout */
        this.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, 50));
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.obd_value_list_entry_view, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        // Sets the images for the previous and next buttons. Uses
        // built-in images so you don't need to add images, but in
        // a real application your images should be in the
        // application package so they are always available.
        //mLblOBDDataDescription = (TextView) this
        //        .findViewById(R.id.obd_value_list_entry_text_view);

        //mLblOBDDataDescription.setText(mLabel + "[" + mUnit + "]");
        //mPrbDataView = (DataView)this
         //       .findViewById(R.id.obd_value_list_entry_data_view);
        //setValue(mValue);
    }
}
