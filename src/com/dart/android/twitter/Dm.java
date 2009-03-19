package com.dart.android.twitter;

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

public class Dm {
  @SuppressWarnings("unused")
  private static final String TAG = "Dm";
  
  public String id;
  public String screenName;
  public String text;
  public String profileImageUrl;
  public Date createdAt;

  public static Dm create(JSONObject jsonObject) throws JSONException {
    Dm dm = new Dm();
    
    dm.id = jsonObject.getLong("id") + "";    
    dm.text = Utils.decodeTwitterJson(jsonObject.getString("text"));
    dm.createdAt = Utils.parseDateTime(jsonObject.getString("created_at"));
    
    JSONObject user = jsonObject.getJSONObject("sender");
    dm.screenName = Utils.decodeTwitterJson(user.getString("screen_name"));
    dm.profileImageUrl = user.getString("profile_image_url");
    
    return dm;
  }
  
}