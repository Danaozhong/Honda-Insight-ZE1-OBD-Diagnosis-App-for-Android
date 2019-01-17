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
    public int miColspan;
    public int miRowspan;
    public int miRow;
    public int miCol;

    public int iGetArea()
    {
        return miColspan * miRowspan;
    }

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
    private Map<Integer, PointerParameters> maoPointerParameters = new HashMap<Integer, PointerParameters>();

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
    /*

    Idee:
    Use two algorithms
    a) for move
    -a) when moving 1x1 cells, swap positions
    -b) when moving larger than 1x1 cells:
    --a) element is fully replaced: swap positions
    --b) element is not fully replaced: shrink the old element

    b) for resize (row, colum, or both added)
    -a) new element completely covered: remember, search for a new spot after completing resize
    -b) new element only partly cut: shrink

    Shrink algorithm: after resize, find the largest rect that has all parameters
    */
    private List<OBDDataViewLayoutParameters> vMoveObdValueItem(final int obdValueItem, OBDDataViewLayoutParameters oNewPosition)
    {
        // Check that parameters are valid
        if (oNewPosition.miRowspan == 0 || oNewPosition.miColspan == 0
                || oNewPosition.miRow < 0 || oNewPosition.miCol < 0)
        {
            return null;
        }

        // Save old position
        OBDDataViewLayoutParameters oOldPosition = this.obdDataViewLayoutParams.get(obdValueItem);

        // Create a table with the current items
        int[][] aaiTable = this.aaiCreateTable();

        // Map to keep track of all the points that need to be relocated
        SparseArray<ArrayList<Point>> aoOccupiedCells = new SparseArray<ArrayList<Point>>();

        for (int idX = 0; idX < oNewPosition.miColspan; idX++)
        {
            for (int idY = 0; idY < oNewPosition.miRowspan; idY++)
            {
                int iX = oNewPosition.miCol + idX;
                int iY = oNewPosition.miRow + idY;

                int iKey = aaiTable[iX][iY];
                if (iKey >= 0)
                {
                    // Check if the OBD Value bar is already in the list
                    if ( aoOccupiedCells.get(iKey) == null)
                    {
                        // Not yet there, add...
                        aoOccupiedCells.append(iKey, new ArrayList<Point>());
                    }
                    // Add the point which is to be removed
                    aoOccupiedCells.get(iKey).add(new Point(iX, iY));

                    // mark the field as occupied by the item moved here
                    aaiTable[iX][iY] = obdValueItem;
                }

            }
        }

        // Deepcopy the current layout params to manipulate them.
        List<OBDDataViewLayoutParameters> aNewLayoutParams = new ArrayList<>();

        try
        {
            for(OBDDataViewLayoutParameters param : obdDataViewLayoutParams)
            {
                aNewLayoutParams.add(param.clone());
            }
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

        // Update the position of the moved item
        aNewLayoutParams.set(obdValueItem, oNewPosition);
        // Calculate move delta

        // TODO this will not work for resizing as well!
        int iMovedX = oNewPosition.miCol - oOldPosition.miCol;
        int iMovedY = oNewPosition.miRow - oOldPosition.miRow;

        iMovedX += Integer.signum(iMovedX) * (oOldPosition.miColspan - 1);
        iMovedY += Integer.signum(iMovedY) * (oOldPosition.miRowspan - 1);


        for(int i = 0; i < aoOccupiedCells.size(); i++) {
            int key = aoOccupiedCells.keyAt(i);
            ArrayList<Point> aRemovedPoints = aoOccupiedCells.get(key);
            if (aRemovedPoints.size() == this.obdDataViewLayoutParams.get(key).iGetArea())
            {
                // data view is fully covered, move to the position where the other view comes from
                // Because we move it where the other came from, subtract
                aNewLayoutParams.get(key).miCol -= iMovedX;
                aNewLayoutParams.get(key).miRow -= iMovedY;

            }
            else
            {
                // data view only partially covered, resize...
                Rect oLargestRemainingRect = oFindLargestRect(aaiTable, i32NumOfCols, i32NumOfRows, key);

                aNewLayoutParams.get(key).miCol = oLargestRemainingRect.left;
                aNewLayoutParams.get(key).miRow = oLargestRemainingRect.top;
                aNewLayoutParams.get(key).miColspan = oLargestRemainingRect.right - oLargestRemainingRect.left;
                aNewLayoutParams.get(key).miRowspan = oLargestRemainingRect.bottom - oLargestRemainingRect.top;
            }
        }
        return aNewLayoutParams;
    }


    private void vRedrawEditModeRect()
    {
        Rect oRedRectDimensions = new Rect();
        List<OBDDataViewLayoutParameters> aNewLayoutParams = new ArrayList<>();

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

                OBDDataViewLayoutParameters newLayoutParams = new OBDDataViewLayoutParameters();

                int newPosAbsX = param.miCol + delta.x;
                int newPosAbsY = param.miRow + delta.y;
                newLayoutParams.miCol = Math.max(newPosAbsX, 0);
                newLayoutParams.miRow = Math.max(newPosAbsY, 0);

                // Right/bottom side crop
                newLayoutParams.miColspan = Math.min(param.miColspan, i32NumOfCols - newLayoutParams.miCol);
                newLayoutParams.miRowspan = Math.min(param.miRowspan, i32NumOfRows - newLayoutParams.miRow);

                // Left/upper side crop
                newLayoutParams.miColspan += (newPosAbsX - newLayoutParams.miCol);
                newLayoutParams.miRowspan += (newPosAbsY - newLayoutParams.miRow);

                Log.d("RESIZE", "New coords, col: " + Integer.toString(newLayoutParams.miCol) + ", row: " + Integer.toString(newLayoutParams.miRow));
                Log.d("RESIZE", "New span, col: " + Integer.toString(newLayoutParams.miColspan) + ", row: " + Integer.toString(newLayoutParams.miRowspan));


                aNewLayoutParams = vMoveObdValueItem(miSelectedOBDBar, newLayoutParams);
                if (null != aNewLayoutParams)
                {
                    //this.vUpdateMainView(aNewLayoutParams);
                }
                // Calculate the new size after moving, make sure to wrap if moving outside of the window.
                oRedRectDimensions.left =  newLayoutParams.miCol * miCellWidth;
                oRedRectDimensions.top =  newLayoutParams.miRow * miCellHeight;
                oRedRectDimensions.right = (newLayoutParams.miCol + newLayoutParams.miColspan) * miCellWidth;
                oRedRectDimensions.bottom = (newLayoutParams.miRow + newLayoutParams.miRowspan) * miCellHeight;
                break;
            }
            case EDIT_MODE_RESIZE:
            {
                OBDDataViewLayoutParameters param = this.obdDataViewLayoutParams.get(miSelectedOBDBar);
                Point minPos = new Point(param.miCol, param.miRow);
                Point maxPos = new Point(param.miCol + param.miColspan - 1, param.miRow + param.miRowspan - 1);
                for (PointerParameters value : maoPointerParameters.values())
                {
                    Point currentCell = oGetCoordinatesCell((int)value.mfX, (int)value.mfY);
                    minPos.x = Math.min(currentCell.x, minPos.x);
                    minPos.y = Math.min(currentCell.y, minPos.y);

                    maxPos.x = Math.max(currentCell.x, maxPos.x);
                    maxPos.y = Math.max(currentCell.y, maxPos.y);
                }

                oRedRectDimensions.left = minPos.x * miCellWidth;
                oRedRectDimensions.top = minPos.y * miCellHeight;
                oRedRectDimensions.right = (maxPos.x + 1) * miCellWidth;
                oRedRectDimensions.bottom = (maxPos.y + 1) * miCellHeight;

                break;
            }
        }
        ImageView imageView = (ImageView) findViewById(R.id.imgSelection);
        OBDDataViewLayoutParameters layoutParameters = this.obdDataViewLayoutParams.get(this.miSelectedOBDBar);
        Bitmap bmp=Bitmap.createBitmap(imageView.getWidth(), imageView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas cnvs=new Canvas(bmp);

        Paint paint=new Paint();
        paint.setColor(Color.RED);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(8);

        Toast.makeText(getApplicationContext(), "Colspan " + layoutParameters.miColspan + ", rospan: " + layoutParameters.miRowspan, Toast.LENGTH_LONG).show();

        cnvs.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        //cnvs.translate(0.0f, 0.0f);

        //Paint transparentPaint = new Paint();
        //transparentPaint.setColor(getResources().getColor(android.R.color.transparent));
        //transparentPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        float x = (float)layoutParameters.miCol * (float)miCellWidth;
        float y = (float)layoutParameters.miRow * (float)miCellHeight;
        float width = layoutParameters.miColspan * miCellWidth;
        float height = (float)layoutParameters.miRowspan * (float) miCellHeight;
        cnvs.drawRect(x, y, x + width, y + height, paint);

        paint.setColor(Color.BLUE);
        cnvs.drawRect(oRedRectDimensions, paint);

        paint.setColor(Color.GREEN);

        for (PointerParameters value : maoPointerParameters.values())
        {
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

            if (layoutParameter.miRow + layoutParameter.miRowspan - 1 > gridLayout.getRowCount() ||
                    layoutParameter.miCol + layoutParameter.miColspan - 1 > gridLayout.getColumnCount())
            {
                // Exception invalid layout
                continue;
            }

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

    //private int mActivePointerId;

    // The ‘active pointer’ is the one currently moving our object.

    private static final int INVALID_POINTER_ID = -1;

    private int mActivePointerId = INVALID_POINTER_ID;
    private float mfDownX;
    private float mfDownY;
    private float mPosX;
    private float mPosY;



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
            return false;
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

                maoPointerParameters.put(iPointerId, new PointerParameters(x,y));
                vRedrawEditModeRect();

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
                final float x = MotionEventCompat.getX(event, pointerIndex);
                final float y = MotionEventCompat.getY(event, pointerIndex);
                //int iPointerId = MotionEventCompat.getPointerId(event, 0);
                maoPointerParameters.put(pointerIndex, new PointerParameters(x,y));
                enGUIEditMode = OBDDataEditMode.EDIT_MODE_RESIZE;
                vRedrawEditModeRect();

                Log.d("MTOUCH","Action pointer down, pid: " + Integer.toString(pointerIndex) + ", x: " + Float.toString(x) + ", y: " + Float.toString(y));


                break;
            }
            case MotionEvent.ACTION_MOVE: {
                // Find the index of the active pointer and fetch its position
                boolean found = false;
                for (int pointerId : this.maoPointerParameters.keySet())
                {
                    final int pointerIndex =  MotionEventCompat.findPointerIndex(event, pointerId);
                    if (-1 == pointerIndex)
                    {
                        continue;
                    }

                    found = true;
                    int iPointerId = MotionEventCompat.getPointerId(event, pointerIndex);

                    if (pointerIndex == 0)
                    {
                        mPosX = event.getX();
                        mPosY = event.getY();
                    }
                    PointerParameters params = maoPointerParameters.get(pointerId);
                    params.mfX = MotionEventCompat.getX(event, pointerId);
                    params.mfY = MotionEventCompat.getY(event, pointerId);

                    vRedrawEditModeRect();

                    Log.d("MTOUCH","Action move, pointer id: " + Integer.toString(pointerId) + ", x: " + Float.toString(params.mfX) + ", y: " + Float.toString(params.mfX));
                }

                if (found == false)
                {
                    Log.d("MTOUCH","NOT FOUND! move, pointer id!");
                }
                // Calculate the distance moved
                //final float dx = x - mLastTouchX;
                //final float dy = y - mLastTouchY;

                //mPosX += dx;
                //mPosY += dy;

                //invalidate();

                // Remember this touch position for the next move event
                //mLastTouchX = x;
                //mLastTouchY = y;

                break;
            }

            case MotionEvent.ACTION_UP: {
                mActivePointerId = INVALID_POINTER_ID;

                Log.d("MTOUCH","Action up!");
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                Log.d("MTOUCH","Action cancel!");
                mActivePointerId = INVALID_POINTER_ID;
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {

                final int pointerIndex = MotionEventCompat.getActionIndex(event);
                final int pointerId = MotionEventCompat.getPointerId(event, pointerIndex);

                if (pointerId != -1)
                {
                    this.maoPointerParameters.remove(pointerId);

                }

                vRedrawEditModeRect();
                if (pointerId == mActivePointerId) {
                    enGUIEditMode = OBDDataEditMode.EDIT_MODE_MOVE;
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    //mLastTouchX = MotionEventCompat.getX(event, newPointerIndex);
                    //mLastTouchY = MotionEventCompat.getY(event, newPointerIndex);

                   // Log.d("MTOUCH","Action pointer up,last x: " + Float.toString(mLastTouchX) + ", y: " + Float.toString(mLastTouchY));

                    mActivePointerId = MotionEventCompat.getPointerId(event, newPointerIndex);
                }
                break;
            }
        }
        return false;
    }

    private void vDisableResizeMode()
    {
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
        vDisableResizeMode();

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
