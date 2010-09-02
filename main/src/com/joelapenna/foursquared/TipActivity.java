/**
 * Copyright 2010 Mark Wyszomierski
 */

package com.joelapenna.foursquared;

import com.joelapenna.foursquare.Foursquare;
import com.joelapenna.foursquare.types.Tip;
import com.joelapenna.foursquare.types.Venue;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Shows actions available for a tip:
 * 
 * <ul>
 *   <li>Add to my to-do list</li>
 *   <li>I've done this!</li>
 * </ul>
 * 
 * The foursquare API doesn't tell us whether we've already marked a tip as
 * to-do or already done, so we just keep presenting the same options to the
 * user every time they look at this screen.
 * 
 * @date March 24, 2010
 * @author Mark Wyszomierski (markww@gmail.com)
 * 
 */
public class TipActivity extends Activity {
    private static final String TAG = "TipActivity";
    private static final boolean DEBUG = FoursquaredSettings.DEBUG;

    public static final String EXTRA_TIP_PARCEL = Foursquared.PACKAGE_NAME
        + ".TipActivity.EXTRA_TIP_PARCEL";
    
    public static final int RESULT_TIP_MARKED_TODO = -2;
    public static final int RESULT_TIP_MARKED_DONE = -3;
    
    private StateHolder mStateHolder;
    private ProgressDialog mDlgProgress;
    

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
        setContentView(R.layout.tip_activity);
        registerReceiver(mLoggedOutReceiver, new IntentFilter(Foursquared.INTENT_ACTION_LOGGED_OUT));
        
        Object retained = getLastNonConfigurationInstance();
        if (retained != null && retained instanceof StateHolder) {
            mStateHolder = (StateHolder) retained;
            mStateHolder.setActivityForTipTask(this);
        } else {
            mStateHolder = new StateHolder();
            if (getIntent().getExtras() != null && 
                getIntent().getExtras().containsKey(EXTRA_TIP_PARCEL)) {
                
                Tip tip = getIntent().getExtras().getParcelable(EXTRA_TIP_PARCEL);
                mStateHolder.setTip(tip);
            } else {
                Log.e(TAG, "TipActivity requires a tip pareclable in its intent extras.");
                finish();
                return;
            }
        }
        
        ensureUi();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        if (mStateHolder.getIsRunningTipTask()) {
            startProgressBar(mStateHolder.getTask());
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        
        if (isFinishing()) {
            unregisterReceiver(mLoggedOutReceiver);
            stopProgressBar();
        }
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        mStateHolder.setActivityForTipTask(null);
        return mStateHolder;
    }

