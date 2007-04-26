/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui;

import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.login.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.audionotifier.*;
import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.service.callhistory.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.msghistory.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The GUI Activator class.
 *
 * @author Yana Stamcheva
 */
public class GuiActivator implements BundleActivator {

    private static Logger logger = Logger.getLogger(GuiActivator.class.getName());

    private static UIServiceImpl uiService = null;
    
    public static BundleContext bundleContext;

    private static ConfigurationService configService;
    
    private static MessageHistoryService msgHistoryService;
    
    private static MetaContactListService metaCListService;
    
    private static CallHistoryService callHistoryService;
    
    private static AudioNotifierService audioNotifierService;

    private static BrowserLauncherService browserLauncherService;
    
    private static Map providerFactoriesMap = new Hashtable();

    /**
     * Called when this bundle is started.
     *
     * @param bundleContext The execution context of the bundle being started.
     */
    public void start(BundleContext bundleContext) throws Exception {

        GuiActivator.bundleContext = bundleContext;
        
        ConfigurationManager.loadGuiConfigurations();
       
        try {
            // Create the ui service
            this.uiService = new UIServiceImpl();

            logger.info("UI Service...[  STARTED ]");

            bundleContext.registerService(UIService.class.getName(),
                    this.uiService, null);
            
            logger.info("UI Service ...[REGISTERED]");

            this.uiService.loadApplicationGui();
            
            logger.logEntry();
        }
        finally {
            logger.logExit();
        }
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle.
     *
     * @param bundleContext The execution context of the bundle being stopped.
     * @throws Exception If this method throws an exception, the bundle is
     *   still marked as stopped, and the Framework will remove the bundle's
     *   listeners, unregister all services registered by the bundle, and
     *   release all services used by the bundle.
     */
    public void stop(BundleContext bundleContext) throws Exception {
        logger.info("UI Service ...[STOPPED]");
    }

    /**
     * Returns all <tt>ProtocolProviderFactory</tt>s obtained from the bundle
     * context.
     * @return all <tt>ProtocolProviderFactory</tt>s obtained from the bundle
     * context
     */
    public static Map getProtocolProviderFactories() {

        ServiceReference[] serRefs = null;
        try {
            //get all registered provider factories
            serRefs = bundleContext.getServiceReferences(
                    ProtocolProviderFactory.class.getName(), null);

        } catch (InvalidSyntaxException e) {
            logger.error("LoginManager : " + e);
        }

        for (int i = 0; i < serRefs.length; i++) {

            ProtocolProviderFactory providerFactory
                = (ProtocolProviderFactory) bundleContext
                    .getService(serRefs[i]);

            providerFactoriesMap.put(serRefs[i].getProperty(
                    ProtocolProviderFactory.PROTOCOL),
                    providerFactory);
        }

        return providerFactoriesMap;
    }

    /**
     * Returns a <tt>ProtocolProviderFactory</tt> for a given protocol
     * provider.
     * @return a <tt>ProtocolProviderFactory</tt> for a given protocol
     * provider
     */
    public static ProtocolProviderFactory getProtocolProviderFactory(
            ProtocolProviderService protocolProvider) {

        ServiceReference[] serRefs = null;

        String osgiFilter = "("
            + ProtocolProviderFactory.PROTOCOL
            + "="+protocolProvider.getProtocolName()+")";

        try {
            serRefs = GuiActivator.bundleContext.getServiceReferences(
                ProtocolProviderFactory.class.getName(), osgiFilter);
        }
        catch (InvalidSyntaxException ex){
            logger.error("GuiActivator : " + ex);
        }

        return (ProtocolProviderFactory) GuiActivator
            .bundleContext.getService(serRefs[0]);
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
     * Returns the <tt>MessageHistoryService</tt> obtained from the bundle
     * context.
     * @return the <tt>MessageHistoryService</tt> obtained from the bundle
     * context
     */
    public static MessageHistoryService getMsgHistoryService() {
        if (msgHistoryService == null) {
            ServiceReference serviceReference = bundleContext
                .getServiceReference(MessageHistoryService.class.getName());

            msgHistoryService = (MessageHistoryService) bundleContext
                .getService(serviceReference);
        }

        return msgHistoryService;
    }
    
    /**
     * Returns the <tt>MetaContactListService</tt> obtained from the bundle
     * context.
     * @return the <tt>MetaContactListService</tt> obtained from the bundle
     * context
     */
    public static MetaContactListService getMetaContactListService() {
        if (metaCListService == null) {
            ServiceReference clistReference = bundleContext
                .getServiceReference(MetaContactListService.class.getName());
    
            metaCListService = (MetaContactListService) bundleContext
                    .getService(clistReference);
        }

        return metaCListService;
    }
    
    /**
     * Returns the <tt>CallHistoryService</tt> obtained from the bundle
     * context.
     * @return the <tt>CallHistoryService</tt> obtained from the bundle
     * context
     */
    public static CallHistoryService getCallHistoryService() {
        if (callHistoryService == null) {
            ServiceReference serviceReference = bundleContext
                .getServiceReference(CallHistoryService.class.getName());

            callHistoryService = (CallHistoryService) bundleContext
                .getService(serviceReference);
        }

        return callHistoryService;
    }
    
    /**
     * Returns the <tt>AudioNotifierService</tt> obtained from the bundle
     * context.
     * @return the <tt>AudioNotifierService</tt> obtained from the bundle
     * context
     */
    public static AudioNotifierService getAudioNotifier() {
        if (audioNotifierService == null) {
            ServiceReference serviceReference = bundleContext
                .getServiceReference(AudioNotifierService.class.getName());

            audioNotifierService = (AudioNotifierService) bundleContext
                .getService(serviceReference);
        }

        return audioNotifierService;
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
     * Returns the current implementation of the <tt>UIService</tt>.
     * @return the current implementation of the <tt>UIService</tt>
     */
    public static UIServiceImpl getUIService() {
        return uiService;
    }
}
