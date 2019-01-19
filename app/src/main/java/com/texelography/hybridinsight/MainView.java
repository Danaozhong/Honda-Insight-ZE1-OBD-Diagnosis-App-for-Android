package com.texelography.hybridinsight;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.GridLayout;
import android.bluetooth.BluetoothAdapter;
import android.view.Menu;
import android.view.ViewGroup;
import android.content.IntentFilter;
import android.view.MenuItem;
import android.view.ViewGroup.LayoutParams;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import java.lang.Runnable;
import java.util.Map;
import java.util.Stack;

import android.content.Context;

import  android.util.Log;
import android.os.Handler;

import android.content.ServiceConnection;
import android.content.ComponentName;
import android.os.IBinder;
import android.content.BroadcastReceiver;
import android.widget.ImageView;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.texelography.hybridinsight.datapool.DataPoolDisplayedElementData;
import com.texelography.hybridinsight.datapool.DataPoolOBDData;
import com.texelography.hybridinsight.datapool.data_pool;

import junit.framework.Test;


class OBDDataViewLayoutParameters implements Cloneable
{
    public OBDDataViewLayoutParameters(int iCol, int iRow, int iColspan, int iRowspan)
    {
        miColspan = iColspan;
        miRowspan = iRowspan;
        miRow = iRow;
        miCol = iCol;
        mboVisible = true;
    }

    /** Default constructor */
    public OBDDataViewLayoutParameters()
    {
        miColspan = 1;
        miRowspan = 1;
        miRow = 0;
        miCol = 0;
        mboVisible = true;
    }

    public int miColspan;
    public int miRowspan;
    public int miRow;
    public int miCol;

    public int iGetArea()
    {
        return miColspan * miRowspan;
    }

    public boolean mboVisible;

    @Override
    public OBDDataViewLayoutParameters clone() throws CloneNotSupportedException
    {
        OBDDataViewLayoutParameters clone = (OBDDataViewLayoutParameters)super.clone();
        return clone;
    }
};

class PointerParameters
{
    public PointerParameters(float flX, float flY)
    {
        mfX = flX;
        mfY = flY;
        miPointerID = 0;
    }

    public float mfX;
    public float mfY;
    public int miPointerID;
}
enum OBDDataEditMode
{
    EDIT_MODE_NONE,
    EDIT_MODE_MOVE,
    EDIT_MODE_RESIZE
};

public class MainView extends AppCompatActivity implements View.OnLongClickListener, View.OnTouchListener
{



    /* Container of all OBD views used in the form */
    private List<DataView> obdDataViews = new ArrayList<DataView>();
    private List<OBDDataViewLayoutParameters> obdDataViewLayoutParams = new ArrayList<OBDDataViewLayoutParameters>();
    private SparseArray<PointerParameters> maoPointerParameters = new SparseArray<PointerParameters>();

    // This list is only valid while the items are moved
    private List<OBDDataViewLayoutParameters> obdDataViewDuringMoveLayoutParams = new ArrayList<OBDDataViewLayoutParameters>();

    private int i32NumOfRows = 6;
    private int i32NumOfCols = 3;
    private int miCellWidth = 0;
    private int miCellHeight = 0;

    private boolean mboEditMode = false;
    OBDDataEditMode enGUIEditMode = OBDDataEditMode.EDIT_MODE_NONE;
    private int miSelectedOBDBar = 0;


    private int mInterval = 50; // Update the OBD values every 50ms
    private Handler mHandler;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private static final int MENU_ITEM_ALL_OBDII_VALUES = 0;
    private static final int MENU_ITEM_DTC = 1;
    private static final int MENU_ITEM_READYNESS_MONITOR = 2;
    private static final int MENU_ITEM_HYBRID_BATTERY_DETAILS = 3;
    private static final int MENU_ITEM_CONFIGURATION = 4;


    int[][] aaiCreateTable()
    {
        int table[][] = new int[i32NumOfCols][i32NumOfRows];

        for (int i = 0; i < i32NumOfCols; i++)
        {
            for (int k = 0; k < i32NumOfRows; k++)
            {
                table[i][k] = iGetOBDDataViewIndex(new Point(i, k));
            }
        }
        return table;
    }

    private Point oGetCoordinatesCell(Point point)
    {
        return oGetCoordinatesCell(point.x, point.y);
    }

    private Point oGetCoordinatesCell(int iX, int iY)
    {
        return(new Point(iX / this.miCellWidth, iY / this.miCellHeight));
    }

    private Point oGetMouseCell()
    {
        return oGetCoordinatesCell((int)this.mPosX, (int)this.mPosY);
    }

