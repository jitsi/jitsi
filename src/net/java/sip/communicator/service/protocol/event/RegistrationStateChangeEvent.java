/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.beans.PropertyChangeEvent;
import net.java.sip.communicator.service.protocol.RegistrationState;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;

/**
 * Instances of this class represent a  change in the status of the provider
 * that triggerred them.
 * @author Emil Ivov
 */
public class RegistrationStateChangeEvent extends PropertyChangeEvent
{

    /**
     * Creates an event instance indicating a change of the property
     * specified by <tt>eventType</tt> from <tt>oldValue</tt> to
     * <tt>newValue</tt>.
     * @param source the provider that generated the event
     * @param oldValue the status the source provider was int before enetering
     * the new state.
     * @param newValue the status the source provider is currently in.
     */
    public RegistrationStateChangeEvent( ProtocolProviderService source,
                                         RegistrationState oldValue,
                                         RegistrationState newValue)
    {
        super(source,
              RegistrationStateChangeEvent.class.getName(),
              oldValue,
              newValue);
    }

    /**
     * Returns the provider that has genereted this event
     * @return the provider that generated the event.
     */
    public ProtocolProviderService getProvider()
    {
        return (ProtocolProviderService)getSource();
    }

    /**
     * Returns the status of the provider before this event took place.
     * @return a RegistrationState instance indicating the event the source
     * provider was in before it entered its new state.
     */
    public RegistrationState getOldState()
    {
        return (RegistrationState)super.getOldValue();
    }

    /**
     * Returns the status of the provider after this event took place.
     * (i.e. at the time the event is being dispatched).
     * @return a RegistrationState instance indicating the event the source
     * provider is in after the status change occurred.
     */
    public RegistrationState getNewState()
    {
        return (RegistrationState)super.getNewValue();
    }

    /**
     * Returns a string representation of this event.
     * @return a String containing the name of the event as well as the names
     * of the old and new <tt>RegistrationState</tt>s
     */
    public String toString()
    {
        return "RegistrationStateChangeEvent[ oldState="
            + getOldState().getStateName()
            + "; newState="+ getNewState()+"]";
    }
}
