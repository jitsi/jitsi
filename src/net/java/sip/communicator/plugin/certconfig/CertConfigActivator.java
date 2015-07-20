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
package net.java.sip.communicator.plugin.certconfig;

import java.util.*;

import net.java.sip.communicator.service.certificate.*;
import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * OSGi Activator for the Certificate Configuration Advanced Form.
 *
 * @author Ingo Bauersachs
 */
public class CertConfigActivator
    implements BundleActivator
{
    /**
     * Indicates if the cert configuration form should be disabled, i.e.
     * not visible to the user.
     */
    private static final String DISABLED_PROP
        = "net.java.sip.communicator.plugin.certconfig.DISABLED";

    private static BundleContext bundleContext;
    static ResourceManagementService R;

    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;
        Dictionary<String, String> properties = new Hashtable<String, String>();
        properties.put(ConfigurationForm.FORM_TYPE,
            ConfigurationForm.ADVANCED_TYPE);

        R = ServiceUtils.getService(bc, ResourceManagementService.class);

        // Checks if the cert configuration form is disabled.
        if(!getConfigService().getBoolean(DISABLED_PROP, false))
        {
            bc.registerService(ConfigurationForm.class.getName(),
                new LazyConfigurationForm(
                    CertConfigPanel.class.getName(),
                    getClass().getClassLoader(),
                    null,
                    "plugin.certconfig.TITLE",
                    2000,
                    true),
                properties
            );
        }
    }

    public void stop(BundleContext arg0) throws Exception
    {
    }

    static BundleContext getBundleContext()
    {
        return bundleContext;
    }

    /**
     * Returns a reference to a ConfigurationService implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     *
     * @return a currently valid implementation of the ConfigurationService.
     */
    public static ConfigurationService getConfigService()
    {
        return ServiceUtils.getService(bundleContext,
            ConfigurationService.class);
    }

    /**
     * Returns a reference to a CertificateService implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     *
     * @return a currently valid implementation of the CertificateService.
     */
    public static CertificateService getCertService()
    {
        return ServiceUtils.getService(bundleContext, CertificateService.class);
    }

    /**
     * Returns a reference to a UIService implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     *
     * @return a currently valid implementation of the UIService.
     */
    public static UIService getUIService()
    {
        return ServiceUtils.getService(bundleContext, UIService.class);
    }

    /**
     * Returns a reference to a CredentialsStorageService implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     *
     * @return a currently valid implementation of the
     *         CredentialsStorageService.
     */
    public static CredentialsStorageService getCredService()
    {
        return ServiceUtils.getService(bundleContext,
            CredentialsStorageService.class);
    }
}
