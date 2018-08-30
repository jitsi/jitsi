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
package net.java.sip.communicator.impl.protocol.jabber.extensions.vcardavatar;

import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.packet.Stanza;

public class VCardTempXUpdateInterceptor implements StanzaListener
{
    private VCardTempXUpdatePresenceExtension presenceExtension;

    /**
     * Creates a new instance of this class.
     * @param extension the extension to add to the presence packets.
     */
    public VCardTempXUpdateInterceptor(
        VCardTempXUpdatePresenceExtension extension)
    {
        this.presenceExtension = extension;
    }

    /**
     * Intercepts sent presence packets in order to add this extension.
     *
     * @param packet The sent presence packet.
     */
    @Override
    public void processStanza(Stanza packet)
        throws NotConnectedException,
        InterruptedException
    {
        // remove the current if any, to no accumulate extensions
        // when updating presence
        packet.removeExtension(
            presenceExtension.getElementName(),
            presenceExtension.getNamespace());

        packet.addExtension(presenceExtension);
    }
}
