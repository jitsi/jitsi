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
