/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.beans.PropertyChangeEvent;
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
    /**
     * The <tt>Logger</tt> used by the
     * <tt>AbstractOperationSetPersistentPresence</tt> class and its instances
     * for logging output.
     */
    private static final Logger logger =
        Logger.getLogger(AbstractOperationSetPersistentPresence.class);

    /**
     * A list of listeners registered for
     * <tt>ContactPresenceStatusChangeEvent</tt>s.
     */
    private final List<ContactPresenceStatusListener> contactPresenceStatusListeners
        = new Vector<ContactPresenceStatusListener>();

    /**
     * The provider that created us.
     */
    protected final T parentProvider;

    /**
     * A list of listeners registered for
     *  <tt>ProviderPresenceStatusChangeEvent</tt>s.
     */
    private final List<ProviderPresenceStatusListener> providerPresenceStatusListeners
        = new Vector<ProviderPresenceStatusListener>();

    /**
     * A list of listeners registered for <tt>ServerStoredGroupChangeEvent</tt>s.
     */
    private final List<ServerStoredGroupListener> serverStoredGroupListeners
        = new Vector<ServerStoredGroupListener>();

    /**
     * The list of listeners interested in <tt>SubscriptionEvent</tt>s.
     */
    private final List<SubscriptionListener> subscriptionListeners
        = new Vector<SubscriptionListener>();

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

    /**
     * Implementation of the corresponding ProtocolProviderService
     * method.
     *
     * @param listener a presence status listener.
     */
    public void addContactPresenceStatusListener(
        ContactPresenceStatusListener listener)
    {
        synchronized (contactPresenceStatusListeners)
        {
            if (!contactPresenceStatusListeners.contains(listener))
                contactPresenceStatusListeners.add(listener);
        }
    }

    /**
     * Adds a listener that would receive events upon changes of the provider
     * presence status.
     * 
     * @param listener
     *            the listener to register for changes in our PresenceStatus.
     */
    public void addProviderPresenceStatusListener(
        ProviderPresenceStatusListener listener)
    {
        synchronized (providerPresenceStatusListeners)
        {
            if (!providerPresenceStatusListeners.contains(listener))
                providerPresenceStatusListeners.add(listener);
        }
    }

    /**
     * Registers a listener that would receive events upon changes in server
     * stored groups.
     * 
     * @param listener
     *            a ServerStoredGroupChangeListener impl that would receive
     *            events upon group changes.
     */
    public void addServerStoredGroupChangeListener(
        ServerStoredGroupListener listener)
    {
        synchronized (serverStoredGroupListeners)
        {
            if (!serverStoredGroupListeners.contains(listener))
                serverStoredGroupListeners.add(listener);
        }
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
     * Notifies all registered listeners of the new event.
     *
     * @param source the contact that has caused the event.
     * @param parentGroup the group that contains the source contact.
     * @param oldValue the status that the source contact detained before
     * changing it.
     */
    protected void fireContactPresenceStatusChangeEvent(Contact source,
                                                        ContactGroup parentGroup,
                                                        PresenceStatus oldValue)
    {
        PresenceStatus newValue = source.getPresenceStatus();

        if (oldValue.equals(newValue)) {
            if (logger.isDebugEnabled())
                logger.debug("Ignored prov stat. change evt. old==new = "
                         + oldValue);
            return;
        }

        fireContactPresenceStatusChangeEvent(
            source,
            parentGroup,
            oldValue,
            newValue);
    }

    public void fireContactPresenceStatusChangeEvent(Contact source,
                                                     ContactGroup parentGroup,
                                                     PresenceStatus oldValue,
                                                     PresenceStatus newValue)
    {
        ContactPresenceStatusChangeEvent evt
            = new ContactPresenceStatusChangeEvent(
                    source,
                    parentProvider,
                    parentGroup,
                    oldValue,
                    newValue);

        Collection<ContactPresenceStatusListener> listeners;
        synchronized (contactPresenceStatusListeners)
        {
            listeners =
                new ArrayList<ContactPresenceStatusListener>(
                        contactPresenceStatusListeners);
        }

        if (logger.isDebugEnabled())
            logger.debug(
            "Dispatching Contact Status Change. Listeners=" + listeners.size()
                + " evt=" + evt);

        for (ContactPresenceStatusListener listener : listeners)
            listener.contactPresenceStatusChanged(evt);
    }

    /**
     * Notify all subscription listeners of the corresponding contact property
     * change event.
     * 
     * @param eventID the String ID of the event to dispatch
     * @param source the ContactJabberImpl instance that this event is
     *            pertaining to.
     * @param oldValue the value that the changed property had before the change
     *            occurred.
     * @param newValue the value that the changed property currently has (after
     *            the change has occurred).
     */
    public void fireContactPropertyChangeEvent(String eventID, Contact source,
        Object oldValue, Object newValue)
    {
        ContactPropertyChangeEvent evt
            = new ContactPropertyChangeEvent(
                    source,
                    eventID,
                    oldValue,
                    newValue);

        Collection<SubscriptionListener> listeners;
        synchronized (subscriptionListeners)
        {
            listeners
                = new ArrayList<SubscriptionListener>(subscriptionListeners);
        }

        if (logger.isDebugEnabled())
            logger.debug("Dispatching a Contact Property Change Event to"
            + listeners.size() + " listeners. Evt=" + evt);

        for (SubscriptionListener listener : listeners)
            listener.contactModified(evt);
    }

    /**
     * Notifies all registered listeners of the new event.
     * 
     * @param oldValue
     *            the presence status we were in before the change.
     */
    protected void fireProviderStatusChangeEvent(PresenceStatus oldValue)
    {
        fireProviderStatusChangeEvent(oldValue, getPresenceStatus());
    }

    /**
     * Notify all provider presence listeners of the corresponding event change
     * 
     * @param oldValue
     *            the status our stack had so far
     * @param newValue
     *            the status we have from now on
     */
    protected void fireProviderStatusChangeEvent(
        PresenceStatus oldValue,
        PresenceStatus newValue)
    {
        ProviderPresenceStatusChangeEvent evt
            = new ProviderPresenceStatusChangeEvent(
                    parentProvider,
                    oldValue,
                    newValue);

        Collection<ProviderPresenceStatusListener> listeners;
        synchronized (providerPresenceStatusListeners)
        {
            listeners
                = new ArrayList<ProviderPresenceStatusListener>(
                        providerPresenceStatusListeners);
        }

        if (logger.isDebugEnabled())
            logger.debug(
            "Dispatching Provider Status Change. Listeners="
                + listeners.size()
                + " evt=" + evt);

        for (ProviderPresenceStatusListener listener : listeners)
            listener.providerStatusChanged(evt);

        if (logger.isDebugEnabled())
            logger.debug("status dispatching done.");
    }

    /**
     * Notify all provider presence listeners that a new status message has been
     * set.
     * 
     * @param oldStatusMessage
     *            the status message our stack had so far
     * @param newStatusMessage
     *            the status message we have from now on
     */
    protected void fireProviderStatusMessageChangeEvent(
        String oldStatusMessage,
        String newStatusMessage)
    {
        PropertyChangeEvent evt
            = new PropertyChangeEvent(
                    parentProvider,
                    ProviderPresenceStatusListener.STATUS_MESSAGE,
                    oldStatusMessage,
                    newStatusMessage);

        Collection<ProviderPresenceStatusListener> listeners;
        synchronized (providerPresenceStatusListeners)
        {
            listeners
                = new ArrayList<ProviderPresenceStatusListener>(
                        providerPresenceStatusListeners);
        }

        if (logger.isDebugEnabled())
            logger.debug(
            "Dispatching  stat. msg change. Listeners="
                + listeners.size()
                + " evt=" + evt);

        for (ProviderPresenceStatusListener listener : listeners)
            listener.providerStatusMessageChanged(evt);
    }

    /**
     * Notifies all registered listeners of the new event.
     *
     * @param source the contact that has caused the event.
     * @param eventID an identifier of the event to dispatch.
     */
    protected void fireServerStoredGroupEvent(
        ContactGroup source,
        int eventID)
    {
        ServerStoredGroupEvent evt
            = new ServerStoredGroupEvent(
                    source,
                    eventID,
                    source.getParentContactGroup(),
                    parentProvider,
                    this);

        Iterable<ServerStoredGroupListener> listeners;
        synchronized (serverStoredGroupListeners)
        {
            listeners
                = new ArrayList<ServerStoredGroupListener>(
                        serverStoredGroupListeners);
        }

        for (ServerStoredGroupListener listener : listeners)
            switch (eventID)
            {
            case ServerStoredGroupEvent.GROUP_CREATED_EVENT:
                listener.groupCreated(evt);
                break;
            case ServerStoredGroupEvent.GROUP_RENAMED_EVENT:
                listener.groupNameChanged(evt);
                break;
            case ServerStoredGroupEvent.GROUP_REMOVED_EVENT:
                listener.groupRemoved(evt);
                break;
            }
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
        SubscriptionEvent evt
            = new SubscriptionEvent(
                    source,
                    parentProvider,
                    parentGroup,
                    eventID,
                    errorCode,
                    errorReason);

        Collection<SubscriptionListener> listeners;
        synchronized (subscriptionListeners)
        {
            listeners =
                new ArrayList<SubscriptionListener>(subscriptionListeners);
        }

        if (logger.isDebugEnabled())
            logger.debug("Dispatching a Subscription Event to" + listeners.size()
            + " listeners. Evt=" + evt);

        for (SubscriptionListener listener : listeners)
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

        Collection<SubscriptionListener> listeners;
        synchronized (subscriptionListeners)
        {
            listeners
                = new ArrayList<SubscriptionListener>(subscriptionListeners);
        }

        if (logger.isDebugEnabled())
            logger.debug("Dispatching a Subscription Event to" + listeners.size()
            + " listeners. Evt=" + evt);

        for (SubscriptionListener listener : listeners)
            listener.subscriptionMoved(evt);
    }

    /**
     * Removes the specified listener so that it won't receive any further
     * updates on contact presence status changes
     *
     * @param listener the listener to remove.
     */
    public void removeContactPresenceStatusListener(
        ContactPresenceStatusListener listener)
    {
        synchronized (contactPresenceStatusListeners)
        {
            contactPresenceStatusListeners.remove(listener);
        }
    }

    /**
     * Unregisters the specified listener so that it does not receive further
     * events upon changes in local presence status.
     * 
     * @param listener
     *            ProviderPresenceStatusListener
     */
    public void removeProviderPresenceStatusListener(
        ProviderPresenceStatusListener listener)
    {
        synchronized (providerPresenceStatusListeners)
        {
            providerPresenceStatusListeners.remove(listener);
        }
    }

    /**
     * Removes the specified group change listener so that it won't receive
     * any further events.
     *
     * @param listener the ServerStoredGroupChangeListener to remove
     */
    public void removeServerStoredGroupChangeListener(
        ServerStoredGroupListener listener)
    {
        synchronized (serverStoredGroupListeners)
        {
            serverStoredGroupListeners.remove(listener);
        }
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
