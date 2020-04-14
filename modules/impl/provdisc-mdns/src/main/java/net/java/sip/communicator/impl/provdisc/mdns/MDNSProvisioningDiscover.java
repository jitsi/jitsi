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
package net.java.sip.communicator.impl.provdisc.mdns;

import java.io.*;
import java.util.*;

import javax.jmdns.*;

import net.java.sip.communicator.service.provdisc.event.*;
import net.java.sip.communicator.util.*;

/**
 * Class that will perform mDNS provisioning discovery.
 *
 * @author Sebastien Vincent
 */
public class MDNSProvisioningDiscover
    implements Runnable
{
    /**
     * Logger.
     */
    private final Logger logger
        = Logger.getLogger(MDNSProvisioningDiscover.class);

    /**
     * MDNS timeout (in milliseconds).
     */
    private static final int MDNS_TIMEOUT = 2000;

    /**
     * List of <tt>ProvisioningListener</tt> that will be notified when
     * a provisioning URL is retrieved.
     */
    private List<DiscoveryListener> listeners =
        new ArrayList<DiscoveryListener>();

    /**
     * Reference to JmDNS singleton.
     */
    private JmDNS jmdns = null;

    /**
     * Constructor.
     */
    public MDNSProvisioningDiscover()
    {
    }

    /**
     * Thread entry point. It runs <tt>discoverProvisioningURL</tt> in a
     * separate thread.
     */
    public void run()
    {
        String url = discoverProvisioningURL();

        if(url != null)
        {
            /* as we run in an asynchronous manner, notify the listener */
            DiscoveryEvent evt = new DiscoveryEvent(this, url);

            for(DiscoveryListener listener : listeners)
            {
                listener.notifyProvisioningURL(evt);
            }
        }
    }

    /**
     * It sends a mDNS to retrieve provisioning URL and wait for a response.
     * Thread stops after first successful answer that contains the provisioning
     * URL.
     *
     * @return provisioning URL or null if no provisioning URL was discovered
     */
    public String discoverProvisioningURL()
    {
        StringBuffer url = new StringBuffer();

        try
        {
            jmdns = JmDNS.create();
        }
        catch(IOException e)
        {
            logger.info("Failed to create JmDNS", e);
            return null;
        }

        ServiceInfo info = jmdns.getServiceInfo("_https._tcp.local",
                "Provisioning URL", MDNS_TIMEOUT);

        if(info == null)
        {
            /* try HTTP */
            info = jmdns.getServiceInfo("_http._tcp.local", "Provisioning URL",
                    MDNS_TIMEOUT);
        }

        if(info != null && info.getName().equals("Provisioning URL"))
        {
            String protocol = info.getApplication();

            url.append(info.getURL(protocol));

            Enumeration<String> en = info.getPropertyNames();

            if(en.hasMoreElements())
            {
                url.append("?");
            }

            /* add the parameters */
            while(en.hasMoreElements())
            {
                String tmp = en.nextElement();

                /* take all other parameters except "path" */
                if(tmp.equals("path"))
                {
                    continue;
                }

                url.append(tmp);
                url.append("=");
                url.append(info.getPropertyString(tmp));

                if(en.hasMoreElements())
                {
                    url.append("&");
                }
            }
        }

        /* close jmdns */
        try
        {
            jmdns.close();
            jmdns = null;
        }
        catch(Exception e)
        {
            logger.warn("Failed to close JmDNS", e);
        }

        return (url.toString().length() > 0) ? url.toString() : null;
    }

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
}
