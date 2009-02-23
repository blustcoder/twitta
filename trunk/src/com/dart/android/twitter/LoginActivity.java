package com.dart.android.twitter;

import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.dart.android.twitter.TwitterApi.AuthException;
import com.google.android.photostream.UserTask;

public class LoginActivity extends Activity {
  private static final String TAG = "LoginActivity";
  
  // Views.
  private EditText mUsernameEdit;
  private EditText mPasswordEdit;
  
  private TextView mStatusText;
  
  private Button mSigninButton;
  
  private View.OnKeyListener loginEnterHandler = new View.OnKeyListener() {
    public boolean onKey(View v, int keyCode, KeyEvent event) {
      if (keyCode == KeyEvent.KEYCODE_ENTER) {
        if (event.getAction() == KeyEvent.ACTION_UP) {
          doLogin();
        }
        return true;
      }      
      return false;
    }
  };
      
  // Sources.
  private TwitterApi mApi;
  
  // Preferences.
  private SharedPreferences mPreferences;    
  
  // Tasks.
  private UserTask<Void, String, Boolean> mLoginTask;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    
    String username = mPreferences.getString(Preferences.USERNAME_KEY, "");
    String password = mPreferences.getString(Preferences.PASSWORD_KEY, "");
    
    if (TwitterApi.isValidCredentials(username, password)) {
      launchTwitterActivity();
      // the method above finishes the activity.
    }
    
    mApi = new TwitterApi();
    
    setContentView(R.layout.login);
            
    mUsernameEdit = (EditText) findViewById(R.id.username_edit);
    mPasswordEdit = (EditText) findViewById(R.id.password_edit);
    mUsernameEdit.setOnKeyListener(loginEnterHandler);
    mPasswordEdit.setOnKeyListener(loginEnterHandler);
    
    mStatusText = (TextView) findViewById(R.id.status_text);
           
    mSigninButton = (Button) findViewById(R.id.signin_button);
    
    mSigninButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        doLogin();
      }
    });    
  }

  @Override  
  protected void onDestroy() {
    if (mLoginTask != null &&
        mLoginTask.getStatus() == UserTask.Status.RUNNING) {
      mLoginTask.cancel(true);
    }    

    super.onDestroy();    
  }

  void launchTwitterActivity() {
    Intent intent = new Intent(); 
    intent.setClass(this, TwitterActivity.class); 
    startActivity(intent); 
    finish();           
  }
    
  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);  
  }
  
  // UI helpers.
  
  private void updateStatus(String status) {
    mStatusText.setText(status);
  }
    
  private void enableLogin() {
    mUsernameEdit.setEnabled(true);
    mPasswordEdit.setEnabled(true);
    mSigninButton.setEnabled(true);    
  }

  private void disableLogin() {
    mUsernameEdit.setEnabled(false);
    mPasswordEdit.setEnabled(false);
    mSigninButton.setEnabled(false);    
  }

  // Actions.

  private void doLogin() {
    mLoginTask = new LoginTask().execute();
  }      

  private void onLoginBegin() {
    disableLogin();        
  }
  
  private void onLoginSuccess() {
    updateStatus("");
    
    String username = mUsernameEdit.getText().toString();
    String password = mPasswordEdit.getText().toString();
    mUsernameEdit.setText("");
    mPasswordEdit.setText("");
        
    Log.i(TAG, "Storing credentials.");
    SharedPreferences.Editor editor = mPreferences.edit();
    editor.putString(Preferences.USERNAME_KEY, username);
    editor.putString(Preferences.PASSWORD_KEY, password);    
    editor.commit();
    
    launchTwitterActivity();
  }

  private void onLoginFailure() {
    enableLogin();
  }
  
  private class LoginTask extends UserTask<Void, String, Boolean> {
    @Override
    public void onPreExecute() {
      onLoginBegin();      
    }

    @Override    
    public Boolean doInBackground(Void... params) {
      String username = mUsernameEdit.getText().toString();
      String password = mPasswordEdit.getText().toString();
            
      publishProgress("Logging in...");

      if (!TwitterApi.isValidCredentials(username, password)) {
        publishProgress("Invalid username or password");
        return false;
      }    
      
      if (isCancelled()) {
        return false;        
      }
      
      try {
        mApi.login(username, password);
      } catch (IOException e) {
        e.printStackTrace();
        publishProgress("Network or connection error");
        return false;
      } catch (AuthException e) {
        publishProgress("Invalid username or password");
        return false;
      }                
            
      return true;
    }

    @Override    
    public void onProgressUpdate(String... progress) {
      updateStatus(progress[0]);
    }
    
    @Override
    public void onPostExecute(Boolean result) {
      if (result) { 
        onLoginSuccess();
      } else {
        onLoginFailure();
      }      
    }
  }
    
}