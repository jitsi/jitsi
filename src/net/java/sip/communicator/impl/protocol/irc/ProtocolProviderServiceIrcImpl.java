/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * An IRC implementation of the ProtocolProviderService.
 *
 * @author Loic Kempf
 * @author Stephane Remy
 */
public class ProtocolProviderServiceIrcImpl
    extends AbstractProtocolProviderService
{
    private static final Logger logger
        = Logger.getLogger(ProtocolProviderServiceIrcImpl.class);
    
    /**
     * The irc server.
     */
    private IrcStack ircStack;
    
    /**
     * The id of the account that this protocol provider represents.
     */
    private AccountID accountID = null;

    /**
     * We use this to lock access to initialization.
     */
    private final Object initializationLock = new Object();

    /**
     * The operation set managing multi user chat.
     */
    private OperationSetMultiUserChatIrcImpl multiUserChat;

    /**
     * Indicates whether or not the provider is initialized and ready for use.
     */
    private boolean isInitialized = false;
    
    /**
     * The icon corresponding to the irc protocol.
     */
    private final ProtocolIconIrcImpl ircIcon = new ProtocolIconIrcImpl();
    
    /**
     * Keeps our current registration state.
     */
    private RegistrationState currentRegistrationState
        = RegistrationState.UNREGISTERED;

    /**
     * The default constructor for the IRC protocol provider.
     */
    public ProtocolProviderServiceIrcImpl()
    {
        if (logger.isTraceEnabled())
            logger.trace("Creating a irc provider.");
    }

    /**
     * Initializes the service implementation, and puts it in a sate where it
     * could operate with other services. It is strongly recommended that
     * properties in this Map be mapped to property names as specified by
     * <tt>AccountProperties</tt>.
     *
     * @param userID the user id of the IRC account we're currently
     * initializing
     * @param accountID the identifier of the account that this protocol
     * provider represents.
     *
     * @see net.java.sip.communicator.service.protocol.AccountID
     */
    protected void initialize(String userID, AccountID accountID)
    {
        synchronized(initializationLock)
        {
            this.accountID = accountID;

            //Initialize the multi user chat support
            multiUserChat = new OperationSetMultiUserChatIrcImpl(this);

            addSupportedOperationSet(
                OperationSetMultiUserChat.class,
                multiUserChat);

            userID = getAccountID().getUserID();

            ircStack
                = new IrcStack(
                        this,
                        userID,
                        userID,
                        "SIP Communicator 1.0",
                        "");

            isInitialized = true;
        }
    }

    /**
     * Returns the AccountID that uniquely identifies the account represented
     * by this instance of the ProtocolProviderService.
     *
     * @return the id of the account represented by this provider.
     */
    public AccountID getAccountID()
    {
        return accountID;
    }

    /**
     * Returns the short name of the protocol that the implementation of this
     * provider is based upon (like SIP, Jabber, ICQ/AIM, or others for
     * example).
     *
     * @return a String containing the short name of the protocol this
     *   service is implementing (most often that would be a name in
     *   ProtocolNames).
     */
    public String getProtocolName()
    {
        return ProtocolNames.IRC;
    }

    /**
     * Returns the state of the registration of this protocol provider with
     * the corresponding registration service.
     *
     * @return ProviderRegistrationState
     */
    public RegistrationState getRegistrationState()
    {
        return currentRegistrationState;
    }

    /**
     * Starts the registration process.
     *
     * @param authority the security authority that will be used for
     *   resolving any security challenges that may be returned during the
     *   registration or at any moment while wer're registered.
     * @throws OperationFailedException with the corresponding code it the
     *   registration fails for some reason (e.g. a networking error or an
     *   implementation problem).
     */
    public void register(SecurityAuthority authority)
        throws OperationFailedException
    {
        AccountID accountID = getAccountID();
        String serverAddress
            = accountID
                .getAccountPropertyString(
                    ProtocolProviderFactory.SERVER_ADDRESS);
        int serverPort
            = accountID
                .getAccountPropertyInt(
                    ProtocolProviderFactory.SERVER_PORT,
                    6667);
        //Verify whether a password has already been stored for this account
        String serverPassword = IrcActivator.
            getProtocolProviderFactory().loadPassword(getAccountID());
        boolean autoNickChange =
            accountID.getAccountPropertyBoolean(
                ProtocolProviderFactory.AUTO_CHANGE_USER_NAME, true);
        boolean passwordRequired =
            accountID.getAccountPropertyBoolean(
                ProtocolProviderFactory.NO_PASSWORD_REQUIRED, true);
            
        //if we don't - retrieve it from the user through the security authority
        if (serverPassword == null && passwordRequired)
        {
            //create a default credentials object
            UserCredentials credentials = new UserCredentials();
            credentials.setUserName(getAccountID().getUserID());

            //request a password from the user
            credentials
                = authority.obtainCredentials(
                    ProtocolNames.IRC,
                    credentials,
                    SecurityAuthority.AUTHENTICATION_REQUIRED);

            //extract the password the user passed us.
            char[] pass = credentials.getPassword();

            // the user didn't provide us a password (canceled the operation)
            if (pass == null)
            {
                fireRegistrationStateChanged(
                    getRegistrationState(),
                    RegistrationState.UNREGISTERED,
                    RegistrationStateChangeEvent.REASON_USER_REQUEST, "");
                return;
            }
            serverPassword = new String(pass);

            //if the user indicated that the password should be saved, we'll ask
            //the proto provider factory to store it for us.
            if (credentials.isPasswordPersistent())
            {
                IrcActivator.getProtocolProviderFactory()
                    .storePassword(getAccountID(), serverPassword);
            }
        }

        this.ircStack.connect(  serverAddress,
                                serverPort,
                                serverPassword,
                                autoNickChange);
    }

    /**
     * Makes the service implementation close all open sockets and release
     * any resources that it might have taken and prepare for
     * shutdown/garbage collection.
     */
    public void shutdown()
    {
        if(!isInitialized)
        {
            return;
        }
        if (logger.isTraceEnabled())
            logger.trace("Killing the Irc Protocol Provider.");

        if(isRegistered())
        {
            try
            {
                //do the un-registration
                synchronized(this.initializationLock)
                {
                    unregister();
                    this.ircStack.dispose();
                    ircStack = null;
                }
            }
            catch (OperationFailedException ex)
            {
                //we're shutting down so we need to silence the exception here
                logger.error(
                    "Failed to properly unregister before shutting down. "
                    + getAccountID()
                    , ex);
            }
        }

        isInitialized = false;
    }

    /**
     * Ends the registration of this protocol provider with the current
     * registration service.
     *
     * @throws OperationFailedException with the corresponding code it the
     *   registration fails for some reason (e.g. a networking error or an
     *   implementation problem).
     */
    public void unregister()
        throws OperationFailedException
    {
        for (ChatRoom joinedChatRoom
                : multiUserChat.getCurrentlyJoinedChatRooms())
            joinedChatRoom.leave();

        if (ircStack.isConnected())
            ircStack.disconnect();
    }

    /**
     * Returns the icon for this protocol.
     * 
     * @return the icon for this protocol
     */
    public ProtocolIcon getProtocolIcon()
    {
        return ircIcon;
    }

    /**
     * Returns the IRC stack implementation.
     * 
     * @return the IRC stack implementation.
     */
    public IrcStack getIrcStack()
    {
        return ircStack;
    }

    /**
     * Returns the current registration state of this protocol provider.
     * 
     * @return the current registration state of this protocol provider
     */
    protected RegistrationState getCurrentRegistrationState()
    {
        return currentRegistrationState;
    }

    /**
     * Sets the current registration state of this protocol provider.
     * 
     * @param regState the new registration state to set
     */
    protected void setCurrentRegistrationState(
        RegistrationState regState)
    {
        RegistrationState oldState = this.currentRegistrationState;

        this.currentRegistrationState = regState;

        fireRegistrationStateChanged(
            oldState,
            this.currentRegistrationState,
            RegistrationStateChangeEvent.REASON_USER_REQUEST,
            null);
    }
}
