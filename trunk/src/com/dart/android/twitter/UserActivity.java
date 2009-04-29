package com.dart.android.twitter;

import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.dart.android.twitter.TwitterApi.ApiException;
import com.dart.android.twitter.TwitterApi.AuthException;
import com.google.android.photostream.UserTask;

public class UserActivity extends BaseActivity {

  private static final String TAG = "UserActivity";

  private String mUser;
  
  private TweetArrayAdapter mAdapter;
  
  // Views.
  private ListView mTweetList;
  private TextView mProgressText;  
  private TextView mUserText;
  private ImageView mProfileImage;
  
  // Tasks.
  private UserTask<Void, Void, TaskResult> mRetrieveTask;

  private static final String EXTRA_USER = "user";

  private static final String LAUNCH_ACTION = "com.dart.android.twitter.USER";

  public static Intent createIntent(String user) {    
    Intent intent = new Intent(LAUNCH_ACTION);    
    intent.putExtra(EXTRA_USER, user);
    
    return intent;
  }
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (!getApi().isLoggedIn()) {
      Log.i(TAG, "Not logged in.");
      handleLoggedOut();
      return;
    }
    
    setContentView(R.layout.user);
    
    mTweetList = (ListView) findViewById(R.id.tweet_list);        
    mProgressText = (TextView) findViewById(R.id.progress_text);
    mUserText = (TextView) findViewById(R.id.tweet_user_text);
    mProfileImage = (ImageView) findViewById(R.id.profile_image);
    
    Intent intent = getIntent();
    Uri data = intent.getData();
    
    mUser = intent.getStringExtra(EXTRA_USER);
    
    if (TextUtils.isEmpty(mUser)) {    
      mUser = data.getLastPathSegment();
    }        

    setTitle(mUser);
    mUserText.setText(mUser);
    
    mAdapter = new TweetArrayAdapter(this);
    mTweetList.setAdapter(mAdapter);
    
    doRetrieve();    
  }
  
  @Override
  protected void onResume() {
    super.onResume();
    
    if (!getApi().isLoggedIn()) {
      Log.i(TAG, "Not logged in.");
      handleLoggedOut();
      return;
    }               
  }
  
  private static final String SIS_RUNNING_KEY = "running";

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    if (mRetrieveTask != null
        && mRetrieveTask.getStatus() == UserTask.Status.RUNNING) {
      outState.putBoolean(SIS_RUNNING_KEY, true);
    }
  }
  
  @Override
  protected void onDestroy() {
    Log.i(TAG, "onDestroy.");

    if (mRetrieveTask != null
        && mRetrieveTask.getStatus() == UserTask.Status.RUNNING) {
      mRetrieveTask.cancel(true);
    }

    super.onDestroy();
  }
  

  // UI helpers.

  private void updateProgress(String progress) {
    mProgressText.setText(progress);
  }
    
  private void update(ArrayList<Tweet> tweets) {
    if (tweets.size() > 0) {
      String imageUrl = tweets.get(0).profileImageUrl;
      
      if (!TextUtils.isEmpty(imageUrl)) {
        mProfileImage.setImageBitmap(getImageManager().get(imageUrl));
      }
    }
    
    mAdapter.refresh(tweets);
    mTweetList.setSelection(0);
  }
  
  
  private enum TaskResult {
    OK, IO_ERROR, AUTH_ERROR, CANCELLED, NOT_FOLLOWED_ERROR
  }
  
  private void doRetrieve() {
    Log.i(TAG, "Attempting retrieve.");

    if (mRetrieveTask != null
        && mRetrieveTask.getStatus() == UserTask.Status.RUNNING) {
      Log.w(TAG, "Already retrieving.");
    } else {
      mRetrieveTask = new RetrieveTask().execute();
    }
  }

  private void onRetrieveBegin() {
    updateProgress("Refreshing...");
  }

  private void onAuthFailure() {
    logout();
  }
  
  private class RetrieveTask extends UserTask<Void, Void, TaskResult> {
    @Override
    public void onPreExecute() {
      onRetrieveBegin();
    }

    ArrayList<Tweet> mTweets = new ArrayList<Tweet>();

    @Override
    public TaskResult doInBackground(Void... params) {
      JSONArray jsonArray;

      TwitterApi api = getApi();
      ImageManager imageManager = getImageManager();
      
      try {
        jsonArray = api.getUserTimeline(mUser);
      } catch (IOException e) {
        Log.e(TAG, e.getMessage(), e);
        return TaskResult.IO_ERROR;
      } catch (AuthException e) {
        Log.i(TAG, "Invalid authorization.");
        return TaskResult.AUTH_ERROR;
      } catch (ApiException e) {
        Log.e(TAG, e.getMessage(), e);
        return TaskResult.IO_ERROR;
      }

      for (int i = 0; i < jsonArray.length(); ++i) {
        if (isCancelled()) {
          return TaskResult.CANCELLED;
        }

        Tweet tweet;

        try {
          JSONObject jsonObject = jsonArray.getJSONObject(i);
          tweet = Tweet.create(jsonObject);
          mTweets.add(tweet);
        } catch (JSONException e) {
          Log.e(TAG, e.getMessage(), e);
          return TaskResult.IO_ERROR;
        }

        if (isCancelled()) {
          return TaskResult.CANCELLED;
        }

        if (!Utils.isEmpty(tweet.profileImageUrl)) {
          // Fetch image to cache.
          try {
            imageManager.put(tweet.profileImageUrl);
          } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
          }
        }
      }

      if (isCancelled()) {
        return TaskResult.CANCELLED;
      }

      return TaskResult.OK;
    }

    @Override
    public void onPostExecute(TaskResult result) {
      if (result == TaskResult.AUTH_ERROR) {
        onAuthFailure();
      } else if (result == TaskResult.OK) {
        update(mTweets);
      } else {
        // Do nothing.
      }

      updateProgress("");
    }    
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuItem item = menu.add(0, OPTIONS_MENU_ID_REFRESH, 0, R.string.refresh);
    item.setIcon(R.drawable.refresh);

    // TODO:
    /*
    item = menu.add(0, OPTIONS_MENU_ID_DM, 0, R.string.dm);
    item.setIcon(android.R.drawable.ic_menu_send);
    */

    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case OPTIONS_MENU_ID_REFRESH:
      doRetrieve();
      return true;
    }

    return super.onOptionsItemSelected(item);
  }
  
}