/**
 * Copyright 2010 Mark Wyszomierski
 */
package com.joelapenna.foursquare.parsers.json;

import com.joelapenna.foursquare.Foursquare;
import com.joelapenna.foursquare.types.Rank;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @date July 13, 2010
 * @author Mark Wyszomierski (markww@gmail.com)
 *
 */
public class RankParser extends AbstractParser<Rank> {

    private static final String TAG = "RankParser";
    private static final boolean DEBUG = Foursquare.PARSER_DEBUG;
    private static final Logger LOG = Logger.getLogger(TipParser.class.getCanonicalName());
    
    @Override
    public Rank parse(JSONObject json) throws JSONException {
        
        if (DEBUG) {
            LOG.log(Level.FINE, "Parser: In " + TAG + ": " + json.toString());
        }
        
        Rank obj = new Rank();
        if (json.has("city")) {
            obj.setCity(json.getString("city"));
        } 
        if (json.has("message")) {
            obj.setMessage(json.getString("message"));
        } 
        if (json.has("position")) {
            obj.setPosition(json.getString("position"));
        }
        
        return obj;
    }
}