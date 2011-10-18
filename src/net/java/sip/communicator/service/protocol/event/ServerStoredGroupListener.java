/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
