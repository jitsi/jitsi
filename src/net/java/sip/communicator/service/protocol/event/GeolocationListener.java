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
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * The listener interface for receiving geolocation events.
 * The class that is interested in processing a geolocation event
 * implements this interface, and the object created with that
 * class is registered with the geolocation operation set, using its
 * <code>addGeolocationListener</code> method. When a geolocation event
 * occurs, that object's <code>contactGeolocationChanged</code> method is
 * invoked.
 *
 * @see GeolocationEvent
 *
 * @author Guillaume Schreiner
 */
public interface GeolocationListener
    extends EventListener
{
    /**
     * Called whenever a change occurs in the GeolocationPresence of one of the
     * contacts that we have subscribed for.
     *
     * @param evt the ContactGeolocationPresenceChangeEvent describing the
     * status change.
     */
    public void contactGeolocationChanged(GeolocationEvent evt);
}
