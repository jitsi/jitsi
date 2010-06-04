/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.simpleaccreg;

import java.awt.*;
import java.util.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

public class SimpleAccountRegistrationActivator
    implements BundleActivator
{
    private static final Logger logger
        = Logger.getLogger(SimpleAccountRegistrationActivator.class);

    public static BundleContext bundleContext;

    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;

        /*
         * Because the stored accounts may be asynchronously loaded, relying
         * only on the registered accounts isn't possible. Instead, presume the
         * stored accounts are valid and will later successfully be registered.
         */
        if (!hasStoredAccounts())
        {
            // If no preferred wizard is specified we launch the default wizard.
            InitialAccountRegistrationFrame accountRegFrame =
                new InitialAccountRegistrationFrame();

            accountRegFrame.pack();

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            accountRegFrame.setLocation(screenSize.width / 2
                - accountRegFrame.getWidth() / 2, screenSize.height / 2
                - accountRegFrame.getHeight() / 2);

            accountRegFrame.setVisible(true);
        }
        
        if (logger.isInfoEnabled())
            logger.info("SIMPLE ACCOUNT REGISTRATION ...[STARTED]");
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

        for (int serRefIndex = 0; serRefIndex < serRefs.length; serRefIndex++)
        {
            ProtocolProviderFactory providerFactory =
                (ProtocolProviderFactory) bundleContext
                    .getService(serRefs[serRefIndex]);

            for (Iterator<AccountID> registeredAccountIter =
                providerFactory.getRegisteredAccounts().iterator();
                registeredAccountIter.hasNext();)
            {
                AccountID accountID = registeredAccountIter.next();
                boolean isHidden = accountID.getAccountProperty(
                    ProtocolProviderFactory.IS_PROTOCOL_HIDDEN) != null;

                if (!isHidden)
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
}
