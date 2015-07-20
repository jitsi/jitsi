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
 * Used to deliver events concerning contact groups in server stored contact
 * lists.
 *
 * @author Emil Ivov
 */
public interface ServerStoredGroupListener
    extends EventListener
{
    /**
     * Called whnever an indication is received that a new server stored group
     * is created.
     * @param evt a ServerStoredGroupEvent containing a reference to the
     * newly created group.
     */
    public void groupCreated(ServerStoredGroupEvent evt);

    /**
     * Called whnever an indication is received that an existing server stored
     * group has been removed.
     * @param evt a ServerStoredGroupEvent containing a reference to the
     * newly created group.
     */
    public void groupRemoved(ServerStoredGroupEvent evt);

    /**
     * Called when an indication is received that the name of a server stored
     * contact group has changed.
     * @param evt a ServerStoredGroupEvent containing the details of the
     * name change.
     */
    public void groupNameChanged(ServerStoredGroupEvent evt);

    /**
     * Called when a contact group has been successfully resolved against the
     * server. ContactGroup-s are considered unresolved when they have been
     * stored locally and thus re-loaded when the application was started. A
     * group is resolved when the fact that it is still present in the server
     * stored contact list has been confirmed by the server.
     * <p>
     * @param evt a ServerStoredGroupEvent containing the source group.
     */
    public void groupResolved(ServerStoredGroupEvent evt);

}
