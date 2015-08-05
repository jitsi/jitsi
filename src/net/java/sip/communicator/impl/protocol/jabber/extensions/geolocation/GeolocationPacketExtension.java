/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.geolocation;

import org.jivesoftware.smack.packet.*;

/**
 * This class implements the Geolocation Extension defined in the XEP-0080
 *
 * @author Guillaume Schreiner
 */
public class GeolocationPacketExtension
    implements PacketExtension
{
    //Names of keys used for storing geolocation data in geolocation Maps.

    /**
     * The name of the geolocation map key corresponding to the altitude in
     * meters above or below sea level (e.g. 1609).
     */
    public final static String ALT = "alt";

    /**
     * The name of the geolocation map key that we use for storing named areas
     * such as a campus or neighborhood (e.g. Central Park).
     */
    public final static String AREA = "area";

    /**
     * The name of the geolocation map key that we use for storing GPS bearing
     * (direction in which the entity is heading to reach its next waypoint),
     * measured in decimal degrees relative to true north.
     */
    public final static String BEARING = "bearing";

    /**
     * The name of the geolocation map key that we use for indicating a
     * specific building on a street or in an area (e.g. The Empire State
     * Building).
     */
    public final static String BUILDING = "building";

    /**
     * The name of the geolocation map key that we use for indicating the
     * nation where the user is located (e.g. Greenland).
     */
    public final static String COUNTRY = "country";

    /**
     * GPS datum.
     */
    public final static String DATUM = "datum";

    /**
     * The name of the geolocation map key that we use for storing a
     * natural-language name for or description of a given location (e.g.
     * Bill's house).
     */
    public final static String DESCRIPTION = "description";

    /**
     * The name of the geolocation map key that we use for storing horizontal
     * GPS errors in arc minutes (e.g. 10).
     */
    public final static String ERROR = "error";

    /**
     * The name of the geolocation map key that we use for storing a particular
     * floor in a building (e.g. 102).
     */
    public final static String FLOOR = "floor";

    /**
     * The name of the geolocation map key that we use for storing geographic
     * latitude in decimal degrees North (e.g. 39.75).
     */
    public final static String LAT = "lat";

    /**
     * The name of the geolocation map key that we use for indicating a
     * locality within the administrative region, such as a town or city (e.g.
     * Paris).
     */
    public final static String LOCALITY = "locality";

    /**
     * The name of the geolocation map key that we use for indicating
     * longitude in decimal degrees East (e.g. -104.99).
     */
    public final static String LON = "lon";

    /**
     * The name of the geolocation map key that we use for storing post codes
     * (or any code used for postal delivery) (e.g. 67000).
     */
    public final static String POSTALCODE = "postalcode";

    /**
     * The name of the geolocation map key that we use for indicating an
     * administrative region of the nation, such as a state or province (e.g.
     * Ile de France).
     */
    public final static String REGION = "region";

    /**
     * The name of the geolocation map key that we use for indicating a
     * particular room in a building (e.g. C-425).
     */
    public final static String ROOM = "room";

    /**
     * The name of the geolocation map key that we use for storing a
     * thoroughfare within a locality, or a crossing of two thoroughfares (e.g.
     * 34th and Broadway).
     */
    public final static String STREET = "street";

    /**
     * The name of the geolocation map key that we use to indicate a catch-all
     * element that captures any other information about the location (e.g.
     * North-West corner of the lobby).
     */
    public final static String TEXT = "text";

    /**
     * The name of the geolocation map key that we use to indicate UTC
     * timestamp specifying the moment when the reading was taken
     * (e.g. 2007-05-27T21:12Z).
     */
    public final static String TIMESTAMP = "timestamp";

    /**
     * Altitude in meters above or below sea level
     * example: 1609
     */
    private float alt = -1;

    /**
     * A named area such as a campus or neighborhood
     * example: Central Park
     */
    private String area = null;

    /**
     * GPS bearing (direction in which the entity is heading to reach its
     * next waypoint), measured in decimal degrees relative to true north
     */
    private float bearing = -1;

    /**
     * A specific building on a street or in an area
     * example: The Empire State Building
     */
    private String building = null;

    /**
     * The nation where the user is located
     * example: USA
     */
    private String country = null;

    /**
     * GPS datum
     */
    private String datum = null;

    /**
     * A natural-language name for or description of the location
     * example: Bill's house
     */
    private String description = null;

    /**
     * Horizontal GPS error in arc minutes
     * example: 10
     */
    private float error = -1;

    /**
     * A particular floor in a building
     * example: 102
     */
    private String floor = null;

    /**
     * Latitude in decimal degrees North
     * example: 39.75
     */
    private float lat = -1;

    /**
     * A locality within the administrative region, such as a town or city
     * example: New York City
     */
    private String locality = null;

    /**
     * Longitude in decimal degrees East
     * example: -104.99
     */
    private float lon = -1;

    /**
     * A code used for postal delivery
     * example: 10027
     */
    private String postalcode = null;

    /**
     * An administrative region of the nation, such as a state or province
     * example: New York
     */
    private String region = null;

    /**
     * A particular room in a building
     * example: Observatory
     */
    private String room = null;

    /**
     * A thoroughfare within the locality, or a crossing of two thoroughfares
     * example: 34th and Broadway
     */
    private String street = null;

    /**
     * A catch-all element that captures any other information about the location
     * example: Northwest corner of the lobby
     */
    private String text = null;

    /**
     * UTC timestamp specifying the moment when the reading was taken
     * example: 2004-02-19T21:12Z
     */
    private String timestamp = null;

    /**
     * Returns the XML representation of the PacketExtension.
     *
     * @return the packet extension as XML.
     */
    public String toXML()
    {
        StringBuffer buf = new StringBuffer();

        // open extension
        buf.append("<").append(getElementName()).
            append(" xmlns=\"").append(getNamespace()).append("\">");

        // adding geolocation extension parameters
        buf = addFloatXmlElement(buf, ALT, this.getAlt());
        buf = addXmlElement(buf, AREA, this.getArea());
        buf = addFloatXmlElement(buf, BEARING, this.getBearing());
        buf = addXmlElement(buf, BUILDING, this.getBuilding());
        buf = addXmlElement(buf, COUNTRY, this.getCountry());
        buf = addXmlElement(buf, DATUM, this.getDatum());
        buf = addXmlElement(buf, DESCRIPTION, this.getDescription());
        buf = addFloatXmlElement(buf, ERROR, this.getError());
        buf = addXmlElement(buf, FLOOR, this.getFloor());
        buf = addFloatXmlElement(buf, LAT, this.getLat());
        buf = addXmlElement(buf, LOCALITY, this.getLocality());
        buf = addFloatXmlElement(buf, LON, this.getLon());
        buf = addXmlElement(buf, POSTALCODE, this.getPostalCode());
        buf = addXmlElement(buf, REGION, this.getRegion());
        buf = addXmlElement(buf, ROOM, this.getRoom());
        buf = addXmlElement(buf, STREET, this.getStreet());
        buf = addXmlElement(buf, TEXT, this.getText());
        buf = addXmlElement(buf, TIMESTAMP, this.getTimestamp());

        // close extension
        buf.append("</").append(getElementName()).append(">");

        return buf.toString();
    }

    /**
     * Creates the xml <tt>String</tt> corresponding to the specified element
     * and value and addsthem to the <tt>buff</tt> StringBuffer.
     *
     * @param buff the <tt>StringBuffer</tt> to add the element and value to.
     * @param element the name of the geolocation element that we're adding.
     * @param value the value of the element we're addding to the xml buffer.
     * @return the <tt>StringBuffer</tt> that we've added the element and its
     * value to.
     */
    private StringBuffer addXmlElement(StringBuffer buff,
                                       String element,
                                       String value)
    {
        if (value != null)
        {
            buff.append("<").
                append(element).append(">").
                append(value).append("</").
                append(element).append(">");
        }

        return buff;
    }

    /**
     * Creates the xml <tt>String</tt> corresponding to the specified element
     * and its float value and adds them to the <tt>buf</tt>
     * <tt>StringBuffer</tt>.
     *
     * @param buff the <tt>StringBuffer</tt> to add the element and value to.
     * @param element the name of the geolocation element that we're adding.
     * @param value the float value of the element we're addding to the xml
     * buffer.
     * @return the <tt>StringBuffer</tt> that we've added the element and its
     * value to.
     */
    private StringBuffer addFloatXmlElement(StringBuffer buff,
                                            String element,
                                            float value)
    {

        if (value != -1)
        {
            buff.append("<").
                append(element).append(">").
                append(value).append("</").
                append(element).append(">");
        }

        return buff;
    }

    /**
     * Returns the XML element name of the extension sub-packet root element.
     * The current implementation would always return "geoloc".
     *
     * @return the XML element name of the packet extension.
     */
    public String getElementName()
    {
        return GeolocationPacketExtensionProvider.ELEMENT_NAME;
    }

    /**
     * Returns the XML namespace of the extension sub-packet root element.
     * The namespace is always "http://jabber.org/protocol/geoloc"
     *
     * @return the XML namespace of the packet extension.
     */
    public String getNamespace()
    {
        return GeolocationPacketExtensionProvider.NAMESPACE;
    }

    /**
     * Returns altitude in meters above or below sea level.
     *
     * @return the altitude in meters above or belos sea level (e.g. 1609).
     */
    public float getAlt()
    {
        return alt;
    }

    /**
     * Sets the altitude in meters above or below sea level.
     *
     * @param alt the altitude in meters above or belos sea level (e.g. 1609).
     */
    public void setAlt(float alt)
    {
        this.alt = alt;
    }

    /**
     * Sets the altitude in meters above or below sea level.
     *
     * @param alt the altitude in meters above or belos sea level (e.g. 1609).
     */
    public void setAlt(String alt)
    {
        this.alt = (new Float(alt)).floatValue();
    }

    /**
     * A named area such as a campus or neighborhood.
     *
     * @return a String indicating a named area such as a campus or a
     * neighborhood (e.g. Central Park).
     */
    public String getArea()
    {
        return area;
    }

    /**
     * Sets the value of a named area such as a campus or neighborhood.
     *
     * @param area the value of a named area such as a campus or neighborhood.
     */
    public void setArea(String area)
    {
        this.area = area;
    }

    /**
     * Returns GPS bearing (direction in which the entity is heading to reach
     * its next waypoint), measured in decimal degrees relative to true north.
     *
     * @return a float value indicating GPS bearing (direction in which the
     * entity is heading to reach its next waypoint), measured in decimal
     * degrees relative to true north.
     */
    public float getBearing()
    {
        return bearing;
    }

    /**
     * Sets GPS bearing (direction in which the entity is heading to reach
     * its next waypoint), measured in decimal degrees relative to true north.
     *
     * @param bearing a float value indicating GPS bearing (direction in which
     * the entity is heading to reach its next waypoint), measured in decimal
     * degrees relative to true north.
     */
    public void setBearing(float bearing)
    {
        this.bearing = bearing;
    }

    /**
     * Sets GPS bearing (direction in which the entity is heading to reach
     * its next waypoint), measured in decimal degrees relative to true north.
     *
     * @param bearing a String value indicating GPS bearing (direction in which
     * the entity is heading to reach its next waypoint), measured in decimal
     * degrees relative to true north.
     */
    public void setBearing(String bearing)
    {
        this.bearing = (new Float(bearing)).floatValue();
    }

    /**
     * Returns the name of a specific building on a street or in an area.
     *
     * @return a String containing the name of a specific building on a street
     * or in an area (e.g. The Empire State Building).
     */
    public String getBuilding()
    {
        return building;
    }

    /**
     * Sets the name of a specific building on a street or in an area.
     *
     * @param building a String indicating the name of a specific building on
     * a street or in an area (e.g. The Empire State Building).
     */
    public void setBuilding(String building)
    {
        this.building = building;
    }

    /**
     * Return the nation where the user is located.
     *
     * @return a String containing the name of the nation where the user is
     * located (e.g. Greenland).
     */
    public String getCountry()
    {
        return country;
    }

    /**
     * Sets the name of the nation where the user is located.
     *
     * @param country a String containing the name of the nation where the user
     * is located (e.g. Greenland).
     */
    public void setCountry(String country)
    {
        this.country = country;
    }

    /**
     * Return the value of GPS Datum.
     *
     * @return a String containing the value of GPS Datum.
     */
    public String getDatum()
    {
        return datum;
    }

    /**
     * Sets the value of GPS Datum.
     *
     * @param datum the value of GPS Datum.
     */
    public void setDatum(String datum)
    {
        this.datum = datum;
    }

    /**
     * Returns a natural-language name for or description of a this location.
     *
     * @return a <tt>java.lang.String</tt> containing a natural-language
     * description of this location (e.g. Bill's house)
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Sets a natural-language name for or description of a this location.
     *
     * @param description a <tt>java.lang.String</tt> containing a
     * natural-language description of this location (e.g. Bill's house)
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * Returns horizontal GPS errors in arc minutes.
     *
     * @return a float indicating horizontal GPS errors in arc minutes
     * (e.g. 10).
     */
    public float getError()
    {
        return error;
    }

    /**
     * Sets the value of horizontal GPS errors in arc minutes.
     * @param error a float indicating horizontal GPS errors in arc minutes
     * (e.g. 10).
     */
    public void setError(float error)
    {
        this.error = error;
    }

    /**
     * Sets the value of horizontal GPS errors in arc minutes.
     * @param error a String indicating horizontal GPS errors in arc minutes
     * (e.g. 10).
     */
    public void setError(String error)
    {
        this.error = (new Float(error)).floatValue();
    }

    /**
     * The number or name of a a particular floor in a building.
     *
     * @return a <tt>String</tt> indicating the number or name of a particular
     * floor in a building (e.g. 102)
     */
    public String getFloor()
    {
        return floor;
    }

    /**
     * Sets the number or name of a a particular floor in a building.
     *
     * @param floor a <tt>String</tt> indicating the number or name of a
     * particular floor in a building (e.g. 102)
     */
    public void setFloor(String floor)
    {
        this.floor = floor;
    }

    /**
     * Returns geographic latitude in decimal degrees North.
     *
     * @return a <tt>float</tt> value indicating geographic latitude in decimal
     * degrees (e.g. 39.75).
     */
    public float getLat()
    {
        return lat;
    }

    /**
     * Sets geographic latitude in decimal degrees North.
     *
     * @param lat a <tt>float</tt> value indicating geographic latitude in
     * decimal degrees (e.g. 39.75).
     */
    public void setLat(float lat)
    {
        this.lat = lat;
    }

    /**
     * Sets geographic latitude in decimal degrees North.
     *
     * @param lat a <tt>String</tt> value indicating geographic latitude in
     * decimal degrees (e.g. 39.75).
     */
    public void setLat(String lat)
    {
        this.lat = (new Float(lat)).floatValue();
    }

    /**
     * Returns a locality within the administrative region, such as a town or
     * city.
     *
     * @return a <tt>String</tt> indicating a locality within the administrative
     * region (e.g. Paris).
     */
    public String getLocality()
    {
        return locality;
    }

    /**
     * Sets a locality within the administrative region, such as a town or
     * city.
     *
     * @param locality a <tt>String</tt> indicating a locality within the
     * administrative region (e.g. Paris).
     */
    public void setLocality(String locality)
    {
        this.locality = locality;
    }

    /**
     * Returns a float containing the longitude in decimal degrees East
     * (e.g. -104.99).
     *
     * @return a <tt>float</tt> containing the longitude in decimal degrees East
     * (e.g. -104.99).
     */
    public float getLon()
    {
        return lon;
    }

    /**
     * Sets the longitude in decimal degrees East.
     *
     * @param lon a <tt>float</tt> containing the longitude in decimal degrees
     * East (e.g. -104.99).
     */
    public void setLon(float lon)
    {
        this.lon = lon;
    }

    /**
     * Sets the longitude in decimal degrees East.
     *
     * @param lon a <tt>String</tt> containing the longitude in decimal degrees
     * East (e.g. -104.99).
     */
    public void setLon(String lon)
    {
        this.lon = (new Float(lon)).floatValue();
    }

    /**
     * Returns a postal code (or any code used for postal delivery).
     *
     * @return a <tt>String</tt> containing the value of a postal or zip code
     * (e.g. 67000).
     */
    public String getPostalCode()
    {
        return postalcode;
    }

    /**
     * Sets a postal code (or any code used for postal delivery).
     *
     * @param postalCode a <tt>String</tt> containing the value of a postal or
     * zip code (e.g. 67000).
     */
    public void setPostalCode(String postalCode)
    {
        this.postalcode = postalCode;
    }

    /**
     * Returns an administrative region of the nation, such as a state or
     * province (e.g. Ile de France).
     *
     * @return a <tt>String</tt> indicating an administrative region of the
     * nation, such as a state or province (e.g. Ile de France).
     */
    public String getRegion()
    {
        return region;
    }

    /**
     * Sets an administrative region of the nation, such as a state or
     * province (e.g. Ile de France).
     *
     * @param region a <tt>String</tt> indicating an administrative region of
     * the nation, such as a state or province (e.g. Ile de France).
     */
    public void setRegion(String region)
    {
        this.region = region;
    }

    /**
     * Returns a <tt>String</tt> indicating a particular room in a building.
     *
     * @return a <tt>String</tt> indicating a particular room in a building
     * (e.g. C-425).
     */
    public String getRoom()
    {
        return room;
    }

    /**
     * Sets the name or number indicating a particular room in a building.
     *
     * @param room a <tt>String</tt> indicating the name ornumber a particular
     * room in a building (e.g. C-425).
     */
    public void setRoom(String room)
    {
        this.room = room;
    }

    /**
     * Returns a <tt>String</tt> indicating a thoroughfare within a locality,
     * or a crossing of two thoroughfares.
     *
     * @return a <tt>String</tt> indicating a thoroughfare within a locality,
     * or a crossing of two thoroughfares (e.g. 34th and Broadway).
     */
    public String getStreet()
    {
        return street;
    }

    /**
     * Sets the name of a street.
     *
     * @param street a <tt>String</tt> indicating a thoroughfare within a
     * locality, or a crossing of two thoroughfares (e.g. 34th and Broadway).
     */
    public void setStreet(String street)
    {
        this.street = street;
    }

    /**
     * Returns a <tt>String</tt> stored in the "Text" element of the geolocation
     * details.
     *
     * @return a <tt>String</tt> stored in the "Text" element of the geolocation
     * details.
     */
    public String getText()
    {
        return text;
    }

    /**
     * Sets a <tt>String</tt> to store in the "Text" element of the geolocation
     * details.
     *
     * @param text a <tt>String</tt> stored in the "Text" element of the
     * geolocation details.
     */
    public void setText(String text)
    {
        this.text = text;
    }

    /**
     * Returns a <tt>String</tt> containing a UTC timestamp specifying the
     * moment when the reading was taken.
     *
     * @return a <tt>String</tt> containing a UTC timestamp specifying the
     * moment when the reading was taken (e.g. 2007-05-27T21:12Z).
     */
    public String getTimestamp()
    {
        return timestamp;
    }

    /**
     * Set timestamp in UTC format as described in XEP-0082: XMPP Date and
     * Time Profiles
     *
     * @param timestamp the timestamp in UTC format (e.g. 2007-05-27T21:12Z).
     */
    public void setTimestamp(String timestamp)
    {
        this.timestamp = timestamp;
    }

    /**
     * Test if latitude and longitude are set.
     *
     * @return true if the geolocation extension contains both latitude and
     * longitude and false otherwise.
     */
    public boolean containsLatLon()
    {

        if (this.lat != -1 && this.lon != -1)
        {
            return true;
        }
        else
        {
            return false;
        }
    }
}
