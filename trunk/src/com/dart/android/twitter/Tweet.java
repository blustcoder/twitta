package com.dart.android.twitter;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

public class Tweet {
  public String id;
  public String screenName;
  public String text;
  public String profileImageUrl;
  public Date createdAt;
  
  public static Tweet create(JSONObject jsonObject) throws JSONException {
    Tweet tweet = new Tweet();
    
    tweet.id = jsonObject.getLong("id") + "";    
    tweet.text = Utils.decodeTwitterJson(jsonObject.getString("text"));
    tweet.createdAt = parseDateTime(jsonObject.getString("created_at"));
    
    JSONObject user = jsonObject.getJSONObject("user");
    tweet.screenName = Utils.decodeTwitterJson(user.getString("screen_name"));
    tweet.profileImageUrl = user.getString("profile_image_url");
    
    return tweet;
  }
  
  public final static DateFormat TWITTER_DATE_FORMATTER =
      new SimpleDateFormat("E MMM d HH:mm:ss Z yyyy");
     
  public final static Date parseDateTime(String dateString) {
    try {
      return TWITTER_DATE_FORMATTER.parse(dateString);
    } catch (ParseException e) {
      e.printStackTrace();
      return null;
    }
  }  

  public final static DateFormat AGO_FULL_DATE_FORMATTER =
    new SimpleDateFormat("h:mm a MMM d");
  
  public static String getRelativeDate(Date date) {
    Date now = new Date();
    
    // Seconds.
    long diff = (now.getTime() - date.getTime()) / 1000;

    if (diff < 60) {
      return diff + " seconds ago";
    } 
    
    // Minutes.
    diff /= 60;
    
    if (diff <= 1) {
      return "about a minute ago";
    } else if (diff < 60) {
      return "about " + diff + " minutes ago";
    }
    
    // Hours.
    diff /= 60;
    
    if (diff <= 1) {
      return "about an hour ago";
    } else if (diff < 24) {
      return "about " + diff + " hours ago";
    }
    
    return AGO_FULL_DATE_FORMATTER.format(date);        
  }
  
}
