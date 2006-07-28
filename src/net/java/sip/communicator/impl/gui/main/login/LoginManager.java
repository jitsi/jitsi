/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.login;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import net.java.sip.communicator.impl.gui.GuiActivator;
import net.java.sip.communicator.impl.gui.customcontrols.SIPCommMsgTextArea;
import net.java.sip.communicator.impl.gui.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.MainFrame;
import net.java.sip.communicator.impl.gui.main.StatusPanel;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.AccountProperties;
import net.java.sip.communicator.service.protocol.ProtocolProviderFactory;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.RegistrationState;
import net.java.sip.communicator.service.protocol.SecurityAuthority;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeEvent;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeListener;
import net.java.sip.communicator.util.Logger;

import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * The <tt>LoginManager</tt> manages the login operation. Here we obtain the
 * <tt>ProtocolProviderFactory</tt>, we make the account installation and we
 * handle all events related to the registration state.
 * <p>
 * The <tt>LoginManager</tt> is the one that opens one or more 
 * <tt>LoginWindow</tt>s for each <tt>ProtocolProviderFactory</tt>. The
 * <tt>LoginWindow</tt> is where user could enter an identifier and password.
 * <p>
 * Note that the behaviour of this class will be changed when the Configuration
 * Service is ready.
 * 
 * @author Yana Stamcheva
 */
