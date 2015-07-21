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
package net.java.sip.communicator.service.ldap.event;

import java.util.*;

/**
 * Implementation of LdapEventManager.
 *
 * Class to be extended by any class which should send LdapEvent-s
 * and register LdapListenerS.
 *
 * @author Sebastien Mazy
 */

public class DefaultLdapEventManager
    implements LdapEventManager
{
    /**
     * All property change listeners registered so far.
     */
    protected Set<LdapListener> ldapListeners =
        Collections.synchronizedSet(new HashSet<LdapListener>());

    /**
     * Adds listener to our list of listeners
     *
     * @param listener  The LdapListener to be added
     *
     * @see net.java.sip.communicator.service.ldap.LdapDirectory#addLdapListener
     */
    public void addLdapListener(LdapListener listener)
    {
        this.ldapListeners.add(listener);
    }

    /**
     * Removes a LdapListener from the listener list.
     *
     * @param listener The LdapListener to be removed
     *
     * @see net.java.sip.communicator.service.ldap.
     * LdapDirectory#removeLdapListener
     */
    public void removeLdapListener(
            LdapListener listener)
    {
        this.ldapListeners.remove(listener);
    }

    /**
     * Fires an existing LdapEvent to any registered listeners.
     * @param event  The LdapEvent object.
     */
    public void fireLdapEvent(LdapEvent event)
    {
        for(LdapListener listener : this.ldapListeners)
        {
            this.fireLdapEvent(event, listener);
        }
    }

    /**
     * Fires an existing LdapEvent to a single listener.
     *
     * @param event  The LdapEvent object.
     * @param listener the listener to send the event to
     */
    public void fireLdapEvent(LdapEvent event, LdapListener listener)
    {
        listener.ldapEventReceived(event);
    }
}
