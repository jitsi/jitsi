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
package net.java.sip.communicator.impl.protocol.jabber.extensions.colibri;

import org.jivesoftware.smack.packet.*;

/**
 * The IQ used to trigger the graceful shutdown mode of the videobridge which
 * receives the stanza(given that source JID is authorized to start it).
 *
 * @author Pawel Domas
 */
public class GracefulShutdownIQ
    extends IQ
{
    public static final String NAMESPACE = ColibriConferenceIQ.NAMESPACE;

    public static final String ELEMENT_NAME = "graceful-shutdown";

    @Override
    public String getChildElementXML()
    {
        return "<" + ELEMENT_NAME + " xmlns='" + NAMESPACE + "' />";
    }
}
