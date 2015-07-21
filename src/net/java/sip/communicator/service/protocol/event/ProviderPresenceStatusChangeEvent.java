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

import java.beans.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * Instances of this class represent a  change in the status of the provider
 * that triggerred them.
 * @author Emil Ivov
 */
public class ProviderPresenceStatusChangeEvent extends PropertyChangeEvent
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Creates an event instance indicating a change of the property
     * specified by <tt>eventType</tt> from <tt>oldValue</tt> to
     * <tt>newValue</tt>.
     * @param source the provider that generated the event
     * @param oldValue the status the source provider was int before enetering
     * the new state.
     * @param newValue the status the source provider is currently in.
     */
    public ProviderPresenceStatusChangeEvent(
                                ProtocolProviderService source,
                                PresenceStatus oldValue,
                                PresenceStatus newValue)
    {
        super( source,
               ProviderPresenceStatusChangeEvent.class.getName(),
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
     * @return a PresenceStatus instance indicating the event the source
     * provider was in before it entered its new state.
     */
    public PresenceStatus getOldStatus()
    {
        return (PresenceStatus)super.getOldValue();
    }

    /**
     * Returns the status of the provider after this event took place.
     * (i.e. at the time the event is being dispatched).
     * @return a PresenceStatus instance indicating the event the source
     * provider is in after the status change occurred.
     */
    public PresenceStatus getNewStatus()
    {
        return (PresenceStatus)super.getNewValue();
    }

    /**
     * Returns a String representation of this ProviderPresenceStatusChangeEvent
     *
     * @return  A a String representation of this
     * ProviderPresenceStatusChangeEvent.
     */
    @Override
    public String toString()
    {
        StringBuffer buff
            = new StringBuffer("ProviderPresenceStatusChangeEvent-[");
        return buff.append("OldStatus=").append(getOldStatus())
            .append(", NewStatus=").append(getNewStatus()).append("]").toString();
    }

}
