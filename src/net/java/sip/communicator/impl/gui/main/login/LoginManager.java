/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.login;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.authorization.*;
import net.java.sip.communicator.impl.gui.utils.Constants;
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
 * Note that the behavior of this class will be changed when the Configuration
 * Service is ready.
 *
 * @author Yana Stamcheva
 */
public class LoginManager
    implements  ServiceListener,
                RegistrationStateChangeListener,
                AccountManagerListener
{
    private final Logger logger = Logger.getLogger(LoginManager.class);

    private final MainFrame mainFrame;

    private boolean manuallyDisconnected = false;

    /**
     * Creates an instance of the <tt>LoginManager</tt>, by specifying the main
     * application window.
     *
     * @param mainFrame the main application window
     */
    public LoginManager(MainFrame mainFrame)
    {
        this.mainFrame = mainFrame;

        GuiActivator.bundleContext.addServiceListener(this);
    }

    /**
     * Registers the given protocol provider.
     *
     * @param protocolProvider the ProtocolProviderService to register.
     */
    public void login(ProtocolProviderService protocolProvider)
    {
        SecurityAuthority secAuth
            = GuiActivator.getUIService()
                .getDefaultSecurityAuthority(protocolProvider);

        mainFrame.getAccountStatusPanel().startConnecting(protocolProvider);

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
        // if someone is late registering catch it
        GuiActivator.getAccountManager().addListener(this);

        for (ProtocolProviderFactory providerFactory : GuiActivator
            .getProtocolProviderFactories().values())
        {
            addAccountsForProtocolProviderFactory(providerFactory);
        }
    }

    /**
     * Notifies that the loading of the stored accounts of a
     * specific <code>ProtocolProviderFactory</code> has finished.
     *
     * @param event the <code>AccountManagerEvent</code> describing the
     *            <code>AccountManager</code> firing the notification and the
     *            other details of the specific notification.
     */
    public void handleAccountManagerEvent(AccountManagerEvent event)
    {
        if(event.getType()
            == AccountManagerEvent.STORED_ACCOUNTS_LOADED)
        {
            addAccountsForProtocolProviderFactory(event.getFactory());
        }
    }

    /**
     * Handles stored accounts for a protocol provider factory and add them
     * to the UI and register them if needed.
     * @param providerFactory the factory to handle.
     */
    private void addAccountsForProtocolProviderFactory(
        ProtocolProviderFactory providerFactory)
    {
        ServiceReference serRef;
        ProtocolProviderService protocolProvider;

        for (AccountID accountID : providerFactory.getRegisteredAccounts())
        {
            serRef = providerFactory.getProviderForAccount(accountID);

            protocolProvider =
                (ProtocolProviderService) GuiActivator.bundleContext
                    .getService(serRef);

            // check whether we have already loaded this provider
            if(this.mainFrame.hasProtocolProvider(protocolProvider))
                continue;

            protocolProvider.addRegistrationStateChangeListener(this);

            this.mainFrame.addProtocolProvider(protocolProvider);

            Object status =
                this.mainFrame
                    .getProtocolProviderLastStatus(protocolProvider);

            if (status == null
                || status.equals(Constants.ONLINE_STATUS)
                || ((status instanceof PresenceStatus)
                    && (((PresenceStatus) status)
                    .getStatus() >= PresenceStatus.ONLINE_THRESHOLD)))
            {
                this.login(protocolProvider);
            }
        }
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
        RegistrationState newState = evt.getNewState();
        ProtocolProviderService protocolProvider = evt.getProvider();
        AccountID accountID = protocolProvider.getAccountID();

        if (logger.isTraceEnabled())
            logger.trace("Protocol provider: " + protocolProvider
            + " changed its state to: " + evt.getNewState().getStateName());

        if (newState.equals(RegistrationState.REGISTERED)
            || newState.equals(RegistrationState.UNREGISTERED)
            || newState.equals(RegistrationState.EXPIRED)
            || newState.equals(RegistrationState.AUTHENTICATION_FAILED)
            || newState.equals(RegistrationState.CONNECTION_FAILED)
            || newState.equals(RegistrationState.CHALLENGED_FOR_AUTHENTICATION)
            || newState.equals(RegistrationState.REGISTERED))
        {
            mainFrame.getAccountStatusPanel().stopConnecting(protocolProvider);
        }

        if (newState.equals(RegistrationState.REGISTERED))
        {
            OperationSetPresence presence
                = MainFrame.getProtocolPresenceOpSet(protocolProvider);

            OperationSetMultiUserChat multiUserChat =
                mainFrame.getMultiUserChatOpSet(protocolProvider);

            if (presence != null)
            {
                presence.setAuthorizationHandler(new AuthorizationHandlerImpl(
                    mainFrame));
            }

            if(multiUserChat != null)
            {
                GuiActivator.getUIService().getConferenceChatManager()
                    .getChatRoomList().synchronizeOpSetWithLocalContactList(
                        protocolProvider, multiUserChat);
            }
        }
        else if (newState.equals(RegistrationState.AUTHENTICATION_FAILED))
        {
            if (evt.getReasonCode() == RegistrationStateChangeEvent
                    .REASON_RECONNECTION_RATE_LIMIT_EXCEEDED)
            {

                String msgText = GuiActivator.getResources().getI18NString(
                    "service.gui.RECONNECTION_LIMIT_EXCEEDED", new String[]
                    { accountID.getUserID(), accountID.getService() });

                new ErrorDialog(
                    null,
                    GuiActivator.getResources()
                        .getI18NString("service.gui.ERROR"),
                    msgText).showDialog();
            }
            else if (evt.getReasonCode() == RegistrationStateChangeEvent
                                                .REASON_NON_EXISTING_USER_ID)
            {
                String msgText = GuiActivator.getResources().getI18NString(
                    "service.gui.NON_EXISTING_USER_ID",
                    new String[]
                    { protocolProvider.getProtocolDisplayName() });

                new ErrorDialog(
                    null,
                    GuiActivator.getResources()
                        .getI18NString("service.gui.ERROR"),
                    msgText).showDialog();
            }
            else if (evt.getReasonCode() == RegistrationStateChangeEvent
                                                .REASON_TLS_REQUIRED)
            {
                String msgText = GuiActivator.getResources().getI18NString(
                    "service.gui.NON_SECURE_CONNECTION",
                    new String[]
                    { accountID.getAccountAddress() });

                new ErrorDialog(
                    null,
                    GuiActivator.getResources()
                        .getI18NString("service.gui.ERROR"),
                    msgText).showDialog();
            }

            if (logger.isTraceEnabled())
                logger.trace(evt.getReason());
        }
//        CONNECTION_FAILED events are now dispatched in reconnect plugin
//        else if (newState.equals(RegistrationState.CONNECTION_FAILED))
//        {
//            String msgText = GuiActivator.getResources().getI18NString(
//                "service.gui.CONNECTION_FAILED_MSG",
//                new String[]
//                {   accountID.getUserID(),
//                    accountID.getService() });
//
//            int result = new MessageDialog(
//                null,
//                GuiActivator.getResources().getI18NString("service.gui.ERROR"),
//                msgText,
//                GuiActivator.getResources().getI18NString("service.gui.RETRY"),
//                false).showDialog();
//
//            if (result == MessageDialog.OK_RETURN_CODE)
//            {
//                this.login(protocolProvider);
//            }
//
//            logger.trace(evt.getReason());
//        }
        else if (newState.equals(RegistrationState.EXPIRED))
        {
            String msgText = GuiActivator.getResources().getI18NString(
                "service.gui.CONNECTION_EXPIRED_MSG",
                new String[]
                { protocolProvider.getProtocolDisplayName() });

            new ErrorDialog(null,
                GuiActivator.getResources().getI18NString("service.gui.ERROR"),
                msgText).showDialog();

            logger.error(evt.getReason());
        }
        else if (newState.equals(RegistrationState.UNREGISTERED))
        {
            if (!manuallyDisconnected)
            {
                if (evt.getReasonCode() == RegistrationStateChangeEvent
                                            .REASON_MULTIPLE_LOGINS)
                {
                    String msgText = GuiActivator.getResources().getI18NString(
                        "service.gui.MULTIPLE_LOGINS",
                        new String[]
                        { accountID.getUserID(), accountID.getService() });

                    new ErrorDialog(null,
                        GuiActivator.getResources()
                            .getI18NString("service.gui.ERROR"),
                        msgText).showDialog();
                }
                else if (evt.getReasonCode() == RegistrationStateChangeEvent
                                            .REASON_CLIENT_LIMIT_REACHED_FOR_IP)
                {
                    String msgText = GuiActivator.getResources().getI18NString(
                        "service.gui.LIMIT_REACHED_FOR_IP", new String[]
                        { protocolProvider.getProtocolDisplayName() });

                    new ErrorDialog(null,
                        GuiActivator.getResources()
                            .getI18NString("service.gui.ERROR"),
                        msgText).showDialog();
                }
                else if (evt.getReasonCode() == RegistrationStateChangeEvent
                                            .REASON_USER_REQUEST)
                {
                    // do nothing
                }
                else
                {
                    String msgText = GuiActivator.getResources().getI18NString(
                        "service.gui.UNREGISTERED_MESSAGE", new String[]
                        { accountID.getUserID(), accountID.getService() });

                    new ErrorDialog(null,
                        GuiActivator.getResources()
                            .getI18NString("service.gui.ERROR"),
                        msgText).showDialog();
                }
                if (logger.isTraceEnabled())
                    logger.trace(evt.getReason());
            }
        }
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
        ServiceReference serviceRef = event.getServiceReference();

        // if the event is caused by a bundle being stopped, we don't want to
        // know
        if (serviceRef.getBundle().getState() == Bundle.STOPPING)
        {
            return;
        }

        Object service = GuiActivator.bundleContext.getService(serviceRef);

        // we don't care if the source service is not a protocol provider
        if (!(service instanceof ProtocolProviderService))
        {
            return;
        }

        switch (event.getType())
        {
        case ServiceEvent.REGISTERED:
            this.handleProviderAdded((ProtocolProviderService) service);
            break;
        case ServiceEvent.UNREGISTERING:
            this.handleProviderRemoved((ProtocolProviderService) service);
            break;
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
        if (logger.isTraceEnabled())
            logger.trace("The following protocol provider was just added: "
            + protocolProvider.getAccountID().getAccountAddress());

        protocolProvider.addRegistrationStateChangeListener(this);
        this.mainFrame.addProtocolProvider(protocolProvider);

        Object status = this.mainFrame
            .getProtocolProviderLastStatus(protocolProvider);

        if (status == null
            || status.equals(Constants.ONLINE_STATUS)
            || ((status instanceof PresenceStatus) && (((PresenceStatus) status)
                .getStatus() >= PresenceStatus.ONLINE_THRESHOLD)))
        {
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

    /**
     * Registers a protocol provider in a separate thread.
     */
    private class RegisterProvider
        extends Thread
    {
        private final ProtocolProviderService protocolProvider;

        private final SecurityAuthority secAuth;

        RegisterProvider(ProtocolProviderService protocolProvider,
            SecurityAuthority secAuth)
        {
            this.protocolProvider = protocolProvider;
            this.secAuth = secAuth;
        }

        /**
         * Registers the contained protocol provider and process all possible
         * errors that may occur during the registration process.
         */
        public void run()
        {
            try
            {
                protocolProvider.register(secAuth);
            }
            catch (OperationFailedException ex)
            {
                handleOperationFailedException(ex);
            }
            catch (Throwable ex)
            {
                logger.error("Failed to register protocol provider. ", ex);

                AccountID accountID = protocolProvider.getAccountID();
                new ErrorDialog(
                    mainFrame,
                    GuiActivator.getResources()
                        .getI18NString("service.gui.ERROR"),
                    GuiActivator.getResources()
                        .getI18NString("service.gui.LOGIN_GENERAL_ERROR",
                    new String[]
                    { accountID.getUserID(),
                      accountID.getProtocolName(),
                      accountID.getService() }))
                .showDialog();
            }
        }

        private void handleOperationFailedException(OperationFailedException ex)
        {
            String errorMessage = "";

            switch (ex.getErrorCode())
            {
            case OperationFailedException.GENERAL_ERROR:
            {
                logger.error("Provider could not be registered"
                    + " due to the following general error: ", ex);

                AccountID accountID = protocolProvider.getAccountID();
                errorMessage =
                    GuiActivator.getResources().getI18NString(
                        "service.gui.LOGIN_GENERAL_ERROR",
                        new String[]
                           { accountID.getUserID(),
                             accountID.getProtocolName(),
                             accountID.getService() });

                new ErrorDialog(mainFrame,
                    GuiActivator.getResources()
                        .getI18NString("service.gui.ERROR"), errorMessage, ex)
                .showDialog();
            }
                break;
            case OperationFailedException.INTERNAL_ERROR:
            {
                logger.error("Provider could not be registered"
                    + " due to the following internal error: ", ex);

                AccountID accountID = protocolProvider.getAccountID();
                errorMessage =
                    GuiActivator.getResources().getI18NString(
                        "service.gui.LOGIN_INTERNAL_ERROR",
                        new String[]
                           { accountID.getUserID(), accountID.getService() });

                new ErrorDialog(mainFrame,
                    GuiActivator.getResources().getI18NString(
                        "service.gui.ERROR"), errorMessage, ex).showDialog();
            }
                break;
            case OperationFailedException.NETWORK_FAILURE:
            {
                if (logger.isInfoEnabled())
                {
                    logger.info("Provider could not be registered"
                            + " due to a network failure: " + ex);
                }

                AccountID accountID = protocolProvider.getAccountID();
                errorMessage = GuiActivator.getResources().getI18NString(
                    "service.gui.LOGIN_NETWORK_ERROR",
                    new String[]
                       { accountID.getUserID(), accountID.getService() });

                int result =
                    new MessageDialog(
                        null,
                        GuiActivator.getResources()
                            .getI18NString("service.gui.ERROR"),
                        errorMessage,
                        GuiActivator.getResources()
                            .getI18NString("service.gui.RETRY"), false)
                    .showDialog();

                if (result == MessageDialog.OK_RETURN_CODE)
                {
                    login(protocolProvider);
                }
            }
                break;
            case OperationFailedException.INVALID_ACCOUNT_PROPERTIES:
            {
                logger.error("Provider could not be registered"
                    + " due to an invalid account property: ", ex);

                AccountID accountID = protocolProvider.getAccountID();
                errorMessage =
                    GuiActivator.getResources().getI18NString(
                        "service.gui.LOGIN_INVALID_PROPERTIES_ERROR",
                        new String[]
                        { accountID.getUserID(), accountID.getService() });

                new ErrorDialog(mainFrame,
                    GuiActivator.getResources().getI18NString("service.gui.ERROR"),
                    errorMessage, ex).showDialog();
            }
                break;
            default:
                logger.error("Provider could not be registered.", ex);
            }
        }
    }

    /**
     * Unregisters a protocol provider in a separate thread.
     */
    private class UnregisterProvider
        extends Thread
    {
        ProtocolProviderService protocolProvider;

        UnregisterProvider(ProtocolProviderService protocolProvider)
        {
            this.protocolProvider = protocolProvider;
        }

        /**
         * Unregisters the contained protocol provider and process all possible
         * errors that may occur during the un-registration process.
         */
        public void run()
        {
            try
            {
                protocolProvider.unregister();
            }
            catch (OperationFailedException ex)
            {
                int errorCode = ex.getErrorCode();

                if (errorCode == OperationFailedException.GENERAL_ERROR)
                {
                    logger.error("Provider could not be unregistered"
                        + " due to the following general error: " + ex);
                }
                else if (errorCode == OperationFailedException.INTERNAL_ERROR)
                {
                    logger.error("Provider could not be unregistered"
                        + " due to the following internal error: " + ex);
                }
                else if (errorCode == OperationFailedException.NETWORK_FAILURE)
                {
                    logger.error("Provider could not be unregistered"
                        + " due to a network failure: " + ex);
                }

                new ErrorDialog(mainFrame,
                    GuiActivator.getResources()
                        .getI18NString("service.gui.ERROR"),
                    GuiActivator.getResources()
                        .getI18NString("service.gui.LOGOFF_NOT_SUCCEEDED",
                        new String[]
                        { protocolProvider.getAccountID().getUserID(),
                            protocolProvider.getAccountID().getService() }))
                    .showDialog();
            }
        }
    }
}
