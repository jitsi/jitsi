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

import java.util.*;

/**
 * Represents a listener of changes in the capabilities of a <tt>Contact</tt> as
 * known by an associated protocol provider delivered in the form of
 * <tt>ContactCapabilitiesEvent</tt>s.
 *
 * @author Lubomir Marinov
 */
public interface ContactCapabilitiesListener
    extends EventListener
{
    /**
     * Notifies this listener that the list of the <tt>OperationSet</tt>
     * capabilities of a <tt>Contact</tt> has changed.
     *
     * @param event a <tt>ContactCapabilitiesEvent</tt> with ID
     * {@link ContactCapabilitiesEvent#SUPPORTED_OPERATION_SETS_CHANGED} which
     * specifies the <tt>Contact</tt> whose list of <tt>OperationSet</tt>
     * capabilities has changed
     */
    public void supportedOperationSetsChanged(ContactCapabilitiesEvent event);
}
