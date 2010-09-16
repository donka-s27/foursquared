/**
 * Copyright 2009 Joe LaPenna
 */

package com.joelapenna.foursquared;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.joelapenna.foursquare.types.Group;
import com.joelapenna.foursquare.types.Tip;
import com.joelapenna.foursquare.types.Venue;
import com.joelapenna.foursquared.app.LoadableListActivity;
import com.joelapenna.foursquared.widget.SeparatedListAdapter;
import com.joelapenna.foursquared.widget.TipsListAdapter;

/**
 * @author Joe LaPenna (joe@joelapenna.com)
 * @author Mark Wyszomierski (markww@gmail.com)
 *   -modified to start TipActivity on tip click (2010-03-25)
 *   -added photos for tips (2010-03-25)
 *   -refactored for new VenueActivity design (2010-09-16)
 */
public class VenueTipsActivity extends LoadableListActivity {
    
	public static final String TAG = "VenueTipsActivity";
    public static final boolean DEBUG = FoursquaredSettings.DEBUG;

    public static final String INTENT_EXTRA_TIPS = Foursquared.PACKAGE_NAME
            + ".VenueTipsActivity.INTENT_EXTRA_TIPS";
    public static final String INTENT_EXTRA_VENUE = Foursquared.PACKAGE_NAME
            + ".VenueTipsActivity.INTENT_EXTRA_VENUE";

    private SeparatedListAdapter mListAdapter;
    private StateHolder mStateHolder;

        
    private BroadcastReceiver mLoggedOutReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) Log.d(TAG, "onReceive: " + intent);
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerReceiver(mLoggedOutReceiver, new IntentFilter(Foursquared.INTENT_ACTION_LOGGED_OUT));

        Object retained = getLastNonConfigurationInstance();
        if (retained != null && retained instanceof StateHolder) {
            mStateHolder = (StateHolder) retained;
        } else {
            mStateHolder = new StateHolder();
            if (getIntent().hasExtra(INTENT_EXTRA_VENUE) && getIntent().hasExtra(INTENT_EXTRA_TIPS)) {
            	mStateHolder.setTips((Venue)getIntent().getExtras().getParcelable(INTENT_EXTRA_TIPS));
            	mStateHolder.setVenue((Venue)getIntent().getExtras().getParcelable(INTENT_EXTRA_VENUE));
            } else {
                Log.e(TAG, "VenueTipsActivity requires a venue parcel its intent extras.");
                finish();
                return;
            }
        }
        
        ensureUi();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        
        if (isFinishing()) {
            mListAdapter.removeObserver();
            unregisterReceiver(mLoggedOutReceiver);
        }
    }
    
    @Override
    public Object onRetainNonConfigurationInstance() {
        return mStateHolder;
    }

    private void ensureUi() {
    	
    	Group<Tip> tips = mStateHolder.getVenue().getTips();

    	TipsListAdapter groupAdapter = new TipsListAdapter(this,
                ((Foursquared) getApplication()).getRemoteResourceManager());
        groupAdapter.setGroup(tips);
        
        String title = getResources().getString(R.string.venue_tips_activity_title, tips.size());
        
    	mListAdapter = new SeparatedListAdapter(this);
        mListAdapter.addSection(title, groupAdapter);
        
        ListView listView = getListView();
        listView.setAdapter(mListAdapter);
        listView.setSmoothScrollbarEnabled(true);
        listView.setDividerHeight(0);
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            	// The tip object doesn't have its venue member set, since it came down from
            	// the /venue api call. TipActivity expects the venue member to be set, so we
            	// need to create a new copy of the venue for the tip.
            	Tip tip = (Tip)parent.getAdapter().getItem(position);
            	tip.setVenue(mStateHolder.getTips());
                
                Intent intent = new Intent(VenueTipsActivity.this, TipActivity.class);
                intent.putExtra(TipActivity.EXTRA_TIP_PARCEL, tip);
                startActivity(intent);
            }
        });
    }
    
    private static class StateHolder {
        
        private Venue mVenue;
        private Venue mTips;
        
        public StateHolder() {
        }
 
        public Venue getVenue() {
            return mVenue;
        }
        
        public void setVenue(Venue venue) {
        	mVenue = venue;
        }
        
        public Venue getTips() {
            return mTips;
        }
        
        public void setTips(Venue tips) {
        	mTips = tips;
        }
    }
}
