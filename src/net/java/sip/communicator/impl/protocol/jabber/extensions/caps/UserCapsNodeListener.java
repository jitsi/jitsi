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
package net.java.sip.communicator.impl.protocol.jabber.extensions.caps;

/**
 * Represents a listener of events notifying about changes in the list of user
 * caps nodes of <tt>EntityCapsManager</tt>.
 *
 * @author Lubomir Marinov
 */
public interface UserCapsNodeListener
{
    /**
     * Notifies this listener that an <tt>EntityCapsManager</tt> has added a
     * record for a specific user about the caps node the user has.
     *
     * @param user the user (full JID)
     * @param node the entity caps node#ver
     * @param online indicates if the user for which we're notified is online
     */
    public void userCapsNodeAdded(String user, String node, boolean online);

    /**
     * Notifies this listener that an <tt>EntityCapsManager</tt> has removed a
     * record for a specific user about the caps node the user has.
     *
     * @param user the user (full JID)
     * @param node the entity caps node#ver
     * @param online indicates if the user for which we're notified is online
     */
    public void userCapsNodeRemoved(String user, String node, boolean online);
}