    private void ensureUi() {
        LinearLayout llHeader = (LinearLayout)findViewById(R.id.tipActivityHeaderView);
        llHeader.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showVenueDetailsActivity(mStateHolder.getTip().getVenue());
            }
        });
        
        TextView tvTitle = (TextView)findViewById(R.id.tipActivityName);
        //tvTitle.setText(
        //    getResources().getString(R.string.tip_activity_by) + " " +
        //    StringFormatters.getUserFullName(mStateHolder.getTip().getUser()));
        tvTitle.setText(mStateHolder.getTip().getVenue().getName());
        
        TextView tvAddress = (TextView)findViewById(R.id.tipActivityAddress);
        tvAddress.setText(mStateHolder.getTip().getVenue().getAddress());
        
        TextView tvBody = (TextView)findViewById(R.id.tipActivityBody);
        tvBody.setText(mStateHolder.getTip().getText());
        
        TextView tvDate = (TextView)findViewById(R.id.tipActivityDate);
        tvDate.setText(mStateHolder.getTip().getCreated());
        
        Button btnAddTodoList = (Button)findViewById(R.id.tipActivityyAddTodoList);
        btnAddTodoList.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mStateHolder.startTipTask(TipActivity.this, mStateHolder.getTip().getId(), 
                        TipTask.ACTION_TODO);
            }
        });
        
        Button btnIveDoneThis = (Button)findViewById(R.id.tipActivityIveDoneThis);
        btnIveDoneThis.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mStateHolder.startTipTask(TipActivity.this, mStateHolder.getTip().getId(), 
                        TipTask.ACTION_DONE);
            }
        });
    }
    
    private void showVenueDetailsActivity(Venue venue) {
        //Intent intent = new Intent(this, UserDetailsActivity.class);
        //intent.putExtra(UserDetailsActivity.EXTRA_USER_ID, userId);
        //intent.putExtra(UserDetailsActivity.EXTRA_SHOW_ADD_FRIEND_OPTIONS, true);
        //startActivity(intent);
    }
    
    private void startProgressBar(int task) {
        if (mDlgProgress == null) {
            
            String message = "";
            switch (task) {
                case TipTask.ACTION_TODO:
  //                  message = getResources().getString(
  //                      R.string.tip_activity_action_todo);
                    break;
                case TipTask.ACTION_DONE:
  //                  message = getResources().getString(
  //                      R.string.tip_activity_action_done_this);
                    break;
            }

            mDlgProgress = ProgressDialog.show(
                this, getResources().getString(R.string.tip_activity_prgoress_title), message);
        }
        mDlgProgress.show();
    }

    private void stopProgressBar() {
        if (mDlgProgress != null) {
            mDlgProgress.dismiss();
            mDlgProgress = null;
        }
    }
    
    private void onTipTaskComplete(Tip tip, int type, Exception ex) {
        stopProgressBar();
        mStateHolder.setIsRunningTipTask(false);
        if (tip != null) {
            String message = "";
            switch (type) {
                case TipTask.ACTION_TODO:
                    message = getResources().getString(
                            R.string.tip_activity_prgoress_complete_todo);
                    setResult(RESULT_TIP_MARKED_TODO);
                    break;
                case TipTask.ACTION_DONE:
                    message = getResources().getString(
                            R.string.tip_activity_prgoress_complete_done);
                    setResult(RESULT_TIP_MARKED_DONE);
                    break;
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, ex.toString(), Toast.LENGTH_LONG).show();
        }
    }
    
    private static class TipTask extends AsyncTask<String, Void, Tip> {
        private TipActivity mActivity;
        private String mTipId;
        private int mTask;
        private Exception mReason;
        
        public static final int ACTION_TODO = 0;
        public static final int ACTION_DONE = 1;

        public TipTask(TipActivity activity, String tipid, int task) {
            mActivity = activity;
            mTipId = tipid;
            mTask = task;
        }

        public void setActivity(TipActivity activity) {
            mActivity = activity;
        }

        @Override
        protected void onPreExecute() {
            mActivity.startProgressBar(mTask);
        }
        
        public int getTask() {
            return mTask;
        }

        @Override
        protected Tip doInBackground(String... params) {
            try {
                Foursquared foursquared = (Foursquared) mActivity.getApplication();
                Foursquare foursquare = foursquared.getFoursquare();

                Tip tip = null;
                switch (mTask) {
                    case ACTION_TODO:
                        tip = foursquare.tipMarkTodo(mTipId);
                        break;
                    case ACTION_DONE:
                        tip = foursquare.tipMarkDone(mTipId);
                        break;
                }
                return tip;
                
            } catch (Exception e) {
                if (DEBUG) Log.d(TAG, "TipTask: Exception performing tip task.", e);
                mReason = e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Tip tip) {
            if (DEBUG) Log.d(TAG, "TipTask: onPostExecute()");
            if (mActivity != null) {
                mActivity.onTipTaskComplete(tip, mTask, mReason);
            }
        }

        @Override
        protected void onCancelled() {
            if (mActivity != null) {
                mActivity.onTipTaskComplete(null, mTask, new Exception("Tip task cancelled."));
            }
        }
    }
    
    private static class StateHolder {
        private Tip mTip;
        private TipTask mTipTask;
        private boolean mIsRunningTipTask;
        
        
        public StateHolder() {
            mIsRunningTipTask = false;
        }
        
        public void setTip(Tip tip) { 
            mTip = tip;
        }
        
        public Tip getTip() {
            return mTip;
        }
        
        public int getTask() {
            return mTipTask.getTask();
        }

        public void startTipTask(TipActivity activity, String tipId, int task) {
            mIsRunningTipTask = true;
            mTipTask = new TipTask(activity, tipId, task);
            mTipTask.execute();
        }

        public void setActivityForTipTask(TipActivity activity) {
            if (mTipTask != null) {
                mTipTask.setActivity(activity);
            }
        }
        
        public void setIsRunningTipTask(boolean isRunningTipTask) {
            mIsRunningTipTask = isRunningTipTask;
        }
        
        public boolean getIsRunningTipTask() {
            return mIsRunningTipTask;
        }
    }
}
