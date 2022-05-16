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
package net.java.sip.communicator.impl.protocol.irc;

/**
 * IRC connection listener interface for events on connection interruptions.
 *
 * @author Danny van Heumen
 */
public interface IrcConnectionListener
{
    /**
     * Event for any kind of connection interruption.
     *
     * IRC recognizes the ERROR message that signals a fatal or serious error in
     * the IRC connection. Upon receiving such an error, IRC assumes unreliable
     * connection thus disconnects its components and issues this event to
     * signal the event to the listener.
     *
     * Some IRC servers will also issue an ERROR message as a reply upon
     * receiving the QUIT command from the client, i.e. the local user.
     *
     * @param connection the connection that gets interrupted
     */
    void connectionInterrupted(IrcConnection connection);
}
