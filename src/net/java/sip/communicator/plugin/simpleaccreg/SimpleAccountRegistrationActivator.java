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
    private static final Logger logger
        = Logger.getLogger(SimpleAccountRegistrationActivator.class);

    public static BundleContext bundleContext;

    private static ConfigurationService configService;

    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;

        if (!hasRegisteredAccounts())
        {
            // If no preferred wizard is specified we launch the default wizard.
            InitialAccountRegistrationFrame accountRegFrame
                = new InitialAccountRegistrationFrame();

            accountRegFrame.pack();

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            accountRegFrame.setLocation(screenSize.width / 2
                - accountRegFrame.getWidth() / 2, screenSize.height / 2
                - accountRegFrame.getHeight() / 2);

            accountRegFrame.setVisible(true);
        }
        
        logger.info("SIMPLE ACCOUNT REGISTRATION ...[STARTED]");
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
                        .get(ProtocolProviderFactory.IS_PROTOCOL_HIDDEN) != null;

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
}
