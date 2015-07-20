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

import net.java.sip.communicator.impl.protocol.jabber.extensions.notification.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;

/**
 * Provides notification for generic events with name and value, also
 * option to generate such events.
 *
 * @author Damian Minkov
 * @author Emil Ivov
 */
public class OperationSetGenericNotificationsJabberImpl
    implements OperationSetGenericNotifications,
               PacketListener
{
    /**
     * Our class logger
     */
    private static final Logger logger =
        Logger.getLogger(OperationSetGenericNotificationsJabberImpl.class);

    /**
     * The provider that created us.
     */
    private final ProtocolProviderServiceJabberImpl jabberProvider;

    /**
     * A reference to the persistent presence operation set that we use
     * to match incoming event to <tt>Contact</tt>s and vice versa.
     */
    private OperationSetPersistentPresenceJabberImpl opSetPersPresence = null;

    /**
     * Listeners that would receive event notifications for
     * new event notifications
     */
    private final Map<String, List<GenericEventListener>>
            genericEventListeners
                = new HashMap<String,
                              List<GenericEventListener>>();

    /**
     * Creates an instance of this operation set.
     * @param provider a reference to the <tt>ProtocolProviderServiceImpl</tt>
     * that created us and that we'll use for retrieving the underlying aim
     * connection.
     */
    OperationSetGenericNotificationsJabberImpl(
            ProtocolProviderServiceJabberImpl provider
    )
    {
        this.jabberProvider = provider;

        provider.addRegistrationStateChangeListener(
                        new RegistrationStateListener());

        // register the notification event Extension in the smack library
        ProviderManager.getInstance()
            .addIQProvider(NotificationEventIQ.ELEMENT_NAME,
                           NotificationEventIQ.NAMESPACE,
                           new NotificationEventIQProvider());
    }

    /**
     * Generates new event notification.
     *
     * @param contact    the contact to receive the notification.
     * @param eventName  the event name of the notification.
     * @param eventValue the event value of the notification.
     */
    public void notifyForEvent(
            Contact contact,
            String eventName,
            String eventValue)
    {
        // if we are not registered do nothing
        if(!jabberProvider.isRegistered())
        {
            if (logger.isTraceEnabled())
                logger.trace("provider not registered. "
                         +"won't send keep alive. acc.id="
                         + jabberProvider.getAccountID()
                            .getAccountUniqueID());
            return;
        }

        NotificationEventIQ newEvent = new NotificationEventIQ();
        newEvent.setEventName(eventName);
        newEvent.setEventValue(eventValue);
        newEvent.setTo(contact.getAddress());
        newEvent.setEventSource(jabberProvider.getOurJID());

        jabberProvider.getConnection().sendPacket(newEvent);
    }

    /**
     * Generates new generic event notification and send it to the
     * supplied contact.
     * @param jid the contact jid which will receive the event notification.
     * @param eventName the event name of the notification.
     * @param eventValue the event value of the notification.
     */
    public void notifyForEvent(
            String jid,
            String eventName,
            String eventValue)
    {
        this.notifyForEvent(jid, eventName, eventValue, null);
    }

    /**
     * Generates new generic event notification and send it to the
     * supplied contact.
     * @param jid the contact jid which will receive the event notification.
     * @param eventName the event name of the notification.
     * @param eventValue the event value of the notification.
     * @param source the source that will be reported in the event.
     */
    public void notifyForEvent(
            String jid,
            String eventName,
            String eventValue,
            String source)
    {
        // if we are not registered do nothing
        if(!jabberProvider.isRegistered())
        {
            if (logger.isTraceEnabled())
                logger.trace("provider not registered. "
                         +"won't send keep alive. acc.id="
                         + jabberProvider.getAccountID()
                            .getAccountUniqueID());
            return;
        }

        //try to convert the jid to a full jid
        String fullJid = jabberProvider.getFullJid(jid);
        if( fullJid != null )
            jid = fullJid;

        NotificationEventIQ newEvent = new NotificationEventIQ();
        newEvent.setEventName(eventName);
        newEvent.setEventValue(eventValue);
        newEvent.setTo(jid);

        if(source != null)
            newEvent.setEventSource(source);
        else
            newEvent.setEventSource(jabberProvider.getOurJID());

        jabberProvider.getConnection().sendPacket(newEvent);
    }

    /**
     * Registers a <tt>GenericEventListener</tt> with this
     * operation set so that it gets notifications for new
     * event notifications.
     *
     * @param eventName register the listener for certain event name.
     * @param listener  the <tt>GenericEventListener</tt>
     *                  to register.
     */
    public void addGenericEventListener(
            String eventName,
            GenericEventListener listener)
    {
        synchronized (genericEventListeners)
        {
            List<GenericEventListener> l =
                    this.genericEventListeners.get(eventName);
            if(l == null)
            {
                l = new ArrayList<GenericEventListener>();
                this.genericEventListeners.put(eventName, l);
            }

            if(!l.contains(listener))
                l.add(listener);
        }
    }

    /**
     * Unregisters <tt>listener</tt> so that it won't receive any further
     * notifications upon new event notifications.
     *
     * @param eventName unregister the listener for certain event name.
     * @param listener  the <tt>GenericEventListener</tt>
     *                  to unregister.
     */
    public void removeGenericEventListener(
            String eventName,
            GenericEventListener listener)
    {
        synchronized (genericEventListeners)
        {
            List<GenericEventListener> listenerList
                = this.genericEventListeners.get(eventName);
            if(listenerList != null)
                listenerList.remove(listener);
        }
    }

    /**
     * Process the next packet sent to this packet listener.<p>
     * <p/>
     *
     * @param packet the packet to process.
     */
    public void processPacket(Packet packet)
    {
        if(packet != null &&  !(packet instanceof NotificationEventIQ))
                return;

        NotificationEventIQ notifyEvent = (NotificationEventIQ)packet;

        if(logger.isDebugEnabled())
        {
            if (logger.isDebugEnabled())
                logger.debug("Received notificationEvent from "
                         + notifyEvent.getFrom()
                         + " msg : "
                         + notifyEvent.toXML());
        }

        //do not notify

        String fromUserID
            = org.jivesoftware.smack.util.StringUtils.parseBareAddress(
                notifyEvent.getFrom());

        Contact sender = opSetPersPresence.findContactByID(fromUserID);

        if(sender == null)
            sender = opSetPersPresence.createVolatileContact(
                notifyEvent.getFrom());

        if(notifyEvent.getType() == Type.GET)
            fireNewEventNotification(
                            sender,
                            notifyEvent.getEventName(),
                            notifyEvent.getEventValue(),
                            notifyEvent.getEventSource(),
                            true);
        else if(notifyEvent.getType() == Type.ERROR)
            fireNewEventNotification(
                            sender,
                            notifyEvent.getEventName(),
                            notifyEvent.getEventValue(),
                            notifyEvent.getEventSource(),
                            false);
    }

    /**
     * Fires new notification event.
     *
     * @param from common from <tt>Contact</tt>.
     * @param eventName the event name.
     * @param eventValue the event value.
     * @param source the name of the contact sending the notification.
     * @param incoming indicates whether the event we are dispatching
     * corresponds to an incoming notification or an error report indicating
     * that an outgoing notification has failed.
     */
    private void fireNewEventNotification(
            Contact from,
            String  eventName,
            String  eventValue,
            String  source,
            boolean incoming)
    {
        String sourceUserID
            = org.jivesoftware.smack.util.StringUtils.parseBareAddress(
                source);
        Contact sourceContact =
                opSetPersPresence.findContactByID(sourceUserID);
        if(sourceContact == null)
            sourceContact = opSetPersPresence
                    .createVolatileContact(source);

        GenericEvent
            event = new GenericEvent(
                jabberProvider, from, eventName, eventValue, sourceContact);

        Iterable<GenericEventListener> listeners;
        synchronized (genericEventListeners)
        {
            List<GenericEventListener> ls =
                    genericEventListeners.get(eventName);

            if(ls == null)
                return;

            listeners = new ArrayList<GenericEventListener>(ls);
        }
        for (GenericEventListener listener : listeners)
        {
            if(incoming)
                listener.notificationReceived(event);
            else
                listener.notificationDeliveryFailed(event);
        }
    }

    /**
     * Our listener that will tell us when we're registered to
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
            if (evt.getNewState() == RegistrationState.REGISTERED)
            {
                OperationSetGenericNotificationsJabberImpl.this.opSetPersPresence
                    = (OperationSetPersistentPresenceJabberImpl)
                        jabberProvider.getOperationSet(
                                OperationSetPersistentPresence.class);

                if(jabberProvider.getConnection() != null)
                {
                    jabberProvider.getConnection().addPacketListener(
                        OperationSetGenericNotificationsJabberImpl.this,
                            new PacketTypeFilter(NotificationEventIQ.class));
                }
            }
            else if(evt.getNewState() == RegistrationState.UNREGISTERED
                || evt.getNewState() == RegistrationState.CONNECTION_FAILED
                || evt.getNewState() == RegistrationState.AUTHENTICATION_FAILED)
            {
                if(jabberProvider.getConnection() != null)
                {
                    jabberProvider.getConnection().removePacketListener(
                            OperationSetGenericNotificationsJabberImpl.this);
                }

                OperationSetGenericNotificationsJabberImpl.this.opSetPersPresence
                    = null;
            }
        }
    }
}
