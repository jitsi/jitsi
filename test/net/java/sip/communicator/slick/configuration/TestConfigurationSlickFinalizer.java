/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.configuration;

import org.osgi.framework.*;
import junit.framework.*;
import net.java.sip.communicator.service.configuration.*;

/**
 * Performs finalization tasks (such as removing the configuration file) at the
 * end of the ConfigurationServiceLick.
 *
 * @author Emil Ivov
 */
public class TestConfigurationSlickFinalizer
    extends TestCase
{
    public TestConfigurationSlickFinalizer()
    {
        super();
    }

    public TestConfigurationSlickFinalizer(String name)
    {
        super(name);
    }

    /**
     * Removes the currently stored configuration.
     */
    public void testPurgeConfiguration()
    {
        BundleContext context = ConfigurationServiceLick.bc;
        ServiceReference ref = context.getServiceReference(
            ConfigurationService.class.getName());
        ConfigurationService configurationService
            = (ConfigurationService)context.getService(ref);

        configurationService.purgeStoredConfiguration();

    }
}
