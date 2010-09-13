/**
 * Copyright 2010 Mark Wyszomierski
 */

package com.joelapenna.foursquared;

import com.joelapenna.foursquare.Foursquare;
import com.joelapenna.foursquare.error.FoursquareException;
import com.joelapenna.foursquare.types.Group;
import com.joelapenna.foursquare.types.Tip;
import com.joelapenna.foursquare.types.Todo;
import com.joelapenna.foursquared.app.LoadableListActivityWithView;
import com.joelapenna.foursquared.location.LocationUtils;
import com.joelapenna.foursquared.util.MenuUtils;
import com.joelapenna.foursquared.util.NotificationsUtil;
import com.joelapenna.foursquared.util.TipUtils;
import com.joelapenna.foursquared.widget.SegmentedButton;
import com.joelapenna.foursquared.widget.TodosListAdapter;
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
import android.widget.AdapterView.OnItemClickListener;

import java.util.Observable;
import java.util.Observer;

/**
 * Shows a list of the user's to-dos.
 * 
 * @date September 12, 2010
 * @author Mark Wyszomierski (markww@gmail.com)
 */
public class TodosActivity extends LoadableListActivityWithView {
    static final String TAG = "TodosActivity";
    static final boolean DEBUG = FoursquaredSettings.DEBUG;
    
    private static final int ACTIVITY_TIP = 500;
    
    private StateHolder mStateHolder;
    private TodosListAdapter mListAdapter;
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
        
