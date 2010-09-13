/**
 * Copyright 2010 Mark Wyszomierski
 */

package com.joelapenna.foursquared;

import com.joelapenna.foursquare.Foursquare;
import com.joelapenna.foursquare.error.FoursquareException;
import com.joelapenna.foursquare.types.Group;
import com.joelapenna.foursquare.types.Tip;
import com.joelapenna.foursquared.app.LoadableListActivityWithView;
import com.joelapenna.foursquared.location.LocationUtils;
import com.joelapenna.foursquared.util.MenuUtils;
import com.joelapenna.foursquared.util.NotificationsUtil;
import com.joelapenna.foursquared.widget.SegmentedButton;
import com.joelapenna.foursquared.widget.TipsListAdapter;
import com.joelapenna.foursquared.widget.SegmentedButton.OnClickListenerSegmentedButton;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import java.util.Observable;
import java.util.Observer;

/**
 * Shows a list of nearby tips. User can sort tips by friends-only.
 * 
 * @date August 31, 2010
 * @author Mark Wyszomierski (markww@gmail.com)
 */
public class TipsActivity extends LoadableListActivityWithView {
    static final String TAG = "TipsActivity";
    static final boolean DEBUG = FoursquaredSettings.DEBUG;
    
    private static final int ACTIVITY_TIP = 500;
    
    private StateHolder mStateHolder;
    private TipsListAdapter mListAdapter;
    private SearchLocationObserver mSearchLocationObserver = new SearchLocationObserver();
    private LinearLayout mLayoutButtons;
    private SegmentedButton mSegmentedButton;
    private ScrollView mLayoutEmpty;
    
