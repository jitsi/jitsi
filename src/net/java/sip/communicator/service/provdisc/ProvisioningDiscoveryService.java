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
package net.java.sip.communicator.service.provdisc;

import net.java.sip.communicator.service.provdisc.event.*;

/**
 * Service that allow to retrieve a provisioning URL to configure
 * SIP Communicator. Implementations (not exhaustive) could use DHCP,
 * DNS (A, AAAA, SRV, TXT) or mDNS (Bonjour).
 *
 * @author Sebastien Vincent
 */
public interface ProvisioningDiscoveryService
{
    /**
     * Get the name of the method name used to retrieve provisioning URL.
     *
     * @return method name
     */
    public String getMethodName();

    /**
     * Launch a discovery for a provisioning URL.
     *
     * This method is asynchronous, the response will be notified to any
     * <tt>ProvisioningListener</tt> registered.
     */
    public void startDiscovery();

    /**
     * Launch a discovery for a provisioning URL. This method is synchronous and
     * may block for some time.
     *
     * @return provisioning URL
     */
    public String discoverURL();

    /**
     * Add a listener that will be notified when the
     * <tt>startDiscovery</tt> has finished.
     *
     * @param listener <tt>ProvisioningListener</tt> to add
     */
    public void addDiscoveryListener(DiscoveryListener listener);

    /**
     * Add a listener that will be notified when the
     * <tt>discoverProvisioningURL</tt> has finished.
     *
     * @param listener <tt>ProvisioningListener</tt> to add
     */
    public void removeDiscoveryListener(DiscoveryListener listener);
}
