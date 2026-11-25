package org.me.gcu.gaughan_nicholas_s2533284;

import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

// Parses DATEX II feed (unplanned events OR roadworks) into Situation objects
public class SituationParser {

    // Standard W3C identifier for the "xsi" prefix (declared at the top of each feed)
    private static final String XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";

    public List<Situation> parse(String xml, SituationCategory category) {
        List<Situation> situations = new ArrayList<>();
        Situation situation = null;

        // Flags: where are we in the document?
        boolean insideComment = false;
        boolean insideFrom = false;
        boolean insideTo = false;

        // Road name text. Descriptor type comes after
        String lastValue = null;

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(new StringReader(xml));

            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {

                if (eventType == XmlPullParser.START_TAG) {
                    String tag = xpp.getName();

                    if (tag.equalsIgnoreCase("situation")) {
                        situation = new Situation(category);
                        situation.setId(xpp.getAttributeValue(null, "id"));
                    }
                    else if (situation == null) {
                        // Ignore anything before the first <situation> (header etc.)
                    }
                    else if (tag.equalsIgnoreCase("situationRecord")) {
                        situation.setType(xpp.getAttributeValue(XSI_NS, "type"));
                    }
                    else if (tag.equalsIgnoreCase("overallStartTime")) {
                        situation.setStartTime(parseDateTimeOrNull(xpp.nextText()));
                    }
                    else if (tag.equalsIgnoreCase("overallEndTime")) {
                        situation.setEndTime(parseDateTimeOrNull(xpp.nextText()));
                    }
                    else if (tag.equalsIgnoreCase("numberOfOperationalLanes")) {
                        situation.setOperationalLanes(parseIntOrNull(xpp.nextText()));
                    }
                    else if (tag.equalsIgnoreCase("numberOfLanesRestricted")) {
                        situation.setRestrictedLanes(parseIntOrNull(xpp.nextText()));
                    }
                    else if (tag.equalsIgnoreCase("delaysType")) {
                        situation.setDelay(DelayType.fromXml(xpp.nextText()));
                    }
                    else if (tag.equalsIgnoreCase("generalPublicComment")) {
                         insideComment = true;
                    }
                    else if (tag.equalsIgnoreCase("tpegDirection")) {
                        situation.getLocation().setDirection(Direction.fromXml(xpp.nextText()));
                    }
                    else if (tag.equalsIgnoreCase("from")) {
                        insideFrom = true;
                    }
                    else if (tag.equalsIgnoreCase("to")) {
                        insideTo = true;
                    }
                    else if (tag.equalsIgnoreCase("latitude")) {
                        Double lat = parseDoubleOrNull(xpp.nextText());
                        if (insideFrom) {
                            situation.getLocation().setStartLatitude(lat);
                        }
                        else if (insideTo) {
                            situation.getLocation().setEndLatitude(lat);
                        }
                    }
                    else if (tag.equalsIgnoreCase("longitude")) {
                        Double lng = parseDoubleOrNull(xpp.nextText());
                        if (insideFrom) {
                            situation.getLocation().setStartLongitude(lng);
                        }
                        else if (insideTo) {
                            situation.getLocation().setEndLongitude(lng);
                        }
                    }
                    else if (tag.equalsIgnoreCase("value")) {
                        // Flags say which <value> tag
                        if (insideComment) {
                            situation.setDescription(xpp.nextText());
                        }
                        else if (insideFrom || insideTo) {
                            lastValue = xpp.nextText();
                        }
                        // otherwise ignore it
                    }
                    else if (tag.endsWith("PointDescriptorType")) {
                        // Covers: tpegOtherPointDescriptorType; tpegIlcPointDescriptorType; tpegJunctionPointDescriptorType
                        String kind = xpp.nextText().trim();
                        if (lastValue != null && (kind.equals("linkName")
                                || kind.equals("tpegIlcName1") || kind.equals("tpegIlcName2"))) {
                            situation.getLocation().addRoad(lastValue);
                        }
                        lastValue = null;
                    }
                }
                else if (eventType == XmlPullParser.END_TAG) {
                    String tag = xpp.getName();

                    if (tag.equalsIgnoreCase("generalPublicComment")) {
                        insideComment = false;
                    }
                    else if (tag.equalsIgnoreCase("from")) {
                        insideFrom = false;
                    }
                    else if (tag.equalsIgnoreCase("to")) {
                        insideTo = false;
                    }
                    else if (tag.equalsIgnoreCase("situation") && situation != null) {
                        situations.add(situation);
                        situation = null;
                    }
                }
                eventType = xpp.next();
            }
        } catch (XmlPullParserException | IOException e) {
            Log.e("SituationParser", "Parsing error: " + e);
        }

        Log.d("SituationParser", category + ": parsed " + situations.size() + " situations");
        return situations;
    }

    // Helpers -----
    // return null instead of crashing on unexpected text

    private Integer parseIntOrNull(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            Log.w("SituationParser", "Bad integer: " + text);
            return null;
        }
    }

    private Double parseDoubleOrNull(String text) {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            Log.w("SituationParser", "Bad number: " + text);
            return null;
        }
    }

    private OffsetDateTime parseDateTimeOrNull(String text) {
        try {
            return OffsetDateTime.parse(text.trim());   // e.g. 2026-10-04T20:34:08+01:00
        } catch (DateTimeParseException e) {
            Log.w("SituationParser", "Bad date/time: " + text);
            return null;
        }
    }
}
