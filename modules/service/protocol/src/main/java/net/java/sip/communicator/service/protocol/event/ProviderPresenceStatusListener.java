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

/**
 * An event listener that should be implemented by parties interested in changes
 * that occur in the state of a ProtocolProvider (e.g. PresenceStatusChanges)
 * @author Emil Ivov
 */
public interface ProviderPresenceStatusListener extends java.util.EventListener
{
    /**
     * The property name of PropertyChangeEvents announcing changes in our
     * status message.
     */
    public static final String STATUS_MESSAGE = "StatusMessage";

    /**
     * The method is called by a ProtocolProvider implementation whenever
     * a change in the presence status of the corresponding provider had
     * occurred.
     * @param evt ProviderStatusChangeEvent the event describing the status
     * change.
     */
    public void providerStatusChanged(ProviderPresenceStatusChangeEvent evt);

    /**
     * The method is called by a ProtocolProvider implementation whenever a
     * change in the status message of the corresponding provider has occurred
     * and has been confirmed by the server.
     *
     * @param evt a PropertyChangeEvent with a STATUS_MESSAGE property name,
     * containing the old and new status messages.
     */
    public void providerStatusMessageChanged(PropertyChangeEvent evt);
}
