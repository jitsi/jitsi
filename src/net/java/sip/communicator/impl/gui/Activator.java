package net.java.sip.communicator.impl.gui;

import javax.swing.SwingUtilities;

import net.java.sip.communicator.impl.gui.main.CommunicatorMain;
import net.java.sip.communicator.impl.gui.main.login.LoginManager;
import net.java.sip.communicator.service.contactlist.MetaContactListService;
import net.java.sip.communicator.service.gui.UIService;
import net.java.sip.communicator.util.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;


public class Activator implements BundleActivator
{
    private Logger logger = Logger.getLogger(Activator.class.getName());

    private UIService uiService = null;

    private CommunicatorMain communicatorMain = new CommunicatorMain();

    private LoginManager	loginManager;

    private BundleContext bundleContext;


    public void start(BundleContext bundleContext) throws Exception
    {
        this.bundleContext = bundleContext;

        this.loginManager = new LoginManager(bundleContext);

        this.loginManager.setMainFrame(communicatorMain.getMainFrame());
        
        try
        {
            logger.logEntry();

            //Create the ui service
            this.uiService =
                new UIServiceImpl();
            
            logger.info("UI Service...[  STARTED ]");

            bundleContext.registerService(
                    UIService.class.getName(), this.uiService, null);

            logger.info("UI Service ...[REGISTERED]");            

            communicatorMain.showCommunicator();
          
            SwingUtilities.invokeLater(new RunLogin());
       
        }
        finally
        {
            logger.logExit();
        }
    }

    public void stop(BundleContext bundleContext) throws Exception
    {
        logger.info("UI Service ...[STOPED]");
    }

    
    private class RunLogin implements Runnable {

        public void run() {
            
            loginManager.showLoginWindow(communicatorMain.getMainFrame());            
        }
    }
}
