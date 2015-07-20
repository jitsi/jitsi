/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.protocol.dict;

import net.java.dict4j.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.version.*;
import org.osgi.framework.*;

/**
 * A Dict implementation of the ProtocolProviderService.
 *
 * @author ROTH Damien
 * @author LITZELMANN Cedric
 */
public class ProtocolProviderServiceDictImpl
    extends AbstractProtocolProviderService
{
    private static final Logger logger
        = Logger.getLogger(ProtocolProviderServiceDictImpl.class);

    /**
     * The name of this protocol.
     */
    public static final String DICT_PROTOCOL_NAME = "Dict";

    /**
     * The id of the account that this protocol provider represents.
     */
    private DictAccountID accountID = null;

    /**
     * We use this to lock access to initialization.
     */
    private Object initializationLock = new Object();

    /**
     * Indicates whether or not the provider is initialized and ready for use.
     */
    private boolean isInitialized = false;

    /**
     * The logo corresponding to the gibberish protocol.
     */
    private ProtocolIconDictImpl dictIcon = new ProtocolIconDictImpl();

    /**
     * The registration state that we are currently in. Note that in a real
     * world protocol implementation this field won't exist and the registration
     * state would be retrieved from the protocol stack.
     */
    private RegistrationState currentRegistrationState
        = RegistrationState.UNREGISTERED;

    /**
     * the <tt>DictConnection</tt> opened by this provider
     */
    private DictConnection dictConnection;

    /**
     * The default constructor for the Dict protocol provider.
     */
    public ProtocolProviderServiceDictImpl()
    {
        if (logger.isTraceEnabled())
            logger.trace("Creating a Dict provider.");
    }

    /**
     * Initializes the service implementation, and puts it in a sate where it
     * could interoperate with other services. It is strongly recomended that
     * properties in this Map be mapped to property names as specified by
     * <tt>AccountProperties</tt>.
     *
     * @param userID the user id of the gibberish account we're currently
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
            this.accountID = (DictAccountID) accountID;

            this.dictConnection = new DictConnection(this.accountID.getHost(),
                    this.accountID.getPort());
            this.dictConnection.setClientName(getSCVersion());

            //initialize the presence operationset
            OperationSetPersistentPresenceDictImpl persistentPresence =
                new OperationSetPersistentPresenceDictImpl(this);

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
            addSupportedOperationSet(
                OperationSetBasicInstantMessaging.class,
                new OperationSetBasicInstantMessagingDictImpl(
                        this,
                        persistentPresence));

            //initialize the typing notifications operation set
            /*OperationSetTypingNotifications typingNotifications =
                new OperationSetTypingNotificationsDictImpl(
                        this, persistentPresence);

            supportedOperationSets.put(
                OperationSetTypingNotifications.class.getName(),
                typingNotifications);
            */
            isInitialized = true;
        }
    }

    /**
     * Returns the <tt>DictConnection</tt> opened by this provider
     * @return the <tt>DictConnection</tt> opened by this provider
     */
    public DictConnection getConnection()
    {
        return this.dictConnection;
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
        return DICT_PROTOCOL_NAME;
    }

    /**
     * Returns the dict protocol icon.
     * @return the dict protocol icon
     */
    public ProtocolIcon getProtocolIcon()
    {
        return this.dictIcon;
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
        // Try to connect to the server
        boolean connected = connect();

        if (connected)
        {
            fireRegistrationStateChanged(
                getRegistrationState(),
                RegistrationState.REGISTERED,
                RegistrationStateChangeEvent.REASON_USER_REQUEST,
                null);
            currentRegistrationState = RegistrationState.REGISTERED;
        }
        else
        {
            fireRegistrationStateChanged(
                getRegistrationState(),
                RegistrationState.CONNECTION_FAILED,
                RegistrationStateChangeEvent.REASON_SERVER_NOT_FOUND,
                null);
            currentRegistrationState = RegistrationState.UNREGISTERED;
        }
    }

    /**
     * Checks if the connection to the dict server is open
     * @return TRUE if the connection is open - FALSE otherwise
     */
    private boolean connect()
    {
        if (this.dictConnection.isConnected())
        {
            return true;
        }

        try
        {
            return this.dictConnection.isAvailable();
        }
        catch (DictException dx)
        {
            if (logger.isInfoEnabled())
                logger.info(dx);
        }

        return false;
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
            logger.trace("Killing the Dict Protocol Provider for account "
                    + this.accountID.getUserID());

        closeConnection();

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
        closeConnection();

        fireRegistrationStateChanged(
                getRegistrationState(),
                RegistrationState.UNREGISTERED,
                RegistrationStateChangeEvent.REASON_USER_REQUEST,
                null);
    }

    /**
     * DICT has no support for secure transport.
     */
    public boolean isSignalingTransportSecure()
    {
        return false;
    }

    /**
     * Returns the "transport" protocol of this instance used to carry the
     * control channel for the current protocol service.
     *
     * @return The "transport" protocol of this instance: TCP.
     */
    public TransportProtocol getTransportProtocol()
    {
        return TransportProtocol.TCP;
    }

    /**
     * Close the connection to the server
     */
    private void closeConnection()
    {
        try
        {
            this.dictConnection.close();
        }
        catch (DictException dx)
        {
            if (logger.isInfoEnabled())
                logger.info(dx);
        }
    }

    /**
     * Returns the current version of SIP-Communicator
     * @return the current version of SIP-Communicator
     */
    private String getSCVersion()
    {
        BundleContext bc = DictActivator.getBundleContext();
        ServiceReference vsr = bc.getServiceReference(VersionService.class.getName());

        VersionService vs = (VersionService) bc.getService(vsr);
        return vs.getCurrentVersion().toString();

    }
}
