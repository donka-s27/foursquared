/**
 * Copyright 2010 Mark Wyszomierski
 */

package com.joelapenna.foursquared;

import com.joelapenna.foursquare.Foursquare;
import com.joelapenna.foursquare.types.User;
import com.joelapenna.foursquared.location.LocationUtils;
import com.joelapenna.foursquared.util.MenuUtils;
import com.joelapenna.foursquared.util.NotificationsUtil;
import com.joelapenna.foursquared.util.RemoteResourceManager;
import com.joelapenna.foursquared.util.StringFormatters;
import com.joelapenna.foursquared.util.UserUtils;
import com.joelapenna.foursquared.widget.PhotoStrip;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

/**
 * @date March 8, 2010.
 * @author Mark Wyszomierski (markww@gmail.com)
 */
public class UserDetailsActivity extends Activity {
    private static final String TAG = "UserDetailsActivity";
    private static final boolean DEBUG = FoursquaredSettings.DEBUG;
    private static final int ACTIVITY_REQUEST_CODE_GALLERY = 814;

    public static final String EXTRA_USER_PARCEL = Foursquared.PACKAGE_NAME
        + ".UserDetailsActivity.EXTRA_USER_PARCEL";
    public static final String EXTRA_USER_ID = Foursquared.PACKAGE_NAME
        + ".UserDetailsActivity.EXTRA_USER_ID";
    
    public static final String EXTRA_SHOW_ADD_FRIEND_OPTIONS = Foursquared.PACKAGE_NAME
        + ".UserDetailsActivity.EXTRA_SHOW_ADD_FRIEND_OPTIONS";
    

    private static final int LOAD_TYPE_USER_NONE    = 0;
    private static final int LOAD_TYPE_USER_ID      = 1;
    private static final int LOAD_TYPE_USER_PARTIAL = 2;
    private static final int LOAD_TYPE_USER_FULL    = 3;
    
    private static final int MENU_FRIEND_REQUESTS    = 0;
    private static final int MENU_SHOUT              = 1;
    
    private StateHolder mStateHolder;
    private RemoteResourceManager mRrm;
    private RemoteResourceManagerObserver mResourcesObserver;
    private Handler mHandler;

    

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
        setContentView(R.layout.user_details_activity);
        registerReceiver(mLoggedOutReceiver, new IntentFilter(Foursquared.INTENT_ACTION_LOGGED_OUT));

        Object retained = getLastNonConfigurationInstance();
        if (retained != null) {
            mStateHolder = (StateHolder) retained;
            mStateHolder.setActivityForTasks(this);
        } else {

            mStateHolder = new StateHolder();
            if (getIntent().getExtras() != null) {
                if (getIntent().getExtras().containsKey(EXTRA_USER_PARCEL)) {
                    User user = getIntent().getExtras().getParcelable(EXTRA_USER_PARCEL);
                    mStateHolder.setUser(user);
                    mStateHolder.setLoadType(LOAD_TYPE_USER_PARTIAL);
                } else if (getIntent().getExtras().containsKey(EXTRA_USER_ID)) {
                    User user = new User();
                    user.setId(getIntent().getExtras().getString(EXTRA_USER_ID));
                    mStateHolder.setUser(user);
                    mStateHolder.setLoadType(LOAD_TYPE_USER_ID);
                } else {
                    Log.e(TAG, "UserDetailsActivity requires a userid in its intent extras.");
                    finish();
                    return;
                }
                
                mStateHolder.setIsLoggedInUser(
                  mStateHolder.getUser().getId().equals(
                      ((Foursquared) getApplication()).getUserId()));
                
            } else {
                Log.e(TAG, "UserDetailsActivity requires a userid in its intent extras.");
                finish();
                return;
            }
        }
        
        mHandler = new Handler();
        mRrm = ((Foursquared) getApplication()).getRemoteResourceManager();
        mResourcesObserver = new RemoteResourceManagerObserver();
        mRrm.addObserver(mResourcesObserver);

        ensureUi();

