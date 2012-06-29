/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.dnsconfig;

import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

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
