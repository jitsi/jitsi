/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.login;

import java.awt.Frame;
import java.util.Hashtable;
import java.util.Map;

import net.java.sip.communicator.impl.gui.Activator;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.AccountManager;
import net.java.sip.communicator.service.protocol.AccountProperties;
import net.java.sip.communicator.service.protocol.ProtocolNames;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.RegistrationState;
import net.java.sip.communicator.service.protocol.SecurityAuthority;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeEvent;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeListener;
import net.java.sip.communicator.slick.protocol.icq.IcqSlickFixture;
import net.java.sip.communicator.util.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class LoginManager implements RegistrationStateChangeListener {

    private BundleContext bc;
    
    private AccountManager accountManager;
         
    private AccountID icqAccountID;
    
    private Logger logger = Logger.getLogger(Activator.class.getName());
    
    private ServiceReference[] serRefs = null;
    
    private String osgiFilter = ""; 
    
    private ProtocolProviderService icqProtocolProvider;
    
    public LoginManager(BundleContext bc){
        
        this.bc = bc;        
        
    }
    
    public void login(String user, String passwd){
        
        this.osgiFilter = "(" + AccountManager.PROTOCOL_PROPERTY_NAME
        + "="+ProtocolNames.ICQ+")";
        
        try {            
            this.serRefs = this.bc.getServiceReferences(
                    AccountManager.class.getName(), osgiFilter);
            
        } catch (InvalidSyntaxException e) {
            
            logger.error("Login : " + e.getMessage());
        }
        
        this.accountManager 
            = (AccountManager)this.bc.getService(serRefs[0]);    
        
        Hashtable icqAccountProperties = new Hashtable();
        icqAccountProperties.put(AccountProperties.PASSWORD, "abc123");

        this.icqAccountID = this.accountManager.installAccount(
                this.bc, "227503712", icqAccountProperties);
        
        this.osgiFilter =
            "(&("+AccountManager.PROTOCOL_PROPERTY_NAME +"="+ProtocolNames.ICQ+")"
             +"(" + AccountManager.ACCOUNT_ID_PROPERTY_NAME
             + "=" + icqAccountID.getAccountID() + "))";

        try {
            this.serRefs = this.bc.getServiceReferences(
                        ProtocolProviderService.class.getName(),
                        osgiFilter);
        } catch (InvalidSyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        icqProtocolProvider 
            = (ProtocolProviderService)this.bc.getService(serRefs[0]);
              
        
        icqProtocolProvider.addRegistrationStateChangeListener(this);
      
        icqProtocolProvider.register(new MySecurityAuthority());
    }
    
   
    public void showLoginWindow(Frame parent){
        
        LoginWindow loginWindow = new LoginWindow(parent);
        
        loginWindow.setLoginManager(this);
        
        loginWindow.showWindow();        
    }
    
    private class MySecurityAuthority implements SecurityAuthority {
        
    }

    public void registrationStateChanged(RegistrationStateChangeEvent evt) {
       
        if(evt.getNewState().equals(RegistrationState.REGISTERED)){
            
            Map supportedOpSets 
                = icqProtocolProvider.getSupportedOperationSets();
            /*
            for (int i = 0; i < supportedOpSets.size(); i ++) {
                
                
            }
            */
        }
    }
}
