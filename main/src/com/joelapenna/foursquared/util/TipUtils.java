/**
 * Copyright 2010 Mark Wyszomierski
 */

package com.joelapenna.foursquared.util;

import com.joelapenna.foursquare.types.Tip;

import android.text.TextUtils;

/**
 * @date September 2, 2010.
 * @author Mark Wyszomierski (markww@gmail.com)
 */
public class TipUtils {

    public static final String TIP_STATUS_TODO = "todo";
    public static final String TIP_STATUS_DONE = "done";

    public static boolean isTodo(Tip tip) {
        if (tip != null) {
            if (!TextUtils.isEmpty(tip.getStatus())) {
                return tip.getStatus().equals(TIP_STATUS_TODO);
            }
        }
        
        return false;
    }
    
    public static boolean isDone(Tip tip) {
        if (tip != null) {
            if (!TextUtils.isEmpty(tip.getStatus())) {
                return tip.getStatus().equals(TIP_STATUS_DONE);
            }
        }
        
        return false;
    }
}
