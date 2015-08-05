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
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.geolocation.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.jivesoftware.smack.util.StringUtils;

/**
 * The Jabber implementation of an OperationSetGeolocation done with the
 * XEP-0080: User Geolocation. This class broadcast our own geolocation and
 * manage the geolocation status of our buddies.
 *
 * Currently, we send geolocation message in presence. We passively listen
 * to buddies geolocation when their presence are updated.
 *
 * @author Guillaume Schreiner
 */
public class OperationSetGeolocationJabberImpl
    implements OperationSetGeolocation
{
    /**
     * Our logger.
     */
    private static final Logger logger =
        Logger.getLogger(OperationSetGeolocationJabberImpl.class);

    /**
     * The list of Geolocation status listeners interested in receiving presence
     * notifications of changes in geolocation of contacts in our contact list.
     */
    private final List<GeolocationListener> geolocationContactsListeners
        = new Vector<GeolocationListener>();

    /**
     * A callback to the provider
     */
    private final ProtocolProviderServiceJabberImpl jabberProvider;

    /**
     * A callback to the persistent presence operation set.
     */
    private final OperationSetPersistentPresence opsetprez;

    /**
     * Constuctor
     *
     * @param provider <tt>ProtocolProviderServiceJabberImpl</tt>
     */
    public OperationSetGeolocationJabberImpl(
        ProtocolProviderServiceJabberImpl provider)
    {
        this.jabberProvider = provider;

        this.opsetprez
                = provider
                    .getOperationSet(OperationSetPersistentPresence.class);

        this.jabberProvider.addRegistrationStateChangeListener(
            new RegistrationStateListener());

        // Add the custom GeolocationExtension to the Smack library
        ProviderManager pManager = ProviderManager.getInstance();
        pManager.addExtensionProvider(
            GeolocationPacketExtensionProvider.ELEMENT_NAME
            , GeolocationPacketExtensionProvider.NAMESPACE
            , new GeolocationPacketExtensionProvider());
    }

    /**
     * Broadcast our current Geolocation trough this provider using a Jabber
     * presence message.
     *
     * @param geolocation our current Geolocation ready to be sent
     */
    public void publishGeolocation(Map<String, String> geolocation)
    {
        GeolocationPresence myGeolocPrez = new GeolocationPresence(opsetprez);

        GeolocationPacketExtension geolocExt = GeolocationJabberUtils
            .convertMapToExtension(geolocation);

        myGeolocPrez.setGeolocationExtention(geolocExt);

        this.jabberProvider.getConnection()
            .sendPacket(myGeolocPrez.getGeolocPresence());
    }

    /**
     * Retrieve the geolocation of the given contact.
     * <p>
     * Note: Currently not implemented because we can not actively poll the
     * server for the presence of a given contact ?
     * <p>
     * @param contactIdentifier the <tt>Contact</tt> we want to retrieve its
     * geolocation by its identifier.
     * @return the <tt>Geolocation</tt> of the contact.
     */
    public Map<String, String> queryContactGeolocation(String contactIdentifier)
    {
        /** @todo implement queryContactGeolocation() */
        return null;
    }

    /**
     * Registers a listener that would get notifications any time a contact
     * refreshed its geolocation via Presence.
     *
     * @param listener the <tt>ContactGeolocationPresenceListener</tt> to
     * register
     */
    public void addGeolocationListener(GeolocationListener listener)
    {
        synchronized (geolocationContactsListeners)
        {
            geolocationContactsListeners.add(listener);
        }
    }

    /**
     * Remove a listener that would get notifications any time a contact
     * refreshed its geolocation via Presence.
     *
     * @param listener the <tt>ContactGeolocationPresenceListener</tt> to
     * register
     */
    public void removeGeolocationListener(GeolocationListener listener)
    {
        synchronized (geolocationContactsListeners)
        {
            geolocationContactsListeners.remove(listener);
        }
    }

    /**
     * Our listener that will tell us when we're registered to server
     * and we are ready to launch the listener for GeolocationPacketExtension
     * packets
     */
    private class RegistrationStateListener
        implements RegistrationStateChangeListener
    {
        /**
         * The method is called by a ProtocolProvider implementation whenever
         * a change in the registration state of the corresponding provider had
         * occurred.
         * @param evt ProviderStatusChangeEvent the event describing the status
         * change.
         */
        public void registrationStateChanged(RegistrationStateChangeEvent evt)
        {
            if (logger.isDebugEnabled())
                logger.debug("The Jabber provider changed state from: "
                         + evt.getOldState()
                         + " to: " + evt.getNewState());

            if (evt.getNewState() == RegistrationState.REGISTERED)
            {

                PacketExtensionFilter filterGeoloc =
                    new PacketExtensionFilter(
                        GeolocationPacketExtensionProvider.ELEMENT_NAME,
                        GeolocationPacketExtensionProvider.NAMESPACE
                    );

                // launch the listener
                try
                {
                    jabberProvider.getConnection().addPacketListener(
                        new GeolocationPresencePacketListener()
                                , filterGeoloc
                        );
                }
                catch (Exception e)
                {
                    logger.error(e);
                }

            }
            else if (evt.getNewState() == RegistrationState.UNREGISTERED
                     || evt.getNewState()
                                == RegistrationState.AUTHENTICATION_FAILED
                     || evt.getNewState()
                                == RegistrationState.CONNECTION_FAILED)
            {

            }
        }
    }

    /**
     * This class listen to GeolocationExtension into Presence Packet.
     * If GeolocationExtension is found, an event is sent.
     *
     * @author Guillaume Schreiner
     */
    private class GeolocationPresencePacketListener
        implements PacketListener
    {
        /**
         * Match incoming packets with geolocation Extension tags for
         * dispatching a new event.
         *
         * @param packet matching Geolocation Extension tags.
         */
        public void processPacket(Packet packet)
        {
            String from = StringUtils.parseBareAddress(packet.getFrom());

            GeolocationPacketExtension geolocExt =
                (GeolocationPacketExtension) packet.getExtension(
                    GeolocationPacketExtensionProvider.ELEMENT_NAME,
                    GeolocationPacketExtensionProvider.NAMESPACE);

            if (geolocExt != null)
            {
                if (logger.isDebugEnabled())
                    logger.debug("GeolocationExtension found from " + from + ":" +
                             geolocExt.toXML());

                Map<String, String> newGeolocation
                    = GeolocationJabberUtils.convertExtensionToMap(geolocExt);

                this.fireGeolocationContactChangeEvent(
                    from,
                    newGeolocation);
            }
        }

        /**
         * Notify registred listeners for a new incoming GeolocationExtension.
         *
         * @param sourceContact which send a new Geolocation.
         * @param newGeolocation the new given Geolocation.
         */
        public void fireGeolocationContactChangeEvent(
                String sourceContact,
                Map<String, String> newGeolocation)
        {
            if (logger.isDebugEnabled())
                logger.debug("Trying to dispatch geolocation contact update for "
                         + sourceContact);

            Contact source = opsetprez.findContactByID(sourceContact);

            GeolocationEvent evt =
                new GeolocationEvent(
                    source
                    , jabberProvider
                    , newGeolocation
                    , OperationSetGeolocationJabberImpl.this);

            if (logger.isDebugEnabled())
                logger.debug("Dispatching  geolocation contact update. Listeners="
                         + geolocationContactsListeners.size()
                         + " evt=" + evt);

            GeolocationListener[] listeners;

            synchronized (geolocationContactsListeners)
            {
                listeners
                    = geolocationContactsListeners.toArray(
                            new GeolocationListener[
                                    geolocationContactsListeners.size()]);
            }

            for (GeolocationListener listener : listeners)
                listener.contactGeolocationChanged(evt);
        }
    }
}
