/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.dict;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.version.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * A Dict implementation of the ProtocolProviderService.
 *
 * @author ROTH Damien
 * @author LITZELMANN Cedric
 */
public class ProtocolProviderServiceDictImpl
    implements ProtocolProviderService
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
    private AccountID accountID = null;

    /**
     * We use this to lock access to initialization.
     */
    private Object initializationLock = new Object();

    /**
     * The hashtable with the operation sets that we support locally.
     */
    private Hashtable supportedOperationSets = new Hashtable();

    /**
     * A list of listeners interested in changes in our registration state.
     */
    private Vector registrationStateListeners = new Vector();

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
     * The default constructor for the Dict protocol provider.
     */
    public ProtocolProviderServiceDictImpl()
    {
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
            this.accountID = accountID;

            //initialize the presence operationset
            OperationSetPersistentPresenceDictImpl persistentPresence =
                new OperationSetPersistentPresenceDictImpl(this);

            supportedOperationSets.put(
                OperationSetPersistentPresence.class.getName(),
                persistentPresence);


            //register it once again for those that simply need presence and
            //won't be smart enough to check for a persistent presence
            //alternative
            supportedOperationSets.put( OperationSetPresence.class.getName(),
                                        persistentPresence);

            //initialize the IM operation set
            OperationSetBasicInstantMessagingDictImpl basicInstantMessaging
                = new OperationSetBasicInstantMessagingDictImpl(
                    this
                    , (OperationSetPersistentPresenceDictImpl)
                            persistentPresence);

            supportedOperationSets.put(
                OperationSetBasicInstantMessaging.class.getName(),
                basicInstantMessaging);

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
     * Registers the specified listener with this provider so that it would
     * receive notifications on changes of its state or other properties such
     * as its local address and display name.
     *
     * @param listener the listener to register.
     */
    public void addRegistrationStateChangeListener(
        RegistrationStateChangeListener listener)
    {
        synchronized(registrationStateListeners)
        {
            if (!registrationStateListeners.contains(listener))
                registrationStateListeners.add(listener);
        }

    }

    /**
     * Removes the specified registration listener so that it won't receive
     * further notifications when our registration state changes.
     *
     * @param listener the listener to remove.
     */
    public void removeRegistrationStateChangeListener(
        RegistrationStateChangeListener listener)
    {
        synchronized(registrationStateListeners)
        {
            registrationStateListeners.remove(listener);
        }
    }

    /**
     * Creates a <tt>RegistrationStateChangeEvent</tt> corresponding to the
     * specified old and new states and notifies all currently registered
     * listeners.
     *
     * @param oldState the state that the provider had before the change
     * occurred
     * @param newState the state that the provider is currently in.
     * @param reasonCode a value corresponding to one of the REASON_XXX fields
     * of the RegistrationStateChangeEvent class, indicating the reason for
     * this state transition.
     * @param reason a String further explaining the reason code or null if
     * no such explanation is necessary.
     */
    private void fireRegistrationStateChanged( RegistrationState oldState,
                                               RegistrationState newState,
                                               int               reasonCode,
                                               String            reason)
    {
        RegistrationStateChangeEvent event =
            new RegistrationStateChangeEvent(
                            this, oldState, newState, reasonCode, reason);

        logger.debug("Dispatching " + event + " to "
                     + registrationStateListeners.size()+ " listeners.");

        Iterator listeners = null;
        synchronized (registrationStateListeners)
        {
            listeners = new ArrayList(registrationStateListeners).iterator();
        }

        while (listeners.hasNext())
        {
            RegistrationStateChangeListener listener
                = (RegistrationStateChangeListener) listeners.next();

            listener.registrationStateChanged(event);
        }

        logger.trace("Done.");
    }

    /**
     * Retrieve the DictAdapter linked with the account. If there is no instance, one is created
     * @return a DictAdapter instance
     */
    public DictAdapter getDictAdapter()
    {
        String host = (String) this.accountID.getAccountProperties()
                .get(ProtocolProviderFactory.SERVER_ADDRESS);
        int port = Integer.parseInt((String) this.accountID.getAccountProperties()
                .get(ProtocolProviderFactory.SERVER_PORT));
        String strategy = (String) this.accountID.getAccountProperties()
                .get(ProtocolProviderFactory.STRATEGY);
        
        String key = this.accountID.getUserID();
        DictAdapter result = DictRegistry.get(key);
        
        if (!(result instanceof DictAdapter))
        {
            result = new DictAdapter(host, port, strategy);
            
            
            // Set the clientname from the current version 
            BundleContext bundleContext = DictActivator.getBundleContext();
            ServiceReference versionServRef = bundleContext
                .getServiceReference(VersionService.class.getName());
            
            VersionService versionService = (VersionService) bundleContext
                .getService(versionServRef);
            
            result.setClientName(versionService.getCurrentVersion().toString());
            
            // Store the DictAdapter
            DictRegistry.put(key, result);
        }
        
        return result;
    }
    
    /**
     * Close the DictAdapter linked with the account ID
     */
    public void closeDictAdapter()
    {
        String key = this.accountID.getUserID();
        DictAdapter result = DictRegistry.get(key);
        
        if ((result instanceof DictAdapter))
        {
            try
            {
                result.close();
                DictRegistry.remove(key);
            }
            catch (Exception ex)
            {
                logger.error(ex);
            }
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
     * Returns the operation set corresponding to the specified class or null
     * if this operation set is not supported by the provider implementation.
     *
     * @param opsetClass the <tt>Class</tt> of the operation set that we're
     *   looking for.
     * @return returns an OperationSet of the specified <tt>Class</tt> if
     *   the undelying implementation supports it or null otherwise.
     */
    public OperationSet getOperationSet(Class opsetClass)
    {
        return (OperationSet) getSupportedOperationSets()
            .get(opsetClass.getName());
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
     * Returns the protocol display name. This is the name that would be used
     * by the GUI to display the protocol name.
     * 
     * @return a String containing the display name of the protocol this service
     * is implementing
     */
    public String getProtocolDisplayName()
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
     * Returns an array containing all operation sets supported by the
     * current implementation.
     *
     * @return a java.util.Map containing instance of all supported
     *   operation sets mapped against their class names (e.g.
     *   OperationSetPresence.class.getName()) .
     */
    public Map getSupportedOperationSets()
    {
        //Copy the map so that the caller is not able to modify it.
        return (Map)supportedOperationSets.clone();
    }

    /**
     * Indicates whether or not this provider is registered
     *
     * @return true if the provider is currently registered and false
     *   otherwise.
     */
    public boolean isRegistered()
    {
        return currentRegistrationState.equals(RegistrationState.REGISTERED);
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
        logger.trace("Killing the Dict Protocol Provider for account "
                + this.accountID.getUserID());
        this.closeDictAdapter();

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
}