        if (mStateHolder.getLoadType() != LOAD_TYPE_USER_FULL && 
           !mStateHolder.getIsRunningUserDetailsTask()) {
            mStateHolder.startTaskUserDetails(this, mStateHolder.getUser().getId());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        
        if (isFinishing()) {
            mStateHolder.cancelTasks();
            mHandler.removeCallbacks(mRunnableUpdateUserPhoto);
            unregisterReceiver(mLoggedOutReceiver);

            RemoteResourceManager rrm = ((Foursquared) getApplication()).getRemoteResourceManager();
            rrm.deleteObserver(mResourcesObserver);
        }
    }

    private void ensureUi() {
        
        View viewProgressBar = findViewById(R.id.venueActivityDetailsProgress);
        TextView tvUsername = (TextView)findViewById(R.id.userDetailsActivityUsername);
        TextView tvLastSeen = (TextView)findViewById(R.id.userDetailsActivityHometownOrLastSeen);
        View viewMayorships = findViewById(R.id.userDetailsActivityGeneralMayorships);
        View viewBadges = findViewById(R.id.userDetailsActivityGeneralBadges);
        View viewTips = findViewById(R.id.userDetailsActivityGeneralTips);
        TextView tvMayorships = (TextView)findViewById(R.id.userDetailsActivityGeneralMayorshipsValue);
        TextView tvBadges = (TextView)findViewById(R.id.userDetailsActivityGeneralBadgesValue);
        TextView tvTips = (TextView)findViewById(R.id.userDetailsActivityGeneralTipsValue);
        ImageView ivMayorshipsChevron = (ImageView)findViewById(R.id.userDetailsActivityGeneralMayorshipsChevron);
        ImageView ivBadgesChevron = (ImageView)findViewById(R.id.userDetailsActivityGeneralBadgesChevron);
        ImageView ivTipsChevron = (ImageView)findViewById(R.id.userDetailsActivityGeneralTipsChevron);
        View viewCheckins = findViewById(R.id.userDetailsActivityCheckins);
        View viewFriendsFollowers = findViewById(R.id.userDetailsActivityFriendsFollowers);
        View viewAddFriends = findViewById(R.id.userDetailsActivityAddFriends);
        View viewTodos = findViewById(R.id.userDetailsActivityTodos);
        View viewFriends = findViewById(R.id.userDetailsActivityFriends);
        TextView tvCheckins = (TextView)findViewById(R.id.userDetailsActivityCheckinsText);
        ImageView ivCheckinsChevron = (ImageView)findViewById(R.id.userDetailsActivityCheckinsChevron);
        TextView tvFriendsFollowers = (TextView)findViewById(R.id.userDetailsActivityFriendsFollowersText);
        ImageView ivFriendsFollowersChevron = (ImageView)findViewById(R.id.userDetailsActivityFriendsFollowersChevron);
        TextView tvTodos = (TextView)findViewById(R.id.userDetailsActivityTodosText);
        ImageView ivTodos = (ImageView)findViewById(R.id.userDetailsActivityTodosChevron);
        TextView tvFriends = (TextView)findViewById(R.id.userDetailsActivityFriendsText);
        ImageView ivFriends = (ImageView)findViewById(R.id.userDetailsActivityFriendsChevron);
        PhotoStrip psFriends = (PhotoStrip)findViewById(R.id.userDetailsActivityFriendsPhotos);
        
        viewProgressBar.setVisibility(View.VISIBLE);
        tvUsername.setText("");
        tvLastSeen.setText("");
        viewMayorships.setFocusable(false);
        viewBadges.setFocusable(false);
        viewTips.setFocusable(false);
        tvMayorships.setText("0");
        tvBadges.setText("0");
        tvTips.setText("0");
        ivMayorshipsChevron.setVisibility(View.INVISIBLE);
        ivBadgesChevron.setVisibility(View.INVISIBLE);
        ivTipsChevron.setVisibility(View.INVISIBLE);

        viewCheckins.setFocusable(false);
        viewFriendsFollowers.setFocusable(false);
        viewAddFriends.setFocusable(false);
        viewTodos.setFocusable(false);
        viewFriends.setFocusable(false);
        viewCheckins.setVisibility(View.GONE);
        viewFriendsFollowers.setVisibility(View.GONE);
        viewAddFriends.setVisibility(View.GONE);
        viewTodos.setVisibility(View.GONE);
        viewFriends.setVisibility(View.GONE);
        ivCheckinsChevron.setVisibility(View.INVISIBLE);
        ivFriendsFollowersChevron.setVisibility(View.INVISIBLE);
        ivTodos.setVisibility(View.INVISIBLE);
        ivFriends.setVisibility(View.INVISIBLE);
        psFriends.setVisibility(View.GONE);
        
        if (mStateHolder.getLoadType() >= LOAD_TYPE_USER_PARTIAL) {
            User user = mStateHolder.getUser();
            
            ensureUiPhoto(user);
        
            if (mStateHolder.getIsLoggedInUser() || UserUtils.isFriend(user)) {
                tvUsername.setText(StringFormatters.getUserFullName(user));
            } else {
                tvUsername.setText(StringFormatters.getUserAbbreviatedName(user));
            }
            
            tvLastSeen.setText(user.getHometown());
        
            if (mStateHolder.getLoadType() == LOAD_TYPE_USER_FULL) {
                viewProgressBar.setVisibility(View.GONE);
                tvMayorships.setText(String.valueOf(user.getMayorCount()));
                tvBadges.setText(String.valueOf(user.getBadgeCount()));
                tvTips.setText(String.valueOf(user.getTipCount()));
                
                if (user.getCheckin() != null && user.getCheckin().getVenue() != null) {
                    tvLastSeen.setText(getResources().getString(
                        R.string.user_details_activity_last_seen, 
                        user.getCheckin().getVenue().getName()));
                }
                
                if (mStateHolder.getIsLoggedInUser() || UserUtils.isFriend(user)) {
                    if (user.getMayorships() != null && user.getMayorships().size() > 0) {
                        viewMayorships.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                startMayorshipsActivity();
                            }
                        });
                        viewMayorships.setFocusable(true);
                        ivMayorshipsChevron.setVisibility(View.VISIBLE);
                    }
                    
                    if (user.getBadges() != null && user.getBadges().size() > 0) {
                        viewBadges.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                startBadgesActivity();
                            }
                        });
                        viewBadges.setFocusable(true);
                        ivBadgesChevron.setVisibility(View.VISIBLE);
                    }
                    
                    if (user.getTipCount() > 0) {
                        viewTips.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Toast.makeText(UserDetailsActivity.this, "Not yet implemented!", Toast.LENGTH_SHORT).show();
                            }
                        });
                        viewTips.setFocusable(true);
                        ivTipsChevron.setVisibility(View.VISIBLE);
                    }
                }
                
                // The rest of the items depend on if we're viewing ourselves or not.
                if (mStateHolder.getIsLoggedInUser()) {
                    viewCheckins.setVisibility(View.VISIBLE);
                    viewFriendsFollowers.setVisibility(View.VISIBLE);
                    viewAddFriends.setVisibility(View.VISIBLE);
                    
                    tvCheckins.setText(
                        user.getCheckinCount() == 1 ? 
                            getResources().getString(
                                R.string.user_details_activity_checkins_text_single, user.getCheckinCount()) :
                            getResources().getString(
                                R.string.user_details_activity_checkins_text_plural, user.getCheckinCount()));
                    if (user.getCheckinCount() > 0) {
                        viewCheckins.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Toast.makeText(UserDetailsActivity.this, "Not yet implemented!", Toast.LENGTH_SHORT).show();
                            }
                        });
                        viewCheckins.setFocusable(true);
                        ivCheckinsChevron.setVisibility(View.VISIBLE);
                    }
                    
                    if (user.getFollowerCount() > 0) {
                        tvFriendsFollowers.setText(
                            user.getFollowerCount() == 1 ? 
                                getResources().getString(
                                    R.string.user_details_activity_friends_followers_text_celeb_single, 
                                    user.getFollowerCount()) :
                                getResources().getString(
                                    R.string.user_details_activity_friends_followers_text_celeb_plural,
                                    user.getFollowerCount()));
                    }
                    
                    tvFriendsFollowers.setText(tvFriendsFollowers.getText().toString() + 
                        (user.getFriendCount() == 1 ?    
                             getResources().getString(
                                 R.string.user_details_activity_friends_followers_text_single,
                                 user.getFriendCount()) :
                             getResources().getString(
                                 R.string.user_details_activity_friends_followers_text_plural,
                                 user.getFriendCount())));
                                    
                    if (user.getFollowerCount() + user.getFriendCount() > 0) {
                        viewFriendsFollowers.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Toast.makeText(UserDetailsActivity.this, "Not yet implemented!", Toast.LENGTH_SHORT).show();
                            }
                        });
                        viewFriendsFollowers.setFocusable(true);
                        ivFriendsFollowersChevron.setVisibility(View.VISIBLE);
                    }
                    
                    viewAddFriends.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Toast.makeText(UserDetailsActivity.this, "Not yet implemented!", Toast.LENGTH_SHORT).show();
                        }
                    });
                    viewAddFriends.setFocusable(true);
                    
                } else {
                    viewTodos.setVisibility(View.VISIBLE);
                    viewFriends.setVisibility(View.VISIBLE); 

                    tvTodos.setText(
                        user.getTodoCount() == 1 ? 
                            getResources().getString(
                                R.string.user_details_activity_todos_text_single, user.getTodoCount()) :
                            getResources().getString(
                                R.string.user_details_activity_todos_text_plural, user.getTodoCount()));

                    if (user.getTodoCount() > 0 && UserUtils.isFriend(user)) {
                        viewTodos.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Toast.makeText(UserDetailsActivity.this, "Not yet implemented!", Toast.LENGTH_SHORT).show();
                            }
                        });
                        viewTodos.setFocusable(true);
                        ivTodos.setVisibility(View.VISIBLE);
                    }
                                     
                    if (user.getFriendCount() > 0) {
                        tvFriends.setText(
                            user.getFriendCount() == 1 ? 
                                getResources().getString(
                                    R.string.user_details_activity_friends_text_single, 
                                    user.getFriendCount()) :
                                getResources().getString(
                                    R.string.user_details_activity_friends_text_plural,
                                    user.getFriendCount()));
                    }
                    
                    int friendsInCommon = user.getFriendsInCommon() == null ? 0 :
                        user.getFriendsInCommon().size();
                    tvFriends.setText(tvFriends.getText().toString() + 
                        (friendsInCommon == 1 ?    
                             getResources().getString(
                                 R.string.user_details_activity_friends_in_common_text_single,
                                 friendsInCommon) :
                             getResources().getString(
                                 R.string.user_details_activity_friends_in_common_text_plural,
                                 friendsInCommon)));
  
                    if (user.getFriendCount() > 0) {
                        viewFriends.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Toast.makeText(UserDetailsActivity.this, "Not yet implemented!", Toast.LENGTH_SHORT).show();
                            }
                        });
                        viewFriends.setFocusable(true);
                        ivFriends.setVisibility(View.VISIBLE);
                        
                        if (friendsInCommon > 0) {
                            psFriends.setVisibility(View.VISIBLE);
                            psFriends.setUsersAndRemoteResourcesManager(user.getFriendsInCommon(), mRrm);
                        } else {
                            tvFriends.setPadding(tvFriends.getPaddingLeft(), tvTodos.getPaddingTop(),
                                    tvFriends.getPaddingRight(), tvTodos.getPaddingBottom());
                        }
                    }
                }
            }
        }
    }
    
    private void ensureUiPhoto(User user) {
        ImageView ivPhoto = (ImageView)findViewById(R.id.userDetailsActivityPhoto);
        
        Uri uriPhoto = Uri.parse(user.getPhoto());
        if (mRrm.exists(uriPhoto)) {
            try {
                Bitmap bitmap = BitmapFactory.decodeStream(mRrm.getInputStream(Uri.parse(user
                        .getPhoto())));
                ivPhoto.setImageBitmap(bitmap);
            } catch (IOException e) {
                setUserPhotoMissing(ivPhoto, user);
            }
        } else {
            mRrm.request(uriPhoto);
            setUserPhotoMissing(ivPhoto, user);
        }
        
        ivPhoto.postInvalidate();
        ivPhoto.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mStateHolder.getLoadType() == LOAD_TYPE_USER_FULL) {
                    User user = mStateHolder.getUser();
                    
                    // If we're viewing our own page, clicking the thumbnail should let the
                    // user choose a new photo from the camera gallery.
                    if (mStateHolder.getIsLoggedInUser()) {
//                        startGalleryIntent();
                        // TODO: Start different activity for setting user photo.
                    }
                    else {
                        // If "_thumbs" exists, remove it to get the url of the
                        // full-size image.
                        String photoUrl = user.getPhoto().replace("_thumbs", "");
    
                        Intent intent = new Intent();
                        intent.setClass(UserDetailsActivity.this, FetchImageForViewIntent.class);
                        intent.putExtra(FetchImageForViewIntent.IMAGE_URL, photoUrl);
                        intent.putExtra(FetchImageForViewIntent.PROGRESS_BAR_TITLE, getResources()
                                .getString(R.string.user_activity_fetch_full_image_title));
                        intent.putExtra(FetchImageForViewIntent.PROGRESS_BAR_MESSAGE, getResources()
                                .getString(R.string.user_activity_fetch_full_image_message));
                        startActivity(intent);
                    }
                }
            }
        });
    }
    
    private void setUserPhotoMissing(ImageView ivPhoto, User user) {
        if (Foursquare.MALE.equals(user.getGender())) {
            ivPhoto.setImageResource(R.drawable.blank_boy);
        } else {
            ivPhoto.setImageResource(R.drawable.blank_girl);
        }
    }
    
    @Override
    public Object onRetainNonConfigurationInstance() {
        mStateHolder.setActivityForTasks(null);
        return mStateHolder;
    }

    private void startBadgesActivity() {
        if (mStateHolder.getUser() != null) {
            Intent intent = new Intent(UserDetailsActivity.this, BadgesActivity.class);
            intent.putParcelableArrayListExtra(BadgesActivity.EXTRA_BADGE_ARRAY_LIST_PARCEL,
                    mStateHolder.getUser().getBadges());
            startActivity(intent);
        }
    }
    
    private void startMayorshipsActivity() {
        if (mStateHolder.getUser() != null) {
            Intent intent = new Intent(UserDetailsActivity.this, UserMayorshipsActivity.class);
            intent.putExtra(UserMayorshipsActivity.EXTRA_USER_ID, mStateHolder.getUser().getId());
            startActivity(intent); 
        }
    }

    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        
        // We have a different set of menu options for the logged-in user vs
        // viewing a friend and potentially a stranger even.
        User user = mStateHolder.getUser();
        if (user != null && user.getId().equals(((Foursquared) getApplication()).getUserId())) {
            menu.add(Menu.NONE, MENU_FRIEND_REQUESTS, Menu.NONE, 
                    R.string.preferences_friend_requests_title).setIcon(R.drawable.ic_menu_friends);
            menu.add(Menu.NONE, MENU_SHOUT, Menu.NONE,  
                    R.string.shout_action_label).setIcon(R.drawable.ic_menu_shout);
            MenuUtils.addPreferencesToMenu(this, menu);
        }
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_FRIEND_REQUESTS:
                startActivity(new Intent(this, FriendRequestsActivity.class));
                return true;
            case MENU_SHOUT:
                Intent intent = new Intent(this, CheckinOrShoutGatherInfoActivity.class);
                intent.putExtra(CheckinOrShoutGatherInfoActivity.INTENT_EXTRA_IS_SHOUT, true);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)  {
        switch (requestCode) {
            case ACTIVITY_REQUEST_CODE_GALLERY:
                if (resultCode == Activity.RESULT_OK) { 
                }
                break;
        }
    }


    private void onUserDetailsTaskComplete(User user, Exception ex) {
        //setProgressBarIndeterminateVisibility(false);
        //mStateHolder.setFetchedUserDetails(true);
        //mStateHolder.setIsRunningUserDetailsTask(false);
        //if (user != null) {
        //    mStateHolder.setUser(user);
        //    populateUiAfterFullUserObjectFetched();
        //} else {
        //    NotificationsUtil.ToastReasonForFailure(this, ex);
        //}
        setProgressBarIndeterminateVisibility(false);
        
        mStateHolder.setIsRunningUserDetailsTask(false);
        if (user != null) {
            mStateHolder.setUser(user);
            mStateHolder.setLoadType(LOAD_TYPE_USER_FULL);
        } else if (ex != null) {
            NotificationsUtil.ToastReasonForFailure(this, ex);
        } else {
            Toast.makeText(this, "A surprising new error has occurred!", Toast.LENGTH_SHORT).show();
        }
        
        ensureUi();
    }
    
    
    /**
     * Even if the caller supplies us with a User object parcelable, it won't
     * have all the badge etc extra info in it. As soon as the activity starts,
     * we launch this task to fetch a full user object, and merge it with
     * whatever is already supplied in mUser.
     */
    private static class UserDetailsTask extends AsyncTask<String, Void, User> {

        private UserDetailsActivity mActivity;
        private Exception mReason;

        public UserDetailsTask(UserDetailsActivity activity) {
            mActivity = activity;
        }

        @Override
        protected void onPreExecute() {
            mActivity.setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected User doInBackground(String... params) {
            try {
                return ((Foursquared) mActivity.getApplication()).getFoursquare().user(
                        params[0],
                        true,
                        true,
                        true,
                        LocationUtils.createFoursquareLocation(((Foursquared) mActivity
                                .getApplication()).getLastKnownLocation()));
            } catch (Exception e) {
                mReason = e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(User user) {
            if (mActivity != null) {
                mActivity.onUserDetailsTaskComplete(user, mReason);
            }
        }

        @Override
        protected void onCancelled() {
            if (mActivity != null) {
                mActivity.onUserDetailsTaskComplete(null, mReason);
            }
        }

        public void setActivity(UserDetailsActivity activity) {
            mActivity = activity;
        }
    }
    
    

    private static class StateHolder {
        private User mUser;
        private boolean mIsLoggedInUser;
        private UserDetailsTask mTaskUserDetails;
        private boolean mIsRunningUserDetailsTask;
        private int mLoadType;
        
        
        public StateHolder() {
            mIsRunningUserDetailsTask = false;
            mIsLoggedInUser = false;
            mLoadType = LOAD_TYPE_USER_NONE;
        }
        
        public boolean getIsLoggedInUser() {
            return mIsLoggedInUser;
        }
        
        public void setIsLoggedInUser(boolean isLoggedInUser) {
            mIsLoggedInUser = isLoggedInUser;
        }

        public User getUser() {
            return mUser;
        }

        public void setUser(User user) {
            mUser = user;
        }
        
        public int getLoadType() {
            return mLoadType;
        }
        
        public void setLoadType(int loadType) {
            mLoadType = loadType;
        }

        public void startTaskUserDetails(UserDetailsActivity activity, String userId) {
            if (!mIsRunningUserDetailsTask) {
                mIsRunningUserDetailsTask = true;
                mTaskUserDetails = new UserDetailsTask(activity);
                mTaskUserDetails.execute(userId);
            }
        }
        
        public void setActivityForTasks(UserDetailsActivity activity) {
            if (mTaskUserDetails != null) {
                mTaskUserDetails.setActivity(activity);
            }
        }
        
        public boolean getIsRunningUserDetailsTask() {
            return mIsRunningUserDetailsTask;
        }

        public void setIsRunningUserDetailsTask(boolean isRunning) {
            mIsRunningUserDetailsTask = isRunning;
        }
        
        public void cancelTasks() {
            if (mTaskUserDetails != null) {
                mTaskUserDetails.setActivity(null);
                mTaskUserDetails.cancel(true);
            }
        }
    }
    
    private class RemoteResourceManagerObserver implements Observer {
        @Override
        public void update(Observable observable, Object data) {
            mHandler.post(mRunnableUpdateUserPhoto);
        }
    }
    
    private Runnable mRunnableUpdateUserPhoto = new Runnable() {
        @Override 
        public void run() {
            ensureUiPhoto(mStateHolder.getUser());
        }
    };
}
