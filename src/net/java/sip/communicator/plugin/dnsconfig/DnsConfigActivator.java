/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.dnsconfig;

import java.util.*;

import net.java.sip.communicator.service.fileaccess.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * OSGi bundle activator for the parallel DNS configuration form.
 * 
 * @author Ingo Bauersachs
 */
public class DnsConfigActivator
    implements BundleActivator
{
    static BundleContext bundleContext;
    private static FileAccessService fileAccessService;
    private ServiceRegistration configForm;

    /**
     * Starts this bundle.
     * @param bc the bundle context
     * @throws Exception if something goes wrong
     */
    public void start(BundleContext bc)
        throws Exception
    {
        bundleContext = bc;
        Dictionary<String, String> properties = new Hashtable<String, String>();
        properties.put(ConfigurationForm.FORM_TYPE,
                       ConfigurationForm.ADVANCED_TYPE);

        configForm = bc.registerService(
            ConfigurationForm.class.getName(),
            new LazyConfigurationForm(
                DnsContainerPanel.class.getName(),
                getClass().getClassLoader(),
                "plugin.dnsconfig.ICON",
                "plugin.dnsconfig.TITLE",
                2000, true),
            properties);
    }

    /**
     * Stops this bundle.
     * @param bc the bundle context
     * @throws Exception if something goes wrong
     */
    public void stop(BundleContext bc)
        throws Exception
    {
        configForm.unregister();
    }

    /**
     * Returns the <tt>FileAccessService</tt> obtained from the bundle context.
     *
     * @return the <tt>FileAccessService</tt> obtained from the bundle context
     */
    public static FileAccessService getFileAccessService()
    {
        if (fileAccessService == null)
        {
            fileAccessService
                = ServiceUtils.getService(
                        bundleContext,
                        FileAccessService.class);
        }
        return fileAccessService;
    }
}