    private int iGetOBDDataViewIndex(final Point oCellCoordinates)
    {
        for (int i = 0; i < this.obdDataViewLayoutParams.size(); ++i)
        {
            OBDDataViewLayoutParameters params = this.obdDataViewLayoutParams.get(i);
            if (    oCellCoordinates.x >= params.miCol &&
                    oCellCoordinates.x < params.miCol + params.miColspan &&
                    oCellCoordinates.y >= params.miRow &&
                    oCellCoordinates.y < params.miRow + params.miRowspan)
            {
                return i;
            }
        }
        return -1;
    }

    // see https://stackoverflow.com/questions/7245/puzzle-find-largest-rectangle-maximal-rectangle-problem
    public static int[] updateCache(int[] cache, int[] matrixRow, int iExpectedValue, int MaxX)
    {
        for (int m = 0; m < MaxX; m++)
        {
            if (matrixRow[m] != iExpectedValue)
            {
                cache[m] = 0;
            }
            else
            {
                cache[m]++;
            }
        }
        return cache;
    }

    private Point oFindFirstCell(int[][] aaiTable, int iWidth, int iHeight, int iValue)
    {
        for (int iX = 0; iX < iWidth; iX++)
        {
            for (int iY = 0; iY < iHeight; iY++)
            {
                if (aaiTable[iX][iY] == iValue)
                {
                    return new Point(iX, iY);
                }
            }
        }
        return null;
    }

    private Rect oFindLargestRect(int[][] aaiTable, int iWidth, int iHeight, int iValue)
    {
        Point best_ll = new Point(0, 0);
        Point best_ur = new Point(-1, -1);
        int best_area = 0;

        Stack<Point> stack = new Stack<Point>();
        int[] cache = new int[iHeight + 1];

        for (int m = 0; m != iHeight + 1; m++)
        {
            cache[m] = 0;
        }

        for (int n = 0; n != iWidth; n++)
        {
            int iOpenHeight = 0; //
            cache = updateCache(cache, aaiTable[n], iValue, iHeight);
            for (int m = 0; m != iHeight + 1; m++)
            {
                if (cache[m] > iOpenHeight)
                {
                    stack.push(new Point(m, iOpenHeight));
                    iOpenHeight = cache[m];
                } else if (cache[m] < iOpenHeight)
                {
                    int area;
                    Point p;
                    do {
                        p = stack.pop();
                        area = iOpenHeight * (m - p.x);
                        if (area > best_area)
                        {
                            best_area = area;
                            best_ll.x = p.x;
                            best_ll.y = n;
                            best_ur.x = m - 1;
                            best_ur.y = n - iOpenHeight + 1;
                        }
                        iOpenHeight = p.y;
                    } while (cache[m] < iOpenHeight);
                    iOpenHeight = cache[m];
                    if (iOpenHeight != 0)
                    {
                        stack.push(p);
                    }
                }
            }
        }
        return new Rect(best_ll.y, best_ll.x, best_ur.y + 1, best_ur.x + 1);
    }
    /**
     * \brief This function moves an OBD Value item to a new position, supporting both moving and
     * resizing (also at the same time).
     * \param[in/out] oLayout The layout parameters. Will be altered to represent the new position.
     * \param[in] NumOfCols the number of cols of the screen.
     * \param[in] NumOfRows The number of rows of the screen.
    */
    private int iMoveObdValueItem( List<OBDDataViewLayoutParameters> oLayout,
                                   final int iNumOfCols, final int iNumOfRows,
                                   final int obdValueItem, final OBDDataViewLayoutParameters oNewPosition)

