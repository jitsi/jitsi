/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.util.*;

import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Represents a default implementation of
 * <tt>OperationSetPersistentPresence</tt> in order to make it easier for
 * implementers to provide complete solutions while focusing on
 * implementation-specific details.
 * 
 * @author Lubomir Marinov
 */
public abstract class AbstractOperationSetPersistentPresence<T extends ProtocolProviderService>
    implements OperationSetPersistentPresence
{
    private static final Logger logger =
        Logger.getLogger(AbstractOperationSetPersistentPresence.class);

    /**
     * The provider that created us.
     */
    protected final T parentProvider;

    /**
     * The list of listeners interested in <tt>SubscriptionEvent</tt>s.
     */
    private final List<SubscriptionListener> subscriptionListeners =
        new Vector<SubscriptionListener>();

    /**
     * Initializes a new <tt>AbstractOperationSetPersistentPresence</tt>
     * instance created by a specific <tt>ProtocolProviderService</tt> .
     * 
     * @param parentProvider the <tt>ProtocolProviderService</tt> which created
     *            the new instance
     */
    protected AbstractOperationSetPersistentPresence(T parentProvider)
    {
        this.parentProvider = parentProvider;
    }

    public void addSubscriptionListener(SubscriptionListener listener)
    {
        synchronized (subscriptionListeners)
        {
            if (!subscriptionListeners.contains(listener))
                subscriptionListeners.add(listener);
        }
    }

    /**
     * Notify all subscription listeners of the corresponding contact property
     * change event.
     * 
     * @param eventID the String ID of the event to dispatch
     * @param sourceContact the ContactJabberImpl instance that this event is
     *            pertaining to.
     * @param oldValue the value that the changed property had before the change
     *            occurred.
     * @param newValue the value that the changed property currently has (after
     *            the change has occurred).
     */
    public void fireContactPropertyChangeEvent(String eventID, Contact source,
        Object oldValue, Object newValue)
    {
        ContactPropertyChangeEvent evt =
            new ContactPropertyChangeEvent(source, eventID, oldValue, newValue);

        List<SubscriptionListener> listeners;
        synchronized (subscriptionListeners)
        {
            listeners =
                new ArrayList<SubscriptionListener>(subscriptionListeners);
        }

        logger.debug("Dispatching a Contact Property Change Event to"
            + listeners.size() + " listeners. Evt=" + evt);

        for (Iterator<SubscriptionListener> listenerIt = listeners.iterator(); listenerIt
            .hasNext();)
            listenerIt.next().contactModified(evt);
    }

    /**
     * Notifies all registered listeners of the new event.
     * 
     * @param source the contact that has caused the event.
     * @param parentGroup the group that contains the source contact.
     * @param eventID an identifier of the event to dispatch.
     */
    public void fireSubscriptionEvent(Contact source, ContactGroup parentGroup,
        int eventID)
    {
        fireSubscriptionEvent(source, parentGroup, eventID,
            SubscriptionEvent.ERROR_UNSPECIFIED, null);
    }

    public void fireSubscriptionEvent(Contact source, ContactGroup parentGroup,
        int eventID, int errorCode, String errorReason)
    {
        SubscriptionEvent evt =
            new SubscriptionEvent(source, parentProvider, parentGroup, eventID,
                errorCode, errorReason);

        List<SubscriptionListener> listeners;
        synchronized (subscriptionListeners)
        {
            listeners =
                new ArrayList<SubscriptionListener>(subscriptionListeners);
        }

        logger.debug("Dispatching a Subscription Event to" + listeners.size()
            + " listeners. Evt=" + evt);

        for (Iterator<SubscriptionListener> listenerIt = listeners.iterator(); listenerIt
            .hasNext();)
        {
            SubscriptionListener listener = listenerIt.next();

            switch (eventID)
            {
            case SubscriptionEvent.SUBSCRIPTION_CREATED:
                listener.subscriptionCreated(evt);
                break;
            case SubscriptionEvent.SUBSCRIPTION_FAILED:
                listener.subscriptionFailed(evt);
                break;
            case SubscriptionEvent.SUBSCRIPTION_REMOVED:
                listener.subscriptionRemoved(evt);
                break;
            case SubscriptionEvent.SUBSCRIPTION_RESOLVED:
                listener.subscriptionResolved(evt);
                break;
            }
        }
    }

    /**
     * Notifies all registered listeners of the new event.
     * 
     * @param source the contact that has been moved..
     * @param oldParent the group where the contact was located before being
     *            moved.
     * @param newParent the group where the contact has been moved.
     */
    public void fireSubscriptionMovedEvent(Contact source,
        ContactGroup oldParent, ContactGroup newParent)
    {
        SubscriptionMovedEvent evt =
            new SubscriptionMovedEvent(source, parentProvider, oldParent,
                newParent);

        List<SubscriptionListener> listeners;
        synchronized (subscriptionListeners)
        {
            listeners =
                new ArrayList<SubscriptionListener>(subscriptionListeners);
        }

        logger.debug("Dispatching a Subscription Event to" + listeners.size()
            + " listeners. Evt=" + evt);

        for (Iterator<SubscriptionListener> listenerIt = listeners.iterator(); listenerIt
            .hasNext();)
            listenerIt.next().subscriptionMoved(evt);
    }

    /**
     * Removes the specified subscription listener.
     * 
     * @param listener the listener to remove.
     */
    public void removeSubscriptionListener(SubscriptionListener listener)
    {
        synchronized (subscriptionListeners)
        {
            subscriptionListeners.remove(listener);
        }
    }
}
