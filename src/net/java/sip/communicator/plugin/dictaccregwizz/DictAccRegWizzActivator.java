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
package net.java.sip.communicator.plugin.dictaccregwizz;

import java.util.*;

import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Registers the <tt>DictAccountRegistrationWizard</tt> in the UI Service.
 *
 * @author ROTH Damien
 * @author LITZELMANN Cedric
 */
public class DictAccRegWizzActivator
    implements BundleActivator
{
    /**
     * OSGi bundle context.
     */
    public static BundleContext bundleContext;

    private static Logger logger = Logger.getLogger(
        DictAccRegWizzActivator.class);

    private static BrowserLauncherService browserLauncherService;

    private static WizardContainer wizardContainer;

    private static DictAccountRegistrationWizard dictWizard;

    private static UIService uiService;

    /**
     * Starts this bundle.
     *
     * @param bc The bundle context.
     */
    public void start(BundleContext bc) throws Exception {

        bundleContext = bc;

        ServiceReference uiServiceRef = bundleContext
            .getServiceReference(UIService.class.getName());

        uiService = (UIService) bundleContext.getService(uiServiceRef);

        wizardContainer = uiService.getAccountRegWizardContainer();

        dictWizard = new DictAccountRegistrationWizard(wizardContainer);

        //wizardContainer.addAccountRegistrationWizard(dictWizard);
        Hashtable<String, String> containerFilter
            = new Hashtable<String, String>();

        containerFilter.put(
                ProtocolProviderFactory.PROTOCOL,
                ProtocolNames.DICT);

        bundleContext.registerService(
            AccountRegistrationWizard.class.getName(),
            dictWizard,
            containerFilter);
    }


    /**
     * Stops this bundle.
     *
     * @param bundleContext The bundle context (unused).
     *
     * @throws Exception    Throws an execption from the
     * "wizardContainer.removeAccountRegistrationWizard" method.
     *
     */
    public void stop(BundleContext bundleContext) throws Exception
    {
        //wizardContainer.removeAccountRegistrationWizard(dictWizard);
    }

    /**
     * Returns the <tt>ProtocolProviderFactory</tt> for the Dict protocol.
     * @return the <tt>ProtocolProviderFactory</tt> for the Dict protocol
     */
    public static ProtocolProviderFactory getDictProtocolProviderFactory() {

        ServiceReference[] serRefs = null;

        String osgiFilter = "("
            + ProtocolProviderFactory.PROTOCOL
            + "="+ProtocolNames.DICT+")";

        try {
            serRefs = bundleContext.getServiceReferences(
                ProtocolProviderFactory.class.getName(), osgiFilter);
        }
        catch (InvalidSyntaxException ex){
            logger.error("DictAccRegWizzActivator : " + ex);
        }

        return (ProtocolProviderFactory) bundleContext.getService(serRefs[0]);
    }

    /**
     * Returns the <tt>BrowserLauncherService</tt> obtained from the bundle
     * context.
     * @return the <tt>BrowserLauncherService</tt> obtained from the bundle
     * context
     */
    public static BrowserLauncherService getBrowserLauncher() {
        if (browserLauncherService == null) {
            ServiceReference serviceReference = bundleContext
                .getServiceReference(BrowserLauncherService.class.getName());

            browserLauncherService = (BrowserLauncherService) bundleContext
                .getService(serviceReference);
        }

        return browserLauncherService;
    }

    /**
     * Returns the <tt>UIService</tt>.
     *
     * @return the <tt>UIService</tt>
     */
    public static UIService getUIService()
    {
        return uiService;
    }
}
