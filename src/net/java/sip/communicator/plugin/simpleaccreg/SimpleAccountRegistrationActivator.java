/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.simpleaccreg;

import java.awt.*;
import java.util.*;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

public class SimpleAccountRegistrationActivator
    implements BundleActivator
{
    private static Logger logger
        = Logger.getLogger(SimpleAccountRegistrationActivator.class);

    public static BundleContext bundleContext;

    private static ConfigurationService configService;
    
    private static MetaContactListService contactListService;

    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;

        if (!hasRegisteredAccounts())
        {
            // If no preferred wizard is specified we launch the default wizard.
            InitialAccountRegistrationFrame accountRegFrame
                = new InitialAccountRegistrationFrame();

            accountRegFrame.pack();
            accountRegFrame.setLocation(
                Toolkit.getDefaultToolkit().getScreenSize().width/2
                - accountRegFrame.getWidth()/2,
            Toolkit.getDefaultToolkit().getScreenSize().height/2
                - accountRegFrame.getHeight()/2
            );

            accountRegFrame.setVisible(true);
        }
    }

    public void stop(BundleContext bc) throws Exception
    {
    }

    /**
     * Returns the <tt>ConfigurationService</tt> obtained from the bundle
     * context.
     * @return the <tt>ConfigurationService</tt> obtained from the bundle
     * context
     */
    public static ConfigurationService getConfigurationService() {
        if(configService == null) {
            ServiceReference configReference = bundleContext
                .getServiceReference(ConfigurationService.class.getName());

            configService = (ConfigurationService) bundleContext
                .getService(configReference);
        }

        return configService;
    }

    /**
     * Returns all <tt>ProtocolProviderFactory</tt>s obtained from the bundle
     * context.
     * @return all <tt>ProtocolProviderFactory</tt>s obtained from the bundle
     * context
     */
    private static boolean hasRegisteredAccounts()
    {
        boolean hasRegisteredAccounts = false;

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

        for (int i = 0; i < serRefs.length; i++)
        {
            ProtocolProviderFactory providerFactory
                = (ProtocolProviderFactory) bundleContext
                    .getService(serRefs[i]);

            ArrayList accountsList = providerFactory.getRegisteredAccounts();

            AccountID accountID;
            ServiceReference serRef;
            ProtocolProviderService protocolProvider;

            for (int j = 0; j < accountsList.size(); j++)
            {
                accountID = (AccountID) accountsList.get(j);

                boolean isHidden = 
                    accountID.getAccountProperties()
                        .get("HIDDEN_PROTOCOL") != null;

                if(!isHidden)
                {
                    hasRegisteredAccounts = true;
                    break;
                }
            }

            if (hasRegisteredAccounts)
                break;
        }

        return hasRegisteredAccounts;
    }
    
    /**
     * Returns the <tt>MetaContactListService</tt> obtained from the bundle
     * context.
     * @return the <tt>MetaContactListService</tt> obtained from the bundle
     * context
     */
    public static MetaContactListService getContactList() 
    {
        if (contactListService == null) {
            ServiceReference serviceReference = bundleContext
                .getServiceReference(MetaContactListService.class.getName());

            contactListService = (MetaContactListService) bundleContext
                .getService(serviceReference);
        }

        return contactListService;
    }
}
