/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.login;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import net.java.sip.communicator.impl.gui.GuiActivator;
import net.java.sip.communicator.impl.gui.main.Account;
import net.java.sip.communicator.impl.gui.main.MainFrame;
import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.utils.Constants;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.AccountManager;
import net.java.sip.communicator.service.protocol.AccountProperties;
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
    
    private Hashtable accountManagersMap = new Hashtable();
         
    private AccountID accountID;
    
    private Logger logger = Logger.getLogger(GuiActivator.class.getName());
    
    private String osgiFilter = ""; 
    
    private MainFrame mainFrame;
    
    public LoginManager(BundleContext bc){
        
        this.bc = bc;  
        
        ServiceReference[] serRefs = null;
        try {            
            //get all registered account managers
            serRefs = this.bc.getServiceReferences(
                    AccountManager.class.getName(), null);
            
        } catch (InvalidSyntaxException e) {
            
            logger.error("LoginManager : " + e);
        }
        
        for (int i = 0; i < serRefs.length; i ++){
            
            AccountManager accountManager = (AccountManager)
                this.bc.getService(serRefs[i]);
            
            this.accountManagersMap
                .put(serRefs[i].getProperty
                        (AccountManager.PROTOCOL_PROPERTY_NAME),
                     accountManager);
        }   
    }
    
    public void login(  AccountManager accountManager,
                        String user, 
                        String passwd){
        
        Hashtable accountProperties = new Hashtable();
        accountProperties.put(AccountProperties.PASSWORD, passwd);

        this.accountID = accountManager.installAccount(
                this.bc, user, accountProperties);
        
        ServiceReference serRef = null;
        
        serRef = accountManager
            .getProviderForAccount(this.accountID);
               
        ProtocolProviderService protocolProvider 
            = (ProtocolProviderService)this.bc.getService(serRef);    
        
        Account account 
            = new Account(  user,
                            protocolProvider);
    
        this.mainFrame.getStatusPanel().addAccount(account);
        this.mainFrame.addAccount(account);
        
        this.mainFrame.getStatusPanel()
                            .startConnecting(protocolProvider.getProtocolName());
    
        protocolProvider.addRegistrationStateChangeListener(this);
  
        protocolProvider.register(new MySecurityAuthority());
    }
    
    public void showLoginWindows(MainFrame parent){
        
        Set set = this.accountManagersMap.entrySet();
        Iterator iter = set.iterator();
        
        while(iter.hasNext()){
            Map.Entry entry = (Map.Entry)iter.next();
            
            AccountManager accountManager = (AccountManager)entry.getValue();
            String protocolName = (String)entry.getKey();
            
            showLoginWindow(parent, protocolName, accountManager);
        }   
    }
    
    public void showLoginWindow(MainFrame parent,
                                String protocolName,
                                AccountManager accoundManager){
        
        LoginWindow loginWindow = new LoginWindow(  parent,
                                                    protocolName,
                                                    accoundManager);        
        loginWindow.setLoginManager(this);
        
        loginWindow.showWindow();    
    }
    
    private class MySecurityAuthority implements SecurityAuthority {
        
    }

    public void registrationStateChanged(RegistrationStateChangeEvent evt) {
       
        if(evt.getNewState().equals(RegistrationState.REGISTERED)){
            
            Map supportedOpSets 
                = evt.getProvider().getSupportedOperationSets();
            
            this.mainFrame.addProtocolProvider(evt.getProvider());
            
            this.mainFrame.addProtocolSupportedOperationSets
                (evt.getProvider(), supportedOpSets);
        }        
        else if(evt.getNewState()
                    .equals(RegistrationState.AUTHENTICATION_FAILED)){
            
            this.mainFrame.getStatusPanel()
                .stopConnecting(evt.getProvider().getProtocolName());
            
            this.mainFrame.getStatusPanel()
                .setSelectedStatus( evt.getProvider().getProtocolName(),
                                    Constants.OFFLINE_STATUS);
            
            JOptionPane.showMessageDialog(null,
                    Messages.getString("authenticationFailed"), 
                    Messages.getString("authenticationFailed"),
                    JOptionPane.ERROR_MESSAGE);            
            
            this.showLoginWindows(this.mainFrame);
        }
        else if(evt.getNewState()
                    .equals(RegistrationState.CONNECTION_FAILED)){
            
            this.mainFrame.getStatusPanel()
                .stopConnecting(evt.getProvider().getProtocolName());
            
            this.mainFrame.getStatusPanel()
                .setSelectedStatus( evt.getProvider().getProtocolName(),
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