public class LoginManager 
    implements  ServiceListener, 
                RegistrationStateChangeListener {

    private Logger logger = Logger.getLogger(LoginManager.class.getName());
    
    private Hashtable loginWindows = new Hashtable();

    private MainFrame mainFrame;
    
    public LoginManager(MainFrame mainFrame) {
        
        this.mainFrame = mainFrame;
        this.mainFrame.setLoginManager(this);
        GuiActivator.bundleContext.addServiceListener(this);
    }
       
    /**
     * In the given <tt>ProtocolProviderFactory</tt> creates an account 
     * for the given user and password.
     * 
     * @param providerFactory The <tt>ProtocolProviderFactory</tt> where the
     * new account is created.
     * @param user The user identifier for this account.
     * @param passwd The password for this account.
     * 
     * @return The <tt>ProtocolProviderService</tt> of the newly created
     * account.
     */
    public ProtocolProviderService installAccount(
                ProtocolProviderFactory providerFactory,
                String user,
                String passwd) {

        Hashtable accountProperties = new Hashtable();
        accountProperties.put(AccountProperties.PASSWORD, passwd);
        
        AccountID accountID = providerFactory.installAccount(
                GuiActivator.bundleContext, user,
                accountProperties);
        
        ServiceReference serRef = providerFactory
                .getProviderForAccount(accountID);
        
        ProtocolProviderService protocolProvider
            = (ProtocolProviderService) GuiActivator.bundleContext
                .getService(serRef);        

        return protocolProvider;
    }
    
    /**
     * Registers the given protocol provider.
     *
     * @param protocolProvider the ProtocolProviderService to register.
     */
    public void login(ProtocolProviderService protocolProvider) {
                
        this.mainFrame.activateAccount(protocolProvider);
        
        protocolProvider.addRegistrationStateChangeListener(this);

        protocolProvider.register(new MySecurityAuthority());
    }

    /**
     * Shows login window for each registered account.
     * @param parent The parent MainFrame window.
     */
    public void runLogin(MainFrame parent) {

        Set set = GuiActivator.getProtocolProviderFactories().entrySet();
        Iterator iter = set.iterator();

        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();

            ProtocolProviderFactory providerFactory
                = (ProtocolProviderFactory) entry.getValue();
            String protocolName = (String) entry.getKey();

            ArrayList accountsList
                = providerFactory.getRegisteredAccounts();
            
            if (accountsList.size() > 0) {
                AccountID accountID;
                ServiceReference serRef;
                ProtocolProviderService protocolProvider;
                
                for (int i = 0; i < accountsList.size(); i ++) {
                    accountID = (AccountID) accountsList.get(i);
                    
                    serRef = providerFactory
                            .getProviderForAccount(accountID);
                    
                    protocolProvider
                        = (ProtocolProviderService) GuiActivator.bundleContext
                            .getService(serRef);
            
                    this.login(protocolProvider);
                }
            }
            else {
                showLoginWindow(parent, protocolName, providerFactory);
            }
            //TEST SUPPORT FOR MORE ACCOUNTS!!!!!
            //showLoginWindow(parent, protocolName, providerFactory);
        }
    }

    /**
     * Shows the login window for the given protocol and providerFactory.
     * @param parent The parent MainFrame window.
     * @param protocolName The protocolName.
     * @param accoundManager The ProtocolProviderFactory to use.
     */
    private void showLoginWindow(MainFrame parent,
                                String protocolName,
                                ProtocolProviderFactory accoundManager) {

        LoginWindow loginWindow = new LoginWindow(parent, protocolName,
                accoundManager);
        loginWindow.setLoginManager(this);

        this.loginWindows.put(protocolName, loginWindow);

        loginWindow.showWindow();
    }

    private class MySecurityAuthority implements SecurityAuthority {

    }

    /**
     * The method is called by a ProtocolProvider implementation whenever
     * a change in the registration state of the corresponding provider had
     * occurred.
     * @param evt ProviderStatusChangeEvent the event describing the status
     * change.
     */
    public void registrationStateChanged(RegistrationStateChangeEvent evt) {
        ProtocolProviderService protocolProvider = evt.getProvider();

        if (evt.getNewState().equals(RegistrationState.REGISTERED)) {
            
            this.mainFrame.addProtocolProvider(protocolProvider);
            
        } else if (evt.getNewState().equals(
                RegistrationState.AUTHENTICATION_FAILED)) {

            StatusPanel statusPanel = this.mainFrame.getStatusPanel();

            statusPanel.stopConnecting(protocolProvider);

            statusPanel.setSelectedStatus(protocolProvider,
                    Constants.OFFLINE_STATUS);

            if (evt.getReasonCode() == RegistrationStateChangeEvent
                    .REASON_RECONNECTION_RATE_LIMIT_EXCEEDED) {
                SIPCommMsgTextArea msgText
                    = new SIPCommMsgTextArea(Messages.getString(
                        "reconnectionLimitExceeded", protocolProvider
                        .getAccountID().getAccountUserID()));

                JOptionPane.showMessageDialog(null, msgText, Messages
                        .getString("error"), JOptionPane.ERROR_MESSAGE);
            }
            else if (evt.getReasonCode() == RegistrationStateChangeEvent
                    .REASON_NON_EXISTING_USER_ID) {
                SIPCommMsgTextArea msgText = new SIPCommMsgTextArea(Messages
                        .getString("nonExistingUserId", protocolProvider
                                .getProtocolName()));

                JOptionPane.showMessageDialog(null, msgText, Messages
                        .getString("error"), JOptionPane.ERROR_MESSAGE);
            }
            else if (evt.getReasonCode() == RegistrationStateChangeEvent
                    .REASON_AUTHENTICATION_FAILED) {
                SIPCommMsgTextArea msgText = new SIPCommMsgTextArea(Messages
                        .getString("authenticationFailed"));

                JOptionPane.showMessageDialog(null, msgText, Messages
                        .getString("error"), JOptionPane.ERROR_MESSAGE);
            }            
            logger.error(evt.getReason());
            
            ((LoginWindow) this.loginWindows.get(protocolProvider
                    .getProtocolName())).showWindow();
        }
        else if (evt.getNewState()
                .equals(RegistrationState.CONNECTION_FAILED)) {

            this.mainFrame.getStatusPanel().stopConnecting(
                    evt.getProvider());

            this.mainFrame.getStatusPanel().setSelectedStatus(
                    evt.getProvider(),
                    Constants.OFFLINE_STATUS);

            SIPCommMsgTextArea msgText
                = new SIPCommMsgTextArea(
                        Messages.getString("connectionFailedMessage"));

            JOptionPane.showMessageDialog(null, msgText, Messages
                    .getString("error"), JOptionPane.ERROR_MESSAGE);
            
            logger.error(evt.getReason());
        }
        else if (evt.getNewState().equals(RegistrationState.EXPIRED)) {
            SIPCommMsgTextArea msgText
                = new SIPCommMsgTextArea(Messages.getString(
                        "connectionExpiredMessage", protocolProvider
                            .getProtocolName()));

            JOptionPane.showMessageDialog(null, msgText,
                    Messages.getString("error"),
                    JOptionPane.ERROR_MESSAGE);
            
            logger.error(evt.getReason());
        }
        else if (evt.getNewState().equals(RegistrationState.UNREGISTERED)) {

            if (evt.getReasonCode() == RegistrationStateChangeEvent
                    .REASON_MULTIPLE_LOGINS) {
                SIPCommMsgTextArea msgText
                    = new SIPCommMsgTextArea(Messages.getString(
                        "multipleLogins", protocolProvider.getAccountID()
                        .getAccountUserID()));

                JOptionPane.showMessageDialog(null, msgText, Messages
                        .getString("error"), JOptionPane.ERROR_MESSAGE);
            } else if (evt.getReasonCode() == RegistrationStateChangeEvent
                    .REASON_CLIENT_LIMIT_REACHED_FOR_IP) {
                SIPCommMsgTextArea msgText
                    = new SIPCommMsgTextArea(Messages
                            .getString("limitReachedForIp", protocolProvider
                                    .getProtocolName()));

                JOptionPane.showMessageDialog(null, msgText, Messages
                        .getString("error"), JOptionPane.ERROR_MESSAGE);
            } else {
                SIPCommMsgTextArea msgText
                    = new SIPCommMsgTextArea(Messages.getString(
                            "unregisteredMessage", protocolProvider
                            .getProtocolName()));

                JOptionPane.showMessageDialog(null, msgText, Messages
                        .getString("error"), JOptionPane.ERROR_MESSAGE);
            }

            logger.error(evt.getReason());
            
            this.mainFrame.getStatusPanel().stopConnecting(
                    evt.getProvider());

            this.mainFrame.getStatusPanel().setSelectedStatus(
                    evt.getProvider(),
                    Constants.OFFLINE_STATUS);
        }
    }

    /**
     * Returns the MainFrame.
     * @return The MainFrame.
     */
    public MainFrame getMainFrame() {
        return mainFrame;
    }

    /**
     * Sets the MainFrame.
     * @param mainFrame The main frame.
     */
    public void setMainFrame(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }
    
    /**
     * Implements the <tt>ServiceListener</tt> method. Verifies whether the
     * passed event concerns a <tt>ProtocolProviderService</tt> and adds the
     * corresponding UI controls.
     *
     * @param event The <tt>ServiceEvent</tt> object.
     */
    public void serviceChanged(ServiceEvent event) {
        Object service = GuiActivator.bundleContext
            .getService(event.getServiceReference());
        
        // we don't care if the source service is not a protocol provider
        if (! (service instanceof ProtocolProviderService)) {
            return;
        }

        if (event.getType() == ServiceEvent.REGISTERED)
        {            
            this.handleProviderAdded( (ProtocolProviderService) service);
        }
        else if (event.getType() == ServiceEvent.UNREGISTERING)
        {
            this.handleProviderRemoved( (ProtocolProviderService) service);
        }
    }
    
    /**
     * Adds all UI components (status selector box, etc) related to the given
     * protocol provider.
     * 
     * @param protocolProvider the <tt>ProtocolProviderService</tt>
     */
    private void handleProviderAdded(
            ProtocolProviderService protocolProvider) {
        this.mainFrame.addAccount(protocolProvider);
        this.login(protocolProvider);
    }
    
    /**
     * Removes all UI components related to the given protocol provider.  
     * @param protocolProvider the <tt>ProtocolProviderService</tt>
     */
    private void handleProviderRemoved(
            ProtocolProviderService protocolProvider) {
        this.mainFrame.removeAccount(protocolProvider);
    }
}
