package net.java.sip.communicator.impl.gui;

import java.util.Hashtable;

import net.java.sip.communicator.impl.gui.main.CommunicatorMain;
import net.java.sip.communicator.service.configuration.ConfigurationService;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.contactlist.MetaContactListException;
import net.java.sip.communicator.service.contactlist.MetaContactListService;
import net.java.sip.communicator.service.contactlist.event.MetaContactListListener;
import net.java.sip.communicator.service.gui.UIService;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.AccountManager;
import net.java.sip.communicator.service.protocol.AccountProperties;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.ProtocolNames;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.slick.protocol.icq.IcqSlickFixture;
import net.java.sip.communicator.util.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;


public class Activator implements BundleActivator
{	
	private Logger logger = Logger.getLogger(Activator.class.getName());
	
	private UIService uiService = null;

	private AccountManager icqAccountManager  = null;
	
	private CommunicatorMain communicatorMain = new CommunicatorMain();
	
	private AccountID icqAccountID;
	
	public void start(BundleContext bundleContext) throws Exception 
	{
		try
		{
            logger.logEntry();            
            
            //Create the ui service
            this.uiService =
                new UIServiceImpl();

            ServiceReference clistReference
            	= bundleContext.getServiceReference(MetaContactListService.class.getName());
           
            //MetaContactListService contactListService
            	//= (MetaContactListService)bundleContext.getService(clistReference);
            
            MetaContactListService contactListService = new MetaContactListServiceImpl(); 
            
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
            
            Object icqProtocolProvider
            			= bundleContext.getService(serRefs[0]);
            
            logger.info("UI Service...[  STARTED ]");

            bundleContext.registerService(
                UIService.class.getName(), this.uiService, null);
                        
            logger.info("UI Service ...[REGISTERED]");
            
            communicatorMain.setContactList(contactListService);
            
            communicatorMain.show();
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
	
	private class MetaContactListServiceImpl 
		implements MetaContactListService {

		public MetaContactGroup getRoot() {
			// TODO Auto-generated method stub
			return null;
		}

		public MetaContact findMetaContactByContact(Contact contact) {
			// TODO Auto-generated method stub
			return null;
		}

		public MetaContact findMetaContactByID(String metaContactID) {
			// TODO Auto-generated method stub
			return null;
		}

		public void addContactListListener(MetaContactListListener l) {
			// TODO Auto-generated method stub
			
		}

		public void removeContactListListener(MetaContactListListener l) {
			// TODO Auto-generated method stub
			
		}

		public void moveContact(Contact contact, MetaContact newParent) throws MetaContactListException {
			// TODO Auto-generated method stub
			
		}

		public void removeContact(Contact contact) throws MetaContactListException {
			// TODO Auto-generated method stub
			
		}

		public void addNewContactToMetaContact(ProtocolProviderService provider, MetaContact metaContact, String contactID) throws MetaContactListException {
			// TODO Auto-generated method stub
			
		}

		public void createMetaContact(ProtocolProviderService provider, MetaContactGroup contactGroup, String contactID) throws MetaContactListException {
			// TODO Auto-generated method stub
			
		}

		public void moveMetaContact(MetaContact metaContact, MetaContactGroup newGroup) throws MetaContactListException {
			// TODO Auto-generated method stub
			
		}

		public void removeMetaContact(MetaContact metaContact) throws MetaContactListException {
			// TODO Auto-generated method stub
			
		}

		public void createMetaContactGroup(String groupName) throws MetaContactListException {
			// TODO Auto-generated method stub
			
		}

		public void removeMetaContactGroup(MetaContactGroup groupToRemove) throws MetaContactListException {
			// TODO Auto-generated method stub
			
		}
		
	}

}
