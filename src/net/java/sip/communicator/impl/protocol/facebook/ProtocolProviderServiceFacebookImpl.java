/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.facebook;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * A Facebook implementation of the ProtocolProviderService.
 * 
 * @author Dai Zhiwei
 */
public class ProtocolProviderServiceFacebookImpl
    extends AbstractProtocolProviderService
{
    private static final Logger logger =
        Logger.getLogger(ProtocolProviderServiceFacebookImpl.class);

    /**
     * The name of this protocol.
     */
    public static final String FACEBOOK_PROTOCOL_NAME = "Facebook";

    /**
     * The id of the account that this protocol provider represents.
     */
    private AccountID accountID = null;

    /**
     * We use this to lock access to initialization.
     */
    private Object initializationLock = new Object();

    /**
     * Indicates whether or not the provider is initialized and ready for use.
     */
    private boolean isInitialized = false;

    /**
     * The logo corresponding to the facebook protocol.
     */
    private ProtocolIconFacebookImpl facebookIcon =
        new ProtocolIconFacebookImpl();

    /**
     * The registration state that we are currently in. Note that in a real
     * world protocol implementation this field won't exist and the registration
     * state would be retrieved from the protocol stack.
     */
    private RegistrationState currentRegistrationState =
        RegistrationState.UNREGISTERED;

    private FacebookAdapter facebookAdapter;

    /**
     * The default constructor for the Facebook protocol provider.
     */
    public ProtocolProviderServiceFacebookImpl()
    {
        logger.trace("Creating a facebook provider.");
    }

    /**
     * Initializes the service implementation, and puts it in a sate where it
     * could interoperate with other services. It is strongly recomended that
     * properties in this Map be mapped to property names as specified by
     * <tt>AccountProperties</tt>.
     * 
     * @param userID the user id of the facebook account we're currently
     *            initializing
     * @param accountID the identifier of the account that this protocol
     *            provider represents.
     * 
     * @see net.java.sip.communicator.service.protocol.AccountID
     */
    protected void initialize(String userID, AccountID accountID)
    {
        synchronized (initializationLock)
        {
            this.accountID = accountID;

            // initialize the presence operationset
            OperationSetPersistentPresenceFacebookImpl persistentPresence =
                new OperationSetPersistentPresenceFacebookImpl(this);

            supportedOperationSets.put(OperationSetPersistentPresence.class
                .getName(), persistentPresence);

            // register it once again for those that simply need presence and
            // won't be smart enough to check for a persistent presence
            // alternative
            supportedOperationSets.put(OperationSetPresence.class.getName(),
                persistentPresence);

            // initialize the IM operation set
            OperationSetBasicInstantMessagingFacebookImpl basicInstantMessaging =
                new OperationSetBasicInstantMessagingFacebookImpl(
                    this,
                    (OperationSetPersistentPresenceFacebookImpl) persistentPresence);

            supportedOperationSets.put(OperationSetBasicInstantMessaging.class
                .getName(), basicInstantMessaging);
            
            // initialize the message operation set
            OperationSetSmsMessagingFacebookImpl basicMessaging =
                new OperationSetSmsMessagingFacebookImpl(
                    this,
                    (OperationSetPersistentPresenceFacebookImpl) persistentPresence);

            supportedOperationSets.put(OperationSetSmsMessaging.class
                .getName(), basicMessaging);

            // initialize the typing notifications operation set
            supportedOperationSets.put(
                OperationSetTypingNotifications.class.getName(),
                new OperationSetTypingNotificationsFacebookImpl(this));

            // initialize the server stored contact info operation set
            OperationSetServerStoredContactInfo contactInfo =
                new OperationSetServerStoredContactInfoFacebookImpl(this);

            supportedOperationSets.put(
                OperationSetServerStoredContactInfo.class.getName(),
                contactInfo);
            
            facebookAdapter =
                new FacebookAdapter(ProtocolProviderServiceFacebookImpl.this);

            isInitialized = true;
        }
    }

    /**
     * Returns the AccountID that uniquely identifies the account represented by
     * this instance of the ProtocolProviderService.
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
     * @return a String containing the short name of the protocol this service
     *         is implementing (most often that would be a name in
     *         ProtocolNames).
     */
    public String getProtocolName()
    {
        return FACEBOOK_PROTOCOL_NAME;
    }

    /**
     * Returns the protocol display name. This is the name that would be used by
     * the GUI to display the protocol name.
     * 
     * @return a String containing the display name of the protocol this service
     *         is implementing
     */
    public String getProtocolDisplayName()
    {
        return FACEBOOK_PROTOCOL_NAME;
    }

    /**
     * Returns the state of the registration of this protocol provider with the
     * corresponding registration service.
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
     * @param authority the security authority that will be used for resolving
     *            any security challenges that may be returned during the
     *            registration or at any moment while wer're registered.
     * @throws OperationFailedException with the corresponding code it the
     *             registration fails for some reason (e.g. a networking error
     *             or an implementation problem).
     */
    public void register(SecurityAuthority authority)
        throws OperationFailedException
    {
        // verify whether a password has already been stored for this account
        String password =
            FacebookActivator.getProtocolProviderFactory().loadPassword(
                getAccountID());

        // if we don't - retrieve it from the user through the security
        // authority
        if (password == null)
        {
            // create a default credentials object
            UserCredentials credentials = new UserCredentials();
            credentials.setUserName(getAccountID().getUserID());

            // request a password from the user
            credentials =
                authority.obtainCredentials(ProtocolNames.FACEBOOK,
                    credentials, SecurityAuthority.AUTHENTICATION_REQUIRED);

            // extract the password the user passed us.
            char[] pass = credentials.getPassword();

            // the user didn't provide us a password (canceled the operation)
            if (pass == null)
            {
                fireRegistrationStateChanged(getRegistrationState(),
                    RegistrationState.UNREGISTERED,
                    RegistrationStateChangeEvent.REASON_USER_REQUEST, "");
                return;
            }
            password = new String(pass);

            // if the user indicated that the password should be saved, we'll
            // ask
            // the proto provider factory to store it for us.
            if (credentials.isPasswordPersistent())
            {
                FacebookActivator.getProtocolProviderFactory().storePassword(
                    getAccountID(), password);
            }
        }

        RegistrationState oldState = currentRegistrationState;

        int initErrorCode =
            facebookAdapter.initialize(getAccountID().getUserID(), password);

        if (initErrorCode == FacebookErrorCode.Error_Global_NoError)
        {
            currentRegistrationState = RegistrationState.REGISTERED;
            fireRegistrationStateChanged(oldState, currentRegistrationState,
                RegistrationStateChangeEvent.REASON_USER_REQUEST, null);
        }
        else
        {
            fireRegistrationStateChanged(getRegistrationState(),
                RegistrationState.UNREGISTERED,
                RegistrationStateChangeEvent.REASON_SERVER_NOT_FOUND, "");
        }
    }

    /**
     * Makes the service implementation close all open sockets and release any
     * resources that it might have taken and prepare for shutdown/garbage
     * collection.
     */
    public void shutdown()
    {
        if (!isInitialized)
        {
            return;
        }
        logger.trace("Killing the Facebook Protocol Provider.");

        if (isRegistered())
        {
            try
            {
                // do the unregistration
                unregister();
            }
            catch (OperationFailedException ex)
            {
                // we're shutting down so we need to silence the exception here
                logger.error(
                    "Failed to properly unregister before shutting down. "
                        + getAccountID(), ex);
            }
        }

        // shut down the http client
        this.facebookAdapter.shutdown();

        isInitialized = false;
    }

    /**
     * Ends the registration of this protocol provider with the current
     * registration service.
     * 
     * @throws OperationFailedException with the corresponding code it the
     *             registration fails for some reason (e.g. a networking error
     *             or an implementation problem).
     */
    public void unregister() throws OperationFailedException
    {
        unregister(RegistrationStateChangeEvent.REASON_USER_REQUEST);
    }
    
    public void unregister(int reason) throws OperationFailedException
    {
        // surspend/pause the httpclient
        this.facebookAdapter.pause();

        RegistrationState oldState = currentRegistrationState;
        currentRegistrationState = RegistrationState.UNREGISTERED;

        fireRegistrationStateChanged(oldState, currentRegistrationState,
            reason, null);
    }

    /**
     * Returns the facebook protocol icon.
     * 
     * @return the facebook protocol icon
     */
    public ProtocolIcon getProtocolIcon()
    {
        return facebookIcon;
    }

    /**
     * Returns the information of the contact who has the specific id. Return
     * null if there's no this contact.
     * 
     * @param contactID the id we wanna look up
     * @return the information of the contact who has the specific id
     */
    public FacebookUser getContactMetaInfoByID(String contactID)
    {
        return facebookAdapter.getBuddyFromCacheByID(contactID);
    }

    /**
     * Return the adapter of this account.
     * 
     * @return the adapter of this account
     */
    public FacebookAdapter getAdapter()
    {
        return this.facebookAdapter;
    }
}
