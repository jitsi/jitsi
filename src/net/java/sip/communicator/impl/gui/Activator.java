package net.java.sip.communicator.impl.gui;

import java.util.Hashtable;

import net.java.sip.communicator.impl.gui.main.CommunicatorMain;
import net.java.sip.communicator.impl.gui.main.login.LoginWindow;
import net.java.sip.communicator.service.gui.UIService;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.AccountManager;
import net.java.sip.communicator.service.protocol.AccountProperties;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.ProtocolNames;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.util.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;


public class Activator implements BundleActivator
{	
	private Logger logger = Logger.getLogger(Activator.class.getName());
	
	private UIService uiService = null;

	private AccountManager icqAccountManager  = null;
	
	private CommunicatorMain communicatorMain = new CommunicatorMain();
	
	private LoginWindow	loginWindow = new LoginWindow();
	
	private AccountID icqAccountID;
	
	public void start(BundleContext bundleContext) throws Exception 
	{
		try
		{
            logger.logEntry();            
            
            //Create the ui service
            this.uiService =
                new UIServiceImpl();

            //ServiceReference clistReference
            	//= bundleContext.getServiceReference(MetaContactListService.class.getName());
           
            //MetaContactListService contactListService
            	//= (MetaContactListService)bundleContext.getService(clistReference);
            
            ServiceReference[] serRefs = null;
            String osgiFilter = "(" + AccountManager.PROTOCOL_PROPERTY_NAME
                                + "="+ProtocolNames.ICQ+")";
            
            serRefs = bundleContext.getServiceReferences(
                    AccountManager.class.getName(), osgiFilter);
                        
            icqAccountManager = (AccountManager)bundleContext.getService(serRefs[0]);
            
            Hashtable icqAccountProperties = new Hashtable();
            icqAccountProperties.put(AccountProperties.PASSWORD, "parolata");
            
            icqAccountID = icqAccountManager.installAccount(
                    bundleContext, "85450845", icqAccountProperties);
            
            osgiFilter =
                "(&("+AccountManager.PROTOCOL_PROPERTY_NAME +"="+ProtocolNames.ICQ+")"
                 +"(" + AccountManager.ACCOUNT_ID_PROPERTY_NAME
                 + "=" + icqAccountID.getAccountID() + "))";
            
            serRefs = bundleContext.getServiceReferences(
                    ProtocolProviderService.class.getName(),
                    osgiFilter);
            
            ProtocolProviderService icqProtocolProvider
            			= (ProtocolProviderService)bundleContext.getService(serRefs[0]);
                   
            logger.info("UI Service...[  STARTED ]");

            bundleContext.registerService(
                UIService.class.getName(), this.uiService, null);
                        
            logger.info("UI Service ...[REGISTERED]");
            
            //communicatorMain.setContactList(contactListService);
            
            communicatorMain.show();
            
            loginWindow.show();
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
