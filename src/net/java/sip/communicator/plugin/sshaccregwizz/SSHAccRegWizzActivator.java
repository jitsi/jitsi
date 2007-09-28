/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 *
 * SSHAccRegWizzActivator.java
 *
 * Created on 22 May, 2007, 8:48 AM
 *
 * SSH Suport in SIP Communicator - GSoC' 07 Project
 */

package net.java.sip.communicator.plugin.sshaccregwizz;

import org.osgi.framework.*;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Registers the <tt>SSHAccountRegistrationWizard</tt> in the UI Service.
 *
 * @author Shobhit Jindal
 */
public class SSHAccRegWizzActivator
    implements BundleActivator
{
    private static Logger logger = Logger.getLogger(
        SSHAccRegWizzActivator.class.getName());

    /**
     * A currently valid bundle context.
     */
    public static BundleContext bundleContext;

    /**
     * A currently valid reference to the configuration service.
     */
    private static ConfigurationService configService;
    
    private static AccountRegistrationWizardContainer wizardContainer;
    
    private static SSHAccountRegistrationWizard sshWizard;

    /**
     * Starts this bundle.
     * @param bc the currently valid <tt>BundleContext</tt>.
     */
    public void start(BundleContext bc)
    {
        logger.info("Loading ssh account wizard.");

        bundleContext = bc;

        ServiceReference uiServiceRef = bundleContext
            .getServiceReference(UIService.class.getName());

        UIService uiService = (UIService) bundleContext
                                            .getService(uiServiceRef);

        wizardContainer = uiService.getAccountRegWizardContainer();

        sshWizard
            = new SSHAccountRegistrationWizard(wizardContainer);

        wizardContainer.addAccountRegistrationWizard(sshWizard);

        logger.info("SSH account registration wizard [STARTED].");
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle.
     *
     * @param bundleContext The execution context of the bundle being stopped.
     */
    public void stop(BundleContext bundleContext) throws Exception
    {
        wizardContainer.removeAccountRegistrationWizard(sshWizard);
    }
    
    /**
     * Returns the <tt>ProtocolProviderFactory</tt> for the SSH protocol.
     * @return the <tt>ProtocolProviderFactory</tt> for the SSH protocol
     */
    public static ProtocolProviderFactory getSSHProtocolProviderFactory()
    {

        ServiceReference[] serRefs = null;

        String osgiFilter = "("
            + ProtocolProviderFactory.PROTOCOL
            + "=" + "SSH" + ")";

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

    /**
     * Returns the bundleContext that we received when we were started.
     *
     * @return a currently valid instance of a bundleContext.
     */
    public BundleContext getBundleContext()
    {
        return bundleContext;
    }
}