        // Recent todos is shown first by default so auto-fetch it if necessary.
        if (!mStateHolder.getRanOnceTodosRecent()) {
            mStateHolder.startTaskTodos(this, true);
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
        LayoutInflater inflater = LayoutInflater.from(this);
        mLayoutButtons = (LinearLayout)inflater.inflate(R.layout.todos_activity_buttons, getHeaderLayout());
        mLayoutButtons.setVisibility(View.VISIBLE);
        
        mLayoutEmpty = (ScrollView)LayoutInflater.from(this).inflate(
                R.layout.todos_activity_empty, null);     
        mLayoutEmpty.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT));
        
        
        mListAdapter = new TodosListAdapter(this, 
            ((Foursquared) getApplication()).getRemoteResourceManager());
        if (mStateHolder.getRecentOnly()) {
            mListAdapter.setGroup(mStateHolder.getTodosRecent());
            if (mStateHolder.getTodosRecent().size() == 0) {
                if (mStateHolder.getRanOnceTodosRecent()) {
                    setEmptyView(mLayoutEmpty);
                } else {
                    setLoadingView();
                }
            }
        } else {
            mListAdapter.setGroup(mStateHolder.getTodosNearby());
            if (mStateHolder.getTodosNearby().size() == 0) {
                if (mStateHolder.getRanOnceTodosNearby()) {
                    setEmptyView(mLayoutEmpty);
                } else {
                    setLoadingView();
                }
            }
        }
        
        
        mSegmentedButton = (SegmentedButton)findViewById(R.id.segmented);
        if (mStateHolder.getRecentOnly()) {
            mSegmentedButton.setPushedButtonIndex(0);
        } else {
            mSegmentedButton.setPushedButtonIndex(1);
        }
        
        mSegmentedButton.setOnClickListener(new OnClickListenerSegmentedButton() {
            @Override
            public void onClick(int index) {
                if (index == 0) {
                    mStateHolder.setRecentOnly(true);
                    mListAdapter.setGroup(mStateHolder.getTodosRecent());
                    if (mStateHolder.getTodosRecent().size() < 1) {
                        if (mStateHolder.getRanOnceTodosRecent()) {
                            setEmptyView(mLayoutEmpty);
                        } else {
                            setLoadingView();
                            mStateHolder.startTaskTodos(TodosActivity.this, true);
                        }
                    }
                } else {
                    mStateHolder.setRecentOnly(false);
                    mListAdapter.setGroup(mStateHolder.getTodosNearby());
                    if (mStateHolder.getTodosNearby().size() < 1) {
                        if (mStateHolder.getRanOnceTodosNearby()) {
                            setEmptyView(mLayoutEmpty);
                        } else {
                            setLoadingView();
                            mStateHolder.startTaskTodos(TodosActivity.this, false);
                        }
                    }
                }
                
                mListAdapter.notifyDataSetChanged();
                getListView().setSelection(0);
            }
        });

        ListView listView = getListView();
        listView.setAdapter(mListAdapter);
        listView.setSmoothScrollbarEnabled(false);
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Todo todo = (Todo) parent.getAdapter().getItem(position);
                Intent intent = new Intent(TodosActivity.this, TipActivity.class);
                intent.putExtra(TipActivity.EXTRA_TIP_PARCEL, todo.getTip());
                intent.putExtra(TipActivity.EXTRA_TIP_TODO_PARENT_ID, todo.getId());
                startActivityForResult(intent, ACTIVITY_TIP);
            }
        });
        
        if (mStateHolder.getIsRunningTaskTodosRecent() || 
            mStateHolder.getIsRunningTaskTodosNearby()) {
            setProgressBarIndeterminateVisibility(true);
        } else {
            setProgressBarIndeterminateVisibility(false);
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
                mStateHolder.startTaskTodos(this, mStateHolder.getRecentOnly());
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ACTIVITY_TIP) {
            Tip tip = (Tip)data.getParcelableExtra(TipActivity.EXTRA_TIP_PARCEL_RETURNED);
            String todoId = data.getStringExtra(TipActivity.EXTRA_TIP_TODO_PARENT_ID_RETURNED);
            updateTodo(todoId, tip);
        }
    }
    
    private void updateTodo(String todoId, Tip tip) {
        mStateHolder.updateTodo(todoId, tip);
        getListView().invalidateViews();
    }
    
    private void onStartTaskTodos() {
        if (mListAdapter != null) {
            if (mStateHolder.getRecentOnly()) {
                mStateHolder.setIsRunningTaskTodosRecent(true);
                mListAdapter.setGroup(mStateHolder.getTodosRecent());
            } else {
                mStateHolder.setIsRunningTaskTodosNearby(true);
                mListAdapter.setGroup(mStateHolder.getTodosNearby());
            }
            mListAdapter.notifyDataSetChanged();
        }

        setProgressBarIndeterminateVisibility(true);
        setLoadingView();
    }
    
    private void onTaskTodosComplete(Group<Todo> group, boolean recentOnly, Exception ex) {
        boolean update = false;
        if (group != null) {
            if (recentOnly) {
                mStateHolder.setTodosRecent(group);
                if (mSegmentedButton.getSelectedButtonIndex() == 0) {
                    mListAdapter.setGroup(mStateHolder.getTodosRecent());
                    update = true;
                }
            } else {
                mStateHolder.setTodosNearby(group);
                if (mSegmentedButton.getSelectedButtonIndex() == 1) {
                    mListAdapter.setGroup(mStateHolder.getTodosNearby());
                    update = true;
                }
            }
        }
        else {
            if (recentOnly) {
                mStateHolder.setTodosRecent(new Group<Todo>());
                if (mSegmentedButton.getSelectedButtonIndex() == 0) {
                    mListAdapter.setGroup(mStateHolder.getTodosRecent());
                    update = true;
                }
            } else {
                mStateHolder.setTodosNearby(new Group<Todo>());
                if (mSegmentedButton.getSelectedButtonIndex() == 1) {
                    mListAdapter.setGroup(mStateHolder.getTodosNearby());
                    update = true;
                }
            }
            
            NotificationsUtil.ToastReasonForFailure(this, ex);
        }
        
        if (recentOnly) {
            mStateHolder.setIsRunningTaskTodosRecent(false);
            mStateHolder.setRanOnceTodosRecent(true);
            if (mStateHolder.getTodosRecent().size() == 0 && 
                    mSegmentedButton.getSelectedButtonIndex() == 0) {
                setEmptyView(mLayoutEmpty);
            }
        } else {
            mStateHolder.setIsRunningTaskTodosNearby(false);
            mStateHolder.setRanOnceTodosNearby(true);
            if (mStateHolder.getTodosNearby().size() == 0 &&
                    mSegmentedButton.getSelectedButtonIndex() == 1) {
                setEmptyView(mLayoutEmpty);
            }
        }
        
        if (update) {
            mListAdapter.notifyDataSetChanged();
            getListView().setSelection(0);
        }
        
        if (!mStateHolder.getIsRunningTaskTodosRecent() &&
            !mStateHolder.getIsRunningTaskTodosNearby()) {
            setProgressBarIndeterminateVisibility(false);
        }
    }
    
    /**
     * Gets friends of the current user we're working for.
     */
    private static class TaskTodos extends AsyncTask<Void, Void, Group<Todo>> {

        private TodosActivity mActivity;
        private boolean mRecentOnly;
        private Exception mReason;

        public TaskTodos(TodosActivity activity, boolean friendsOnly) {
            mActivity = activity;
            mRecentOnly = friendsOnly;
        }
        
        @Override
        protected void onPreExecute() {
            mActivity.onStartTaskTodos();
        }

        @Override
        protected Group<Todo> doInBackground(Void... params) {
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
                
                return foursquare.todos(
                        LocationUtils.createFoursquareLocation(loc), 
                        mRecentOnly, !mRecentOnly,
                        30);
            } catch (Exception e) {
                mReason = e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Group<Todo> todos) {
            if (mActivity != null) {
                mActivity.onTaskTodosComplete(todos, mRecentOnly, mReason);
            }
        }

        @Override
        protected void onCancelled() {
            if (mActivity != null) {
                mActivity.onTaskTodosComplete(null, mRecentOnly, mReason);
            }
        }
        
        public void setActivity(TodosActivity activity) {
            mActivity = activity;
        }
    }
    
     
    private static class StateHolder {
        
        private Group<Todo> mTodosRecent;
        private Group<Todo> mTodosNearby;
        private boolean mIsRunningTaskTodosRecent;
        private boolean mIsRunningTaskTodosNearby;
        private boolean mRecentOnly;
        private boolean mRanOnceTodosRecent;
        private boolean mRanOnceTodosNearby;
        private TaskTodos mTaskTodosRecent;
        private TaskTodos mTaskTodosNearby;
        
        
        public StateHolder() {
            mIsRunningTaskTodosRecent = false;
            mIsRunningTaskTodosNearby = false;
            mRanOnceTodosRecent = false;
            mRanOnceTodosNearby = false;
            mTodosRecent = new Group<Todo>();
            mTodosNearby = new Group<Todo>();
            mRecentOnly = true;
        }
        
        public Group<Todo> getTodosRecent() {
            return mTodosRecent;
        }
        
        public void setTodosRecent(Group<Todo> todosRecent) {
            mTodosRecent = todosRecent;
        }
        
        public Group<Todo> getTodosNearby() {
            return mTodosNearby;
        }
        
        public void setTodosNearby(Group<Todo> todosNearby) {
            mTodosNearby = todosNearby;
        }
        
        public void startTaskTodos(TodosActivity activity,
                                   boolean recentOnly) {
            mRecentOnly = recentOnly;
            if (recentOnly) {
                if (mIsRunningTaskTodosRecent) {
                    return;
                }
                mIsRunningTaskTodosRecent = true;
                mTaskTodosRecent = new TaskTodos(activity, recentOnly);
                mTaskTodosRecent.execute();
            } else {
                if (mIsRunningTaskTodosNearby) {
                    return;
                }
                mIsRunningTaskTodosNearby = true;
                mTaskTodosNearby = new TaskTodos(activity, recentOnly);
                mTaskTodosNearby.execute();
            }
        }

        public void setActivity(TodosActivity activity) {
            if (mTaskTodosRecent != null) {
                mTaskTodosRecent.setActivity(activity);
            }
            if (mTaskTodosNearby != null) {
                mTaskTodosNearby.setActivity(activity);
            }
        }

        public boolean getIsRunningTaskTodosRecent() {
            return mIsRunningTaskTodosRecent;
        }
        
        public void setIsRunningTaskTodosRecent(boolean isRunning) {
            mIsRunningTaskTodosRecent = isRunning;
        }
        
        public boolean getIsRunningTaskTodosNearby() {
            return mIsRunningTaskTodosNearby;
        }
        
        public void setIsRunningTaskTodosNearby(boolean isRunning) {
            mIsRunningTaskTodosNearby = isRunning;
        }

        public void cancelTasks() {
            if (mTaskTodosRecent != null) {
                mTaskTodosRecent.setActivity(null);
                mTaskTodosRecent.cancel(true);
            }
            if (mTaskTodosNearby != null) {
                mTaskTodosNearby.setActivity(null);
                mTaskTodosNearby.cancel(true);
            }
        }
        
        public boolean getRecentOnly() {
            return mRecentOnly;
        }
        
        public void setRecentOnly(boolean recentOnly) {
            mRecentOnly = recentOnly;
        }
        
        public boolean getRanOnceTodosRecent() {
            return mRanOnceTodosRecent;
        }
        
        public void setRanOnceTodosRecent(boolean ranOnce) {
            mRanOnceTodosRecent = ranOnce;
        }
        
        public boolean getRanOnceTodosNearby() {
            return mRanOnceTodosNearby;
        }
        
        public void setRanOnceTodosNearby(boolean ranOnce) {
            mRanOnceTodosNearby = ranOnce;
        }
        
        public void updateTodo(String todoId, Tip tip) {
            updateTodoFromArray(todoId, tip, mTodosRecent);
            updateTodoFromArray(todoId, tip, mTodosNearby);
        }
        
        private void updateTodoFromArray(String todoId, Tip tip, Group<Todo> target) {
            for (int i = 0, m = target.size(); i < m; i++) {
                Todo todo = target.get(i);
                if (todo.getId().equals(todoId)) {
                    if (TipUtils.isDone(tip) || !TipUtils.isTodo(tip)) {
                        target.remove(i);       
                    }
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
