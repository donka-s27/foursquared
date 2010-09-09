/**
 * Copyright 2010 Mark Wyszomierski
 */

package com.joelapenna.foursquared.util;

import com.joelapenna.foursquare.Foursquare;
import com.joelapenna.foursquare.types.Tip;
import com.joelapenna.foursquare.types.User;
import com.joelapenna.foursquared.Foursquared;
import com.joelapenna.foursquared.R;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import java.io.IOException;
import java.util.Observable;

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
