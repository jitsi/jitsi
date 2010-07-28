/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.util.*;

import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Represents a default implementation of
 * <tt>OperationSetContactCapabilities</tt> which attempts to make it easier for
 * implementers to provide complete solutions while focusing on
 * implementation-specific functionality.
 * 
 * @param <T> the type of the <tt>ProtocolProviderService</tt> implementation
 * providing the <tt>AbstractOperationSetContactCapabilities</tt> implementation
 *
 * @author Lubomir Marinov
 */
public abstract class AbstractOperationSetContactCapabilities<T extends ProtocolProviderService>
    implements OperationSetContactCapabilities
{
    /**
     * The <tt>Logger</tt> used by the
     * <tt>AbstractOperationSetContactCapabilities</tt> class and its instances
     * for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(AbstractOperationSetContactCapabilities.class);

    /**
     * The list of <tt>ContactCapabilitiesListener</tt>s registered to be
     * notified about changes in the list of <tt>OperationSet</tt> capabilities
     * of <tt>Contact</tt>s.
     */
    private final List<ContactCapabilitiesListener> contactCapabilitiesListeners
        = new LinkedList<ContactCapabilitiesListener>();

    /**
     * The <tt>ProtocolProviderService</tt> which provides this
     * <tt>OperationSetContactCapabilities</tt>.
     */
    protected final T parentProvider;

    /**
     * Initializes a new <tt>AbstractOperationSetContactCapabilities</tt>
     * instance which is to be provided by a specific
     * <tt>ProtocolProviderService</tt> implementation.
     *
     * @param parentProvider the <tt>ProtocolProviderService</tt> implementation
     * which will provide the new instance
     */
    protected AbstractOperationSetContactCapabilities(T parentProvider)
    {
        if (parentProvider == null)
            throw new NullPointerException("parentProvider");

        this.parentProvider = parentProvider;
    }

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
                switch (eventID)
                {
                case ContactCapabilitiesEvent.SUPPORTED_OPERATION_SETS_CHANGED:
                    listener.supportedOperationSetsChanged(event);
                    break;
                default:
                    if (logger.isDebugEnabled())
                    {
                        logger.debug(
                                "Cannot fire ContactCapabilitiesEvent with"
                                    + " unsupported eventID: "
                                    + eventID);
                    }
                    throw new IllegalArgumentException("eventID");
                }
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
     * @param <U> the type extending <tt>OperationSet</tt> for which the
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
    public <U extends OperationSet> U getOperationSet(
            Contact contact,
            Class<U> opsetClass)
    {
        Map<String, OperationSet> supportedOperationSets
            = getSupportedOperationSets(contact);

        if (supportedOperationSets != null)
        {
            OperationSet opset
                = supportedOperationSets.get(opsetClass.getName());

            if (opsetClass.isInstance(opset))
                return (U) opset;
        }
        return null;
    }

    /**
     * Gets the <tt>OperationSet</tt>s supported by a specific <tt>Contact</tt>.
     * The returned <tt>OperationSet</tt>s are considered by the associated
     * protocol provider to capabilities possessed by the specified
     * <tt>contact</tt>. The default implementation returns the result of
     * calling {@link ProtocolProviderService#getSupportedOperationSets()} on
     * the associated <tt>ProtocolProviderService</tt> implementation. Extenders
     * have to override the default implementation in order to provide actual
     * capability detection for the specified <tt>contact</tt>.
     *
     * @param contact the <tt>Contact</tt> for which the supported
     * <tt>OperationSet</tt> capabilities are to be retrieved
     * @return a <tt>Map</tt> listing the <tt>OperationSet</tt>s considered by
     * the associated protocol provider to be supported by the specified
     * <tt>contact</tt> (i.e. to be possessed as capabilities). Each supported
     * <tt>OperationSet</tt> capability is represented by a <tt>Map.Entry</tt>
     * with key equal to the <tt>OperationSet</tt> class name and value equal to
     * the respective <tt>OperationSet</tt> instance
     * @see OperationSetContactCapabilities#getSupportedOperationSets(Contact)
     */
    public Map<String, OperationSet> getSupportedOperationSets(Contact contact)
    {
        return parentProvider.getSupportedOperationSets();
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
        if (listener != null)
        {
            synchronized (contactCapabilitiesListeners)
            {
                contactCapabilitiesListeners.remove(listener);
            }
        }
    }
}