    {
        // Check that parameters are valid
        if (oNewPosition.miRowspan == 0 || oNewPosition.miColspan == 0
                || oNewPosition.miRow < 0 || oNewPosition.miCol < 0
                || oNewPosition.miRow + oNewPosition.miRowspan > iNumOfRows
                || oNewPosition.miCol + oNewPosition.miColspan > iNumOfCols )
        {
            return -1;
        }

        // Save old position
        OBDDataViewLayoutParameters oOldPosition = null;
        try
        {
            oOldPosition = oLayout.get(obdValueItem).clone();
        }
        catch (CloneNotSupportedException e)
        {
            e.printStackTrace();
        }
        // Create a table with the current items
        int[][] aaiTable = this.aaiCreateTable();

        // Mark the old position as available
        for (int idX = 0; idX < oOldPosition.miColspan; idX++)
        {
            for (int idY = 0; idY < oOldPosition.miRowspan; idY++)
            {
                aaiTable[oOldPosition.miCol + idX][oOldPosition.miRow + idY] = -1;
            }
        }

        // Map to keep track of all the points that need to be relocated
        SparseArray<ArrayList<Point>> aoOccupiedCells = new SparseArray<ArrayList<Point>>();

        // Find all other view elements that are where the OBV value item is moved to
        for (int idX = 0; idX < oNewPosition.miColspan; idX++)
        {
            for (int idY = 0; idY < oNewPosition.miRowspan; idY++)
            {
                int iX = oNewPosition.miCol + idX;
                int iY = oNewPosition.miRow + idY;

                // the field value is the index (iterator) of the OBD views
                final int iKey = aaiTable[iX][iY];
                if (iKey >= 0 && iKey != obdValueItem)
                {
                    // Check if the OBD Value bar is already in the list
                    if ( aoOccupiedCells.get(iKey) == null)
                    {
                        // Not yet there, add...
                        aoOccupiedCells.append(iKey, new ArrayList<Point>());
                    }
                    // Add the point which is to be removed
                    aoOccupiedCells.get(iKey).add(new Point(iX, iY));
                }
                // mark the field as occupied by the item moved here
                aaiTable[iX][iY] = obdValueItem;
            }
        }

        // Update the position of the moved item
        try
        {
            oLayout.set(obdValueItem, oNewPosition.clone());
        }
        catch (CloneNotSupportedException e)
        {
            e.printStackTrace();
        }

        for(int i = 0; i < aoOccupiedCells.size(); i++)
        {
            int key = aoOccupiedCells.keyAt(i);
            ArrayList<Point> aRemovedPoints = aoOccupiedCells.get(key);
            if (aRemovedPoints.size() == oLayout.get(key).iGetArea())
            {
                // data view is fully covered, find a new place for it.
                Point oOccupiedViewNewPosition = oFindFirstCell(aaiTable, iNumOfCols, iNumOfRows, -1);
                if (oOccupiedViewNewPosition != null)
                {
                    // New position found, move to new position
                    oLayout.get(key).miRow = oOccupiedViewNewPosition.y;
                    oLayout.get(key).miCol = oOccupiedViewNewPosition.x;
                    oLayout.get(key).miRowspan = 1;
                    oLayout.get(key).miColspan = 1;
                    aaiTable[oOccupiedViewNewPosition.x][oOccupiedViewNewPosition.y] = key;
                }
                else
                {
                    // Mark the item to be deleted (does not fit into the view)
                    oLayout.get(key).mboVisible = false;
                }

            }
            else
            {
                // data view only partially covered, resize...
                Rect oLargestRemainingRect = oFindLargestRect(aaiTable, i32NumOfCols, i32NumOfRows, key);

                oLayout.get(key).miCol = oLargestRemainingRect.left;
                oLayout.get(key).miRow = oLargestRemainingRect.top;
                oLayout.get(key).miColspan = oLargestRemainingRect.right - oLargestRemainingRect.left;
                oLayout.get(key).miRowspan = oLargestRemainingRect.bottom - oLargestRemainingRect.top;
            }
        }
        return 0;
    }

