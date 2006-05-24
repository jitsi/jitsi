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

import net.java.sip.communicator.impl.gui.main.MainFrame;
import net.java.sip.communicator.impl.gui.main.StatusPanel;
import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.utils.Constants;
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

/**
 * The LoginManager manages the login operation.
 * @author Yana Stamcheva
 */
public class LoginManager implements RegistrationStateChangeListener {

    private BundleContext bc;

    private Hashtable accountManagersMap = new Hashtable();

    private Hashtable loginWindows = new Hashtable();

    private AccountID accountID;

    private Logger logger = Logger.getLogger(LoginManager.class.getName());

    private MainFrame mainFrame;

    public LoginManager(BundleContext bc) {

        this.bc = bc;

        ServiceReference[] serRefs = null;
        try {
            //get all registered account managers
            serRefs = this.bc.getServiceReferences(AccountManager.class
                    .getName(), null);

        } catch (InvalidSyntaxException e) {

            logger.error("LoginManager : " + e);
        }

        for (int i = 0; i < serRefs.length; i++) {

            AccountManager accountManager = (AccountManager) this.bc
                    .getService(serRefs[i]);

            this.accountManagersMap.put(serRefs[i]
                    .getProperty(AccountManager.PROTOCOL_PROPERTY_NAME),
                    accountManager);
        }
    }

    /**
     * Implements the login. Installs the account and registers
     * the appropriate protocol provider.
     * 
     * @param accountManager The AccountManager where the account 
     * should be installed.
     * @param user The user identifier.
     * @param passwd The password.
     */
    public void login(AccountManager accountManager, 
                        String user, 
                        String passwd) {

        Hashtable accountProperties = new Hashtable();
        accountProperties.put(AccountProperties.PASSWORD, passwd);

        this.accountID = accountManager.installAccount(this.bc, user,
                accountProperties);

        ServiceReference serRef = accountManager
                .getProviderForAccount(this.accountID);

        ProtocolProviderService protocolProvider 
            = (ProtocolProviderService) this.bc.getService(serRef);

        this.mainFrame.addAccount(user, protocolProvider);

        protocolProvider.addRegistrationStateChangeListener(this);

        protocolProvider.register(new MySecurityAuthority());
    }

    /**
     * Shows login window for each registered account.
     * @param parent The parent MainFrame window.
     */
    public void showLoginWindows(MainFrame parent) {

        Set set = this.accountManagersMap.entrySet();
        Iterator iter = set.iterator();

        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();

            AccountManager accountManager = (AccountManager) entry.getValue();
            String protocolName = (String) entry.getKey();

            showLoginWindow(parent, protocolName, accountManager);
        }
    }

    /**
     * Shows the login window for the given protocol and accountManager.
     * @param parent The parent MainFrame window.
     * @param protocolName The protocolName.
     * @param accoundManager The AccountManager.
     */
    private void showLoginWindow(MainFrame parent, 
                                String protocolName,
                                AccountManager accoundManager) {

        LoginWindow loginWindow = new LoginWindow(parent, protocolName,
                accoundManager);
        loginWindow.setLoginManager(this);

        this.loginWindows.put(protocolName, loginWindow);

        loginWindow.showWindow();
    }

    private class MySecurityAuthority implements SecurityAuthority {

    }

    /**
     * The method is called by a ProtocolProvider implementation whenver
     * a change in the registration state of the corresponding provider had
     * occurred.
     * @param evt ProviderStatusChangeEvent the event describing the status
     * change.
     */
    public void registrationStateChanged(RegistrationStateChangeEvent evt) {
        ProtocolProviderService protocolProvider = evt.getProvider();

        if (evt.getNewState().equals(RegistrationState.REGISTERED)) {

            Map supportedOpSets = protocolProvider.getSupportedOperationSets();

            this.mainFrame.addProtocolProvider(protocolProvider);

            this.mainFrame.addProtocolSupportedOperationSets(protocolProvider,
                    supportedOpSets);
        } else if (evt.getNewState().equals(
                RegistrationState.AUTHENTICATION_FAILED)) {

            StatusPanel statusPanel = this.mainFrame.getStatusPanel();

            statusPanel.stopConnecting(protocolProvider.getProtocolName());

            statusPanel.setSelectedStatus(protocolProvider.getProtocolName(),
                    Constants.OFFLINE_STATUS);

            if (evt.getReasonCode() == RegistrationStateChangeEvent
                    .REASON_RECONNECTION_RATE_LIMIT_EXCEEDED) {
                JOptionPane.showMessageDialog(null, Messages.getString(
                        "reconnectionLimitExceeded", protocolProvider
                                .getAccountID().getAccountUserID()), Messages
                        .getString("error"), JOptionPane.ERROR_MESSAGE);
            } else if (evt.getReasonCode() == RegistrationStateChangeEvent
                    .REASON_NON_EXISTING_USER_ID) {
                JOptionPane.showMessageDialog(null, Messages
                        .getString("nonExistingUserId", protocolProvider
                                .getProtocolName()), Messages
                        .getString("error"), JOptionPane.ERROR_MESSAGE);
            } else if (evt.getReasonCode() == RegistrationStateChangeEvent
                    .REASON_AUTHENTICATION_FAILED) {
                JOptionPane.showMessageDialog(null, Messages
                        .getString("authenticationFailed"), Messages
                        .getString("error"), JOptionPane.ERROR_MESSAGE);
            }

            ((LoginWindow) this.loginWindows.get(protocolProvider
                    .getProtocolName())).showWindow();
        } else if (evt.getNewState()
                .equals(RegistrationState.CONNECTION_FAILED)) {

            this.mainFrame.getStatusPanel().stopConnecting(
                    evt.getProvider().getProtocolName());

            this.mainFrame.getStatusPanel().setSelectedStatus(
                    evt.getProvider().getProtocolName(),
                    Constants.OFFLINE_STATUS);

            JOptionPane.showMessageDialog(null, Messages
                    .getString("connectionFailedMessage"), Messages
                    .getString("error"), JOptionPane.ERROR_MESSAGE);
        } else if (evt.getNewState().equals(RegistrationState.EXPIRED)) {
            JOptionPane.showMessageDialog(null, Messages.getString(
                    "connectionExpiredMessage", protocolProvider
                            .getProtocolName()), Messages.getString("error"),
                    JOptionPane.ERROR_MESSAGE);
        } else if (evt.getNewState().equals(RegistrationState.UNREGISTERED)) {

            if (evt.getReasonCode() == RegistrationStateChangeEvent
                    .REASON_MULTIPLE_LOGINS) {
                JOptionPane.showMessageDialog(null, Messages.getString(
                        "multipleLogins", protocolProvider.getAccountID()
                                .getAccountUserID()), Messages
                        .getString("error"), JOptionPane.ERROR_MESSAGE);
            } else if (evt.getReasonCode() == RegistrationStateChangeEvent
                    .REASON_CLIENT_LIMIT_REACHED_FOR_IP) {
                JOptionPane.showMessageDialog(null, Messages
                        .getString("limitReachedForIp", protocolProvider
                                .getProtocolName()), Messages
                        .getString("error"), JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, Messages.getString(
                        "unregisteredMessage", protocolProvider
                                .getProtocolName()), Messages
                        .getString("error"), JOptionPane.ERROR_MESSAGE);
            }

            this.mainFrame.getStatusPanel().stopConnecting(
                    evt.getProvider().getProtocolName());

            this.mainFrame.getStatusPanel().setSelectedStatus(
                    evt.getProvider().getProtocolName(),
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
}
