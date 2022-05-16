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

package net.java.sip.communicator.service.argdelegation;

/**
 * This interface is meant to be implemented by all bundles that wish to handle
 * URIs passed as invocation arguments.
 *
 * @author Emil Ivov <emcho at sip-communicator.org>
 */
public interface UriHandler
{
    /**
     * The name of the property that we use in the service registration
     * properties to store a protocol name when registering <tt>UriHandler</tt>s
     */
    public static final String PROTOCOL_PROPERTY = "ProtocolName";

    /**
     * Returns the protocols that this handler is responsible for.
     *
     * @return protocols that this handler is responsible for
     */
    public String[] getProtocol();

    /**
     * Handles/opens the URI.
     *
     * @param uri the URI that the handler has to open.
     */
    public void handleUri(String uri);
}
