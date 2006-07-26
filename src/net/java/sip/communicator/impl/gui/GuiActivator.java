/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui;

import java.util.Hashtable;
import java.util.Map;

import net.java.sip.communicator.impl.gui.main.CommunicatorMain;
import net.java.sip.communicator.impl.gui.main.MainFrame;
import net.java.sip.communicator.impl.gui.main.WelcomeWindow;
import net.java.sip.communicator.impl.gui.main.login.LoginManager;
import net.java.sip.communicator.service.configuration.ConfigurationService;
import net.java.sip.communicator.service.contactlist.MetaContactListService;
import net.java.sip.communicator.service.gui.UIService;
import net.java.sip.communicator.service.protocol.ProtocolProviderFactory;
import net.java.sip.communicator.util.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * The GUI Activator class.
 * 
 * @author Yana Stamcheva
 */
public class GuiActivator implements BundleActivator {
    
    private static Logger logger = Logger.getLogger(GuiActivator.class.getName());

    private static UIService uiService = null;

    private CommunicatorMain communicatorMain = new CommunicatorMain();

    private LoginManager loginManager;

    public static BundleContext bundleContext;
    
    private static ConfigurationService configService;
    
    private static Map providerFactoriesMap = new Hashtable();
    
    /**
     * Called when this bundle is started.
     *
     * @param context The execution context of the bundle being started.
     * @throws Exception If
     */
    public void start(BundleContext bundleContext) throws Exception {

        GuiActivator.bundleContext = bundleContext;
        
        MainFrame mainFrame = communicatorMain.getMainFrame();
        
        this.loginManager = new LoginManager();
        
        this.loginManager.setMainFrame(mainFrame);

        try {
            ServiceReference clistReference = bundleContext
                .getServiceReference(MetaContactListService.class.getName());

            MetaContactListService contactListService 
                = (MetaContactListService) bundleContext
                    .getService(clistReference);

            mainFrame.setContactList(contactListService);
            
            logger.logEntry();

            //Create the ui service
            this.uiService = new UIServiceImpl(mainFrame);

            logger.info("UI Service...[  STARTED ]");

            bundleContext.registerService(UIService.class.getName(),
                    this.uiService, null);

            logger.info("UI Service ...[REGISTERED]");

            /*
             * TO BE UNCOMMENTED when the welcome window is removed.
             * this.uiService.setVisible(true);
             * SwingUtilities.invokeLater(new RunLogin()); 
             */

            WelcomeWindow welcomeWindow = new WelcomeWindow(communicatorMain,
                    loginManager, bundleContext);

            welcomeWindow.showWindow();
        } finally {
            logger.logExit();
        }
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle.
     *
     * @param context The execution context of the bundle being stopped.
     * @throws Exception If this method throws an exception, the bundle is
     *   still marked as stopped, and the Framework will remove the bundle's
     *   listeners, unregister all services registered by the bundle, and
     *   release all services used by the bundle.
     */
    public void stop(BundleContext bundleContext) throws Exception {
        logger.info("UI Service ...[STOPPED]");
    }

    /**
     * The <tt>RunLogin</tt> implements the Runnable interface and is used to
     * shows the login windows in new thread.
     */
    private class RunLogin implements Runnable {
        public void run() {
            loginManager.runLogin(communicatorMain.getMainFrame());
        }
    }
    
    public static ConfigurationService getConfigurationService() {
        if(configService == null) {
            ServiceReference configReference = bundleContext
                .getServiceReference(ConfigurationService.class.getName());

            configService = (ConfigurationService) bundleContext
                .getService(configReference);
        }
        
        return configService;
    }
    
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
                    ProtocolProviderFactory.PROTOCOL_PROPERTY_NAME),
                    providerFactory);
        }
        
        return providerFactoriesMap;
    }
    
    public static UIService getUIService() {
        return uiService;
    }
}
