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
 * An LdapEvent is triggered when
 * the state of the LDAP connection changes
 * or when a search result is received
 *
 * @author Sebastien Mazy
 */
public interface LdapListener
    extends EventListener
{
    /**
     * This method gets called when
     * a server need to send a message (person found, search status)
     *
     * @param event An LdapEvent probably sent by an LdapDirectory
     */
    public void ldapEventReceived(LdapEvent event);
}
