/**
 * Copyright 2010 Mark Wyszomierski
 */
package com.joelapenna.foursquare.parsers.json;

import com.joelapenna.foursquare.types.Emails;
import com.joelapenna.foursquare.types.FriendInvitesResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;


/**
 * @date July 13, 2010
 * @author Mark Wyszomierski (markww@gmail.com)
 *
 */
public class FriendInvitesResultParser extends AbstractParser<FriendInvitesResult> {
    
    @Override
    public FriendInvitesResult parse(JSONObject json) throws JSONException {
        
        FriendInvitesResult obj = new FriendInvitesResult();
        if (json.has("users")) {
            obj.setContactsOnFoursquare(
                new GroupParser(
                    new UserParser()).parse(json.getJSONArray("users")));
        } 
        if (json.has("emails")) {
            Emails emails = new Emails();
            JSONArray array = json.getJSONArray("emails");
            for (int i = 0; i < array.length(); i++) {
                emails.add(emails.get(i));
            }
            obj.setContactEmailsOnNotOnFoursquare(emails);
        }
        if (json.has("invited")) {
            Emails emails = new Emails();
            JSONArray array = json.getJSONArray("invited");
            for (int i = 0; i < array.length(); i++) {
                emails.add(emails.get(i));
            }
            obj.setContactEmailsOnNotOnFoursquareAlreadyInvited(emails);
        }
        return obj;
    }
}