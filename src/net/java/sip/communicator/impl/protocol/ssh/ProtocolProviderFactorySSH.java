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

package net.java.sip.communicator.impl.protocol.ssh;

import net.java.sip.communicator.service.protocol.*;

import org.osgi.framework.*;

/**
 *
 * @author Shobhit Jindal
 */
public abstract class ProtocolProviderFactorySSH
        extends ProtocolProviderFactory
{
    /**
     * The name of a property representing the IDENTITY_FILE of the protocol for
     * a ProtocolProviderFactory.
     */
    public static final String IDENTITY_FILE = "IDENTITY_FILE";

    /**
     * The name of a property representing the KNOWN_HOSTS_FILE of the protocol
     * for a ProtocolProviderFactory.
     */
    public static final String KNOWN_HOSTS_FILE = "KNOWN_HOSTS_FILE";

    protected ProtocolProviderFactorySSH(BundleContext bundleContext,
        String protocolName)
    {
        super(bundleContext, protocolName);
    }
}