    // TODO This does not only redraw, but also recalculate. Split up
    private void vRedrawEditModeRect()
    {
        Rect oRedRectDimensions = new Rect();
        boolean boRedraw = false;
        OBDDataViewLayoutParameters newLayoutParams = null;

        // Check which edit mode is currently active (move or resize)
        switch(this.enGUIEditMode)
        {
            case EDIT_MODE_MOVE:
            {
                // Find which field is currently hovered
                Point delta = new Point();

                delta.x = oGetMouseCell().x - oGetCoordinatesCell((int) mfDownX, (int) mfDownY).x;
                delta.y = oGetMouseCell().y - oGetCoordinatesCell((int) mfDownX, (int) mfDownY).y;

                Log.d("MTOUCH", "DELTA, x: " + Float.toString(delta.x) + ", y: " + Float.toString(delta.y));

                OBDDataViewLayoutParameters param = this.obdDataViewLayoutParams.get(miSelectedOBDBar);

                int newPosAbsX = param.miCol + delta.x;
                int newPosAbsY = param.miRow + delta.y;


                newLayoutParams = new OBDDataViewLayoutParameters(
                        Math.max(newPosAbsX, 0), Math.max(newPosAbsY, 0), 1, 1);

                // Right/bottom side crop
                newLayoutParams.miColspan = Math.min(param.miColspan, i32NumOfCols - newLayoutParams.miCol);
                newLayoutParams.miRowspan = Math.min(param.miRowspan, i32NumOfRows - newLayoutParams.miRow);

                // Left/upper side crop
                newLayoutParams.miColspan += (newPosAbsX - newLayoutParams.miCol);
                newLayoutParams.miRowspan += (newPosAbsY - newLayoutParams.miRow);

                boRedraw = true;
                break;
            }
            case EDIT_MODE_RESIZE:
            {
                OBDDataViewLayoutParameters param = this.obdDataViewLayoutParams.get(miSelectedOBDBar);

                if (maoPointerParameters.size() < 2)
                {
                    // TODO store exception
                    this.enGUIEditMode = OBDDataEditMode.EDIT_MODE_NONE;
                }
                else
                {
                    Point minPos = oGetCoordinatesCell((int)maoPointerParameters.valueAt(0).mfX, (int)maoPointerParameters.valueAt(0).mfY);
                    Point maxPos = new Point(minPos);

                    for (int i = 0; i < maoPointerParameters.size(); ++i)
                    {
                        PointerParameters value = maoPointerParameters.valueAt(i);
                        Point currentCell = oGetCoordinatesCell((int) value.mfX, (int) value.mfY);
                        minPos.x = Math.min(currentCell.x, minPos.x);
                        minPos.y = Math.min(currentCell.y, minPos.y);

                        maxPos.x = Math.max(currentCell.x, maxPos.x);
                        maxPos.y = Math.max(currentCell.y, maxPos.y);
                    }

                    newLayoutParams = new OBDDataViewLayoutParameters();
                    newLayoutParams.miRow = minPos.y;
                    newLayoutParams.miCol = minPos.x;
                    newLayoutParams.miRowspan = maxPos.y - minPos.y + 1;
                    newLayoutParams.miColspan = maxPos.x - minPos.x + 1;
                    boRedraw = true;
                }

                break;
            }
        }

        if(boRedraw == true)
        {
                /* Create new layout parameters that would represent the new location of the
                elements, but do not activate them yet. They will become valid after the user has
                lift off their fingers.
                 */
            this.obdDataViewDuringMoveLayoutParams = new ArrayList<>();

            try
            {
                for (OBDDataViewLayoutParameters oParameter : this.obdDataViewLayoutParams)
                {
                    this.obdDataViewDuringMoveLayoutParams.add(oParameter.clone());
                }
            } catch (CloneNotSupportedException e)
            {
                e.printStackTrace();
            }

            if (0 == iMoveObdValueItem(this.obdDataViewDuringMoveLayoutParams, i32NumOfCols, i32NumOfRows, miSelectedOBDBar, newLayoutParams))
            {
                // Create a preview
                this.vUpdateMainView(this.obdDataViewDuringMoveLayoutParams);
            }
            // Calculate the new size after moving
            oRedRectDimensions.left =  newLayoutParams.miCol * miCellWidth;
            oRedRectDimensions.top =  newLayoutParams.miRow * miCellHeight;
            oRedRectDimensions.right = (newLayoutParams.miCol + newLayoutParams.miColspan) * miCellWidth;
            oRedRectDimensions.bottom = (newLayoutParams.miRow + newLayoutParams.miRowspan) * miCellHeight;
        }

        ImageView imageView = (ImageView) findViewById(R.id.imgSelection);
        OBDDataViewLayoutParameters layoutParameters = this.obdDataViewLayoutParams.get(this.miSelectedOBDBar);
        Bitmap bmp=Bitmap.createBitmap(imageView.getWidth(), imageView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas cnvs=new Canvas(bmp);

        cnvs.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        Paint paint=new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(8);
        cnvs.drawRect(oRedRectDimensions, paint);

        paint.setColor(Color.GREEN);
        for (int i = 0; i < maoPointerParameters.size(); i++)
        {
            PointerParameters value = maoPointerParameters.valueAt(i);
            cnvs.drawCircle(value.mfX, value.mfY, 10.0f, paint);
        }

        imageView.setImageBitmap(bmp);

    }

    private void vUpdateMainView(final List<OBDDataViewLayoutParameters> aLayoutParams)
    {
        GridLayout gridLayout = (GridLayout) findViewById(R.id.valueContainer);
        for (int i = 0; i < aLayoutParams.size(); i++)
        {
            DataView oOBDDataView = obdDataViews.get(i);
            OBDDataViewLayoutParameters layoutParameter = aLayoutParams.get(i);

            if (layoutParameter.mboVisible == false)
            {
                // Element is not supposed to be seen.
                oOBDDataView.setVisibility(View.INVISIBLE);
                continue;
            }

            if (layoutParameter.miRow + layoutParameter.miRowspan - 1 > gridLayout.getRowCount() ||
                    layoutParameter.miCol + layoutParameter.miColspan - 1 > gridLayout.getColumnCount()
                    )
            {
                // TODO Exception invalid layout
                oOBDDataView.setVisibility(View.INVISIBLE);
                continue;
            }

            oOBDDataView.setVisibility(View.VISIBLE);

            GridLayout.Spec rowSpec = GridLayout.spec(layoutParameter.miRow, layoutParameter.miRowspan);
            GridLayout.Spec colSpec = GridLayout.spec(layoutParameter.miCol, layoutParameter.miColspan);

            LayoutParams lpGl = new GridLayout.LayoutParams(rowSpec, colSpec);
            oOBDDataView.setLayoutParams(lpGl);
        }

        ViewGroup vg = findViewById(R.id.mainLayout);
        int width = vg.getWidth();
        int height = vg.getHeight();

        miCellWidth = height / i32NumOfRows;
        miCellHeight = width / i32NumOfCols;

        for (int i = 0; i < obdDataViews.size(); ++i)
        {
            DataView obdValueBar = obdDataViews.get(i);
            OBDDataViewLayoutParameters obdLayout = aLayoutParams.get(i);
            LayoutParams params = obdValueBar.getLayoutParams();

            //params.width = miCellWidth;
            //params.height = miCellHeight;

            params.width = miCellWidth * obdLayout.miColspan;
            params.height = miCellHeight * obdLayout.miRowspan;
            obdValueBar.setLayoutParams(params);
        }
        gridLayout.forceLayout();
        vg.invalidate();
    }

    private void vEnableEditMode(int iIndex)
    {
        this.miSelectedOBDBar = iIndex;
        this.enGUIEditMode = OBDDataEditMode.EDIT_MODE_MOVE;
        ImageView imageView = (ImageView) findViewById(R.id.imgSelection);
        imageView.setVisibility(View.VISIBLE);
        vRedrawEditModeRect();
    }

    private void vTakeOverMoveLayoutParameters()
    {
        // Take over the current moving parameters and apply them.
        for (int i = 0; i < this.obdDataViewLayoutParams.size(); ++i)
        {
            try
            {
                this.obdDataViewLayoutParams.set(i, this.obdDataViewDuringMoveLayoutParams.get(i).clone());
            }
            catch (CloneNotSupportedException e)
            {
                e.printStackTrace();
            }
        }
    }

    //private int mActivePointerId;

    // The ‘active pointer’ is the one currently moving our object.

    private static final int INVALID_POINTER_ID = -1;

    private int mActivePointerId = INVALID_POINTER_ID;
    private float mfDownX;
    private float mfDownY;
    private float mPosX;
    private float mPosY;

    // The ‘active pointer’ is the one currently moving our object.
    //private int mActivePointerId = INVALID_POINTER_ID;


        @Override
    //public boolean onTouchEvent(View view, MotionEvent event)
    public boolean onTouch(View v, MotionEvent event)
    //public boolean onTouchEvent(final View v)
    {

        // Remember where the cursor is at the moment
        final int action2 = event.getAction();
        switch (action2 & MotionEvent.ACTION_MASK)
        {
            case MotionEvent.ACTION_DOWN:
            {
                mPosX = event.getX();
                mPosY = event.getY();
                break;
            }
        }

        if (this.enGUIEditMode == OBDDataEditMode.EDIT_MODE_NONE)
        {
        //    return false;
        }
        final int action = MotionEventCompat.getActionMasked(event);

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                final int pointerIndex = MotionEventCompat.getActionIndex(event);
                if (-1 == pointerIndex)
                {
                    return false;
                }
                final float x = MotionEventCompat.getX(event, pointerIndex);
                final float y = MotionEventCompat.getY(event, pointerIndex);

                this.mfDownX = x;
                this.mfDownY = y;
                int iPointerId = MotionEventCompat.getPointerId(event, 0);

                // save the ID of this pointer (this is the main pointer)
                mActivePointerId = iPointerId;
                maoPointerParameters.put(iPointerId, new PointerParameters(x,y));

                if (this.enGUIEditMode != OBDDataEditMode.EDIT_MODE_NONE)
                {
                    vRedrawEditModeRect();
                }


                Log.d("MTOUCH","Action down, pid: " + Integer.toString(iPointerId) + ", x: " + Float.toString(x) + ", y: " + Float.toString(y));


                break;
            }
            case MotionEvent.ACTION_POINTER_DOWN: {
                Log.d("MTOUCH","Action pointer down");
                final int pointerIndex = MotionEventCompat.getActionIndex(event);

                if (-1 == pointerIndex)
                {
                    return false;
                }
                int iPointerId = MotionEventCompat.getPointerId(event, pointerIndex);

                final float x = MotionEventCompat.getX(event, pointerIndex);
                final float y = MotionEventCompat.getY(event, pointerIndex);
                //int iPointerId = MotionEventCompat.getPointerId(event, 0);
                maoPointerParameters.put(iPointerId, new PointerParameters(x,y));
                enGUIEditMode = OBDDataEditMode.EDIT_MODE_RESIZE;
                if (this.enGUIEditMode != OBDDataEditMode.EDIT_MODE_NONE)
                {
                    vTakeOverMoveLayoutParameters();
                    vRedrawEditModeRect();
                }

                Log.d("MTOUCH","Action pointer down, pid: " + Integer.toString(iPointerId) + ", x: " + Float.toString(x) + ", y: " + Float.toString(y));


                break;
            }
            case MotionEvent.ACTION_MOVE: {
                // Find the index of the active pointer and fetch its position
                boolean found = false;

                for(int i = 0; i < maoPointerParameters.size(); i++)
                {
                    int pointerId = maoPointerParameters.keyAt(i);

                    final int pointerIndex =  MotionEventCompat.findPointerIndex(event, pointerId);
                    Log.d("MTOUCH","pid: " + Integer.toString(pointerId) + ", pointer index: " + Integer.toString(pointerIndex));
                    if (-1 == pointerIndex)
                    {
                        continue;
                    }

                    found = true;

                    // remember the position of the main cursor
                    if (pointerIndex == mActivePointerId)
                    {
                        mPosX = event.getX();
                        mPosY = event.getY();
                    }

                    PointerParameters params = maoPointerParameters.get(pointerId);
                    params.mfX = MotionEventCompat.getX(event, pointerIndex);
                    params.mfY = MotionEventCompat.getY(event, pointerIndex);

                    if (this.enGUIEditMode != OBDDataEditMode.EDIT_MODE_NONE)
                    {

                        vRedrawEditModeRect();
                    }

                    Log.d("MTOUCH","Action move, pointer id: " + Integer.toString(pointerId) + ", x: " + Float.toString(params.mfX) + ", y: " + Float.toString(params.mfX));
                }

                if (found == false)
                {
                    Log.d("MTOUCH","NOT FOUND! move, pointer id:");
                }
                break;
            }

            case MotionEvent.ACTION_UP: {
                mActivePointerId = INVALID_POINTER_ID;
                this.maoPointerParameters.clear();

                this.vDisableResizeMode(true);

                Log.d("MTOUCH","Action up!");
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                Log.d("MTOUCH","Action cancel!");
                mActivePointerId = INVALID_POINTER_ID;
                this.maoPointerParameters.clear();

                this.vDisableResizeMode(true);
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                Log.d("MTOUCH","Action Pointer up!");
                final int pointerIndex = MotionEventCompat.getActionIndex(event);
                final int pointerId = MotionEventCompat.getPointerId(event, pointerIndex);

                if (pointerId != -1)
                {
                    this.maoPointerParameters.remove(pointerId);

                }

                // BLABLA just finish everything and apply the new size + position
                mActivePointerId = INVALID_POINTER_ID;
                this.maoPointerParameters.clear();

                this.vDisableResizeMode(true);
                if (pointerId == mActivePointerId) {
                    //enGUIEditMode = OBDDataEditMode.EDIT_MODE_MOVE;
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mActivePointerId = MotionEventCompat.getPointerId(event, newPointerIndex);
                }



                break;
            }
        }
        return false;
    }

    private void vDisableResizeMode(boolean boTakeOverModeParameters)
    {
        if (true == boTakeOverModeParameters)
        {
            this.vTakeOverMoveLayoutParameters();
        }
        this.mboEditMode = false;
        this.enGUIEditMode = OBDDataEditMode.EDIT_MODE_NONE;
        ImageView imageView = (ImageView) findViewById(R.id.imgSelection);
        imageView.setVisibility(View.INVISIBLE);
    }

    @Override
    public boolean onLongClick(final View v)
    {
        //vEnableEditMode(0);
        // Get the OBD bar that is used
        int selectedBar = iGetOBDDataViewIndex(oGetMouseCell());
        if (selectedBar >= 0)
        {
            vEnableEditMode(selectedBar);
            Toast.makeText(getApplicationContext(), "clicked element is: " + this.obdDataViews.get(selectedBar).getCaption(), Toast.LENGTH_LONG).show();
        }
        return true;
    }

    public void setTableSize(int iRows, int iCols)
    {
        i32NumOfRows = iRows;
        i32NumOfCols = iCols;

        GridLayout gridLayout = (GridLayout) findViewById(R.id.valueContainer);

        gridLayout.setRowCount(i32NumOfRows);
        gridLayout.setColumnCount(i32NumOfCols);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        menu.add(Menu.NONE, MENU_ITEM_ALL_OBDII_VALUES, Menu.NONE, "All OBDII Values");
        menu.add(Menu.NONE, MENU_ITEM_DTC, Menu.NONE, "DTCs");
        menu.add(Menu.NONE, MENU_ITEM_READYNESS_MONITOR, Menu.NONE, "Readyness Monitor");
        menu.add(Menu.NONE, MENU_ITEM_HYBRID_BATTERY_DETAILS, Menu.NONE, "IMA Battery");
        menu.add(Menu.NONE, MENU_ITEM_CONFIGURATION, Menu.NONE, "Configuration");

        //MenuInflater inflater = getMenuInflater();
        //inflater.inflate(R.menu., menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        Intent intent = null;
         switch (item.getItemId()) {
            case MENU_ITEM_ALL_OBDII_VALUES:
                intent = new Intent(this, OBDValueListView.class);
                startActivity(intent);
                return true;
            case MENU_ITEM_DTC:
                intent = new Intent(this, OBDErrorCodesView.class);
                startActivity(intent);
                return true;
            case MENU_ITEM_READYNESS_MONITOR:
                intent = new Intent(this, ReadynessFlagsView.class);
                startActivity(intent);
                return true;
            case MENU_ITEM_HYBRID_BATTERY_DETAILS:
                intent = new Intent(this, IMABatteryView.class);
                startActivity(intent);
                return true;
            case MENU_ITEM_CONFIGURATION:
                // code for subOption2
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private int objindex = 0;
    public int createOBDValueBar(float min, float max, float zero, float value, String description, final String unit)
    {

        if(min < max)
        {
            max = min;
        }

        final Context gui_context = this;
        DataView obdValueBar = new DataView(gui_context);
        obdValueBar.setMin(min);
        obdValueBar.setMax(max);
        obdValueBar.setZero(zero);
        obdValueBar.setValue(value);
        obdValueBar.setCaption(description);
        obdValueBar.setUnit(unit);

        obdDataViews.add(obdValueBar);

        OBDDataViewLayoutParameters OBDDataViewLayoutParameters = new OBDDataViewLayoutParameters();
        OBDDataViewLayoutParameters.miCol = objindex % this.i32NumOfCols;
        OBDDataViewLayoutParameters.miRow = objindex / this.i32NumOfCols;
        OBDDataViewLayoutParameters.miRowspan = 1;
        OBDDataViewLayoutParameters.miColspan = 1;


        if (objindex == 4 || objindex == 6)
        {
            OBDDataViewLayoutParameters.miColspan = 2;
            objindex++;
        }
        obdDataViewLayoutParams.add(OBDDataViewLayoutParameters);
        objindex++;

        return obdDataViews.size() - 1;
    }

    public void addOBDValueBar(final int obdValueBarIndex)
    {
        GridLayout gridLayout = (GridLayout) findViewById(R.id.valueContainer);

        // Load the layout parameters
        OBDDataViewLayoutParameters layoutParameter = obdDataViewLayoutParams.get(obdValueBarIndex);


        // add the OBD Value bar to the GridLayout
        DataView obdValueBar = obdDataViews.get(obdValueBarIndex);

        // Set cell size
        //LayoutParams currentLayoutparam = obdValueBar.getLayoutParams();
        //currentLayoutparam.width = miCellWidth * layoutParameter.miColspan;
        //currentLayoutparam.height = miCellHeight * layoutParameter.miRowspan;
        //obdValueBar.setLayoutParams(currentLayoutparam);

        // Grid View Position and col / rowspan
        gridLayout.addView(obdValueBar);
        // Register for long-click listener
        //obdValueBar.setOnLongClickListener(this);
        //obdValueBar.setOnTouchListener(this);

        gridLayout.setOnLongClickListener(this);
        gridLayout.setOnTouchListener(this);

        //obdValueBar.setClickable(false);
        //gridLayout.setClickable(false);


    }

    public void setOBDValueVarValue(final int obdValueBarIndex, final float min, final float max, final float zero, final float value)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DataView obdValueBar =  obdDataViews.get(obdValueBarIndex);
                obdValueBar.setValue(value);
                obdValueBar.invalidate();
                ViewGroup vg = findViewById (R.id.mainLayout);
                vg.invalidate();
            }
        });
    }

    private final static int REQUEST_ENABLE_BT = 1;
    private BluetoothService mBluetoothService;
    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection()
    {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothService = ((BluetoothService.LocalBinder) service).getService();
            if (!mBluetoothService.initialize()) {
                Log.e("BT initialization", "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothService.connect("Test123");

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothService = null;
        }
    };

    protected void initializeBLE()
    {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // No explanation needed, we can request the permission.

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    1234);
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // No explanation needed, we can request the permission.

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1234);
        }

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null)
        {
            // Device doesn't support Bluetooth
            Log.e("BT initialization", "Devide does not support Bluetooth, app will not work properly!");
        }
        else
        {
            if (!mBluetoothAdapter.isEnabled())
            {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }


            Intent gattServiceIntent = new Intent(this, BluetoothService.class);
            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        }


    }

    public void clearMainView()
    {
        vDisableResizeMode(false);

        /* Clear the old (dummy) GUI content */
        GridLayout gridLayout = (GridLayout) findViewById(R.id.valueContainer);
        gridLayout.removeAllViewsInLayout();
    }

    protected void populateMainView()
    {
        /* Read from data pool, which elements are supposed to be displayed */
        for (int i = 0; i < data_pool.get_main_view_elements_of_interest_count(); ++i)
        {
            DataPoolDisplayedElementData current_displayed_element = data_pool.get_main_view_elements_of_interest_array_element(i);
            DataPoolOBDData current_obd_data = data_pool.get_obd_data_array_element((int)current_displayed_element.getU32_element());

            /* Create the HMI widget to display the selected OBD value */
            int hmi_obd_widget_index = createOBDValueBar(current_obd_data.getF_min(), current_obd_data.getF_max(),
                    0.0f, current_obd_data.getF_value(), current_obd_data.getStr_description(),
                    current_obd_data.getStr_unit());

            this.addOBDValueBar(hmi_obd_widget_index);

        }


        final ViewGroup vg = findViewById(R.id.mainLayout);
        vg.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                //Remove it here unless you want to get this callback for EVERY
                //layout pass, which can get you into infinite loops if you ever
                //modify the layout from within this method.
                vg.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                vUpdateMainView(obdDataViewLayoutParams);
            }
        });
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_view);

        /* Start the CPP code */
        android_on_create_form();

        initializeBLE();

        // Create HMI
        //sleep(200);
        //data_pool myDataPoolAccessor = new data_pool();

        Log.e("CPP Interface", "Num of elements: " + Integer.toString(data_pool.get_main_view_elements_of_interest_count()));
        /* Retrieve the currently displayed OBD data from Data Pool */

        /* Create a runnable to cyclically update the content */
        mHandler = new Handler();
        startCyclicOBDValueHMIUpdate();

    }


    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothService.ACTION_GATT_CONNECTED.equals(action)) {
                //mConnected = true;
                //updateConnectionState(R.string.connected);
                //invalidateOptionsMenu();
            } else if (BluetoothService.ACTION_GATT_DISCONNECTED.equals(action)) {
                //mConnected = false;
                //updateConnectionState(R.string.disconnected);
                //invalidateOptionsMenu();
                //clearUI();
            } else if (BluetoothService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                //displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothService.ACTION_DATA_AVAILABLE.equals(action)) {
                //displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                if (intent.hasExtra(BluetoothService.EXTRA_DATA))
                {
                    final byte[] data = intent.getByteArrayExtra(BluetoothService.EXTRA_DATA);
                    if (data != null && data.length > 0)
                    {
                        /* Forward received data to C++ */
                        //String string_data = new String(data);
                        //Log.d("Bluetooth", "Data received (length " + Integer.toString(data.length) +  " ), forwarning...");

                        /* Get the UUID */
                        //final String uuid = intent.getStringExtra(BluetoothService.EXTRA_SENDER_UUID);

                        //android_ble_data_received(uuid.getBytes(), uuid.length(), data, data.length);
                        //mBluetoothService.sendBytes("test", "test".getBytes());
                    } else
                    {
                        Log.d("Bluetooth", "Main window invalid data!");
                    }
                }
            }
            else
            {
                Log.d("Bluetooth", "Unknown broadcast received: " + action);
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }




    @Override
    protected void onResume()
    {


        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        /* Populate the main view */
        clearMainView();
        this.setTableSize(4,3);
        populateMainView();

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

        /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native void android_on_create_form();
   // public native void android_ble_data_received(byte[] uuid, int uuid_length, byte[] value, int length);
    //public native void android_hmi_trigger_state_machine(int new_state);

    Runnable mUpdateOBDValuesRunnable = new Runnable() {
        @Override
        public void run() {
        try
        {
            /* Retrieve the currently displayed OBD data from Data Pool */
            for (int i = 0; i < data_pool.get_main_view_elements_of_interest_count(); ++i)
            {
                DataPoolDisplayedElementData current_displayed_element = data_pool.get_main_view_elements_of_interest_array_element(i);
                DataPoolOBDData current_obd_data = data_pool.get_obd_data_array_element((int)current_displayed_element.getU32_element());

                if (obdDataViews.size() > i)
                {
                    obdDataViews.get(i).setMin(current_obd_data.getF_min());
                    obdDataViews.get(i).setMax(current_obd_data.getF_max());

                    obdDataViews.get(i).setValue(current_obd_data.getF_value());
                    obdDataViews.get(i).invalidate();
                }
            }
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
