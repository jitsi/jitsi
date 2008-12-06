/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.geolocation;

import java.lang.reflect.*;
import java.util.*;
import java.util.Map.*;

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

    protected static final Logger logger =
        Logger.getLogger(GeolocationJabberUtils.class);

    /**
     * Convert geolocation from GeolocationExtension format to Map format
     *
     * @param geolocExt the GeolocationExtension XML message
     * @return a Map with geolocation information
     */
    public static Map convertExtensionToMap(
                                        GeolocationPacketExtension geolocExt)
    {
        Map geolocMap = new Hashtable();

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
    private static void addFloatToMap(Map map, String key, float value)
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
    private static void addStringToMap(Map map, String key, String value)
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
                                                            Map geolocation)
    {
        GeolocationPacketExtension geolocExt = new GeolocationPacketExtension();

        Set entries = geolocation.entrySet();
        Iterator itLine = entries.iterator();

        while (itLine.hasNext())
        {

            Map.Entry line = (Entry) itLine.next();

            String curParam = (String) line.getKey();
            String curValue = (String) line.getValue();

            String prototype = Character.toUpperCase(curParam.charAt(0))
                + curParam.substring(1);

            String setterFunction = "set" + prototype;

            try
            {

                Method toCall = GeolocationPacketExtension
                    .class.getMethod(setterFunction, new Class[]{String.class});
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
