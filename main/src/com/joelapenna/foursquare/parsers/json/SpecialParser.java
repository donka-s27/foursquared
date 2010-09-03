/**
 * Copyright 2010 Mark Wyszomierski
 */
package com.joelapenna.foursquare.parsers.json;

import com.joelapenna.foursquare.Foursquare;
import com.joelapenna.foursquare.types.Special;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @date July 13, 2010
 * @author Mark Wyszomierski (markww@gmail.com)
 *
 */
public class SpecialParser extends AbstractParser<Special> {

    private static final String TAG = "SpecialParser";
    private static final boolean DEBUG = Foursquare.PARSER_DEBUG;
    private static final Logger LOG = Logger.getLogger(TipParser.class.getCanonicalName());
    
    @Override
    public Special parse(JSONObject json) throws JSONException {
        
        if (DEBUG) {
            LOG.log(Level.FINE, "Parser: In " + TAG + ": " + json.toString());
        }
        
        Special obj = new Special();
        if (json.has("id")) {
            obj.setId(json.getString("id"));
        } 
        if (json.has("message")) {
            obj.setMessage(json.getString("message"));
        } 
        if (json.has("type")) {
            obj.setType(json.getString("type"));
        } 
        if (json.has("venue")) {
            obj.setVenue(new VenueParser().parse(json.getJSONObject("venue")));
        }
        
        return obj;
    }
}