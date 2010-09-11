/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.geolocation;

import java.util.*;

import net.java.sip.communicator.service.geolocation.event.*;

/**
 * Instances of  GeolocationService allow to retrieve the current geolocation
 * for SIP Communicator. You need to use this interface in order to
 * implements geolocation services like GPS, GeoIP, etc...
 *
 * @author Guillaume Schreiner
 */
public interface GeolocationService
{
    /**
     * Returns the <tt>Geolocation</tt> currently set for the provider
     *
     * @return the last <tt>Geolocation</tt> that we have set by a geolocation
     * backend.
     */
    public Map<String, String> getCurrentGeolocation();

    /**
     * Registers a listener that would get notifications any time the provider
     * geolocation was succesfully refreshed.
     *
     * @param listener the <tt>ProviderGeolocationPresenceListener</tt> to
     * register
     */
    public void addGeolocationListener(GeolocationListener listener);

    /**
     * Remove a listener that would get notifications any time the provider
     * geolocation was succesfully refreshed.
     *
     * @param listener the <tt>ProviderGeolocationPresenceListener</tt> to
     * remove
     */
    public void removeGeolocationListener(GeolocationListener listener);
}
