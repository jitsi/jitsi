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
package net.java.sip.communicator.impl.protocol.sip;

import gov.nist.javax.sip.address.*;
import gov.nist.javax.sip.header.*;
import gov.nist.javax.sip.message.*;

import java.net.*;
import java.text.*;
import java.util.*;

import javax.sip.*;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;

import net.java.sip.communicator.impl.protocol.sip.net.*;
import net.java.sip.communicator.impl.protocol.sip.security.*;
import net.java.sip.communicator.service.dns.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.Logger;

import org.jitsi.service.version.Version;
import org.jitsi.util.*;
import org.osgi.framework.*;

/**
 * A SIP implementation of the Protocol Provider Service.
 *
 * @author Emil Ivov
 * @author Lubomir Marinov
 * @author Alan Kelly
 * @author Grigorii Balutsel
 */
public class ProtocolProviderServiceSipImpl
  extends AbstractProtocolProviderService
  implements SipListener,
             RegistrationStateChangeListener
{
    /**
     * Our class logger.
     */
    private static final Logger logger =
        Logger.getLogger(ProtocolProviderServiceSipImpl.class);

    /**
     * The identifier of the account that this provider represents.
     */
    private AccountID accountID = null;

    /**
     * We use this to lock access to initialization.
     */
    private final Object initializationLock = new Object();

    /**
     * indicates whether or not the provider is initialized and ready for use.
     */
    private boolean isInitialized = false;

    /**
     * A list of all events registered for this provider.
     */
    private final List<String> registeredEvents = new ArrayList<String>();

    /**
     * The AddressFactory used to create URLs ans Address objects.
     */
    private AddressFactoryEx addressFactory;

    /**
     * The HeaderFactory used to create SIP message headers.
     */
    private HeaderFactory headerFactory;

    /**
     * The Message Factory used to create SIP messages.
     */
    private SipMessageFactory messageFactory;

    /**
      * The class in charge of event dispatching and managing common JAIN-SIP
      * resources
      */
    private static SipStackSharing sipStackSharing = null;

    /**
     * A table mapping SIP methods to method processors (every processor must
     * implement the SipListener interface). Whenever a new message arrives we
     * extract its method and hand it to the processor instance registered
     */
    private final Hashtable<String, List<MethodProcessor>> methodProcessors =
        new Hashtable<String, List<MethodProcessor>>();

    /**
     * The name of the property under which the user may specify a transport
     * to use for destinations whose preferred transport is unknown.
     */
    private static final String DEFAULT_TRANSPORT
        = "net.java.sip.communicator.impl.protocol.sip.DEFAULT_TRANSPORT";

    /**
     * The name of the property under which the user may specify if the desktop
     * streaming or sharing should be disabled.
     */
    private static final String IS_DESKTOP_STREAMING_DISABLED
        = "net.java.sip.communicator.impl.protocol.sip.DESKTOP_STREAMING_DISABLED";

    /**
     * The name of the property under which the user may specify if audio/video
     * calls should be disabled.
     */
    private static final String IS_CALLING_DISABLED
        = "net.java.sip.communicator.impl.protocol.sip.CALLING_DISABLED";

    /**
     * The name of the property under which the user may specify if
     * chat messaging should be disabled.
     */
    private static final String IS_MESSAGING_DISABLED
        = "net.java.sip.communicator.impl.protocol.sip.MESSAGING_DISABLED";

    /**
     * The name of the property which, if enabled, will cause a session-level
     * attribute for the session direction (e.g. a=sendonly, a=recvonly) to be
     * added to SDP offers/answers which we send.
     */
    public static final String USE_SESSION_LEVEL_DIRECTION_IN_SDP
            = "net.java.sip.communicator.impl.protocol.sip."
                    + "USE_SESSION_LEVEL_DIRECTION_IN_SDP";

    /**
     * Default number of times that our requests can be forwarded.
     */
    private static final int  MAX_FORWARDS = 70;

    /**
     * The default maxForwards header that we use in our requests.
     */
    private MaxForwardsHeader maxForwardsHeader = null;

    /**
     * The header that we use to identify ourselves.
     */
    private UserAgentHeader userAgentHeader = null;

    /**
     * The name that we want to send others when calling or chatting with them.
     */
    private String ourDisplayName = null;

    /**
     * Our current connection with the registrar.
     */
    private SipRegistrarConnection sipRegistrarConnection = null;

    /**
     * The SipSecurityManager instance that would be taking care of our
     * authentications.
     */
    private SipSecurityManager sipSecurityManager = null;

    /**
     * Address resolver for the outbound proxy connection.
     */
    private ProxyConnection connection;

    /**
     * The logo corresponding to the jabber protocol.
     */
    private ProtocolIconSipImpl protocolIcon;

    /**
     * The presence status set supported by this provider
     */
    private SipStatusEnum sipStatusEnum;

    /**
     * A list of early processors that can do early processing of received
     * messages (requests or responses).
     */
    private final List<SipMessageProcessor> earlyProcessors =
        new ArrayList<SipMessageProcessor>();

    /**
     * Whether we has enabled FORCE_PROXY_BYPASS for current account.
     */
    private boolean forceLooseRouting = false;

    /**
     * <tt>ClientCapabilities</tt> used by this instance.
     */
    private ClientCapabilities capabilities;

    /**
     * <tt>OperationSetPresenceSipImpl</tt> used by this instance.
     */
    private OperationSetPresenceSipImpl opSetPersPresence;

    /**
     * <tt>OperationSetBasicInstantMessagingSipImpl</tt> used by this instance.
     */
    private OperationSetBasicInstantMessagingSipImpl opSetBasicIM;

    /**
     * <tt>OperationSetMessageWaitingSipImpl</tt> used by this instance.
     */
    private OperationSetMessageWaitingSipImpl opSetMWI;

    /**
     * Used instance of <tt>OperationSetServerStoredAccountInfoSipImpl</tt>.
     */
    private OperationSetServerStoredAccountInfoSipImpl opSetSSAccountInfo;

    /**
     * <tt>OperationSetTypingNotificationsSipImpl</tt> used by this instance.
     */
    private OperationSetTypingNotificationsSipImpl opSetTypingNotif;

    /**
     * Returns the AccountID that uniquely identifies the account represented by
     * this instance of the ProtocolProviderService.
     * @return the id of the account represented by this provider.
     */
    public AccountID getAccountID()
    {
        return accountID;
    }

    /**
     * Returns the state of the registration of this protocol provider with the
     * corresponding registration service.
     * @return ProviderRegistrationState
     */
    public RegistrationState getRegistrationState()
    {
        if(this.sipRegistrarConnection == null )
        {
            return RegistrationState.UNREGISTERED;
        }
        return sipRegistrarConnection.getRegistrationState();
    }

    /**
     * Returns the short name of the protocol that the implementation of this
     * provider is based upon (like SIP, Jabber, ICQ/AIM,  or others for
     * example). If the name of the protocol has been enumerated in
     * ProtocolNames then the value returned by this method must be the same as
     * the one in ProtocolNames.
     * @return a String containing the short name of the protocol this service
     * is implementing (most often that would be a name in ProtocolNames).
     */
    public String getProtocolName()
    {
        return ProtocolNames.SIP;
    }

    /**
     * Register a new event taken in account by this provider. This is usefull
     * to generate the Allow-Events header of the OPTIONS responses and to
     * generate 489 responses.
     *
     * @param event The event to register
     */
    public void registerEvent(String event)
    {
        synchronized (this.registeredEvents)
        {
            if (!this.registeredEvents.contains(event))
            {
                this.registeredEvents.add(event);
            }
        }
    }

    /**
     * Returns the list of all the registered events for this provider.
     *
     * @return The list of all the registered events
     */
    public List<String> getKnownEventsList()
    {
        return this.registeredEvents;
    }

    /**
     * Starts the registration process. Connection details such as
     * registration server, user name/number are provided through the
     * configuration service through implementation specific properties.
     *
     * @param authority the security authority that will be used for resolving
     *        any security challenges that may be returned during the
     *        registration or at any moment while wer're registered.
     *
     * @throws OperationFailedException with the corresponding code it the
     * registration fails for some reason (e.g. a networking error or an
     * implementation problem).
     */
    public void register(SecurityAuthority authority)
        throws OperationFailedException
    {
        if(!isInitialized)
        {
            throw new OperationFailedException(
                "Provided must be initialized before being able to register."
                , OperationFailedException.GENERAL_ERROR);
        }

        if (isRegistered())
        {
            return;
        }

        /**
         * Evaluate whether FORCE_PROXY_BYPASS is enabled for the account
         * before registering.
         */
        forceLooseRouting
                = getAccountID().getAccountPropertyBoolean(
                        ProtocolProviderFactory.FORCE_PROXY_BYPASS, false);

        sipStackSharing.addSipListener(this);
        // be warned when we will unregister, so that we can
        // then remove us as SipListener
        this.addRegistrationStateChangeListener(this);

        // Enable the user name modification. Setting this property to true
        // we'll allow the user to change the user name stored in the given
        //authority.
        authority.setUserNameEditable(true);

        //init the security manager before doing the actual registration to
        //avoid being asked for credentials before being ready to provide them
        sipSecurityManager.setSecurityAuthority(authority);

        initRegistrarConnection();

        //connect to the Registrar.
        connection = ProxyConnection.create(this);
        if(!registerUsingNextAddress())
        {
            logger.error("No address found for " + this);
            fireRegistrationStateChanged(
                RegistrationState.REGISTERING,
                RegistrationState.CONNECTION_FAILED,
                RegistrationStateChangeEvent.REASON_SERVER_NOT_FOUND,
                "Invalid or inaccessible server address.");
        }
    }

    /**
     * Ends the registration of this protocol provider with the current
     * registration service.
     *
     * @throws OperationFailedException with the corresponding code it the
     * registration fails for some reason (e.g. a networking error or an
     * implementation problem).
     */
    public void unregister()
        throws OperationFailedException
    {
        unregister(false);
    }

    /**
     * Ends the registration of this protocol provider with the current
     * registration service.
     *
     * @throws OperationFailedException with the corresponding code it the
     * registration fails for some reason (e.g. a networking error or an
     * implementation problem).
     */
    public void unregister(boolean userRequest)
        throws OperationFailedException
    {
        if(getRegistrationState().equals(RegistrationState.UNREGISTERED)
            || getRegistrationState().equals(RegistrationState.UNREGISTERING)
            || getRegistrationState().equals(RegistrationState.CONNECTION_FAILED))
        {
            return;
        }

        sipRegistrarConnection.unregister(userRequest);
        sipSecurityManager.setSecurityAuthority(null);
    }

    /**
     * Initializes the service implementation, and puts it in a state where it
     * could interoperate with other services.
     *
     * @param sipAddress the account id/uin/screenname of the account that we're
     * about to create
     * @param accountID the identifier of the account that this protocol
     * provider represents.
     *
     * @throws OperationFailedException with code INTERNAL_ERROR if we fail
     * initializing the SIP Stack.
     * @throws java.lang.IllegalArgumentException if one or more of the account
     * properties have invalid values.
     *
     * @see net.java.sip.communicator.service.protocol.AccountID
     */
    protected void initialize(String    sipAddress,
                              SipAccountIDImpl accountID)
        throws OperationFailedException, IllegalArgumentException
    {
        synchronized (initializationLock)
        {

            this.accountID = accountID;

            String protocolIconPath =
                accountID
                    .getAccountPropertyString(ProtocolProviderFactory.PROTOCOL_ICON_PATH);

            if (protocolIconPath == null)
                protocolIconPath = "resources/images/protocol/sip";

            this.protocolIcon = new ProtocolIconSipImpl(protocolIconPath);

            this.sipStatusEnum = new SipStatusEnum(protocolIconPath);

            if(sipStackSharing == null)
                sipStackSharing = new SipStackSharing();

            // get the presence options
            boolean enablePresence =
                accountID.getAccountPropertyBoolean(
                    ProtocolProviderFactory.IS_PRESENCE_ENABLED, true);

            boolean forceP2P = accountID.getAccountPropertyBoolean(
                            ProtocolProviderFactory.FORCE_P2P_MODE, true);

            int pollingValue =
                accountID.getAccountPropertyInt(
                    ProtocolProviderFactory.POLLING_PERIOD, 30);

            int subscriptionExpiration =
                accountID.getAccountPropertyInt(
                    ProtocolProviderFactory.SUBSCRIPTION_EXPIRATION, 3600);

            //create SIP factories.
            headerFactory = new HeaderFactoryImpl();
            addressFactory = new AddressFactoryImpl();

            //initialize our display name
            ourDisplayName = accountID.getAccountPropertyString(
                                    ProtocolProviderFactory.DISPLAY_NAME);

            if(ourDisplayName == null
               || ourDisplayName.trim().length() == 0)
            {
                ourDisplayName = accountID.getUserID();
            }

            //init our call processor
            OperationSetBasicTelephonySipImpl opSetBasicTelephonySipImpl
                = new OperationSetBasicTelephonySipImpl(this);

            boolean isCallingDisabled
                = SipActivator.getConfigurationService()
                    .getBoolean(IS_CALLING_DISABLED, false);

            boolean isCallingDisabledForAccount
                = accountID.getAccountPropertyBoolean(
                    ProtocolProviderFactory.IS_CALLING_DISABLED_FOR_ACCOUNT,
                    false);

            if (!isCallingDisabled && !isCallingDisabledForAccount)
            {
                addSupportedOperationSet(
                    OperationSetBasicTelephony.class,
                    opSetBasicTelephonySipImpl);

                addSupportedOperationSet(
                    OperationSetAdvancedTelephony.class,
                    opSetBasicTelephonySipImpl);

                OperationSetAutoAnswerSipImpl autoAnswerOpSet
                    = new OperationSetAutoAnswerSipImpl(this);
                addSupportedOperationSet(
                    OperationSetBasicAutoAnswer.class, autoAnswerOpSet);
                addSupportedOperationSet(
                    OperationSetAdvancedAutoAnswer.class, autoAnswerOpSet);

                // init call security
                addSupportedOperationSet(
                    OperationSetSecureZrtpTelephony.class,
                    opSetBasicTelephonySipImpl);
                addSupportedOperationSet(
                    OperationSetSecureSDesTelephony.class,
                    opSetBasicTelephonySipImpl);

                // OperationSetVideoTelephony
                addSupportedOperationSet(
                    OperationSetVideoTelephony.class,
                    new OperationSetVideoTelephonySipImpl(
                            opSetBasicTelephonySipImpl));

                addSupportedOperationSet(
                    OperationSetTelephonyConferencing.class,
                    new OperationSetTelephonyConferencingSipImpl(this));

                // init DTMF (from JM Heitz)
                OperationSetDTMFSipImpl operationSetDTMFSip
                    = new OperationSetDTMFSipImpl(this);
                addSupportedOperationSet(
                    OperationSetDTMF.class, operationSetDTMFSip);

                addSupportedOperationSet(
                    OperationSetIncomingDTMF.class,
                    new OperationSetIncomingDTMFSipImpl(
                            this, operationSetDTMFSip));

                boolean isDesktopStreamingDisabled
                    = SipActivator.getConfigurationService()
                        .getBoolean(IS_DESKTOP_STREAMING_DISABLED, false);

                boolean isAccountDesktopStreamingDisabled
                    = accountID.getAccountPropertyBoolean(
                        ProtocolProviderFactory.IS_DESKTOP_STREAMING_DISABLED,
                        false);

                if (!isDesktopStreamingDisabled
                    && !isAccountDesktopStreamingDisabled)
                {
                    // OperationSetDesktopStreaming
                    addSupportedOperationSet(
                        OperationSetDesktopStreaming.class,
                        new OperationSetDesktopStreamingSipImpl(
                                opSetBasicTelephonySipImpl));

                    if(!accountID.getAccountPropertyBoolean(
                        ProtocolProviderFactory
                            .IS_DESKTOP_REMOTE_CONTROL_DISABLED,
                        false))
                    {
                        // OperationSetDesktopSharingServer
                        addSupportedOperationSet(
                           OperationSetDesktopSharingServer.class,
                           new OperationSetDesktopSharingServerSipImpl(
                                   opSetBasicTelephonySipImpl));

                        // OperationSetDesktopSharingClient
                        addSupportedOperationSet(
                            OperationSetDesktopSharingClient.class,
                            new OperationSetDesktopSharingClientSipImpl(this));
                    }
                }

                // Jitsi Meet Tools
                addSupportedOperationSet(
                    OperationSetJitsiMeetTools.class,
                    new OperationSetJitsiMeetToolsSipImpl());

                boolean isParkingEnabled
                    = accountID.getAccountPropertyBoolean(
                        OperationSetTelephonyPark.IS_CALL_PARK_ENABLED,
                        false);
                if(isParkingEnabled)
                {
                    addSupportedOperationSet(
                        OperationSetTelephonyPark.class,
                        new OperationSetTelephonyParkSipImpl(this));
                }
            }

            if (enablePresence)
            {
                //init presence op set.
                this.opSetPersPresence
                    = new OperationSetPresenceSipImpl(this, enablePresence,
                            forceP2P, pollingValue, subscriptionExpiration);

                addSupportedOperationSet(
                    OperationSetPersistentPresence.class,
                    opSetPersPresence);
                //also register with standard presence
                addSupportedOperationSet(
                    OperationSetPresence.class,
                    opSetPersPresence);

                // Only init messaging and typing if enabled.
                boolean isMessagingDisabled
                    = SipActivator.getConfigurationService()
                        .getBoolean(IS_MESSAGING_DISABLED, false);

                if (!isMessagingDisabled)
                {
                    // init instant messaging
                    this.opSetBasicIM =
                        new OperationSetBasicInstantMessagingSipImpl(this);

                    addSupportedOperationSet(
                        OperationSetBasicInstantMessaging.class,
                        opSetBasicIM);

                    // init typing notifications
                    this.opSetTypingNotif
                        = new OperationSetTypingNotificationsSipImpl(
                            this, opSetBasicIM);
                    addSupportedOperationSet(
                        OperationSetTypingNotifications.class,
                        opSetTypingNotif);

                    addSupportedOperationSet(
                        OperationSetInstantMessageTransform.class,
                        new OperationSetInstantMessageTransformImpl());
                }

                this.opSetSSAccountInfo =
                    new OperationSetServerStoredAccountInfoSipImpl(this);

                // Set the display name.
                opSetSSAccountInfo.setOurDisplayName(ourDisplayName);

                // init avatar
                addSupportedOperationSet(
                    OperationSetServerStoredAccountInfo.class,
                    opSetSSAccountInfo);

                addSupportedOperationSet(
                    OperationSetAvatar.class,
                    new OperationSetAvatarSipImpl(this, opSetSSAccountInfo));
            }

            // MWI is enabled by default
            if(accountID.getAccountPropertyBoolean(
                    ProtocolProviderFactory.VOICEMAIL_ENABLED,
                    true))
            {
                this.opSetMWI = new OperationSetMessageWaitingSipImpl(this);
                addSupportedOperationSet(
                    OperationSetMessageWaiting.class,
                    opSetMWI);
            }

            if(getAccountID().getAccountPropertyString(
                ProtocolProviderFactory.CUSAX_PROVIDER_ACCOUNT_PROP) != null)
            {
                addSupportedOperationSet(
                    OperationSetCusaxUtils.class,
                    new OperationSetCusaxUtilsSipImpl(this));
            }

            if(accountID.getAccountPropertyBoolean(
                OperationSetTelephonyBLFSipImpl.BLF_ENABLED_ACC_PROP,
                false))
            {
                addSupportedOperationSet(
                    OperationSetTelephonyBLF.class,
                    new OperationSetTelephonyBLFSipImpl(this));
            }

            //initialize our OPTIONS handler
            this.capabilities = new ClientCapabilities(this);

            //init the security manager
            this.sipSecurityManager = new SipSecurityManager(accountID, this);
            sipSecurityManager.setHeaderFactory(headerFactory);

            // register any available custom extensions
            ProtocolProviderExtensions.registerCustomOperationSets(this);

            isInitialized = true;
        }
    }

    /**
     * Adds a specific <tt>OperationSet</tt> implementation to the set of
     * supported <tt>OperationSet</tt>s of this instance. Serves as a type-safe
     * wrapper around {@link #supportedOperationSets} which works with class
     * names instead of <tt>Class</tt> and also shortens the code which performs
     * such additions.
     *
     * @param <T> the exact type of the <tt>OperationSet</tt> implementation to
     * be added
     * @param opsetClass the <tt>Class</tt> of <tt>OperationSet</tt> under the
     * name of which the specified implementation is to be added
     * @param opset the <tt>OperationSet</tt> implementation to be added
     */
    @Override
    protected <T extends OperationSet> void addSupportedOperationSet(
            Class<T> opsetClass,
            T opset)
    {
        super.addSupportedOperationSet(opsetClass, opset);
    }

    /**
     * Removes an <tt>OperationSet</tt> implementation from the set of
     * supported <tt>OperationSet</tt>s for this instance.
     *
     * @param <T> the exact type of the <tt>OperationSet</tt> implementation to
     * be added
     * @param opsetClass the <tt>Class</tt> of <tt>OperationSet</tt> under the
     * name of which the specified implementation is to be added
     */
    @Override
    protected <T extends OperationSet> void removeSupportedOperationSet(
                                                Class<T> opsetClass)
    {
        super.removeSupportedOperationSet(opsetClass);
    }

    /**
     * Never called.
     *
     * @param exceptionEvent the IOExceptionEvent containing the cause.
     */
    public void processIOException(IOExceptionEvent exceptionEvent) {}

    /**
     * Processes a Response received on a SipProvider upon which this
     * SipListener is registered.
     * <p>
     *
     * @param responseEvent the responseEvent fired from the SipProvider to the
     * SipListener representing a Response received from the network.
     */
    public void processResponse(ResponseEvent responseEvent)
    {
        ClientTransaction clientTransaction = responseEvent
            .getClientTransaction();
        if (clientTransaction == null)
        {
            if (logger.isDebugEnabled())
                logger.debug("ignoring a transactionless response");
            return;
        }

        Response response = responseEvent.getResponse();

        earlyProcessMessage(responseEvent);

        String method = ( (CSeqHeader) response.getHeader(CSeqHeader.NAME))
            .getMethod();

        //find the object that is supposed to take care of responses with the
        //corresponding method
        List<MethodProcessor> processors = methodProcessors.get(method);

        if (processors != null)
        {
            if (logger.isDebugEnabled())
                logger.debug("Found " + processors.size()
                        + " processor(s) for method " + method);

            for (MethodProcessor processor : processors)
                if (processor.processResponse(responseEvent))
                    break;
        }
    }

    /**
     * Processes a retransmit or expiration Timeout of an underlying
     * {@link Transaction} handled by this SipListener. This Event notifies the
     * application that a retransmission or transaction Timer expired in the
     * SipProvider's transaction state machine. The TimeoutEvent encapsulates
     * the specific timeout type and the transaction identifier either client or
     * server upon which the timeout occurred. The type of Timeout can by
     * determined by:
     * <code>timeoutType = timeoutEvent.getTimeout().getValue();</code>
     *
     * @param timeoutEvent -
     *            the timeoutEvent received indicating either the message
     *            retransmit or transaction timed out.
     */
    public void processTimeout(TimeoutEvent timeoutEvent)
    {
        Transaction transaction;
        if(timeoutEvent.isServerTransaction())
            transaction = timeoutEvent.getServerTransaction();
        else
            transaction = timeoutEvent.getClientTransaction();

        if (transaction == null)
        {
            if (logger.isDebugEnabled())
                logger.debug("ignoring a transactionless timeout event");
            return;
        }

        earlyProcessMessage(timeoutEvent);

        Request request = transaction.getRequest();
        if (logger.isDebugEnabled())
            logger.debug("received timeout for req=" + request);

        //find the object that is supposed to take care of responses with the
        //corresponding method
        String method = request.getMethod();
        List<MethodProcessor> processors = methodProcessors.get(method);

        if (processors != null)
        {
            if (logger.isDebugEnabled())
                logger.debug("Found " + processors.size()
                + " processor(s) for method " + method);

            for (MethodProcessor processor : processors)
            {
                if (processor.processTimeout(timeoutEvent))
                {
                    break;
                }
            }
        }
    }

    /**
     * Process an asynchronously reported TransactionTerminatedEvent.
     * When a transaction transitions to the Terminated state, the stack
     * keeps no further records of the transaction. This notification can be used by
     * applications to clean up any auxiliary data that is being maintained
     * for the given transaction.
     *
     * @param transactionTerminatedEvent -- an event that indicates that the
     *       transaction has transitioned into the terminated state.
     * @since v1.2
     */
    public void processTransactionTerminated(TransactionTerminatedEvent
                                             transactionTerminatedEvent)
    {
        Transaction transaction;
        if(transactionTerminatedEvent.isServerTransaction())
            transaction = transactionTerminatedEvent.getServerTransaction();
        else
            transaction = transactionTerminatedEvent.getClientTransaction();

        if (transaction == null)
        {
            if (logger.isDebugEnabled())
                logger.debug(
                        "ignoring a transactionless transaction terminated event");
            return;
        }

        Request request = transaction.getRequest();

        //find the object that is supposed to take care of responses with the
        //corresponding method
        String method = request.getMethod();
        List<MethodProcessor> processors = methodProcessors.get(method);

        if (processors != null)
        {
            if (logger.isDebugEnabled())
                logger.debug("Found " + processors.size()
                        + " processor(s) for method " + method);

            for (MethodProcessor processor : processors)
            {
                if (processor.processTransactionTerminated(
                    transactionTerminatedEvent))
                {
                    break;
                }
            }
        }
    }

    /**
     * Process an asynchronously reported DialogTerminatedEvent.
     * When a dialog transitions to the Terminated state, the stack
     * keeps no further records of the dialog. This notification can be used by
     * applications to clean up any auxiliary data that is being maintained
     * for the given dialog.
     *
     * @param dialogTerminatedEvent -- an event that indicates that the
     *       dialog has transitioned into the terminated state.
     * @since v1.2
     */
    public void processDialogTerminated(DialogTerminatedEvent
                                        dialogTerminatedEvent)
    {
        if (logger.isDebugEnabled())
            logger.debug("Dialog terminated for req="
                    + dialogTerminatedEvent.getDialog());
    }

    /**
     * Processes a Request received on a SipProvider upon which this SipListener
     * is registered.
     * <p>
     * @param requestEvent requestEvent fired from the SipProvider to the
     * SipListener representing a Request received from the network.
     */
    public void processRequest(RequestEvent requestEvent)
    {
        Request request = requestEvent.getRequest();

        if(sipRegistrarConnection != null
           && !sipRegistrarConnection.isRegistrarless()
           && !sipRegistrarConnection.isRequestFromSameConnection(request)
           && !forceLooseRouting)
        {
            logger.warn("Received request not from our proxy, ignoring it! "
                + "Request:" + request);
            if (requestEvent.getServerTransaction() != null)
            {
                try
                {
                    requestEvent.getServerTransaction().terminate();
                }
                catch (Throwable e)
                {
                    logger.warn("Failed to properly terminate transaction for "
                                    +"a rogue request. Well ... so be it "
                                    + "Request:" + request);
                }
            }
            return;
        }

        earlyProcessMessage(requestEvent);

        // test if an Event header is present and known
        EventHeader eventHeader = (EventHeader)
            request.getHeader(EventHeader.NAME);

        if (eventHeader != null)
        {
            boolean eventKnown;

            synchronized (this.registeredEvents)
            {
                eventKnown = this.registeredEvents.contains(
                        eventHeader.getEventType());
            }

            if (!eventKnown)
            {
                // send a 489 / Bad Event response
                ServerTransaction serverTransaction = requestEvent
                    .getServerTransaction();

                if (serverTransaction == null)
                {
                    try
                    {
                        serverTransaction = SipStackSharing.
                            getOrCreateServerTransaction(requestEvent);
                    }
                    catch (TransactionAlreadyExistsException ex)
                    {
                        //let's not scare the user and only log a message
                        logger.error("Failed to create a new server"
                            + "transaction for an incoming request\n"
                            + "(Next message contains the request)"
                            , ex);
                        return;
                    }
                    catch (TransactionUnavailableException ex)
                    {
                        //let's not scare the user and only log a message
                        logger.error("Failed to create a new server"
                            + "transaction for an incoming request\n"
                            + "(Next message contains the request)"
                            , ex);
                            return;
                    }
                }

                Response response = null;
                try
                {
                    response = this.getMessageFactory().createResponse(
                            Response.BAD_EVENT, request);
                }
                catch (ParseException e)
                {
                    logger.error("failed to create the 489 response", e);
                    return;
                }

                try
                {
                    serverTransaction.sendResponse(response);
                    return;
                }
                catch (SipException e)
                {
                    logger.error("failed to send the response", e);
                }
                catch (InvalidArgumentException e)
                {
                    // should not happen
                    logger.error("invalid argument provided while trying" +
                            " to send the response", e);
                }
            }
        }


        String method = request.getMethod();

        //find the object that is supposed to take care of responses with the
        //corresponding method
        List<MethodProcessor> processors = methodProcessors.get(method);

        //raise this flag if at least one processor handles the request.
        boolean processedAtLeastOnce = false;

        if (processors != null)
        {
            if (logger.isDebugEnabled())
                logger.debug("Found " + processors.size()
                        + " processor(s) for method " + method);

            for (MethodProcessor processor : processors)
            {
                if (processor.processRequest(requestEvent))
                {
                    processedAtLeastOnce = true;
                    break;
                }
            }
        }

        //send an error response if no one processes this
        if (!processedAtLeastOnce)
        {
            ServerTransaction serverTransaction;
            try
            {
                serverTransaction = SipStackSharing.getOrCreateServerTransaction(requestEvent);

                if (serverTransaction == null)
                {
                    logger.warn("Could not create a transaction for a "
                               +"non-supported method " + request.getMethod());
                    return;
                }

                TransactionState state = serverTransaction.getState();

                if( TransactionState.TRYING.equals(state))
                {
                    Response response = this.getMessageFactory().createResponse(
                                    Response.NOT_IMPLEMENTED, request);
                    serverTransaction.sendResponse(response);
                }
            }
            catch (Throwable exc)
            {
                logger.warn("Could not respond to a non-supported method "
                                + request.getMethod(), exc);
            }
        }
    }

    /**
     * Makes the service implementation close all open sockets and release
     * any resources that it might have taken and prepare for shutdown/garbage
     * collection.
     */
    public void shutdown()
    {
        if(!isInitialized)
        {
            return;
        }

        // don't run in Thread cause shutting down may finish before
        // we were able to unregister
        new ShutdownThread().run();
    }

    /**
     * The thread that we use in order to send our unREGISTER request upon
     * system shut down.
     */
    protected class ShutdownThread implements Runnable
    {
        /**
         * Shutdowns operation sets that need it then calls the
         * <tt>SipRegistrarConnection.unregister()</tt> method.
         */
        public void run()
        {
            if (logger.isTraceEnabled())
                logger.trace("Killing the SIP Protocol Provider.");
            //kill all active calls
            OperationSetBasicTelephonySipImpl telephony
                = (OperationSetBasicTelephonySipImpl)getOperationSet(
                    OperationSetBasicTelephony.class);
            telephony.shutdown();

            if(isRegistered())
            {
                try
                {
                    //create a listener that would notify us when
                    //un-registration has completed.
                    ShutdownUnregistrationBlockListener listener
                        = new ShutdownUnregistrationBlockListener();
                    addRegistrationStateChangeListener(listener);

                    //do the un-registration
                    unregister();

                    //leave ourselves time to complete un-registration (may include
                    //2 REGISTER requests in case notification is needed.)
                    listener.waitForEvent(3000L);
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

            // Shutdown capabilities
            if(capabilities != null)
            {
                capabilities.shutdown();
                capabilities = null;
            }
            // Shutdown presence
            if(opSetPersPresence != null)
            {
                opSetPersPresence.shutdown();
                opSetPersPresence = null;
            }
            // Shutdown IM
            if(opSetBasicIM != null)
            {
                opSetBasicIM.shutdown();
                opSetBasicIM = null;
            }
            // Shutdown MWI
            if(opSetMWI != null)
            {
                opSetMWI.shutdown();
                opSetMWI = null;
            }
            // Shutdown server stored account info
            if(opSetSSAccountInfo != null)
            {
                opSetSSAccountInfo.shutdown();
                opSetSSAccountInfo = null;
            }
            // Shutdown typing notifications
            if(opSetTypingNotif != null)
            {
                opSetTypingNotif.shutdown();
                opSetTypingNotif = null;
            }

            headerFactory = null;
            messageFactory = null;
            addressFactory = null;
            sipSecurityManager = null;

            connection = null;

            methodProcessors.clear();

            isInitialized = false;
        }
    }

    /**
     * Initializes and returns an ArrayList with a single ViaHeader
     * containing a localhost address usable with the specified
     * s<tt>destination</tt>. This ArrayList may be used when sending
     * requests to that destination.
     * <p>
     * @param intendedDestination The address of the destination that the
     * request using the via headers will be sent to.
     *
     * @return ViaHeader-s list to be used when sending requests.
     * @throws OperationFailedException code INTERNAL_ERROR if a ParseException
     * occurs while initializing the array list.
     *
     */
    public ArrayList<ViaHeader> getLocalViaHeaders(Address intendedDestination)
        throws OperationFailedException
    {
        return getLocalViaHeaders((SipURI)intendedDestination.getURI());
    }

    /**
     * Initializes and returns an ArrayList with a single ViaHeader
     * containing a localhost address usable with the specified
     * s<tt>destination</tt>. This ArrayList may be used when sending
     * requests to that destination.
     * <p>
     * @param intendedDestination The address of the destination that the
     * request using the via headers will be sent to.
     *
     * @return ViaHeader-s list to be used when sending requests.
     * @throws OperationFailedException code INTERNAL_ERROR if a ParseException
     * occurs while initializing the array list.
     *
     */
    public ArrayList<ViaHeader> getLocalViaHeaders(SipURI intendedDestination)
        throws OperationFailedException
    {
        ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();

        ListeningPoint srcListeningPoint
                = getListeningPoint(intendedDestination.getTransportParam());
        try
        {
            InetSocketAddress targetAddress =
                        getIntendedDestination(intendedDestination);

            InetAddress localAddress = SipActivator
                .getNetworkAddressManagerService().getLocalHost(
                    targetAddress.getAddress());

            int localPort = srcListeningPoint.getPort();
            String transport = srcListeningPoint.getTransport();
            if (ListeningPoint.TCP.equalsIgnoreCase(transport)
                || ListeningPoint.TLS.equalsIgnoreCase(transport))
            {
                InetSocketAddress localSockAddr
                    = sipStackSharing.getLocalAddressForDestination(
                                    targetAddress.getAddress(),
                                    targetAddress.getPort(),
                                    localAddress,
                                    transport);
                localPort = localSockAddr.getPort();
            }

            ViaHeader viaHeader = headerFactory.createViaHeader(
                localAddress.getHostAddress(),
                localPort,
                transport,
                null
                );
            viaHeaders.add(viaHeader);
            if (logger.isDebugEnabled())
                logger.debug("generated via headers:" + viaHeader);
            return viaHeaders;
        }
        catch (ParseException ex)
        {
            logger.error(
                "A ParseException occurred while creating Via Headers!", ex);
            throw new OperationFailedException(
                "A ParseException occurred while creating Via Headers!"
                ,OperationFailedException.INTERNAL_ERROR
                ,ex);
        }
        catch (InvalidArgumentException ex)
        {
            logger.error(
                "Unable to create a via header for port "
                + sipStackSharing.getLP(ListeningPoint.UDP).getPort(),
                ex);
            throw new OperationFailedException(
                "Unable to create a via header for port "
                + sipStackSharing.getLP(ListeningPoint.UDP).getPort()
                ,OperationFailedException.INTERNAL_ERROR
                ,ex);
        }
        catch (java.io.IOException ex)
        {
            logger.error(
                "Unable to create a via header for port "
                + sipStackSharing.getLP(ListeningPoint.UDP).getPort(),
                ex);
            throw new OperationFailedException(
                "Unable to create a via header for port "
                + sipStackSharing.getLP(ListeningPoint.UDP).getPort()
                ,OperationFailedException.NETWORK_FAILURE
                ,ex);
        }
    }

    /**
     * Initializes and returns this provider's default maxForwardsHeader field
     * using the value specified by MAX_FORWARDS.
     *
     * @return an instance of a MaxForwardsHeader that can be used when
     * sending requests
     *
     * @throws OperationFailedException with code INTERNAL_ERROR if MAX_FORWARDS
     * has an invalid value.
     */
    public MaxForwardsHeader getMaxForwardsHeader() throws
        OperationFailedException
    {
        if (maxForwardsHeader == null)
        {
            try
            {
                maxForwardsHeader = headerFactory.createMaxForwardsHeader(
                    MAX_FORWARDS);
                if (logger.isDebugEnabled())
                    logger.debug("generated max forwards: "
                            + maxForwardsHeader.toString());
            }
            catch (InvalidArgumentException ex)
            {
                throw new OperationFailedException(
                    "A problem occurred while creating MaxForwardsHeader"
                    , OperationFailedException.INTERNAL_ERROR
                    , ex);
            }
        }

        return maxForwardsHeader;
    }

    /**
     * Returns a Contact header containing a sip URI based on a localhost
     * address.
     *
     * @param intendedDestination the destination that we plan to be sending
     * this contact header to.
     *
     * @return a Contact header based upon a local inet address.
     */
    public ContactHeader getContactHeader(Address intendedDestination)
    {
        return getContactHeader((SipURI)intendedDestination.getURI());
    }

    /**
     * Returns a Contact header containing a sip URI based on a localhost
     * address and therefore usable in REGISTER requests only.
     *
     * @param intendedDestination the destination that we plan to be sending
     * this contact header to.
     *
     * @return a Contact header based upon a local inet address.
     */
    public ContactHeader getContactHeader(SipURI intendedDestination)
    {
        ContactHeader registrationContactHeader = null;

        ListeningPoint srcListeningPoint
                                  = getListeningPoint(intendedDestination);

        InetSocketAddress targetAddress =
                getIntendedDestination(intendedDestination);

        try
        {
            //find the address to use with the target
            InetAddress localAddress = SipActivator
                .getNetworkAddressManagerService()
                .getLocalHost(targetAddress.getAddress());

            SipURI contactURI = addressFactory.createSipURI(
                getAccountID().getUserID()
                , localAddress.getHostAddress() );

            String transport = srcListeningPoint.getTransport();
            contactURI.setTransportParam(transport);

            int localPort = srcListeningPoint.getPort();

            //if we are using tcp, make sure that we include the port of the
            //socket that we are actually using and not that of LP
            if (ListeningPoint.TCP.equalsIgnoreCase(transport)
                || ListeningPoint.TLS.equalsIgnoreCase(transport))
            {
                InetSocketAddress localSockAddr
                    = sipStackSharing.getLocalAddressForDestination(
                                    targetAddress.getAddress(),
                                    targetAddress.getPort(),
                                    localAddress,
                                    transport);
                localPort = localSockAddr.getPort();
            }

            contactURI.setPort(localPort);

            // set a custom param to ease incoming requests dispatching in case
            // we have several registrar accounts with the same username
            String paramValue = getContactAddressCustomParamValue();
            if (paramValue != null)
            {
                contactURI.setParameter(
                        SipStackSharing.CONTACT_ADDRESS_CUSTOM_PARAM_NAME,
                        paramValue);
            }

            Address contactAddress = addressFactory.createAddress( contactURI );

            String ourDisplayName = getOurDisplayName();
            if (ourDisplayName != null)
            {
                contactAddress.setDisplayName(ourDisplayName);
            }
            registrationContactHeader = headerFactory.createContactHeader(
                contactAddress);

            if (logger.isDebugEnabled())
                logger.debug("generated contactHeader:"
                        + registrationContactHeader);
        }
        catch (ParseException ex)
        {
            logger.error(
                "A ParseException occurred while creating From Header!", ex);
            throw new IllegalArgumentException(
                "A ParseException occurred while creating From Header!"
                , ex);
        }
        catch (java.io.IOException ex)
        {
            logger.error(
                "A ParseException occurred while creating From Header!", ex);
            throw new IllegalArgumentException(
                "A ParseException occurred while creating From Header!"
                , ex);
        }
        return registrationContactHeader;
    }

    /**
     * Returns null for a registraless account, a value for the contact address
     * custom parameter otherwise. This will help the dispatching of incoming
     * requests between accounts with the same username. For address-of-record
     * user@example.com, the returned value woud be example_com.
     *
     * @return null for a registraless account, a value for the
     * "registering_acc" contact address parameter otherwise
     */
    public String getContactAddressCustomParamValue()
    {
            SipRegistrarConnection src = sipRegistrarConnection;
            if (src != null && !src.isRegistrarless())
            {
                // if we don't replace the dots in the hostname, we get
                // "476 No Server Address in Contacts Allowed"
                // from certain registrars (ippi.fr for instance)
                String hostValue = ((SipURI) src.getAddressOfRecord().getURI())
                    .getHost().replace('.', '_');
                return hostValue;
            }
            return null;
    }

    /**
     * Returns the AddressFactory used to create URLs ans Address objects.
     *
     * @return the AddressFactory used to create URLs ans Address objects.
     */
    public AddressFactoryEx getAddressFactory()
    {
        return addressFactory;
    }

    /**
     * Returns the HeaderFactory used to create SIP message headers.
     *
     * @return the HeaderFactory used to create SIP message headers.
     */
    public HeaderFactory getHeaderFactory()
    {
        return headerFactory;
    }

    /**
     * Returns the Message Factory used to create SIP messages.
     *
     * @return the Message Factory used to create SIP messages.
     */
    public SipMessageFactory getMessageFactory()
    {
        if (messageFactory == null)
        {
            messageFactory =
                new SipMessageFactory(this, new MessageFactoryImpl());
        }
        return messageFactory;
    }

    /**
     * Returns all running instances of ProtocolProviderServiceSipImpl
     *
     * @return all running instances of ProtocolProviderServiceSipImpl
     */
    public static Set<ProtocolProviderServiceSipImpl> getAllInstances()
    {
        try
        {
            Set<ProtocolProviderServiceSipImpl> instances
                = new HashSet<ProtocolProviderServiceSipImpl>();
            BundleContext context = SipActivator.getBundleContext();
            ServiceReference[] references = context.getServiceReferences(
                    ProtocolProviderService.class.getName(),
                    null
                    );
            for(ServiceReference reference : references)
            {
                Object service = context.getService(reference);
                if(service instanceof ProtocolProviderServiceSipImpl)
                    instances.add((ProtocolProviderServiceSipImpl) service);
            }
            return instances;
        }
        catch(InvalidSyntaxException ex)
        {
            if (logger.isDebugEnabled())
                logger.debug("Problem parcing an osgi expression", ex);
            // should never happen so crash if it ever happens
            throw new RuntimeException(
                    "getServiceReferences() wasn't supposed to fail!"
                    );
        }
     }

    /**
     * Returns the default listening point that we use for communication over
     * <tt>transport</tt>.
     *
     * @param transport the transport that the returned listening point needs
     * to support.
     *
     * @return the default listening point that we use for communication over
     * <tt>transport</tt> or null if no such transport is supported.
     */
    public ListeningPoint getListeningPoint(String transport)
    {
        if(logger.isTraceEnabled())
            logger.trace("Query for a " + transport + " listening point");

        //override the transport in case we have an outbound proxy.
        if(connection.getAddress() != null)
        {
            if (logger.isTraceEnabled())
                logger.trace("Will use proxy address");
            transport = connection.getTransport();
        }

        if(!isValidTransport(transport))
        {
            transport = getDefaultTransport();
        }

        ListeningPoint lp = null;

        if(transport.equalsIgnoreCase(ListeningPoint.UDP))
        {
            lp = sipStackSharing.getLP(ListeningPoint.UDP);
        }
        else if(transport.equalsIgnoreCase(ListeningPoint.TCP))
        {
            lp = sipStackSharing.getLP(ListeningPoint.TCP);
        }
        else if(transport.equalsIgnoreCase(ListeningPoint.TLS))
        {
            lp = sipStackSharing.getLP(ListeningPoint.TLS);
        }

        if(logger.isTraceEnabled())
        {
            logger.trace("Returning LP " + lp + " for transport ["
                            + transport + "] and ");
        }
        return lp;
    }

    /**
     * Returns the default listening point that we should use to contact the
     * intended destination.
     *
     * @param intendedDestination the address that we will be trying to contact
     * through the listening point we are trying to obtain.
     *
     * @return the listening point that we should use to contact the
     * intended destination.
     */
    public ListeningPoint getListeningPoint(SipURI intendedDestination)
    {
        return getListeningPoint(intendedDestination.getTransportParam());
    }

    /**
     * Returns the default jain sip provider that we use for communication over
     * <tt>transport</tt>.
     *
     * @param transport the transport that the returned provider needs
     * to support.
     *
     * @return the default jain sip provider that we use for communication over
     * <tt>transport</tt> or null if no such transport is supported.
     */
    public SipProvider getJainSipProvider(String transport)
    {
        return sipStackSharing.getJainSipProvider(transport);
    }

    /**
     * Reurns the currently valid sip security manager that everyone should
     * use to authenticate SIP Requests.
     * @return the currently valid instace of a SipSecurityManager that everyone
     * sould use to authenticate SIP Requests.
     */
    public SipSecurityManager getSipSecurityManager()
    {
        return sipSecurityManager;
    }

    /**
     * Initializes the SipRegistrarConnection that this class will be using.
     *
     * @throws java.lang.IllegalArgumentException if one or more account
     * properties have invalid values.
     */
    private void initRegistrarConnection()
        throws IllegalArgumentException
    {
        //First init the registrar address
        String registrarAddressStr =
            accountID
                .getAccountPropertyString(ProtocolProviderFactory.SERVER_ADDRESS);

        //if there is no registrar address, parse the user_id and extract it
        //from the domain part of the SIP URI.
        if (registrarAddressStr == null)
        {
            String userID =
                accountID
                    .getAccountPropertyString(ProtocolProviderFactory.USER_ID);
            int index = userID.indexOf('@');
            if ( index > -1 )
                registrarAddressStr = userID.substring( index+1);
        }

        //if we still have no registrar address or if the registrar address
        //string is one of our local host addresses this means the users does
        //not want to use a registrar connection
        if(registrarAddressStr == null
           || registrarAddressStr.trim().length() == 0)
        {
            initRegistrarlessConnection();
            return;
        }

        //init registrar port
        int registrarPort = ListeningPoint.PORT_5060;

        // check if user has overridden the registrar port.
        registrarPort =
            accountID.getAccountPropertyInt(
                ProtocolProviderFactory.SERVER_PORT, registrarPort);
        if (registrarPort > NetworkUtils.MAX_PORT_NUMBER)
        {
            throw new IllegalArgumentException(registrarPort
                + " is larger than " + NetworkUtils.MAX_PORT_NUMBER
                + " and does not therefore represent a valid port number.");
        }

        //Initialize our connection with the registrar
        // we insert the default transport if none is specified
        // use it for registrar connection
        this.sipRegistrarConnection = new SipRegistrarConnection(
            registrarAddressStr
            , registrarPort
            , getRegistrarTransport()
            , this);
    }

    /**
     * Initializes the SipRegistrarConnection that this class will be using.
     *
     * @throws java.lang.IllegalArgumentException if one or more account
     * properties have invalid values.
     */
    private void initRegistrarlessConnection()
        throws IllegalArgumentException
    {
        //registrar transport
        String registrarTransport = getRegistrarTransport();

        //Initialize our connection with the registrar
        this.sipRegistrarConnection
               = new SipRegistrarlessConnection(this, registrarTransport);
    }

    /**
     * Returns the registrar transport from this account's configuration or
     * the default transport if no specific transport has been configured for
     * this account's registrar.
     *
     * @return the value if the {@link ProtocolProviderFactory#SERVER_TRANSPORT}
     * account property or the  {@link #DEFAULT_TRANSPORT} configuration
     * property if the former is <tt>null<tt/>.
     */
    private String getRegistrarTransport()
    {
        String registrarTransport = accountID.getAccountPropertyString(
            ProtocolProviderFactory.SERVER_TRANSPORT);

        if(!StringUtils.isNullOrEmpty(registrarTransport))
        {
            if( ! registrarTransport.equals(ListeningPoint.UDP)
                && !registrarTransport.equals(ListeningPoint.TCP)
                && !registrarTransport.equals(ListeningPoint.TLS))
            {
                throw new IllegalArgumentException(registrarTransport
                    + " is not a valid transport protocol. SERVER_TRANSPORT "
                    + "must be left blank or set to TCP, UDP or TLS.");
            }
        }
        else
        {
            registrarTransport = getDefaultTransport();
        }

        return registrarTransport;
    }

    /**
     * Returns the SIP address of record (Display Name <user@server.net>) that
     * this account is created for. The method takes into account whether or
     * not we are running in Registar or "No Registar" mode and either returns
     * the AOR we are using to register or an address constructed using the
     * local address.
     *
     * @param intendedDestination the destination that we would be using the
     * local address to communicate with.
     *
     * @return our Address Of Record that we should use in From headers.
     */
    public Address getOurSipAddress(Address intendedDestination)
    {
        return getOurSipAddress((SipURI)intendedDestination.getURI());
    }

    /**
     * Returns the SIP address of record (Display Name <user@server.net>) that
     * this account is created for. The method takes into account whether or
     * not we are running in Registar or "No Registar" mode and either returns
     * the AOR we are using to register or an address constructed using the
     * local address
     *
     * @param intendedDestination the destination that we would be using the
     * local address to communicate with.
     * .
     * @return our Address Of Record that we should use in From headers.
     */
    public Address getOurSipAddress(SipURI intendedDestination)
    {
        SipRegistrarConnection src = sipRegistrarConnection;
        if (src != null && !src.isRegistrarless())
            return src.getAddressOfRecord();

        //we are apparently running in "No Registrar" mode so let's create an
        //address by ourselves.
        InetSocketAddress destinationAddr
                    = getIntendedDestination(intendedDestination);

        InetAddress localHost = SipActivator.getNetworkAddressManagerService()
            .getLocalHost(destinationAddr.getAddress());

        String userID = getAccountID().getUserID();

        try
        {
            SipURI ourSipURI = getAddressFactory()
                .createSipURI(userID, localHost.getHostAddress());

            ListeningPoint lp = getListeningPoint(intendedDestination);

            ourSipURI.setTransportParam(lp.getTransport());
            ourSipURI.setPort(lp.getPort());

            Address ourSipAddress = getAddressFactory()
                .createAddress(getOurDisplayName(), ourSipURI);

            ourSipAddress.setDisplayName(getOurDisplayName());

            return ourSipAddress;
        }
        catch (ParseException exc)
        {
            if (logger.isTraceEnabled())
                logger.trace("Failed to create our SIP AOR address", exc);
            // this should never happen since we are using InetAddresses
            // everywhere so parsing could hardly go wrong.
            throw new IllegalArgumentException(
                            "Failed to create our SIP AOR address"
                            , exc);
        }
    }

    /**
     * Returns the <tt>ProxyConnection</tt>.
     *
     * @return the <tt>ProxyConnection</tt>
     */
    public ProxyConnection getConnection()
    {
        return connection;
    }

    /**
     * Indicates if the SIP transport channel is using a TLS secured socket.
     *
     * @return True when TLS is used the SIP transport protocol, false
     *         otherwise or when no proxy is being used.
     */
    public boolean isSignalingTransportSecure()
    {
        return ListeningPoint.TLS.equalsIgnoreCase(connection.getTransport());
    }

    /**
     * Returns the "transport" protocol of this instance used to carry the
     * control channel for the current protocol service.
     *
     * @return The "transport" protocol of this instance: UDP, TCP, TLS or
     * UNKNOWN.
     */
    public TransportProtocol getTransportProtocol()
    {
        // The transport protocol is not set properly when dealing with a
        // RegistrarLess account. This is why we return "UNKNOWN" in this case.
        if(this.sipRegistrarConnection == null
                || this.sipRegistrarConnection instanceof
                SipRegistrarlessConnection)
        {
            return TransportProtocol.UNKNOWN;
        }
        return TransportProtocol.parse(
                sipRegistrarConnection.getTransport());
    }

    /**
     * Registers <tt>methodProcessor</tt> in the <tt>methorProcessors</tt> table
     * so that it would receives all messages in a transaction initiated by a
     * <tt>method</tt> request. If any previous processors exist for the same
     * method, they will be replaced by this one.
     *
     * @param method a String representing the SIP method that we're registering
     *            the processor for (e.g. INVITE, REGISTER, or SUBSCRIBE).
     * @param methodProcessor a <tt>MethodProcessor</tt> implementation that
     *            would handle all messages received within a <tt>method</tt>
     *            transaction.
     */
    public void registerMethodProcessor(String method,
        MethodProcessor methodProcessor)
    {
        List<MethodProcessor> processors = methodProcessors.get(method);
        if (processors == null)
        {
            processors = new LinkedList<MethodProcessor>();
            methodProcessors.put(method, processors);
        }
        else
        {
            /*
             * Prevent the registering of multiple instances of one and the same
             * OperationSet class and take only the latest registration into
             * account.
             */
            Iterator<MethodProcessor> processorIter = processors.iterator();
            Class<? extends MethodProcessor> methodProcessorClass
                = methodProcessor.getClass();
            /*
             * EventPackageSupport and its extenders provide a generic mechanizm
             * for building support for a specific event package so allow them
             * to register multiple instances of one and the same class as long
             * as they are handling different event packages.
             */
            String eventPackage
                = (methodProcessor instanceof EventPackageSupport)
                    ? ((EventPackageSupport) methodProcessor).getEventPackage()
                    : null;

            while (processorIter.hasNext())
            {
                MethodProcessor processor = processorIter.next();

                if (!processor.getClass().equals(methodProcessorClass))
                    continue;
                if ((eventPackage != null)
                        && (processor instanceof EventPackageSupport)
                        && !eventPackage
                                .equals(
                                    ((EventPackageSupport) processor)
                                        .getEventPackage()))
                    continue;

                processorIter.remove();
            }
        }
        processors.add(methodProcessor);
    }

    /**
     * Unregisters <tt>methodProcessor</tt> from the <tt>methorProcessors</tt>
     * table so that it won't receive further messages in a transaction
     * initiated by a <tt>method</tt> request.
     *
     * @param method the name of the method whose processor we'd like to
     * unregister.
     * @param methodProcessor the <tt>MethodProcessor</tt> that we'd like to
     * unregister.
     */
    public void unregisterMethodProcessor(String method,
        MethodProcessor methodProcessor)
    {
        List<MethodProcessor> processors = methodProcessors.get(method);
        if ((processors != null) && processors.remove(methodProcessor)
            && (processors.size() <= 0))
        {
            methodProcessors.remove(method);
        }
    }

    /**
     * Returns the transport that we should use if we have no clear idea of our
     * destination's preferred transport. The method would first check if
     * we are running behind an outbound proxy and if so return its transport.
     * If no outbound proxy is set, the method would check the contents of the
     * DEFAULT_TRANSPORT property and return it if not null. Otherwise the
     * method would return UDP;
     *
     * @return The first non null password of the following:
     * a) the transport we use to communicate with our registrar
     * b) the transport of our outbound proxy,
     * c) the transport specified by the DEFAULT_TRANSPORT property, UDP.
     */
    public String getDefaultTransport()
    {
        if(sipRegistrarConnection != null
            && !sipRegistrarConnection.isRegistrarless()
            && connection != null
            && connection.getAddress() != null
            && connection.getTransport() != null)
        {
            return connection.getTransport();
        }
        else
        {
            String userSpecifiedDefaultTransport
                = SipActivator.getConfigurationService()
                    .getString(DEFAULT_TRANSPORT);

            if(userSpecifiedDefaultTransport != null)
            {
                return userSpecifiedDefaultTransport;
            }
            else
            {
                String defTransportDefaultValue = SipActivator.getResources()
                        .getSettingsString(DEFAULT_TRANSPORT);

                if(!StringUtils.isNullOrEmpty(defTransportDefaultValue))
                    return defTransportDefaultValue;
                else
                    return ListeningPoint.UDP;
            }
        }
    }

    /**
     * Returns the provider that corresponds to the transport returned by
     * getDefaultTransport(). Equivalent to calling
     * getJainSipProvider(getDefaultTransport())
     *
     * @return the Jain SipProvider that corresponds to the transport returned
     * by getDefaultTransport().
     */
    public SipProvider getDefaultJainSipProvider()
    {
        return getJainSipProvider(getDefaultTransport());
    }

    /**
     * Returns the display name string that the user has set as a display name
     * for this account.
     *
     * @return the display name string that the user has set as a display name
     * for this account.
     */
    public String getOurDisplayName()
    {
        return ourDisplayName;
    }

    /**
     * Changes the display name string.
     *
     * @return whether we have successfully changed the display name.
     */
    boolean setOurDisplayName(String newDisplayName)
    {
        // if we really want to change the display name
        // and it is existing, change it.
        if(newDisplayName != null && !ourDisplayName.equals(newDisplayName))
        {
            getAccountID().putAccountProperty(
                    ProtocolProviderFactory.DISPLAY_NAME,
                    newDisplayName);

            ourDisplayName = newDisplayName;

            OperationSetServerStoredAccountInfoSipImpl accountInfoOpSet
                = (OperationSetServerStoredAccountInfoSipImpl)getOperationSet(
                    OperationSetServerStoredAccountInfo.class);

            if(accountInfoOpSet != null)
                accountInfoOpSet.setOurDisplayName(newDisplayName);

            return true;
        }

        return false;
    }

    /**
     * Returns a User Agent header that could be used for signing our requests.
     *
     * @return a <tt>UserAgentHeader</tt> that could be used for signing our
     * requests.
     */
    public UserAgentHeader getSipCommUserAgentHeader()
    {
        if(userAgentHeader == null)
        {
            try
            {
                List<String> userAgentTokens = new LinkedList<String>();

                Version ver =
                        SipActivator.getVersionService().getCurrentVersion();

                userAgentTokens.add(ver.getApplicationName());
                userAgentTokens.add(ver.toString());

                String osName = System.getProperty("os.name");
                userAgentTokens.add(osName);

                userAgentHeader
                    = this.headerFactory.createUserAgentHeader(userAgentTokens);
            }
            catch (ParseException ex)
            {
                //shouldn't happen
                return null;
            }
        }
        return userAgentHeader;
    }

    /**
     * Send an error response with the <tt>errorCode</tt> code using
     * <tt>serverTransaction</tt> and do not surface exceptions. The method
     * is useful when we are sending the error response in a stack initiated
     * operation and don't have the possibility to escalate potential
     * exceptions, so we can only log them.
     *
     * @param serverTransaction the transaction that we'd like to send an error
     * response in.
     * @param errorCode the code that the response should have.
     */
    public void sayErrorSilently(ServerTransaction serverTransaction,
                                 int               errorCode)
    {
        try
        {
            sayError(serverTransaction, errorCode);
        }
        catch (OperationFailedException exc)
        {
            if (logger.isDebugEnabled())
                logger.debug("Failed to send an error " + errorCode + " response",
                        exc);
        }
    }

    /**
     * Sends an ACK request in the specified dialog.
     *
     * @param clientTransaction the transaction that resulted in the ACK we are
     * about to send (MUST be an INVITE transaction).
     *
     * @throws InvalidArgumentException if there is a problem with the supplied
     * CSeq ( for example <= 0 ).
     * @throws SipException if the CSeq does not relate to a previously sent
     * INVITE or if the Method that created the Dialog is not an INVITE ( for
     * example SUBSCRIBE) or if we fail to send the INVITE for whatever reason.
     */
    public void sendAck(ClientTransaction clientTransaction)
        throws SipException, InvalidArgumentException
    {
            Request ack = messageFactory.createAck(clientTransaction);

            clientTransaction.getDialog().sendAck(ack);
    }

    /**
     * Send an error response with the <tt>errorCode</tt> code using
     * <tt>serverTransaction</tt>.
     *
     * @param serverTransaction the transaction that we'd like to send an error
     * response in.
     * @param errorCode the code that the response should have.
     *
     * @throws OperationFailedException if we failed constructing or sending a
     * SIP Message.
     */
    public void sayError(ServerTransaction serverTransaction, int errorCode)
        throws OperationFailedException
    {
        sayError(serverTransaction, errorCode, null);
    }

    /**
     * Send an error response with the <tt>errorCode</tt> code using
     * <tt>serverTransaction</tt>.
     *
     * @param serverTransaction the transaction that we'd like to send an error
     * response in.
     * @param errorCode the code that the response should have.
     * @param header SIP header
     * @throws OperationFailedException if we failed constructing or sending a
     * SIP Message.
     */
    public void sayError(ServerTransaction serverTransaction,
                         int errorCode,
                         Header header)
        throws OperationFailedException
    {
        Request request = serverTransaction.getRequest();
        Response errorResponse = null;
        try
        {
            errorResponse = getMessageFactory().createResponse(
                            errorCode, request);

            if(header != null)
                errorResponse.setHeader(header);

            //we used to be adding a To tag here and we shouldn't. 3261 says:
            //"Dialogs are created through [...] non-failure responses". and
            //we are using this method for failure responses only.

        }
        catch (ParseException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to construct an OK response to an INVITE request",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }

        try
        {
            serverTransaction.sendResponse(errorResponse);
            if (logger.isDebugEnabled())
                logger.debug("sent response: " + errorResponse);
        }
        catch (Exception ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to send an OK response to an INVITE request",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }
    }

    /**
     * Sends a specific <tt>Request</tt> through a given
     * <tt>SipProvider</tt> as part of the conversation associated with a
     * specific <tt>Dialog</tt>.
     *
     * @param sipProvider the <tt>SipProvider</tt> to send the specified
     * request through
     * @param request the <tt>Request</tt> to send through
     * <tt>sipProvider</tt>
     * @param dialog the <tt>Dialog</tt> as part of which the specified
     * <tt>request</tt> is to be sent
     *
     * @throws OperationFailedException if creating a transaction or sending
     * the <tt>request</tt> fails.
     */
    public void sendInDialogRequest(SipProvider sipProvider,
                                    Request request,
                                    Dialog dialog)
        throws OperationFailedException
    {
        ClientTransaction clientTransaction = null;
        try
        {
            clientTransaction = sipProvider.getNewClientTransaction(request);
        }
        catch (TransactionUnavailableException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to create a client transaction for request:\n"
                + request, OperationFailedException.INTERNAL_ERROR, ex, logger);
        }

        try
        {
            dialog.sendRequest(clientTransaction);
        }
        catch (SipException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to send request:\n" + request,
                OperationFailedException.NETWORK_FAILURE,
                ex,
                logger);
        }

        if (logger.isDebugEnabled())
            logger.debug("Sent request:\n" + request);
    }

    /**
     * Returns a List of Strings corresponding to all methods that we have a
     * processor for.
     *
     * @return a List of methods that we support.
     */
    public List<String> getSupportedMethods()
    {
        return new ArrayList<String>(methodProcessors.keySet());
    }

    /**
     * A utility class that allows us to block until we receive a
     * <tt>RegistrationStateChangeEvent</tt> notifying us of an unregistration.
     */
    private static class ShutdownUnregistrationBlockListener
        implements RegistrationStateChangeListener
    {
        /**
         * The list where we store <tt>RegistationState</tt>s received while
         * waiting.
         */
        public List<RegistrationState> collectedNewStates =
                                        new LinkedList<RegistrationState>();

        /**
         * The method would simply register all received events so that they
         * could be available for later inspection by the unit tests. In the
         * case where a registration event notifying us of a completed
         * registration is seen, the method would call notifyAll().
         *
         * @param evt ProviderStatusChangeEvent the event describing the
         * status change.
         */
        public void registrationStateChanged(RegistrationStateChangeEvent evt)
        {
            if (logger.isDebugEnabled())
                logger.debug("Received a RegistrationStateChangeEvent: " + evt);

            collectedNewStates.add(evt.getNewState());

            if (evt.getNewState().equals(RegistrationState.UNREGISTERED))
            {
                if (logger.isDebugEnabled())
                    logger.debug(
                            "We're unregistered and will notify those who wait");
                synchronized (this)
                {
                    notifyAll();
                }
            }
        }

        /**
         * Blocks until an event notifying us of the awaited state change is
         * received or until waitFor milliseconds pass (whichever happens first).
         *
         * @param waitFor the number of milliseconds that we should be waiting
         * for an event before simply bailing out.
         */
        public void waitForEvent(long waitFor)
        {
            if (logger.isTraceEnabled())
                logger.trace("Waiting for a "
                        +"RegistrationStateChangeEvent.UNREGISTERED");

            synchronized (this)
            {
                if (collectedNewStates.contains(
                        RegistrationState.UNREGISTERED))
                {
                    if (logger.isTraceEnabled())
                        logger.trace("Event already received. "
                                + collectedNewStates);
                    return;
                }

                try
                {
                    wait(waitFor);

                    if (collectedNewStates.size() > 0)
                        if (logger.isTraceEnabled())
                            logger.trace(
                                    "Received a RegistrationStateChangeEvent.");
                    else
                        if (logger.isTraceEnabled())
                            logger.trace(
                                    "No RegistrationStateChangeEvent received for "
                                    + waitFor + "ms.");

                }
                catch (InterruptedException ex)
                {
                    if (logger.isDebugEnabled())
                        logger.debug(
                                "Interrupted while waiting for a "
                                +"RegistrationStateChangeEvent"
                                , ex);
                }
            }
        }
    }

    /**
     * Returns the sip protocol icon.
     * @return the sip protocol icon
     */
    public ProtocolIcon getProtocolIcon()
    {
        return protocolIcon;
    }

    /**
     * Returns the current instance of <tt>SipStatusEnum</tt>.
     *
     * @return the current instance of <tt>SipStatusEnum</tt>.
     */
    SipStatusEnum getSipStatusEnum()
    {
        return sipStatusEnum;
    }
    /**
     * Returns the current instance of <tt>SipRegistrarConnection</tt>.
     * @return SipRegistrarConnection
     */
    public SipRegistrarConnection getRegistrarConnection()
    {
        return sipRegistrarConnection;
    }

    /**
     * Parses the the <tt>uriStr</tt> string and returns a JAIN SIP URI.
     *
     * @param uriStr a <tt>String</tt> containing the uri to parse.
     *
     * @return a URI object corresponding to the <tt>uriStr</tt> string.
     * @throws ParseException if uriStr is not properly formatted.
     */
    public Address parseAddressString(String uriStr)
        throws ParseException
    {
        uriStr = uriStr.trim();

        //we don't know how to handle the "tel:" and "callto:" schemes ... or
        // rather we handle them same as sip so replace:
        if(uriStr.toLowerCase().startsWith("tel:"))
            uriStr = "sip:" + uriStr.substring("tel:".length());
        else if(uriStr.toLowerCase().startsWith("callto:"))
            uriStr = "sip:" + uriStr.substring("callto:".length());

        //Handle default domain name (i.e. transform 1234 -> 1234@sip.com)
        //assuming that if no domain name is specified then it should be the
        //same as ours.
        if (uriStr.indexOf('@') == -1)
        {
            //if we have a registrar, then we could append its domain name as
            //default
            SipRegistrarConnection src = sipRegistrarConnection;
            if(src != null && !src.isRegistrarless() )
            {
                uriStr = uriStr + "@"
                    + ((SipURI)src.getAddressOfRecord().getURI()).getHost();
            }

            //else this could only be a host ... but this should work as is.
        }

        //Let's be uri fault tolerant and add the sip: scheme if there is none.
        if (!uriStr.toLowerCase().startsWith("sip:")) //no sip scheme
        {
            uriStr = "sip:" + uriStr;
        }

        Address toAddress = getAddressFactory().createAddress(uriStr);

        return toAddress;
    }

    /**
     * Returns the <tt>InetAddress</tt> that is most likely to be to be used
     * as a next hop when contacting the specified <tt>destination</tt>. This is
     * an utility method that is used whenever we have to choose one of our
     * local addresses to put in the Via, Contact or (in the case of no
     * registrar accounts) From headers. The method also takes into account
     * the existence of an outbound proxy and in that case returns its address
     * as the next hop.
     *
     * @param destination the destination that we would contact.
     *
     * @return the <tt>InetSocketAddress</tt> that is most likely to be to be
     * used as a next hop when contacting the specified <tt>destination</tt>.
     *
     * @throws IllegalArgumentException if <tt>destination</tt> is not a valid
     * host/ip/fqdn
     */
    public InetSocketAddress getIntendedDestination(Address destination)
        throws IllegalArgumentException
    {
        return getIntendedDestination((SipURI)destination.getURI());
    }

    /**
     * Returns the <tt>InetAddress</tt> that is most likely to be to be used
     * as a next hop when contacting the specified <tt>destination</tt>. This is
     * an utility method that is used whenever we have to choose one of our
     * local addresses to put in the Via, Contact or (in the case of no
     * registrar accounts) From headers. The method also takes into account
     * the existence of an outbound proxy and in that case returns its address
     * as the next hop.
     *
     * @param destination the destination that we would contact.
     *
     * @return the <tt>InetSocketAddress</tt> that is most likely to be to be
     * used as a next hop when contacting the specified <tt>destination</tt>.
     *
     * @throws IllegalArgumentException if <tt>destination</tt> is not a valid
     * host/ip/fqdn
     */
    public InetSocketAddress getIntendedDestination(SipURI destination)
        throws IllegalArgumentException
    {
        // Address
        InetSocketAddress destinationInetAddress = null;

        //resolveSipAddress() verifies whether our destination is valid
        //but the destination could only be known to our outbound proxy
        //if we have one. If this is the case replace the destination
        //address with that of the proxy.(report by Dan Bogos)
        InetSocketAddress outboundProxy = connection.getAddress();

        if(outboundProxy != null)
        {
            if (logger.isTraceEnabled())
                logger.trace("Will use proxy address");
            destinationInetAddress = outboundProxy;
        }
        else
        {
            String transport = destination.getTransportParam();
            if (transport == null)
            {
                transport = getDefaultTransport();
            }

            int port = destination.getPort();
            if (port == -1)
            {
                port = ListeningPoint.PORT_5060;
            }

            ProxyConnection tempConn = new AutoProxyConnection(
                (SipAccountIDImpl)getAccountID(),
                destination.getHost(),
                port,
                transport);
            try
            {
                if(tempConn.getNextAddress())
                    destinationInetAddress = tempConn.getAddress();
                else
                    throw new IllegalArgumentException(destination.getHost()
                        + " could not be resolved to an internet address.");
            }
            catch (DnssecException e)
            {
                logger.error("unable to obtain next hop address", e);
            }
        }

        if(logger.isDebugEnabled())
            logger.debug("Returning address " + destinationInetAddress
                 + " for destination " + destination.getHost());

        return destinationInetAddress;
    }

    /**
     * Stops dispatching SIP messages to a SIP protocol provider service
     * once it's been unregistered.
     *
     * @param event the change event in the registration state of a provider.
     */
    public void registrationStateChanged(RegistrationStateChangeEvent event)
    {
        if(event.getNewState() == RegistrationState.UNREGISTERED ||
           event.getNewState() == RegistrationState.CONNECTION_FAILED)
        {
            ProtocolProviderServiceSipImpl listener
                = (ProtocolProviderServiceSipImpl) event.getProvider();
            sipStackSharing.removeSipListener(listener);
            listener.removeRegistrationStateChangeListener(this);
        }
    }

    /**
     * Logs a specific message and associated <tt>Throwable</tt> cause as an
     * error using the current <tt>Logger</tt> and then throws a new
     * <tt>OperationFailedException</tt> with the message, a specific error code
     * and the cause.
     *
     * @param message the message to be logged and then wrapped in a new
     * <tt>OperationFailedException</tt>
     * @param errorCode the error code to be assigned to the new
     * <tt>OperationFailedException</tt>
     * @param cause the <tt>Throwable</tt> that has caused the necessity to log
     * an error and have a new <tt>OperationFailedException</tt> thrown
     * @param logger the logger that we'd like to log the error <tt>message</tt>
     * and <tt>cause</tt>.
     *
     * @throws OperationFailedException the exception that we wanted this method
     * to throw.
     */
    public static void throwOperationFailedException( String    message,
                                                      int       errorCode,
                                                      Throwable cause,
                                                      Logger    logger)
        throws OperationFailedException
    {
        logger.error(message, cause);

        if(cause == null)
            throw new OperationFailedException(message, errorCode);
        else
            throw new OperationFailedException(message, errorCode, cause);
    }

    /**
     * Registers early message processor.
     * @param processor early message processor.
     */
    void addEarlyMessageProcessor(SipMessageProcessor processor)
    {
        synchronized (earlyProcessors)
        {
            if (!earlyProcessors.contains(processor))
            {
                this.earlyProcessors.add(processor);
            }
        }
    }

    /**
     * Removes the early message processor.
     * @param processor early message processor.
     */
    void removeEarlyMessageProcessor(SipMessageProcessor processor)
    {
        synchronized (earlyProcessors)
        {
            this.earlyProcessors.remove(processor);
        }
    }

    /**
     * Early process an incoming message from interested listeners.
     * @param message the message to process.
     */
    void earlyProcessMessage(EventObject message)
    {
        synchronized(earlyProcessors)
        {
            for (SipMessageProcessor listener : earlyProcessors)
            {
                try
                {
                    if(message instanceof RequestEvent)
                        listener.processMessage((RequestEvent)message);
                    else if(message instanceof ResponseEvent)
                        listener.processResponse((ResponseEvent)message, null);
                    else  if(message instanceof TimeoutEvent)
                        listener.processTimeout((TimeoutEvent)message, null);
                }
                catch(Throwable t)
                {
                    logger.error("Error pre-processing message", t);
                }
            }
        }
    }

    /**
     * Finds the next address to retry registering. If doesn't process anything
     * (we have already tried the last one) return false.
     *
     * @return <tt>true</tt> if we triggered new register with next address.
     */
    boolean registerUsingNextAddress()
    {
        if(connection == null)
            return false;

        try
        {
            if(sipRegistrarConnection.isRegistrarless())
            {
                sipRegistrarConnection.setTransport(getDefaultTransport());
                sipRegistrarConnection.register();
                return true;

            }
            else if(connection.getNextAddress())
            {
                sipRegistrarConnection.setTransport(connection.getTransport());
                sipRegistrarConnection.register();
                return true;
            }
        }
        catch (DnssecException e)
        {
            logger.error("DNSSEC failure while getting address for "
                + this, e);
            fireRegistrationStateChanged(
                RegistrationState.REGISTERING,
                RegistrationState.UNREGISTERED,
                RegistrationStateChangeEvent.REASON_USER_REQUEST,
                "Invalid or inaccessible server address.");
            return true;
        }
        catch (Throwable e)
        {
            logger.error("Cannot send register!", e);
            sipRegistrarConnection.setRegistrationState(
                RegistrationState.CONNECTION_FAILED,
                RegistrationStateChangeEvent.REASON_NOT_SPECIFIED,
                "A timeout occurred while trying to connect to the server.");
        }

        // as we reached the last address lets change it to the first one
        // so we don't get stuck to the last one forever, and the next time
        // use again the first one
        connection.reset();
        return false;
    }

    /**
     * If somewhere we got for example timeout of receiving answer to our
     * requests we consider problem with network and notify the provider.
     */
    protected void notifyConnectionFailed()
    {
        if(getRegistrationState().equals(RegistrationState.REGISTERED)
            && sipRegistrarConnection != null)
            sipRegistrarConnection.setRegistrationState(
                RegistrationState.CONNECTION_FAILED,
                RegistrationStateChangeEvent.REASON_NOT_SPECIFIED,
                "A timeout occurred while trying to connect to the server.");

        if(registerUsingNextAddress())
            return;

        // don't alert the user if we're already off
        if (!getRegistrationState().equals(RegistrationState.UNREGISTERED))
        {
            sipRegistrarConnection.setRegistrationState(
                RegistrationState.CONNECTION_FAILED,
                RegistrationStateChangeEvent.REASON_NOT_SPECIFIED,
                "A timeout occurred while trying to connect to the server.");
        }
    }

    /**
     * Determines whether the supplied transport is a known SIP transport method
     *
     * @param transport the SIP transport to check
     * @return True when transport is one of UDP, TCP or TLS.
     */
    public static boolean isValidTransport(String transport)
    {
        return ListeningPoint.UDP.equalsIgnoreCase(transport)
            || ListeningPoint.TLS.equalsIgnoreCase(transport)
            || ListeningPoint.TCP.equalsIgnoreCase(transport);
    }
}
