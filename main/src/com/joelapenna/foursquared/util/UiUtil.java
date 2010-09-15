/**
 * Copyright 2010 Mark Wyszomierski
 */

package com.joelapenna.foursquared.util;

import android.content.Context;
import android.graphics.drawable.StateListDrawable;
import android.view.View;

import com.joelapenna.foursquared.R;

/**
 * @date September 15, 2010.
 * @author Mark Wyszomierski (markww@gmail.com)
 */
public class UiUtil {

	/**
	 * Take the system list_selector_background state list drawable, and overwrite the
	 * android:state_window_focused state with our own drawable. This lets us simulate
	 * the same selectable appearance found in a list view element. Set the target
	 * focusable=true.
	 */
    public static StateListDrawable buildListViewItemSelectable(Context context) {
    	StateListDrawable sld =  (StateListDrawable)context.getResources().getDrawable(
    			android.R.drawable.list_selector_background);
        sld.addState(new int[] { android.R.attr.state_window_focused }, 
        		context.getResources().getDrawable(R.drawable.listview_background));
        return sld;
    }
    
    public static void buildListViewItemSelectable(Context context, View view) {
    	int[] padding = { view.getPaddingLeft(), view.getPaddingTop(), 
    			view.getPaddingRight(), view.getPaddingBottom() };
    	StateListDrawable sld =  (StateListDrawable)context.getResources().getDrawable(
    			android.R.drawable.list_selector_background);
        sld.addState(new int[] { android.R.attr.state_window_focused }, 
        		context.getResources().getDrawable(R.drawable.listview_background));
        view.setBackgroundDrawable(sld);
        view.setPadding(padding[0], padding[1], padding[2], padding[3]);
    }
}
