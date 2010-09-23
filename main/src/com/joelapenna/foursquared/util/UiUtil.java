/**
 * Copyright 2010 Mark Wyszomierski
 */

package com.joelapenna.foursquared.util;

import android.os.Build;

/**
 * @date September 15, 2010.
 * @author Mark Wyszomierski (markww@gmail.com)
 */
public class UiUtil {

    public static int sdkVersion() {
    	return new Integer(Build.VERSION.SDK).intValue();
    }
}
