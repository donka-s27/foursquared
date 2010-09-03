/**
 * Copyright 2010 Mark Wyszomierski
 */
package com.joelapenna.foursquare.parsers.json;

import com.joelapenna.foursquare.Foursquare;
import com.joelapenna.foursquare.types.Badge;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @date July 13, 2010
 * @author Mark Wyszomierski (markww@gmail.com)
 *
 */
public class BadgeParser extends AbstractParser<Badge> {

    private static final String TAG = "BadgeParser";
    private static final boolean DEBUG = Foursquare.PARSER_DEBUG;
    private static final Logger LOG = Logger.getLogger(TipParser.class.getCanonicalName());
    
    @Override
    public Badge parse(JSONObject json) throws JSONException {
        
        if (DEBUG) {
            LOG.log(Level.FINE, "Parser: In " + TAG + ": " + json.toString());
        }
        
        Badge obj = new Badge();
        if (json.has("description")) {
            obj.setDescription(json.getString("description"));
        } 
        if (json.has("icon")) {
            obj.setIcon(json.getString("icon"));
        } 
        if (json.has("id")) {
            obj.setId(json.getString("id"));
        } 
        if (json.has("name")) {
            obj.setName(json.getString("name"));
        }
        
        return obj;
    }
}