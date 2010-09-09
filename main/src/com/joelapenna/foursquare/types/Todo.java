/**
 * Copyright 2010 Mark Wyszomierski
 */

package com.joelapenna.foursquare.types;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @date September 2, 2010
 * @author Mark Wyszomierski (markww@gmail.com)
 */
public class Todo implements FoursquareType, Parcelable {

    private Tip mTip;
    private Venue mVenue;

    public Todo() {
    }

    private Todo(Parcel in) {
        if (in.readInt() == 1) {
            mTip = in.readParcelable(Tip.class.getClassLoader());
        }
        
        if (in.readInt() == 1) {
            mVenue = in.readParcelable(Venue.class.getClassLoader());
        }
    }
    
    public static final Parcelable.Creator<Todo> CREATOR = new Parcelable.Creator<Todo>() {
        public Todo createFromParcel(Parcel in) {
            return new Todo(in);
        }

        @Override
        public Todo[] newArray(int size) {
            return new Todo[size];
        }
    };

    public Tip getTip() {
        return mTip;
    }

    public void setTip(Tip tip) {
        mTip = tip;
    }

    public Venue getVenue() {
        return mVenue;
    }

    public void setVenue(Venue venue) {
        mVenue = venue;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        if (mTip != null) {
            out.writeInt(1);
            out.writeParcelable(mTip, flags);
        } else {
            out.writeInt(0);
        }
        
        if (mVenue != null) {
            out.writeInt(1);
            out.writeParcelable(mVenue, flags);
        } else {
            out.writeInt(0);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