    private static final int MENU_REFRESH = 0;


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
            mStateHolder.setActivity(this);
        } else {
            mStateHolder = new StateHolder();
        }

        ensureUi();
        
        if (!mStateHolder.getRanOnce()) {
            mStateHolder.startTaskTips(this, true);
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();

        ((Foursquared) getApplication()).requestLocationUpdates(mSearchLocationObserver);
    }

    @Override
    public void onPause() {
        super.onPause();
        
        ((Foursquared) getApplication()).removeLocationUpdates(mSearchLocationObserver);
        
        if (isFinishing()) {
            mStateHolder.cancelTasks();
            mListAdapter.removeObserver();
            unregisterReceiver(mLoggedOutReceiver);
        }
    }
    
    @Override
    public Object onRetainNonConfigurationInstance() {
        mStateHolder.setActivity(null);
        return mStateHolder;
    }

    private void ensureUi() {
        mListAdapter = new TipsListAdapter(this, 
            ((Foursquared) getApplication()).getRemoteResourceManager());
        if (mStateHolder.getFriendsOnly()) {
            mListAdapter.setGroup(mStateHolder.getTipsFriends());
        } else {
            mListAdapter.setGroup(mStateHolder.getTipsEveryone());
        }
        
        LayoutInflater inflater = LayoutInflater.from(this);
        mLayoutButtons = (LinearLayout)inflater.inflate(R.layout.tips_activity_buttons, getHeaderLayout());
        mLayoutButtons.setVisibility(View.VISIBLE);
        
        mSegmentedButton = (SegmentedButton)findViewById(R.id.segmented);
        if (mStateHolder.mFriendsOnly) {
            mSegmentedButton.setPushedButtonIndex(0);
        } else {
            mSegmentedButton.setPushedButtonIndex(1);
        }
        
        mSegmentedButton.setOnClickListener(new OnClickListenerSegmentedButton() {
            @Override
            public void onClick(int index) {
                if (index == 0) {
                    if (mStateHolder.getTipsFriends().size() < 1) {
                        mStateHolder.startTaskTips(TipsActivity.this, true);
                    } else {
                        mStateHolder.setFriendsOnly(true);
                        mListAdapter.setGroup(mStateHolder.getTipsFriends());
                    } 
                } else {
                    if (mStateHolder.getTipsEveryone().size() < 1) {
                        mStateHolder.startTaskTips(TipsActivity.this, false);
                    } else {
                        mStateHolder.setFriendsOnly(false);
                        mListAdapter.setGroup(mStateHolder.getTipsEveryone());
                    }
                }
            }
        });

        ListView listView = getListView();
        listView.setAdapter(mListAdapter);
        listView.setSmoothScrollbarEnabled(false);
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Tip tip = (Tip) parent.getAdapter().getItem(position);
                Intent intent = new Intent(TipsActivity.this, TipActivity.class);
                intent.putExtra(TipActivity.EXTRA_TIP_PARCEL, tip);
                startActivityForResult(intent, ACTIVITY_TIP);
            }
        });
        
        mLayoutEmpty = (ScrollView)LayoutInflater.from(this).inflate(
                R.layout.tips_activity_empty, null);     
        mLayoutEmpty.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT));
        
        if (!mStateHolder.getRanOnce() || mStateHolder.getIsRunningTaskTips()) {
            setProgressBarIndeterminateVisibility(true);
            setLoadingView();
        }
        else {
            setProgressBarIndeterminateVisibility(false);
            if (mStateHolder.getFriendsOnly()) {
                if (mStateHolder.getTipsFriends().size() == 0) {
                    setEmptyView(mLayoutEmpty);
                }
            } else {
                if (mStateHolder.getTipsEveryone().size() == 0) {
                    setEmptyView(mLayoutEmpty);
                }
            }
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        
        menu.add(Menu.NONE, MENU_REFRESH, Menu.NONE, R.string.tips_activity_menu_refresh)
            .setIcon(R.drawable.ic_menu_refresh);
        MenuUtils.addPreferencesToMenu(this, menu);
        
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_REFRESH:
                mStateHolder.startTaskTips(this, mStateHolder.getFriendsOnly());
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ACTIVITY_TIP) {
            updateTip((Tip)data.getParcelableExtra(TipActivity.EXTRA_TIP_PARCEL_RETURNED));
        }
    }
    
    private void updateTip(Tip tip) {
        mStateHolder.updateTip(tip);
        getListView().invalidateViews();
    }
    
    private void onStartTaskTips() {
        mStateHolder.setIsRunningTaskTips(true);
        
        if (mListAdapter != null) {
            mListAdapter.removeObserver();
            mListAdapter = new TipsListAdapter(this, 
                    ((Foursquared) getApplication()).getRemoteResourceManager());
            if (mStateHolder.getFriendsOnly()) {
                mListAdapter.setGroup(mStateHolder.getTipsFriends());
            } else {
                mListAdapter.setGroup(mStateHolder.getTipsEveryone());
            }
            
            getListView().setAdapter(mListAdapter);
        }
        
        setProgressBarIndeterminateVisibility(true);
        setLoadingView();
    }
    
    private void onTaskTipsComplete(Group<Tip> group, boolean friendsOnly, Exception ex) {
        mListAdapter.removeObserver();
        mListAdapter = new TipsListAdapter(this, 
            ((Foursquared) getApplication()).getRemoteResourceManager());
        if (group != null) {
            if (friendsOnly) {
                mStateHolder.setTipsFriends(group);
                mListAdapter.setGroup(mStateHolder.getTipsFriends());
            } else {
                mStateHolder.setTipsEveryone(group);
                mListAdapter.setGroup(mStateHolder.getTipsEveryone());
            }
        }
        else {
            if (friendsOnly) {
                mStateHolder.setTipsFriends(new Group<Tip>());
                mListAdapter.setGroup(mStateHolder.getTipsFriends());
            } else {
                mStateHolder.setTipsEveryone(new Group<Tip>());
                mListAdapter.setGroup(mStateHolder.getTipsEveryone());
            }
            
            NotificationsUtil.ToastReasonForFailure(this, ex);
        }
        getListView().setAdapter(mListAdapter);
        
        mStateHolder.setIsRunningTaskTips(false);
        mStateHolder.setRanOnce(true);
        setProgressBarIndeterminateVisibility(false);
        
        if (mStateHolder.getFriendsOnly()) {
            if (mStateHolder.getTipsFriends().size() == 0) {
                setEmptyView(mLayoutEmpty);
            }
        } else {
            if (mStateHolder.getTipsEveryone().size() == 0) {
                setEmptyView(mLayoutEmpty);
            }
        }
    }
    
    /**
     * Gets friends of the current user we're working for.
     */
    private static class TaskTips extends AsyncTask<Void, Void, Group<Tip>> {

        private TipsActivity mActivity;
        private boolean mFriendsOnly;
        private Exception mReason;

        public TaskTips(TipsActivity activity, boolean friendsOnly) {
            mActivity = activity;
            mFriendsOnly = friendsOnly;
        }
        
        @Override
        protected void onPreExecute() {
            mActivity.onStartTaskTips();
        }

        @Override
        protected Group<Tip> doInBackground(Void... params) {
            try {
                Foursquared foursquared = (Foursquared) mActivity.getApplication();
                Foursquare foursquare = foursquared.getFoursquare();
             
                Location loc = foursquared.getLastKnownLocation();
                if (loc == null) {
                    try { Thread.sleep(3000); } catch (InterruptedException ex) {}
                    loc = foursquared.getLastKnownLocation();
                    if (loc == null) {
                        throw new FoursquareException("Your location could not be determined!");
                    }
                } 
                
                return foursquare.tips(
                        LocationUtils.createFoursquareLocation(loc), 
                        mFriendsOnly ? "friends" : "nearby",
                        30);
            } catch (Exception e) {
                mReason = e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Group<Tip> tips) {
            if (mActivity != null) {
                mActivity.onTaskTipsComplete(tips, mFriendsOnly, mReason);
            }
        }

        @Override
        protected void onCancelled() {
            if (mActivity != null) {
                mActivity.onTaskTipsComplete(null, mFriendsOnly, mReason);
            }
        }
        
        public void setActivity(TipsActivity activity) {
            mActivity = activity;
        }
    }
    
    
    private static class StateHolder {
        
        /** Tips by friends. */
        private Group<Tip> mTipsFriends;
        
        /** Tips by everyone. */
        private Group<Tip> mTipsEveryone;
        
        private TaskTips mTaskTips;
        private boolean mIsRunningTaskTips;
        
        private boolean mFriendsOnly;

        private boolean mRanOnce;
        
        
        public StateHolder() {
            mIsRunningTaskTips = false;
            mRanOnce = false;
            mTipsFriends = new Group<Tip>();
            mTipsEveryone = new Group<Tip>();
            mFriendsOnly = true;
        }
        
        public Group<Tip> getTipsFriends() {
            return mTipsFriends;
        }
        
        public void setTipsFriends(Group<Tip> tipsFriends) {
            mTipsFriends = tipsFriends;
        }
        
        public Group<Tip> getTipsEveryone() {
            return mTipsEveryone;
        }
        
        public void setTipsEveryone(Group<Tip> tipsEveryone) {
            mTipsEveryone = tipsEveryone;
        }
        
        public void startTaskTips(TipsActivity activity,
                                  boolean friendsOnly) {
            mFriendsOnly = friendsOnly;
            mIsRunningTaskTips = true;
            mTaskTips = new TaskTips(activity, friendsOnly);
            mTaskTips.execute();
        }

        public void setActivity(TipsActivity activity) {
            if (mTaskTips != null) {
                mTaskTips.setActivity(activity);
            }
        }

        public boolean getIsRunningTaskTips() {
            return mIsRunningTaskTips;
        }
        
        public void setIsRunningTaskTips(boolean isRunning) {
            mIsRunningTaskTips = isRunning;
        }

        public void cancelTasks() {
            if (mTaskTips != null) {
                mTaskTips.setActivity(null);
                mTaskTips.cancel(true);
            }
        }
        
        public boolean getFriendsOnly() {
            return mFriendsOnly;
        }
        
        public void setFriendsOnly(boolean friendsOnly) {
            mFriendsOnly = friendsOnly;
        }
        
        public boolean getRanOnce() {
            return mRanOnce;
        }
        
        public void setRanOnce(boolean ranOnce) {
            mRanOnce = ranOnce;
        }
        
        public void updateTip(Tip tip) {
            updateTipFromArray(tip, mTipsFriends);
            updateTipFromArray(tip, mTipsEveryone);
        }
        
        private void updateTipFromArray(Tip tip, Group<Tip> target) {
            for (Tip it : target) {
                if (it.getId().equals(tip.getId())) {
                    it.setStatus(tip.getStatus());
                    break;
                }
            }
        }
    }
    
    /** 
     * This is really just a dummy observer to get the GPS running
     * since this is the new splash page. After getting a fix, we
     * might want to stop registering this observer thereafter so
     * it doesn't annoy the user too much.
     */
    private class SearchLocationObserver implements Observer {
        @Override
        public void update(Observable observable, Object data) {
        }
    }
}
