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
package net.java.sip.communicator.plugin.dnsconfig;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * Container for all DNS configuration panels.
 *
 * @author Ingo Bauersachs
 */
public class DnsContainerPanel
    extends SIPCommTabbedPane
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    //service references
    private ResourceManagementService R;

    //panels
    private ParallelDnsPanel parallelDnsPanel;
    private DnssecPanel dnssecPanel;

    /**
     * Creates a new instance of this class. Loads all panels.
     */
    public DnsContainerPanel()
    {
        initServices();

        parallelDnsPanel = new ParallelDnsPanel();
        addTab(R.getI18NString("plugin.dnsconfig.PARALLEL_DNS"),
            parallelDnsPanel);

        dnssecPanel = new DnssecPanel(parallelDnsPanel);
        addTab(R.getI18NString("plugin.dnsconfig.DNSSEC"),
            dnssecPanel);
    }

    /**
     * Loads all service references
     */
    private void initServices()
    {
        BundleContext bc = DnsConfigActivator.bundleContext;
        R = ServiceUtils.getService(bc, ResourceManagementService.class);
    }
}
