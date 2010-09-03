/**
 * Copyright 2010 Mark Wyszomierski
 */
package com.joelapenna.foursquare.parsers.json;

import com.joelapenna.foursquare.Foursquare;
import com.joelapenna.foursquare.types.City;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @date July 13, 2010
 * @author Mark Wyszomierski (markww@gmail.com)
 *
 */
public class CityParser extends AbstractParser<City> {

    private static final String TAG = "CityParser";
    private static final boolean DEBUG = Foursquare.PARSER_DEBUG;
    private static final Logger LOG = Logger.getLogger(TipParser.class.getCanonicalName());
    
    @Override
    public City parse(JSONObject json) throws JSONException {
        
        if (DEBUG) {
            LOG.log(Level.FINE, "Parser: In " + TAG + ": " + json.toString());
        }
        
        City obj = new City();
        if (json.has("geolat")) {
            obj.setGeolat(json.getString("geolat"));
        } 
        if (json.has("geolong")) {
            obj.setGeolong(json.getString("geolong"));
        } 
        if (json.has("id")) {
            obj.setId(json.getString("id"));
        } 
        if (json.has("name")) {
            obj.setName(json.getString("name"));
        } 
        if (json.has("shortname")) {
            obj.setShortname(json.getString("shortname"));
        } 
        if (json.has("timezone")) {
            obj.setTimezone(json.getString("timezone"));
        }
        
        return obj;
    }
}