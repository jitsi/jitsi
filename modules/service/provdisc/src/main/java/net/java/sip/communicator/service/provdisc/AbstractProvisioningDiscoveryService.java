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

import java.util.*;

import net.java.sip.communicator.service.provdisc.event.*;

/**
 * Abstract base class of ProvisioningDiscoveryService that ease implementation
 *
 * @author seb
 *
 */
public abstract class AbstractProvisioningDiscoveryService
    implements ProvisioningDiscoveryService
{
    /**
     * List of <tt>ProvisioningListener</tt> that will be notified when
     * a provisioning URL is retrieved.
     */
    private List<DiscoveryListener> listeners =
        new ArrayList<DiscoveryListener>();

    /**
     * Get the name of the method name used to retrieve provisioning URL.
     *
     * @return method name
     */
    public abstract String getMethodName();

    /**
     * Launch a discovery for a provisioning URL.
     *
     * This method is asynchronous, the response will be notified to any
     * <tt>ProvisioningListener</tt> registered.
     */
    public abstract void startDiscovery();

    /**
     * Launch a discovery for a provisioning URL. This method is synchronous and
     * may block for some time.
     *
     * @return provisioning URL
     */
    public abstract String discoverURL();

    /**
     * Add a listener that will be notified when the
     * <tt>discoverProvisioningURL</tt> has finished.
     *
     * @param listener <tt>ProvisioningListener</tt> to add
     */
    public void addDiscoveryListener(DiscoveryListener listener)
    {
        if(!listeners.contains(listener))
        {
            listeners.add(listener);
        }
    }

    /**
     * Add a listener that will be notified when the
     * <tt>discoverProvisioningURL</tt> has finished.
     *
     * @param listener <tt>ProvisioningListener</tt> to add
     */
    public void removeDiscoveryListener(DiscoveryListener listener)
    {
        if(listeners.contains(listener))
        {
            listeners.remove(listener);
        }
    }

    /**
     * Notify all listeners about a <tt>DiscoveryEvent</tt>.
     *
     * @param event <tt>DiscoveryEvent</tt> that contains provisioning URL
     */
    public void fireDiscoveryEvent(DiscoveryEvent event)
    {
        for(DiscoveryListener listener : listeners)
        {
            listener.notifyProvisioningURL(event);
        }
    }
}
