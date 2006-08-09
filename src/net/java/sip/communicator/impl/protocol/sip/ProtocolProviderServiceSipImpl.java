/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import javax.sip.*;
import javax.sip.message.*;
import javax.sip.header.*;
import javax.sip.address.*;
import net.java.sip.communicator.util.*;

/**
 * A SIP implementation of the Protocol Provider Service.
 * @author Emil Ivov
 */
public class ProtocolProviderServiceSipImpl
  implements ProtocolProviderService
{
    private static final Logger logger =
        Logger.getLogger(ProtocolProviderServiceSipImpl.class);

    /**
     * The hashtable with the operation sets that we support locally.
     */
    private Hashtable supportedOperationSets = new Hashtable();

    /**
     * The identifier of the account that this provider represents.
     */
    private AccountID accountID = null;

    /**
     * We use this to lock access to initialization.
     */
    private Object initializationLock = new Object();

    /**
     * indicates whether or not the provider is initialized and ready for use.
     */
    private boolean isInitialized = false;

    /**
     * A list of all listeners registered for
     * <tt>RegistrationStateChangeEvent</tt>s.
     */
    private List registrationListeners = new ArrayList();

    /**
     * The SipFactory instance used to create the SipStack and the Address
     * Message and Header Factories.
     */
    private SipFactory sipFactory;

    /**
     * The AddressFactory used to create URLs ans Address objects.
     */
    private AddressFactory addressFactory;

    /**
     * The HeaderFactory used to create SIP message headers.
     */
    private HeaderFactory headerFactory;

    /**
     * The Message Factory used to create SIP messages.
     */
    private MessageFactory messageFactory;

    /**
     * The sipStack instance that handles SIP communications.
     */
    private SipStack sipStack;

    /**
     * The default (and currently the only) SIP listening point of the
     * application.
     */
    private ListeningPoint listeningPoint;

    /**
     * The JAIN SIP SipProvider instance.
     */
    public SipProvider sipProvider;


    /**
     * Registers the specified listener with this provider so that it would
     * receive notifications on changes of its state or other properties such
     * as its local address and display name.
     * @param listener the listener to register.
     */
    public void addRegistrationStateChangeListener(
        RegistrationStateChangeListener listener)
    {
        registrationListeners.add(listener);
    }

    /**
     * Creates a RegistrationStateChange event corresponding to the specified
     * old and new jain sip states and notifies all currently registered
     * listeners.
     *
     */
    private void fireRegistrationStateChanged(  )
    {
        RegistrationStateChangeEvent event = null;

        logger.debug("Dispatching " + event + " to "
                     + registrationListeners.size()+ " listeners.");

        for (int i = 0; i < registrationListeners.size(); i++)
        {
            RegistrationStateChangeListener listener
                = (RegistrationStateChangeListener)registrationListeners.get(i);
            listener.registrationStateChanged(event);
        }

        logger.trace("Done.");
    }


    /**
     * Ends the registration of this protocol provider with the current
     * registration service.
     */
    public void unregister()
    {
        /** @todo implement unregister() */
    }

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
        /** @todo implement getRegistrationState() */
        return null;
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
     * Indicates whether or not this provider is registered
     * @return true if the provider is currently registered and false otherwise.
     */
    public boolean isRegistered()
    {
        /** @todo implement isRegistered() */
        return false;
    }

    /**
     * Removes the specified listener.
     * @param listener the listener to remove.
     */
    public void removeRegistrationStateChangeListener(
        RegistrationStateChangeListener listener)
    {
        this.registrationListeners.remove(listener);
    }

    /**
     * Returns an array containing all operation sets supported by the current
     * implementation. When querying this method users must be prepared to
     * receive any sybset of the OperationSet-s defined by this service. They
     * MUST ignore any OperationSet-s that they are not aware of and that may be
     * defined by future version of this service. Such "unknown" OperationSet-s
     * though not encouraged, may also be defined by service implementors.
     *
     * @return a java.util.Map containing instance of all supported operation
     * sets mapped against their class names (e.g.
     * OperationSetPresence.class.getName()) .
     */
    public Map getSupportedOperationSets()
    {
        return supportedOperationSets;
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
     */
    public void register(SecurityAuthority authority)
    {
        /** @todo implement register() */
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
     * @see net.java.sip.communicator.service.protocol.AccountID
     */
    protected void initialize(String    sipAddress,
                              AccountID accountID)
    {
        synchronized (initializationLock)
        {
            this.accountID = accountID;

            sipFactory = SipFactory.getInstance();
            sipFactory.setPathName("gov.nist");
            Properties properties = new Properties();

            //set the proxy
            String proxyAddress
                = (String)accountID.getAccountProperties()
                    .get(ProtocolProviderFactory.PROXY_ADDRESS);

//            if (proxyAddress != null && proxyAddress.trim().length() > 0)
//            {
//                String proxyPortStr
//                    = accountID.getAccountProperties()
//                    .get(ProtocolProviderFactory.PROXY_PORT);
//
//                int proxyPortStr
//
//            //
//                String proxyPort
//                    properties.setProperty("javax.sip.OUTBOUND_PROXY", peerHostPort + "/"
//                                           + transport);
//            }
            // If you want to use UDP then uncomment this.
            properties.setProperty("javax.sip.STACK_NAME", "SIP Communicator:"
                + getAccountID().getAccountUniqueID());

            // NIST SIP specific properties
            properties.setProperty("gov.nist.javax.sip.DEBUG_LOG"
                                   , "shootistdebug.txt");
            properties.setProperty("gov.nist.javax.sip.SERVER_LOG",
                                    "shootistlog.txt");

            // Drop the client connection after we are done with the transaction.
            properties.setProperty("gov.nist.javax.sip.CACHE_CLIENT_CONNECTIONS"
                                   , "false");

            // Log level
            properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "TRACE");

            try
            {
                // Create SipStack object
                gov.nist.javax.sip.SipStackImpl astack = null;
                sipStack = sipFactory.createSipStack(properties);
                logger.debug("Created stack: " + sipStack);
            }
            catch (PeerUnavailableException e)
            {
                // could not find
                // gov.nist.jain.protocol.ip.sip.SipStackImpl
                // in the classpath
                logger.fatal("Failed to initialize SIP Stack.", e);
            }

            try
            {
                headerFactory = sipFactory.createHeaderFactory();
                addressFactory = sipFactory.createAddressFactory();
                messageFactory = sipFactory.createMessageFactory();
                //    udpListeningPoint = sipStack.createListeningPoint("127.0.0.1",
                //        5060, "udp");
                //    sipProvider = sipStack.createSipProvider(udpListeningPoint);
                //    sipProvider.addSipListener(null);
            }
            catch (Throwable t)
            {}

            isInitialized = true;
        }
    }

    /**
     * Makes the service implementation close all open sockets and release
     * any resources that it might have taken and prepare for shutdown/garbage
     * collection.
     */
    public void shutdown()
    {
    }
}
