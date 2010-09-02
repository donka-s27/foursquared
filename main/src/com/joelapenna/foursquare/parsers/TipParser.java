/**
 * Copyright 2009 Joe LaPenna
 */

package com.joelapenna.foursquare.parsers;

import com.joelapenna.foursquare.Foursquare;
import com.joelapenna.foursquare.error.FoursquareError;
import com.joelapenna.foursquare.error.FoursquareParseException;
import com.joelapenna.foursquare.types.Tip;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Auto-generated: 2009-11-13 21:59:24.576039
 *
 * @author Joe LaPenna (joe@joelapenna.com)
 * @param <T>
 */
public class TipParser extends AbstractParser<Tip> {
    private static final Logger LOG = Logger.getLogger(TipParser.class.getCanonicalName());
    private static final boolean DEBUG = Foursquare.PARSER_DEBUG;

    @Override
    public Tip parseInner(XmlPullParser parser) throws XmlPullParserException, IOException,
            FoursquareError, FoursquareParseException {
        parser.require(XmlPullParser.START_TAG, null, null);

        Tip tip = new Tip();

        while (parser.nextTag() == XmlPullParser.START_TAG) {
            String name = parser.getName();
            if ("created".equals(name)) {
                tip.setCreated(parser.nextText());

            } else if ("distance".equals(name)) {
                tip.setDistance(parser.nextText());

            } else if ("id".equals(name)) {
                tip.setId(parser.nextText());

            } else if ("stats".equals(name)) {
                tip.setStats(new TipParser.StatsParser().parse(parser));
                
            } else if ("status".equals(name)) {
                tip.setStatus(parser.nextText());
                
            } else if ("text".equals(name)) {
                tip.setText(parser.nextText());

            } else if ("user".equals(name)) {
                tip.setUser(new UserParser().parse(parser));

            } else if ("venue".equals(name)) {
                tip.setVenue(new VenueParser().parse(parser));

            } else {
                // Consume something we don't understand.
                if (DEBUG) LOG.log(Level.FINE, "Found tag that we don't recognize: " + name);
                skipSubTree(parser);
            }
        }
        return tip;
    }
    
    public static class StatsParser extends AbstractParser<Tip.Stats> {
        private static final Logger LOG = Logger.getLogger(StatsParser.class.getCanonicalName());
        private static final boolean DEBUG = Foursquare.PARSER_DEBUG;

        @Override
        public Tip.Stats parseInner(XmlPullParser parser) throws XmlPullParserException, IOException,
                FoursquareError, FoursquareParseException {
            parser.require(XmlPullParser.START_TAG, null, null);

            Tip.Stats stats = new Tip.Stats();

            while (parser.nextTag() == XmlPullParser.START_TAG) {
                String name = parser.getName();
                if ("donecount".equals(name)) {
                    stats.setDoneCount(Integer.parseInt(parser.nextText()));
                    
                } else if ("todocount".equals(name)) {
                    stats.setTodoCount(Integer.parseInt(parser.nextText()));

                } else {
                    // Consume something we don't understand.
                    if (DEBUG) LOG.log(Level.FINE, "Found tag that we don't recognize: " + name);
                    skipSubTree(parser);
                }
            }
            return stats;
        }
    }
}
