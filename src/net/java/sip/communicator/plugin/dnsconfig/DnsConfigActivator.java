package net.java.sip.communicator.plugin.dnsconfig;

import java.util.*;
import net.java.sip.communicator.service.gui.*;

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

        bc.registerService(
            ConfigurationForm.class.getName(),
            new LazyConfigurationForm(
                DnsConfigPanel.class.getName(),
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
    }
}
