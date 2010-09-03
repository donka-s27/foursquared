/**
 * Copyright 2010 Mark Wyszomierski
 */
package com.joelapenna.foursquare.parsers.json;

import com.joelapenna.foursquare.types.FriendInvitesResult;

import org.json.JSONException;
import org.json.JSONObject;


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
            obj.setContactEmailsOnNotOnFoursquare(new EmailsParser().parse(json.getJSONObject("emails")));
        } 
        if (json.has("invited")) {
            obj.setContactEmailsOnNotOnFoursquareAlreadyInvited(new EmailsParser().parse(json.getJSONObject("invited")));
        }
                
        return obj;
    }
}