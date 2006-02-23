/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.login;

import java.util.Hashtable;
import java.util.Map;

import javax.swing.JOptionPane;

import net.java.sip.communicator.impl.gui.Activator;
import net.java.sip.communicator.impl.gui.main.MainFrame;
import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.utils.Constants;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.AccountManager;
import net.java.sip.communicator.service.protocol.AccountProperties;
import net.java.sip.communicator.service.protocol.ProtocolNames;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.RegistrationState;
import net.java.sip.communicator.service.protocol.SecurityAuthority;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeEvent;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeListener;
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
    
    private MainFrame mainFrame;
    
    public LoginManager(BundleContext bc){
        
        this.bc = bc;  
        
        this.osgiFilter = "(" + AccountManager.PROTOCOL_PROPERTY_NAME
        + "="+ProtocolNames.ICQ+")";
        
        try {            
            this.serRefs = this.bc.getServiceReferences(
                    AccountManager.class.getName(), osgiFilter);
            
        } catch (InvalidSyntaxException e) {
            
            logger.error("LoginManager : " + e.getMessage());
        }
        
        this.accountManager 
            = (AccountManager)this.bc.getService(serRefs[0]);    
        
        
    }
    
    public void login(String user, String passwd){
        
        Hashtable icqAccountProperties = new Hashtable();
        icqAccountProperties.put(AccountProperties.PASSWORD, passwd);

        this.icqAccountID = this.accountManager.installAccount(
                this.bc, user, icqAccountProperties);
        
        this.osgiFilter =
            "(&("+AccountManager.PROTOCOL_PROPERTY_NAME +"="+ProtocolNames.ICQ+")"
             +"(" + AccountManager.ACCOUNT_ID_PROPERTY_NAME
             + "=" + icqAccountID.getAccountID() + "))";

        try {
            this.serRefs = this.bc.getServiceReferences(
                        ProtocolProviderService.class.getName(),
                        osgiFilter);
        } catch (InvalidSyntaxException e) {
            
            this.logger.error("LoginManager: " + e.getMessage());
        }
        
        icqProtocolProvider 
            = (ProtocolProviderService)this.bc.getService(serRefs[0]);              
        
        icqProtocolProvider.addRegistrationStateChangeListener(this);
      
        icqProtocolProvider.register(new MySecurityAuthority());     
        
    }
    
   
    public void showLoginWindow(MainFrame parent){
        
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
            
            this.mainFrame.setProtocolProvider(icqProtocolProvider);
            
            this.mainFrame.setSupportedOperationSets(supportedOpSets);
            
        }        
        else if(evt.getNewState()
                    .equals(RegistrationState.AUTHENTICATION_FAILED)){
            
            this.mainFrame.getStatusPanel().stopConnecting(Constants.ICQ);
            
            this.mainFrame.getStatusPanel().setSelectedStatus(Constants.ICQ,
                    Constants.OFFLINE_STATUS);
            
            JOptionPane.showMessageDialog(null,
                    Messages.getString("authenticationFailed"), 
                    Messages.getString("authenticationFailed"),
                    JOptionPane.ERROR_MESSAGE);            
            
            this.showLoginWindow(this.mainFrame);
        }
        else if(evt.getNewState()
                    .equals(RegistrationState.CONNECTION_FAILED)){
            
            this.mainFrame.getStatusPanel().stopConnecting(Constants.ICQ);
            
            this.mainFrame.getStatusPanel().setSelectedStatus(Constants.ICQ,
                    Constants.OFFLINE_STATUS);
            
            JOptionPane.showMessageDialog(null,                    
                    Messages.getString("connectionFailedMessage"), 
                    Messages.getString("connectionFailed"),
                    JOptionPane.ERROR_MESSAGE);
        }
        else if(evt.getNewState()
                    .equals(RegistrationState.EXPIRED)){
            
            //TODO: Registration state changed listener: EXPIRED
        }        
    }

    public MainFrame getMainFrame() {
        return mainFrame;
    }

    public void setMainFrame(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }
}
