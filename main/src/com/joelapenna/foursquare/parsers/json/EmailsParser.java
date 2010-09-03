/**
 * Copyright 2010 Mark Wyszomierski
 */
package com.joelapenna.foursquare.parsers.json;

import com.joelapenna.foursquare.types.Emails;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * @date July 13, 2010
 * @author Mark Wyszomierski (markww@gmail.com)
 *
 */
public class EmailsParser extends AbstractParser<Emails> {
    
    @Override
    public Emails parse(JSONObject json) throws JSONException {
        Emails obj = new Emails();
        if (json.has("email")) {
            obj.add(json.getString("email"));
        } 
        
        return obj;
    }
}