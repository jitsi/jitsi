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
package net.java.sip.communicator.plugin.simpleaccreg;

import java.awt.*;
import java.util.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

import javax.swing.*;

/**
 *
 * @author Yana Stamcheva
 */
public class SimpleAccountRegistrationActivator
    extends AbstractServiceDependentActivator
    implements ServiceListener
{
    private static final Logger logger
        = Logger.getLogger(SimpleAccountRegistrationActivator.class);

    /**
     * Advanced config form class name.
     */
    private static final String advancedConfigFormClassName
        =   "net.java.sip.communicator.plugin" +
            ".advancedconfig.AdvancedConfigurationPanel";

    /**
     * Provisioning form class name.
     */
    private static final String provisioningFormClassName
        = "net.java.sip.communicator.plugin.provisioning.ProvisioningForm";

    /**
     * Indicates if the configuration wizard should be disabled, i.e.
     * not visible to the user.
     */
    private static final String DISABLED_PROP
        = "net.java.sip.communicator.plugin.simpleaccreg.DISABLED";

    /**
     * OSGi bundle context.
     */
    public static BundleContext bundleContext;

    private static ResourceManagementService resourcesService;

    private int numWizardsRegistered = 0;

    private String[] protocolNames;

    public void start(Object dependentService)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    init();
                }
            });
            return;
        }

        init();
    }

    /**
     * The dependent class. We are waiting for the ui service.
     * @return the ui service class.
     */
    @Override
    public Class<?> getDependentServiceClass()
    {
        return UIService.class;
    }

    /**
     * Sets the bundle context to use.
     * @param context a reference to the currently active bundle context.
     */
    @Override
    public void setBundleContext(BundleContext context)
    {
        bundleContext = context;
    }

    /**
     * Handles registration of a new account wizard.
     */
    public void serviceChanged(ServiceEvent event)
    {
        if (event.getType() == ServiceEvent.REGISTERED)
        {
            if (++numWizardsRegistered == protocolNames.length)
            {
                showDialog();
            }
        }
    }

    /**
     * Initialize and displays the initial registration frame.
     */
    private void init()
    {
        /*
         * Because the stored accounts may be asynchronously loaded, relying
         * only on the registered accounts isn't possible. Instead, presume the
         * stored accounts are valid and will later successfully be registered.
         *
         * And if the account registration wizard is disabled don't continue.
         */
        if (!hasStoredAccounts()
                && !getConfigService().getBoolean(DISABLED_PROP, false))
        {
            // If no preferred wizard is specified we launch the default wizard.
            String protocolOrder
                = SimpleAccountRegistrationActivator.getConfigService()
                    .getString("plugin.simpleaccreg.PROTOCOL_ORDER");
            if (protocolOrder == null)
            {
                return;
            }

            String protocolFilter = "";
            protocolNames = protocolOrder.split("\\|");
            for (int i = 0; i < protocolNames.length; i++)
            {
                if (i > 0)
                {
                    protocolFilter = "(|" + protocolFilter;
                }

                protocolFilter += "(" + ProtocolProviderFactory.PROTOCOL
                    + "=" + protocolNames[i] + ")";
                if (i > 0)
                {
                    protocolFilter += ")";
                }
            }

            try
            {
                ServiceReference[] refs = bundleContext.getAllServiceReferences(
                    AccountRegistrationWizard.class.getName(), protocolFilter);
                if (refs != null)
                {
                    numWizardsRegistered = refs.length;
                }

                // not all requested wizard are available, wait for them
                if (numWizardsRegistered < protocolNames.length)
                {
                    bundleContext.addServiceListener(this, "(&(objectclass="
                        + AccountRegistrationWizard.class.getName() + ")"
                        + protocolFilter + ")");
                }
                else
                {
                    showDialog();
                }
            }
            catch (InvalidSyntaxException e)
            {
                logger.error(e);
            }
        }

        if (logger.isInfoEnabled())
            logger.info("SIMPLE ACCOUNT REGISTRATION ...[STARTED]");
    }

    private void showDialog()
    {
        InitialAccountRegistrationFrame accountRegFrame =
            new InitialAccountRegistrationFrame();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        accountRegFrame.setLocation(screenSize.width / 2
            - accountRegFrame.getWidth() / 2, screenSize.height / 2
            - accountRegFrame.getHeight() / 2);

        accountRegFrame.setVisible(true);
    }

    public void stop(BundleContext bc) throws Exception
    {
    }

    /**
     * Returns all <tt>ProtocolProviderFactory</tt>s obtained from the bundle
     * context.
     * @return all <tt>ProtocolProviderFactory</tt>s obtained from the bundle
     * context
     */
    private static boolean hasRegisteredAccounts()
    {
        ServiceReference[] serRefs = null;

        try
        {
            //get all registered provider factories
            serRefs = bundleContext.getServiceReferences(
                    ProtocolProviderFactory.class.getName(), null);
        }
        catch (InvalidSyntaxException e)
        {
            logger.error("Unable to obtain service references. " + e);
        }

        boolean hasRegisteredAccounts = false;

        if (serRefs != null)
        {
            for (ServiceReference serRef : serRefs)
            {
                ProtocolProviderFactory providerFactory
                    = (ProtocolProviderFactory)
                        bundleContext.getService(serRef);

                for (Iterator<AccountID> registeredAccountIter
                            = providerFactory.getRegisteredAccounts()
                                    .iterator();
                        registeredAccountIter.hasNext();)
                {
                    AccountID accountID = registeredAccountIter.next();

                    if (!accountID.isHidden())
                    {
                        hasRegisteredAccounts = true;
                        break;
                    }
                }

                if (hasRegisteredAccounts)
                    break;
            }
        }

        return hasRegisteredAccounts;
    }

    private static boolean hasStoredAccounts()
    {
        ServiceReference accountManagerReference =
            bundleContext.getServiceReference(AccountManager.class.getName());
        boolean hasStoredAccounts = false;

        if (accountManagerReference != null)
        {
            AccountManager accountManager =
                (AccountManager) bundleContext
                    .getService(accountManagerReference);

            if (accountManager != null)
            {
                hasStoredAccounts =
                    accountManager.hasStoredAccounts(null, false);
            }
        }
        return hasStoredAccounts;
    }

    /**
     * Returns the <tt>MetaContactListService</tt> obtained from the bundle
     * context.
     * <p>
     * <b>Note</b>: Because this plug-in is meant to be initially displayed (if
     * necessary) and not get used afterwards, the method doesn't cache the
     * return value. Make sure you call it as little as possible if execution
     * speed is under consideration.
     * </p>
     *
     * @return the <tt>MetaContactListService</tt> obtained from the bundle
     *         context
     */
    public static MetaContactListService getContactList()
    {
        ServiceReference serviceReference =
            bundleContext.getServiceReference(MetaContactListService.class
                .getName());

        return (MetaContactListService) bundleContext
            .getService(serviceReference);
    }

    /**
     * Returns the <tt>UIService</tt> obtained from the bundle
     * context.
     * <p>
     * <b>Note</b>: Because this plug-in is meant to be initially displayed (if
     * necessary) and not get used afterwards, the method doesn't cache the
     * return value. Make sure you call it as little as possible if execution
     * speed is under consideration.
     * </p>
     *
     * @return the <tt>MetaContactListService</tt> obtained from the bundle
     *         context
     */
    public static UIService getUIService()
    {
        ServiceReference serviceReference
            = bundleContext.getServiceReference(UIService.class.getName());

        return (UIService) bundleContext
            .getService(serviceReference);
    }

    /**
     * Returns the <tt>ResourceManagementService</tt>, through which we will
     * access all resources.
     *
     * @return the <tt>ResourceManagementService</tt>, through which we will
     * access all resources.
     */
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
        {
            resourcesService
                = ServiceUtils.getService(
                        bundleContext,
                        ResourceManagementService.class);
        }
        return resourcesService;
    }

    /**
     * Returns the first available advanced configuration form.
     *
     * @return the first available advanced configuration form
     */
    public static ConfigurationForm getAdvancedConfigForm()
    {
        // General configuration forms only.
        String osgiFilter = "("
            + ConfigurationForm.FORM_TYPE
            + "="+ConfigurationForm.GENERAL_TYPE+")";

        ServiceReference[] confFormsRefs = null;
        try
        {
            confFormsRefs = bundleContext
                .getServiceReferences(
                    ConfigurationForm.class.getName(),
                    osgiFilter);
        }
        catch (InvalidSyntaxException ex)
        {}

        if(confFormsRefs != null)
        {
            for (int i = 0; i < confFormsRefs.length; i++)
            {
                ConfigurationForm form
                    = (ConfigurationForm) bundleContext
                        .getService(confFormsRefs[i]);

                if (form instanceof LazyConfigurationForm)
                {
                    LazyConfigurationForm lazyConfigForm
                        = (LazyConfigurationForm) form;

                    if (lazyConfigForm.getFormClassName().equals(
                            advancedConfigFormClassName))
                        return form;
                }
                else if (form.getClass().getName().equals(
                            advancedConfigFormClassName))
                {
                    return form;
                }
            }
        }

        return null;
    }

    /**
     * Returns the first available provisioning configuration form.
     *
     * @return the first available provisioning configuration form
     */
    public static ConfigurationForm getProvisioningConfigForm()
    {
     // General configuration forms only.
        String osgiFilter = "("
            + ConfigurationForm.FORM_TYPE
            + "="+ConfigurationForm.ADVANCED_TYPE+")";

        ServiceReference[] confFormsRefs = null;
        try
        {
            confFormsRefs = bundleContext
                .getServiceReferences(
                    ConfigurationForm.class.getName(),
                    osgiFilter);
        }
        catch (InvalidSyntaxException ex)
        {}

        if(confFormsRefs != null)
        {
            for (int i = 0; i < confFormsRefs.length; i++)
            {
                ConfigurationForm form
                    = (ConfigurationForm) bundleContext
                        .getService(confFormsRefs[i]);

                if (form instanceof LazyConfigurationForm)
                {
                    LazyConfigurationForm lazyConfigForm
                        = (LazyConfigurationForm) form;

                    if (lazyConfigForm.getFormClassName().equals(
                            provisioningFormClassName))
                    {
                        return form;
                    }
                }
            }
        }

        return null;
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
}
