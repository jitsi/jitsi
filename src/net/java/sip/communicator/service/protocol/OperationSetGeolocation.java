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
package net.java.sip.communicator.service.protocol;

import java.util.*;

import net.java.sip.communicator.service.protocol.event.*;

/**
 * This interface is an extension of the operation set, meant to be
 * implemented by protocols that support exchange of geolocation details (like
 * Jabber for example).
 *
 * @author Guillaume Schreiner
 */
public interface OperationSetGeolocation
    extends OperationSet
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
     * Publish the location contained in the <tt>geolocation</tt> map to all
     * contacts in our contact list.
     *
     * @param geolocation a <tt>java.uil.Map</tt> containing the geolocation
     * details of the position we'd like to publish.
     */
    public void publishGeolocation(Map<String, String> geolocation);

    /**
     * Retrieve the geolocation of the contact corresponding to
     * <tt>contactIdentifier</tt>.
     *
     * @param contactIdentifier the address of the <tt>Contact</tt> whose
     * geolocation details we'd like to retrieve.
     *
     * @return a <tt>java.util.Map</tt> containing the geolocation details of
     * the contact with address <tt>contactIdentifier</tt>.
     */
    public Map<String, String> queryContactGeolocation(String contactIdentifier);

    /**
     * Registers a listener that would get notifications any time a contact
     * publishes a new geolocation.
     *
     * @param listener the <tt>GeolocationListener</tt> to register
     */
    public void addGeolocationListener( GeolocationListener listener);

    /**
     * Removes a listener previously registered for notifications of changes in
     * the contact geolocation details.
     *
     * @param listener the <tt>GeolocationListener</tt> to unregister
     */
    public void removeGeolocationListener( GeolocationListener listener);
}
