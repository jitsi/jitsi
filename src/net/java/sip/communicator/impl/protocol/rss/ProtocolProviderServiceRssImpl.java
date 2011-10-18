/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.rss;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * A Rss implementation of the ProtocolProviderService.
 *
 * @author Jean-Albert Vescovo
 * @author Emil Ivov
 */
public class ProtocolProviderServiceRssImpl
    extends AbstractProtocolProviderService
{
    private static final Logger logger
        = Logger.getLogger(ProtocolProviderServiceRssImpl.class);

    /**
     * The name of this protocol.
     */
    public static final String RSS_PROTOCOL_NAME = "RSS";

    /**
     * The id of the account that this protocol provider represents.
     */
    private AccountID accountID = null;

    /**
     * We use this to lock access to initialization.
     */
    private final Object initializationLock = new Object();

    /**
     * Indicates whether or not the provider is initialized and ready for use.
     */
    private boolean isInitialized = false;

    /**
     * The logo corresponding to the rss protocol.
     */
    private final ProtocolIconRssImpl rssIcon = new ProtocolIconRssImpl();

    /**
     * A reference to the IM operation set
     */
    private OperationSetBasicInstantMessagingRssImpl basicInstantMessaging;

    /**
     * The registration state that we are currently in. Note that in a real
     * world protocol implementation this field won't exist and the registration
     * state would be retrieved from the protocol stack.
     */
    private RegistrationState currentRegistrationState
        = RegistrationState.UNREGISTERED;

    /**
     * The default constructor for the Rss protocol provider.
     */
    public ProtocolProviderServiceRssImpl()
    {
        if (logger.isTraceEnabled())
            logger.trace("Creating a rss provider.");
    }

    /**
     * Initializes the service implementation, and puts it in a state where it
     * could interoperate with other services. It is strongly recomended that
     * properties in this Map be mapped to property names as specified by
     * <tt>AccountProperties</tt>.
     *
     * @param userID the user id of the rss account we're currently
     * initializing
     * @param accountID the identifier of the account that this protocol
     * provider represents.
     *
     * @see net.java.sip.communicator.service.protocol.AccountID
     */
    protected void initialize(String userID,
                              AccountID accountID)
    {
        synchronized(initializationLock)
        {
            this.accountID = accountID;

            //initialize the presence operationset
            OperationSetPersistentPresenceRssImpl persistentPresence =
                new OperationSetPersistentPresenceRssImpl(this);

            addSupportedOperationSet(
                OperationSetPersistentPresence.class,
                persistentPresence);
            //register it once again for those that simply need presence and
            //won't be smart enough to check for a persistent presence
            //alternative
            addSupportedOperationSet(
                OperationSetPresence.class,
                persistentPresence);

            //initialize the IM operation set
            //OperationSetBasicInstantMessagingRssImpl
            basicInstantMessaging
                = new OperationSetBasicInstantMessagingRssImpl(
                        this,
                        persistentPresence);
            addSupportedOperationSet(
                OperationSetBasicInstantMessaging.class,
                basicInstantMessaging);

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
        return RSS_PROTOCOL_NAME;
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
        RegistrationState oldState = currentRegistrationState;
        currentRegistrationState = RegistrationState.REGISTERED;

        fireRegistrationStateChanged(
            oldState
            , currentRegistrationState
            , RegistrationStateChangeEvent.REASON_USER_REQUEST
            , null);
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
            logger.trace("Killing the Rss Protocol Provider.");

        if(isRegistered())
        {
            try
            {
                //do the unregistration
                unregister();
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
        RegistrationState oldState = currentRegistrationState;
        currentRegistrationState = RegistrationState.UNREGISTERED;

        fireRegistrationStateChanged(
            oldState
            , currentRegistrationState
            , RegistrationStateChangeEvent.REASON_USER_REQUEST
            , null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.java.sip.communicator.service.protocol.ProtocolProviderService#
     * isSignallingTransportSecure()
     */
    public boolean isSignalingTransportSecure()
    {
        return false;
    }

    /**
     * Returns the rss protocol icon.
     * @return the rss protocol icon
     */
    public ProtocolIcon getProtocolIcon()
    {
        return rssIcon;
    }

    /**
     * Returns the IM set
     * @return the IM set
     */
    public OperationSetBasicInstantMessagingRssImpl getBasicInstantMessaging()
    {
        return this.basicInstantMessaging;
    }
    
    /**
     * Returns a reference to the instant messaging operation set that we are 
     * currently using for this provider. 
     * @return a reference to the instant messaging operation set that we are 
     * currently using for this provider. 
     */
    public OperationSetBasicInstantMessagingRssImpl getOperationSetBasicIM()
    {
        return (OperationSetBasicInstantMessagingRssImpl)
            getOperationSet(OperationSetBasicInstantMessaging.class);
    }
    
    /**
     * Returns a reference to the presence operation set that we are currently
     * using for this provider. 
     * @return a reference to the presence operation set that we are currently
     * using for this provider. 
     */
    public OperationSetPersistentPresenceRssImpl getOperationSetPresence()
    {
        return (OperationSetPersistentPresenceRssImpl)
            getOperationSet(OperationSetPersistentPresence.class);
    }
}
