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

import java.lang.reflect.*;
import java.util.*;
import java.util.Map.Entry;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * This class give some static methods for converting a geolocation message
 * to a different format.
 *
 * @author Guillaume Schreiner
 */
public class GeolocationJabberUtils
{
    /**
     * The logger of this class.
     */
    private static final Logger logger =
        Logger.getLogger(GeolocationJabberUtils.class);

    /**
     * Convert geolocation from GeolocationExtension format to Map format
     *
     * @param geolocExt the GeolocationExtension XML message
     * @return a Map with geolocation information
     */
    public static Map<String, String> convertExtensionToMap(
                                        GeolocationPacketExtension geolocExt)
    {
        Map<String, String> geolocMap = new Hashtable<String, String>();

        addFloatToMap(  geolocMap
                      , OperationSetGeolocation.ALT
                      , geolocExt.getAlt());

        addStringToMap(  geolocMap
                       , OperationSetGeolocation.AREA
                       , geolocExt.getArea());

        addFloatToMap(geolocMap
                      , OperationSetGeolocation.BEARING
                      , geolocExt.getBearing());

        addStringToMap(geolocMap
                       , OperationSetGeolocation.BUILDING
                       , geolocExt.getBuilding());

        addStringToMap(geolocMap
                       , OperationSetGeolocation.COUNTRY
                       , geolocExt.getCountry());

        addStringToMap(geolocMap
                       , OperationSetGeolocation.DATUM
                       , geolocExt.getDatum());

        addStringToMap(geolocMap
                       , OperationSetGeolocation.DESCRIPTION
                       , geolocExt.getDescription());

        addFloatToMap(geolocMap
                      , OperationSetGeolocation.ERROR
                      , geolocExt.getError());

        addStringToMap(geolocMap
                       , OperationSetGeolocation.FLOOR
                       , geolocExt.getFloor());

        addFloatToMap(geolocMap
                      , OperationSetGeolocation.LAT
                      , geolocExt.getLat());

        addStringToMap(geolocMap
                       , OperationSetGeolocation.LOCALITY
                       , geolocExt.getLocality());

        addFloatToMap(geolocMap
                      , OperationSetGeolocation.LON
                      , geolocExt.getLon());

        addStringToMap(geolocMap
                       , OperationSetGeolocation.POSTALCODE
                       , geolocExt.getPostalCode());

        addStringToMap(geolocMap
                       , OperationSetGeolocation.REGION
                       , geolocExt.getRegion());

        addStringToMap(geolocMap
                       , OperationSetGeolocation.ROOM
                       , geolocExt.getRoom());

        addStringToMap(geolocMap
                       , OperationSetGeolocation.STREET
                       , geolocExt.getStreet());

        addStringToMap(geolocMap
                       , OperationSetGeolocation.TEXT
                       , geolocExt.getText());

        addStringToMap(geolocMap
                       , OperationSetGeolocation.TIMESTAMP
                       , geolocExt.getTimestamp());

        return geolocMap;
    }

    /**
     * Utility function for adding a float var to a Map.
     *
     * @param map the map we're adding a new value to.
     * @param key the key that we're adding the new value against
     * @param value the float var that we're adding to <tt>map</tt> against the
     * <tt>key</tt> key.
     */
    private static void addFloatToMap(Map<String, String> map,
                                      String key,
                                      float value)
    {
        if (value != -1)
        {
            Float valor = new Float(value);
            map.put(key, valor.toString());
        }
    }

    /**
     * Utility function that we use when adding a String to a map ()
     * @param map Map
     * @param key String
     * @param value String
     */
    private static void addStringToMap(Map<String, String> map,
                                       String key,
                                       String value)
    {
        if (value != null)
        {
            map.put(key, value);
        }
    }

    /**
     * Convert a geolocation details Map to a GeolocationPacketExtension format
     *
     * @param geolocation a Map with geolocation information
     * @return a GeolocationExtension ready to be included into a Jabber
     * message
     */
    public static GeolocationPacketExtension convertMapToExtension(
                                               Map<String, String> geolocation)
    {
        GeolocationPacketExtension geolocExt = new GeolocationPacketExtension();

        Set<Entry<String, String>> entries = geolocation.entrySet();
        Iterator<Entry<String, String>> itLine = entries.iterator();

        while (itLine.hasNext())
        {

            Entry<String, String> line = itLine.next();

            String curParam = line.getKey();
            String curValue = line.getValue();

            String prototype = Character.toUpperCase(curParam.charAt(0))
                + curParam.substring(1);

            String setterFunction = "set" + prototype;

            try
            {

                Method toCall
                    = GeolocationPacketExtension
                        .class.getMethod(setterFunction, String.class);
                Object[] arguments = new Object[]{curValue};

                try
                {
                    toCall.invoke(geolocExt, arguments);
                }
                catch (IllegalArgumentException exc)
                {
                    logger.error(exc);
                }
                catch (IllegalAccessException exc)
                {
                    logger.error(exc);
                }
                catch (InvocationTargetException exc)
                {
                    logger.error(exc);
                }
            }
            catch (SecurityException exc)
            {
                logger.error(exc);
            }
            catch (NoSuchMethodException exc)
            {
                logger.error(exc);
            }
        }
        return geolocExt;
    }
}
