package com.texelography.hybridinsight;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.texelography.hybridinsight.datapool.DataPoolDisplayedElementData;
import com.texelography.hybridinsight.datapool.DataPoolOBDData;
import com.texelography.hybridinsight.datapool.data_pool;
import com.travijuu.numberpicker.library.NumberPicker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Callable;

public class OBDValueListView extends AppCompatActivity
{
    private LinearLayout m_linOBDValueListContainer;
    private List<OBDValueListEntry> m_aOBDValueListEntries = new ArrayList<OBDValueListEntry>();

    private int mInterval = 100; // Update the OBD values every 100ms
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_obd_value_list_view);

        /* Change the data transmission mode to transfer all OBD data */
        data_pool.set_read_all_obd_values(true);

        /* Update the UI by first, cleaning the container.... */
        m_linOBDValueListContainer = (LinearLayout) this.findViewById(R.id.OBDValueListContainer);
        m_linOBDValueListContainer.removeAllViewsInLayout();

        /* Then, add all OBD data entries. */
        for (int i = 0; i < data_pool.get_obd_data_array_num_of_elements(); ++i)
        {
            DataPoolOBDData currentDpoolData = data_pool.get_obd_data_array_element(i);

            OBDValueListEntry obdValueListEntry = new OBDValueListEntry(this);
            obdValueListEntry.setMin(currentDpoolData.getF_min());
            obdValueListEntry.setMax(currentDpoolData.getF_max());
            obdValueListEntry.setZero(currentDpoolData.getF_zero());
            obdValueListEntry.setValue(currentDpoolData.getF_value());
            obdValueListEntry.setCaption(currentDpoolData.getStr_description());
            obdValueListEntry.setUnit(currentDpoolData.getStr_unit());
            obdValueListEntry.setChecked(false);
            m_aOBDValueListEntries.add(obdValueListEntry);
            m_linOBDValueListContainer.addView(obdValueListEntry);

            /* Set to a fixed height */
            obdValueListEntry.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 75));
        }

        /* Get the list of selected (main view) items */
        for (int i = 0; i < data_pool.get_main_view_elements_of_interest_count(); ++i)
        {
            m_aOBDValueListEntries.get((int)data_pool.get_main_view_elements_of_interest_array_element(i).getU32_element()).setChecked(true);
        }

        ((NumberPicker) this.findViewById(R.id.NumOfCols)).setValue(data_pool.get_main_view_num_of_cols());
        ((NumberPicker) this.findViewById(R.id.NumOfRows)).setValue(data_pool.get_main_view_num_of_rows());

        /* Redraw window */
        //m_linOBDValueListContainer.forceLayout();
        //m_linOBDValueListContainer.invalidate();

        /* Create a runnable to cyclically update the content */
        mHandler = new Handler();
        startCyclicOBDValueHMIUpdate();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed()
    {
        stopCyclicOBDValueHMIUpdate();

        /* When going back to the main menu, a large part of the OBD values are not of interest.
        Change the data transfer mode to high freq, but only selected OBD values.
         */
        data_pool.set_read_all_obd_values(false);

        /* Save main view configuration in data pool */
        List<Integer> checkedEntries = new ArrayList<>();

        /* Iterate over all items and choose the ones that are selected */
        for (int i = 0; i < m_aOBDValueListEntries.size(); ++i)
        {
            if (true == m_aOBDValueListEntries.get(i).getChecked())
            {
                DataPoolDisplayedElementData currentElement = new DataPoolDisplayedElementData();
                currentElement.setU32_element(i);
                currentElement.setU32_col(0l);
                currentElement.setU32_row(0l);
                data_pool.set_main_view_elements_of_interest_array_element(checkedEntries.size(), currentElement);
                checkedEntries.add(i);
            }
        }
        data_pool.set_main_view_elements_of_interest_count(checkedEntries.size());

        int numOfCols = ((NumberPicker) this.findViewById(R.id.NumOfCols)).getValue();
        int numOfRows = ((NumberPicker) this.findViewById(R.id.NumOfRows)).getValue();

        /* Make sure the elements fit the number of rows / cols */
        if (numOfCols * numOfRows < checkedEntries.size())
        {
            /* Doesn't fit, add rows as necessary */
            numOfRows = checkedEntries.size() / numOfCols;
            if (checkedEntries.size() % numOfCols != 0)
            {
                numOfRows++;
            }
        }

        /* Store in data pool */
        data_pool.set_main_view_num_of_cols(numOfCols);
        data_pool.set_main_view_num_of_rows(numOfRows);

        finish();
    }


    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }

    Runnable mUpdateOBDValuesRunnable = new Runnable() {
        @Override
        public void run() {
            try
            {
                for (int i = 0; i < data_pool.get_obd_data_array_num_of_elements(); ++i)
                {
                    DataPoolOBDData currentDpoolData = data_pool.get_obd_data_array_element(i);


                    OBDValueListEntry obdValueListEntry = m_aOBDValueListEntries.get(i);
                    obdValueListEntry.setValue(currentDpoolData.getF_value());
                    obdValueListEntry.invalidate();
                }

               // updateStatus(); //this function can change value of mInterval.

            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                mHandler.postDelayed(mUpdateOBDValuesRunnable, mInterval);
            }
        }
    };

    void startCyclicOBDValueHMIUpdate() {
        mUpdateOBDValuesRunnable.run();
    }

    void stopCyclicOBDValueHMIUpdate() {
        mHandler.removeCallbacks(mUpdateOBDValuesRunnable);
    }

}
