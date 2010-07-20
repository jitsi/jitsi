/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.util.*;

import net.java.sip.communicator.service.protocol.event.*;

/**
 * Represents a default implementation of
 * <tt>OperationSetContactCapabilities</tt> which attempts to make it easier for
 * implementers to provide complete solutions while focusing on
 * implementation-specific functionality.
 * 
 * @author Lubomir Marinov
 */
public abstract class AbstractOperationSetContactCapabilities
    implements OperationSetContactCapabilities
{

    /**
     * The list of <tt>ContactCapabilitiesListener</tt>s registered to be
     * notified about changes in the list of <tt>OperationSet</tt> capabilities
     * of <tt>Contact</tt>s.
     */
    private final List<ContactCapabilitiesListener> contactCapabilitiesListeners
        = new LinkedList<ContactCapabilitiesListener>();

    /**
     * Registers a specific <tt>ContactCapabilitiesListener</tt> to be notified
     * about changes in the list of <tt>OperationSet</tt> capabilities of
     * <tt>Contact</tt>s. If the specified <tt>listener</tt> has already been
     * registered, adding it again has no effect.
     *
     * @param listener the <tt>ContactCapabilitiesListener</tt> which is to be
     * notified about changes in the list of <tt>OperationSet</tt> capabilities
     * of <tt>Contact</tt>s
     * @see OperationSetContactCapabilities#addContactCapabilitiesListener(
     * ContactCapabilitiesListener)
     */
    public void addContactCapabilitiesListener(
            ContactCapabilitiesListener listener)
    {
        if (listener == null)
            throw new NullPointerException("listener");

        synchronized (contactCapabilitiesListeners)
        {
            if (!contactCapabilitiesListeners.contains(listener))
                contactCapabilitiesListeners.add(listener);
        }
    }

    /**
     * Fires a new <tt>ContactCapabilitiesEvent</tt> to notify the registered
     * <tt>ContactCapabilitiesListener</tt>s that a specific <tt>Contact</tt>
     * has changed its list of <tt>OperationSet</tt> capabilities.
     *
     * @param sourceContact the <tt>Contact</tt> which is the source/cause of
     * the event to be fired
     * @param eventID the ID of the event to be fired which indicates the
     * specifics of the change of the list of <tt>OperationSet</tt> capabilities
     * of the specified <tt>sourceContact</tt> and the details of the event
     */
    protected void fireContactCapabilitiesEvent(
            Contact sourceContact,
            int eventID)
    {
        ContactCapabilitiesListener[] listeners;

        synchronized (contactCapabilitiesListeners)
        {
            listeners
                = contactCapabilitiesListeners.toArray(
                        new ContactCapabilitiesListener[
                                contactCapabilitiesListeners.size()]);
        }
        if (listeners.length != 0)
        {
            ContactCapabilitiesEvent event
                = new ContactCapabilitiesEvent(sourceContact, eventID);

            for (ContactCapabilitiesListener listener : listeners)
            {
                // TODO Auto-generated method stub
            }
        }
    }

    /**
     * Gets the <tt>OperationSet</tt> corresponding to the specified
     * <tt>Class</tt> and supported by the specified <tt>Contact</tt>. If the
     * returned value is non-<tt>null</tt>, it indicates that the
     * <tt>Contact</tt> is considered by the associated protocol provider to
     * possess the <tt>opsetClass</tt> capability. Otherwise, the associated
     * protocol provider considers <tt>contact</tt> to not have the
     * <tt>opsetClass</tt> capability.
     * <tt>AbstractOperationSetContactCapabilities</tt> looks for the name of
     * the specified <tt>opsetClass</tt> in the <tt>Map</tt> returned by
     * {@link #getSupportedOperationSets(Contact)} and returns the associated
     * <tt>OperationSet</tt>. Since the implementation is suboptimal due to the
     * temporary <tt>Map</tt> allocations and loopups, extenders are advised to
     * override.
     *
     * @param <T> the type extending <tt>OperationSet</tt> for which the
     * specified <tt>contact</tt> is to be checked whether it possesses it as a
     * capability 
     * @param contact the <tt>Contact</tt> for which the <tt>opsetClass</tt>
     * capability is to be queried
     * @param opsetClass the <tt>OperationSet</tt> <tt>Class</tt> for which the
     * specified <tt>contact</tt> is to be checked whether it possesses it as a
     * capability
     * @return the <tt>OperationSet</tt> corresponding to the specified
     * <tt>opsetClass</tt> which is considered by the associated protocol
     * provider to be possessed as a capability by the specified
     * <tt>contact</tt>; otherwise, <tt>null</tt>
     * @see OperationSetContactCapabilities#getOperationSet(Contact, Class)
     */
    @SuppressWarnings("unchecked")
    public <T extends OperationSet> T getOperationSet(
            Contact contact,
            Class<T> opsetClass)
    {
        Map<String, OperationSet> supportedOperationSets
            = getSupportedOperationSets(contact);

        if (supportedOperationSets != null)
        {
            OperationSet opset
                = supportedOperationSets.get(opsetClass.getName());

            if (opsetClass.isInstance(opset))
                return (T) opset;
        }
        return null;
    }

    /**
     * Unregisters a specific <tt>ContactCapabilitiesListener</tt> to no longer
     * be notified about changes in the list of <tt>OperationSet</tt>
     * capabilities of <tt>Contact</tt>s. If the specified <tt>listener</tt> has
     * already been unregistered or has never been registered, removing it has
     * no effect.
     *
     * @param listener the <tt>ContactCapabilitiesListener</tt> which is to no
     * longer be notified about changes in the list of <tt>OperationSet</tt>
     * capabilities of <tt>Contact</tt>s
     * @see OperationSetContactCapabilities#removeContactCapabilitiesListener(
     * ContactCapabilitiesListener)
     */
    public void removeContactCapabilitiesListener(
            ContactCapabilitiesListener listener)
    {
        synchronized (contactCapabilitiesListeners)
        {
            contactCapabilitiesListeners.remove(listener);
        }
    }
}
