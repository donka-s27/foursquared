package com.joelapenna.foursquared.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Adapter;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.joelapenna.foursquared.R;

/**
 * A single horizontal strip of user photo views. This gets the job done for the
 * photo strip, more work would be needed to make it a generic control.
 * 
 * @date September 15, 2010
 * @author Mark Wyszomierski (markww@gmail.com)
 */
public class HorizontalViewStrip extends LinearLayout {
    private Adapter mAdapter;
    
    private int mColumnWidth;
    private int mColumnSpacing;
    
    
    public HorizontalViewStrip(Context context) {
        super(context);
        setOrientation(LinearLayout.HORIZONTAL);
    }
    
    public HorizontalViewStrip(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.HorizontalViewStrip, 0, 0);

        mColumnWidth = a.getDimensionPixelSize(R.styleable.HorizontalViewStrip_columnWidth, 44);
        mColumnSpacing = a.getDimensionPixelSize(R.styleable.HorizontalViewStrip_columnSpacing, 10);

        a.recycle();

        setOrientation(LinearLayout.HORIZONTAL);
    }
    
    public void setAdapter(Adapter adapter) {
    	boolean firstSet = mAdapter == null;
    	mAdapter = adapter;
    	mAdapter.registerDataSetObserver(mObserver);
    	if (firstSet) {
    		layout(getRight()-getLeft());
    	}
    }
    
    /**
     * Tries to create as many child views as thereis available horizontal
     * space. The width of the column is specified through the xml layout
     * file. The actual size of the supplied views might be different, 
     * we couldn't tell until after layout is complete. This would need
     * to be updated to make this a more generic control.
     */
    private void layout(int width) {
    	removeAllViews();
        
    	int max = mAdapter.getCount();
        int sum = mColumnWidth + mColumnSpacing;
        int index = 0;
        while (sum < width) {
        	if (index == max) {
        		break;
        	}
        	
        	View view = mAdapter.getView(index++, null, null);
        	
        	LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
        			mColumnWidth, mColumnWidth);
        	llp.setMargins(0, 0, mColumnSpacing, 0);
        	LinearLayout llMargin = new LinearLayout(getContext());
        	llMargin.setLayoutParams(llp);
        	llMargin.addView(view);
            addView(llMargin);
        	sum += (mColumnWidth + mColumnSpacing);
        }
    }
    
    /**
     * This will ask the adapter for refreshed instances of the views,
     * it won't cause a new 'layout'.
     */
    private void datasetInvalidated() {
    	for (int i = 0; i < getChildCount(); i++) {
    		LinearLayout ll = (LinearLayout)getChildAt(i);
    		ImageView iv = (ImageView)ll.getChildAt(i);
    		mAdapter.getView(i, iv, null);
    	}
    	invalidate();
    }
    
    private DataSetObserver mObserver = new DataSetObserver() {
    	@Override
    	public void onChanged() {
    	}
    	
    	@Override
    	public void onInvalidated() {
    		datasetInvalidated();
    	}
    };
}
