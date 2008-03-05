/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.zeroconf;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * An implementation of the protocol provider service over the Zeroconf protocol
 *
 * @author Christian Vincenot
 * @author Maxime Catelin
 */
public class ProtocolProviderServiceZeroconfImpl
    implements ProtocolProviderService
{
    private static final Logger logger =
        Logger.getLogger(ProtocolProviderServiceZeroconfImpl.class);
        
    /**
     * The hashtable with the operation sets that we support locally.
     */
    private Hashtable supportedOperationSets = new Hashtable();
    
    /**
     * A list of all listeners registered for
     * <tt>RegistrationStateChangeEvent</tt>s.
     */
    private Vector registrationStateListeners = new Vector();
    
    /**
     * We use this to lock access to initialization.
     */
    private Object initializationLock = new Object();
    
    /**
     * The id of the account that this protocol provider represents.
     */
    private AccountID accountID = null;
    
    /**
     * Indicates whether or not the provider is initialized and ready for use.
     */
    private boolean isInitialized = false;
    
    /**
     * The logo corresponding to the zeroconf protocol.
     */
    private ProtocolIconZeroconfImpl zeroconfIcon
        = new ProtocolIconZeroconfImpl();
    
    /**
     * The registration state that we are currently in. Note that in a real
     * world protocol implementation this field won't exist and the registration
     * state would be retrieved from the protocol stack.
     */
    private RegistrationState currentRegistrationState
        = RegistrationState.UNREGISTERED;
    
    /**
     * The BonjourService corresponding to this ProtocolProviderService
     */
    
    private BonjourService bonjourService;
    
    /**
     * The default constructor for the Zeroconf protocol provider.
     */
    public ProtocolProviderServiceZeroconfImpl()
    {
        logger.trace("Creating a zeroconf provider.");
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
     * Returns the Bonjour Service that handles the Bonjour protocol stack.
     * 
     *@return the Bonjour Service linked with this Protocol Provider
     */
    public BonjourService getBonjourService() 
    {
        return bonjourService;
    }
    
    
    /**
     * Initializes the service implementation, and puts it in a sate where it
     * could interoperate with other services. It is strongly recomended that
     * properties in this Map be mapped to property names as specified by
     * <tt>AccountProperties</tt>.
     *
     * @param userID the user id of the zeroconf account we're currently
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
            OperationSetPersistentPresenceZeroconfImpl persistentPresence =
                new OperationSetPersistentPresenceZeroconfImpl(this);

            supportedOperationSets.put(
                OperationSetPersistentPresence.class.getName(),
                persistentPresence);


            //register it once again for those that simply need presence and
            //won't be smart enough to check for a persistent presence
            //alternative
            supportedOperationSets.put( OperationSetPresence.class.getName(),
                                        persistentPresence);

            //initialize the IM operation set
            OperationSetBasicInstantMessagingZeroconfImpl basicInstantMessaging
                = new OperationSetBasicInstantMessagingZeroconfImpl(
                    this, persistentPresence);

            supportedOperationSets.put(
                OperationSetBasicInstantMessaging.class.getName(),
                basicInstantMessaging);

            //initialize the typing notifications operation set
            OperationSetTypingNotifications typingNotifications =
                new OperationSetTypingNotificationsZeroconfImpl(
                        this, persistentPresence);

            supportedOperationSets.put(
                OperationSetTypingNotifications.class.getName(),
                typingNotifications);

            isInitialized = true;
            
        }
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
        return ProtocolNames.ZEROCONF;
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
        return ProtocolNames.ZEROCONF;
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
        //we don't  need a password here since there's no server in
        //zeroconf.

        RegistrationState oldState = currentRegistrationState;
        currentRegistrationState = RegistrationState.REGISTERED;

        
        //ICI : creer le service Zeroconf !!
        logger.info("ZEROCONF: Starting the service");
        ZeroconfAccountID acc = (ZeroconfAccountID)accountID;
        this.bonjourService = new BonjourService(5298, this);
        
        //bonjourService.changeStatus(ZeroconfStatusEnum.ONLINE);
        
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
        logger.trace("Killing the Zeroconf Protocol Provider.");

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

        bonjourService.shutdown();
        
        fireRegistrationStateChanged(
            oldState
            , currentRegistrationState
            , RegistrationStateChangeEvent.REASON_USER_REQUEST
            , null);
    }
    
    /**
     * Returns the zeroconf protocol icon.
     * @return the zeroconf protocol icon
     */
    public ProtocolIcon getProtocolIcon()
    {           
        return zeroconfIcon;
    }
}
