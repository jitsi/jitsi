/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.ircaccregwizz;

import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Registers the <tt>IrcAccountRegistrationWizard</tt> in the UI Service.
 *
 * @author Lionel Ferreira & Michael Tarantino
 */
public class IrcAccRegWizzActivator
    extends AbstractServiceDependentActivator
{
    private static Logger logger = Logger.getLogger(
        IrcAccRegWizzActivator.class.getName());
    
    static BundleContext bundleContext;
    
    private static UIService uiService;
    
    private static WizardContainer wizardContainer;

    private IrcAccountRegistrationWizard ircWizard;

    public void start(Object dependentService)
    {
        if (logger.isInfoEnabled())
            logger.info("Loading irc account wizard.");

        uiService = (UIService)dependentService;
        
        wizardContainer = uiService.getAccountRegWizardContainer();
        
        ircWizard = new IrcAccountRegistrationWizard(wizardContainer);
        
        Hashtable<String, String> containerFilter = new Hashtable<String, String>();
        containerFilter.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.IRC);
        
        bundleContext.registerService(AccountRegistrationWizard.class.getName(), ircWizard, containerFilter);

        if (logger.isInfoEnabled())
            logger.info("IRC account registration wizard [STARTED].");
    }
    
    public Class<?> getDependentServiceClass()
    {
        return UIService.class;
    }
    
    @Override
    public void setBundleContext(BundleContext context)
    {
        bundleContext = context;
    }
    
    public void stop(BundleContext bundleContext)
    {
    }

    /**
     * Returns the <tt>ProtocolProviderFactory</tt> for the IRC protocol.
     * @return the <tt>ProtocolProviderFactory</tt> for the IRC protocol
     */
    public static ProtocolProviderFactory getIrcProtocolProviderFactory()
    {
        ServiceReference[] serRefs = null;

        String osgiFilter = "("
            + ProtocolProviderFactory.PROTOCOL
            + "=" + "IRC" + ")";

        try
        {
            serRefs = bundleContext.getServiceReferences(
                ProtocolProviderFactory.class.getName(), osgiFilter);
        }
        catch (InvalidSyntaxException ex)
        {
            logger.error(ex);
        }

        return (ProtocolProviderFactory) bundleContext.getService(serRefs[0]);
    }
    
    public static UIService getUIService()
    {
        return uiService;
    }
}
