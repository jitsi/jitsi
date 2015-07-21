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
package net.java.sip.communicator.slick.configuration;

import java.util.*;

import junit.framework.*;

import org.jitsi.service.configuration.*;
import org.osgi.framework.*;

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

        List<String> propertyNames
            = configurationService.getPropertyNamesByPrefix("", false);

        // Assertion removed as default properties will stay after configuration
        // is purged.
        //assertTrue(
        //    "The configuration service contains properties after purging.",
        //    (propertyNames == null) || (propertyNames.size() <= 0));
    }
}
