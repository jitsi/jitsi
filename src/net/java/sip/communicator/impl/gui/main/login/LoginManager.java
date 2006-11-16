/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.login;

import java.awt.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.account.*;
import net.java.sip.communicator.impl.gui.main.authorization.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

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
    implements ServiceListener, RegistrationStateChangeListener
{

    private Logger logger = Logger.getLogger(LoginManager.class.getName());

    private Hashtable loginWindows = new Hashtable();

    private MainFrame mainFrame;

    private boolean manuallyDisconnected = false;

    public LoginManager(MainFrame mainFrame)
    {

        this.mainFrame = mainFrame;
        this.mainFrame.setLoginManager(this);
        GuiActivator.bundleContext.addServiceListener(this);
    }

    /**
     * In the given <tt>ProtocolProviderFactory</tt> creates an account for
     * the given user and password.
     * 
     * @param providerFactory The <tt>ProtocolProviderFactory</tt> where the
     *            new account is created.
     * @param user The user identifier for this account.
     * @param passwd The password for this account.
     * 
     * @return The <tt>ProtocolProviderService</tt> of the newly created
     *         account.
     */
    public ProtocolProviderService installAccount(
        ProtocolProviderFactory providerFactory, String user, String passwd)
    {

        Hashtable accountProperties = new Hashtable();
        accountProperties.put(ProtocolProviderFactory.PASSWORD, passwd);

        AccountID accountID = providerFactory.installAccount(user,
            accountProperties);

        ServiceReference serRef = providerFactory
            .getProviderForAccount(accountID);

        ProtocolProviderService protocolProvider = (ProtocolProviderService) GuiActivator.bundleContext
            .getService(serRef);

        return protocolProvider;
    }

    /**
     * Registers the given protocol provider.
     * 
     * @param protocolProvider the ProtocolProviderService to register.
     */
    public void login(ProtocolProviderService protocolProvider)
    {

        SecurityAuthorityImpl secAuth = new SecurityAuthorityImpl(mainFrame,
            protocolProvider);

        this.mainFrame.activateAccount(protocolProvider);

        new RegisterProvider(protocolProvider, secAuth).start();
    }

    /**
     * Unregisters the given protocol provider.
     * 
     * @param protocolProvider the ProtocolProviderService to unregister
     */
    public void logoff(ProtocolProviderService protocolProvider)
    {
        new UnregisterProvider(protocolProvider).start();
    }

    /**
     * Shows login window for each registered account.
     * 
     * @param parent The parent MainFrame window.
     */
    public void runLogin(MainFrame parent)
    {

        Set set = GuiActivator.getProtocolProviderFactories().entrySet();
        Iterator iter = set.iterator();

        boolean hasRegisteredAccounts = false;

        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();

            ProtocolProviderFactory providerFactory
                = (ProtocolProviderFactory) entry.getValue();

            ArrayList accountsList = providerFactory.getRegisteredAccounts();

            AccountID accountID;
            ServiceReference serRef;
            ProtocolProviderService protocolProvider;

            for (int i = 0; i < accountsList.size(); i++) {
                hasRegisteredAccounts = true;

                accountID = (AccountID) accountsList.get(i);

                serRef = providerFactory.getProviderForAccount(accountID);

                protocolProvider = (ProtocolProviderService) GuiActivator.bundleContext
                    .getService(serRef);

                protocolProvider.addRegistrationStateChangeListener(this);

                this.mainFrame.addProtocolProvider(protocolProvider);

                PresenceStatus status = this.mainFrame
                    .getProtocolProviderLastStatus(protocolProvider);

                if (status == null
                    || status.getStatus() > PresenceStatus.ONLINE_THRESHOLD) {

                    this.login(protocolProvider);
                }
            }
        }

        if (!hasRegisteredAccounts) {
            this.showAccountRegistrationWizard();
        }
    }

    /**
     * Shows the wizard, which allows to register a new account.
     */
    private void showAccountRegistrationWizard()
    {
        AccountRegWizardContainerImpl wizard = (AccountRegWizardContainerImpl) GuiActivator
            .getUIService().getAccountRegWizardContainer();

        NoAccountFoundPage noAccountFoundPage = new NoAccountFoundPage();

        wizard.registerWizardPage(noAccountFoundPage.getIdentifier(),
            noAccountFoundPage);

        wizard.setTitle(Messages.getString("accountRegistrationWizard"));

        wizard.setLocation(
            Toolkit.getDefaultToolkit().getScreenSize().width / 2 - 250,
            Toolkit.getDefaultToolkit().getScreenSize().height / 2 - 100);

        wizard.newAccount(noAccountFoundPage.getIdentifier());

        wizard.showDialog(true);
    }

    /**
     * The method is called by a ProtocolProvider implementation whenever a
     * change in the registration state of the corresponding provider had
     * occurred.
     * 
     * @param evt ProviderStatusChangeEvent the event describing the status
     *            change.
     */
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        ProtocolProviderService protocolProvider = evt.getProvider();

        OperationSetPresence presence = mainFrame
            .getProtocolPresence(protocolProvider);

        if (evt.getNewState().equals(RegistrationState.REGISTERED)) {

            this.mainFrame.getStatusPanel().updateStatus(evt.getProvider());

            if (presence != null) {
                presence
                    .setAuthorizationHandler(
                        new AuthorizationHandlerImpl(mainFrame));
            }
        }
        else if (evt.getNewState().equals(
            RegistrationState.AUTHENTICATION_FAILED)) {

            this.mainFrame.getStatusPanel().updateStatus(evt.getProvider());

            if (evt.getReasonCode() == RegistrationStateChangeEvent
                    .REASON_RECONNECTION_RATE_LIMIT_EXCEEDED) {
                SIPCommMsgTextArea msgText = new SIPCommMsgTextArea(Messages
                    .getString("reconnectionLimitExceeded", protocolProvider
                        .getAccountID().getUserID()));

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
                    .getString("authenticationFailed",
                            protocolProvider.getAccountID().getAccountAddress()));

                JOptionPane.showMessageDialog(null, msgText, Messages
                    .getString("error"), JOptionPane.ERROR_MESSAGE);
            }
            logger.error(evt.getReason());
        }
        else if (evt.getNewState().equals(RegistrationState.CONNECTION_FAILED)) {

            this.mainFrame.getStatusPanel().updateStatus(evt.getProvider());

            SIPCommMsgTextArea msgText = new SIPCommMsgTextArea(Messages
                .getString("connectionFailedMessage",
                        protocolProvider.getAccountID().getAccountAddress()));

            JOptionPane.showMessageDialog(null, msgText, Messages
                .getString("error"), JOptionPane.ERROR_MESSAGE);

            logger.error(evt.getReason());
        }
        else if (evt.getNewState().equals(RegistrationState.EXPIRED)) {

            this.mainFrame.getStatusPanel().updateStatus(evt.getProvider());

            SIPCommMsgTextArea msgText = new SIPCommMsgTextArea(Messages
                .getString("connectionExpiredMessage", protocolProvider
                    .getProtocolName()));

            JOptionPane.showMessageDialog(null, msgText, Messages
                .getString("error"), JOptionPane.ERROR_MESSAGE);

            logger.error(evt.getReason());
        }
        else if (evt.getNewState().equals(RegistrationState.UNREGISTERED)) {

            this.mainFrame.getStatusPanel().updateStatus(evt.getProvider());

            if (!manuallyDisconnected) {
                if (evt.getReasonCode() == RegistrationStateChangeEvent
                        .REASON_MULTIPLE_LOGINS) {
                    SIPCommMsgTextArea msgText = new SIPCommMsgTextArea(
                        Messages.getString("multipleLogins", protocolProvider
                            .getAccountID().getUserID()));

                    JOptionPane.showMessageDialog(null, msgText, Messages
                        .getString("error"), JOptionPane.ERROR_MESSAGE);
                }
                else if (evt.getReasonCode() == RegistrationStateChangeEvent
                        .REASON_CLIENT_LIMIT_REACHED_FOR_IP) {
                    SIPCommMsgTextArea msgText = new SIPCommMsgTextArea(
                        Messages.getString("limitReachedForIp",
                            protocolProvider.getProtocolName()));

                    JOptionPane.showMessageDialog(null, msgText, Messages
                        .getString("error"), JOptionPane.ERROR_MESSAGE);
                }
                else {
                    SIPCommMsgTextArea msgText = new SIPCommMsgTextArea(
                        Messages.getString("unregisteredMessage",
                            protocolProvider.getProtocolName()));

                    JOptionPane.showMessageDialog(null, msgText, Messages
                        .getString("error"), JOptionPane.ERROR_MESSAGE);
                }
                logger.error(evt.getReason());
            }
        }
    }

    /**
     * Returns the MainFrame.
     * 
     * @return The MainFrame.
     */
    public MainFrame getMainFrame()
    {
        return mainFrame;
    }

    /**
     * Sets the MainFrame.
     * 
     * @param mainFrame The main frame.
     */
    public void setMainFrame(MainFrame mainFrame)
    {
        this.mainFrame = mainFrame;
    }

    /**
     * Implements the <tt>ServiceListener</tt> method. Verifies whether the
     * passed event concerns a <tt>ProtocolProviderService</tt> and adds the
     * corresponding UI controls.
     * 
     * @param event The <tt>ServiceEvent</tt> object.
     */
    public void serviceChanged(ServiceEvent event)
    {
        Object service = GuiActivator.bundleContext.getService(event
            .getServiceReference());

        // we don't care if the source service is not a protocol provider
        if (!(service instanceof ProtocolProviderService)) {
            return;
        }

        if (event.getType() == ServiceEvent.REGISTERED) {
            this.handleProviderAdded((ProtocolProviderService) service);
        }
        else if (event.getType() == ServiceEvent.UNREGISTERING) {
            this.handleProviderRemoved((ProtocolProviderService) service);
        }
    }

    /**
     * Adds all UI components (status selector box, etc) related to the given
     * protocol provider.
     * 
     * @param protocolProvider the <tt>ProtocolProviderService</tt>
     */
    private void handleProviderAdded(ProtocolProviderService protocolProvider)
    {
        protocolProvider.addRegistrationStateChangeListener(this);
        this.mainFrame.addProtocolProvider(protocolProvider);

        PresenceStatus status = this.mainFrame
            .getProtocolProviderLastStatus(protocolProvider);

        if (status == null
            || status.getStatus() > PresenceStatus.ONLINE_THRESHOLD) {
            this.login(protocolProvider);
        }
    }

    /**
     * Removes all UI components related to the given protocol provider.
     * 
     * @param protocolProvider the <tt>ProtocolProviderService</tt>
     */
    private void handleProviderRemoved(ProtocolProviderService protocolProvider)
    {
        this.mainFrame.removeProtocolProvider(protocolProvider);
    }

    public boolean isManuallyDisconnected()
    {
        return manuallyDisconnected;
    }

    public void setManuallyDisconnected(boolean manuallyDisconnected)
    {
        this.manuallyDisconnected = manuallyDisconnected;
    }

    private class RegisterProvider
        extends Thread
    {
        ProtocolProviderService protocolProvider;

        SecurityAuthority secAuth;

        RegisterProvider(ProtocolProviderService protocolProvider,
            SecurityAuthority secAuth)
        {
            this.protocolProvider = protocolProvider;
            this.secAuth = secAuth;
        }

        public void run()
        {
            try {
                protocolProvider.register(secAuth);
            }
            catch (OperationFailedException ex) {                
                logger.fatal("Unhandled exeption", ex);
            }
        }
    }
    
    private class UnregisterProvider
        extends Thread
    {
        ProtocolProviderService protocolProvider;
        
        UnregisterProvider(ProtocolProviderService protocolProvider)
        {
            this.protocolProvider = protocolProvider;
        }
    
        public void run()
        {
            try {
                protocolProvider.unregister();
            }
            catch (OperationFailedException ex) {
                logger.fatal("Unhandled exeption", ex);
            }
        }
    }
}
