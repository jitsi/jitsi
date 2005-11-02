package net.java.sip.communicator.impl.gui;

import net.java.sip.communicator.service.gui.UIService;
import net.java.sip.communicator.util.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;


public class Activator implements BundleActivator
{

	private Logger logger = Logger.getLogger(Activator.class.getName());
	
	private UIService uiService = null;
	
	public void start(BundleContext bundleContext) throws Exception 
	{
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

}
