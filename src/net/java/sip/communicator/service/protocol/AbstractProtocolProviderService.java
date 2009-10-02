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
 * Implements "standard" functionality of <tt>ProtocolProviderService</tt> in
 * order to make it easier for implementers to provide complete solutions while
 * focusing on protocol-specific details.
 */
public abstract class AbstractProtocolProviderService
    implements ProtocolProviderService
{
    /**
     * Logger of this class
     */
    private static final Logger logger =
        Logger.getLogger(AbstractProtocolProviderService.class);

    /**
     * A list of all listeners registered for
     * <tt>RegistrationStateChangeEvent</tt>s.
     */
    private final List<RegistrationStateChangeListener> registrationListeners =
        new ArrayList<RegistrationStateChangeListener>();

    /**
     * The hashtable with the operation sets that we support locally.
     */
    protected final Map<String, OperationSet> supportedOperationSets
        = new Hashtable<String, OperationSet>();

    /**
     * Registers the specified listener with this provider so that it would
     * receive notifications on changes of its state or other properties such
     * as its local address and display name.
     *
     * @param listener the listener to register.
     */
    public void addRegistrationStateChangeListener(
        RegistrationStateChangeListener listener)
    {
        synchronized(registrationListeners)
        {
            if (!registrationListeners.contains(listener))
                registrationListeners.add(listener);
        }
    }

    /**
     * Creates a RegistrationStateChange event corresponding to the specified
     * old and new states and notifies all currently registered listeners.
     *
     * @param oldState the state that the provider had before the change
     * occurred
     * @param newState the state that the provider is currently in.
     * @param reasonCode a value corresponding to one of the REASON_XXX fields
     * of the RegistrationStateChangeEvent class, indicating the reason for
     * this state transition.
     * @param reason a String further explaining the reason code or null if
     * no such explanation is necessary.
     */
    public void fireRegistrationStateChanged( RegistrationState oldState,
                                               RegistrationState newState,
                                               int               reasonCode,
                                               String            reason)
    {
        RegistrationStateChangeEvent event =
            new RegistrationStateChangeEvent(
                            this, oldState, newState, reasonCode, reason);

        RegistrationStateChangeListener[] listeners;
        synchronized (registrationListeners)
        {
            listeners
                = registrationListeners.toArray(
                        new RegistrationStateChangeListener[
                                registrationListeners.size()]);
        }

        if (logger.isDebugEnabled())
            logger.debug(
                "Dispatching "
                    + event
                    + " to "
                    + listeners.length
                    + " listeners.");

        for (RegistrationStateChangeListener listener : listeners)
            try
            {
                listener.registrationStateChanged(event);
            }
            catch (Throwable throwable)
            {

                /*
                 * The registration state has already changed and we're not
                 * using the RegistrationStateChangeListeners to veto the change
                 * so it doesn't make sense to, for example, disconnect because
                 * one of them malfunctioned.
                 *
                 * Of course, death cannot be ignored.
                 */
                if (throwable instanceof ThreadDeath)
                    throw (ThreadDeath) throwable;
                logger.error(
                    "An error occurred while executing RegistrationStateChangeLister#registrationStateChanged(RegistrationStateChangeEvent) of "
                        + listener,
                    throwable);
            }

        logger.trace("Done.");
    }

    /**
     * Returns the operation set corresponding to the specified class or null if
     * this operation set is not supported by the provider implementation.
     * 
     * @param opsetClass the <tt>Class</tt> of the operation set that we're
     * looking for.
     * @return returns an <tt>OperationSet</tt> of the specified <tt>Class</tt>
     * if the undelying implementation supports it; <tt>null</tt>, otherwise.
     */
    @SuppressWarnings("unchecked")
    public <T extends OperationSet> T getOperationSet(Class<T> opsetClass)
    {
        return (T) doGetSupportedOperationSets().get(opsetClass.getName());
    }

    /**
     * Returns the protocol display name. This is the name that would be used
     * by the GUI to display the protocol name.
     * 
     * @return a String containing the display name of the protocol this service
     * is implementing
     */
    public String getProtocolDisplayName()
    {
        String displayName =
            getAccountID().getAccountPropertyString(
                ProtocolProviderFactory.PROTOCOL);
        return (displayName == null) ? getProtocolName() : displayName;
    }

    /**
     * Returns an array containing all operation sets supported by the current
     * implementation. When querying this method users must be prepared to
     * receive any subset of the OperationSet-s defined by this service. They
     * MUST ignore any OperationSet-s that they are not aware of and that may be
     * defined by future version of this service. Such "unknown" OperationSet-s
     * though not encouraged, may also be defined by service implementors.
     * 
     * @return a java.util.Map containing instance of all supported operation
     *         sets mapped against their class names (e.g.
     *         OperationSetPresence.class.getName()) .
     */
    public Map<String, OperationSet> getSupportedOperationSets()
    {
        Map<String, OperationSet> supportedOperationSets =
            doGetSupportedOperationSets();

        return new Hashtable<String, OperationSet>(supportedOperationSets);
    }

    protected Map<String, OperationSet> doGetSupportedOperationSets()
    {
        return supportedOperationSets;
    }

    /**
     * Indicates whether or not this provider is registered
     *
     * @return true if the provider is currently registered and false
     *   otherwise.
     */
    public boolean isRegistered()
    {
        return getRegistrationState().equals(RegistrationState.REGISTERED);
    }

    /**
     * Removes the specified registration state change listener so that it does
     * not receive any further notifications upon changes of the
     * RegistrationState of this provider.
     *
     * @param listener the listener to register for
     * <tt>RegistrationStateChangeEvent</tt>s.
     */
    public void removeRegistrationStateChangeListener(
        RegistrationStateChangeListener listener)
    {
        synchronized(registrationListeners)
        {
            registrationListeners.remove(listener);
        }
    }
}
