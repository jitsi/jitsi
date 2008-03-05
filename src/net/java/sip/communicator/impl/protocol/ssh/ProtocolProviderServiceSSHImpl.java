/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 *
 * ProtocolProviderServiceSSHImpl.java
 *
 * SSH Suport in SIP Communicator - GSoC' 07 Project
 *
 */

package net.java.sip.communicator.impl.protocol.ssh;

import java.io.*;
import java.util.*;
import com.jcraft.jsch.*;
import javax.swing.*;

import org.osgi.framework.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.service.gui.*;

/**
 * A SSH implementation of the ProtocolProviderService.
 *
 * @author Shobhit Jindal
 */
public class ProtocolProviderServiceSSHImpl
        implements ProtocolProviderService
{
    private static final Logger logger
            = Logger.getLogger(ProtocolProviderServiceSSHImpl.class);
    
    /**
     * The name of this protocol.
     */
    public static final String SSH_PROTOCOL_NAME = ProtocolNames.SSH;
    
//    /**
//     * The identifier for SSH Stack
//     * Java Secure Channel JSch
//     */
//    JSch jsch = new JSch();
    
    /**
     * The test command given after each command to determine the reply length 
     * of the command
     */
    private final String testCommand = Resources.getString("testCommand");
    
    /**
     * A reference to the protocol provider of UIService
     */
    private static ServiceReference ppUIServiceRef;
    
    /**
     * Connection timeout to a remote server in milliseconds
     */
    private static int connectionTimeout = 30000;
    
    /**
     * A reference to UI Service
     */
    private static UIService uiService;
    
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
    private final Hashtable supportedOperationSets = new Hashtable();
    
    private OperationSetBasicInstantMessagingSSHImpl basicInstantMessaging;
    
    private OperationSetFileTransferSSHImpl fileTranfer;
    
    /**
     * A list of listeners interested in changes in our registration state.
     */
    private Vector registrationStateListeners = new Vector();
    
    /**
     * Indicates whether or not the provider is initialized and ready for use.
     */
    private boolean isInitialized = false;
    
    /**
     * The logo corresponding to the ssh protocol.
     */
    private ProtocolIconSSHImpl sshIcon
            = new ProtocolIconSSHImpl();
    
    /**
     * The registration state of SSH Provider is taken to be registered by
     * default as it doesn't correspond to the state on remote server
     */
    private RegistrationState currentRegistrationState
            = RegistrationState.REGISTERED;
    
    /**
     * The default constructor for the SSH protocol provider.
     */
    public ProtocolProviderServiceSSHImpl()
    {
        logger.trace("Creating a ssh provider.");
        
        try
        {
            // converting to milliseconds
            connectionTimeout = Integer.parseInt(Resources.getString(
                                    "connectionTimeout")) * 1000;
        }
        catch(NumberFormatException ex)
        {
            logger.error("Connection Timeout set to 30 seconds");
        }
    }
    
    /**
     * Initializes the service implementation, and puts it in a sate where it
     * could interoperate with other services. It is strongly recomended that
     * properties in this Map be mapped to property names as specified by
     * <tt>AccountProperties</tt>.
     *
     * @param userID the user id of the ssh account we're currently
     * initializing
     * @param accountID the identifier of the account that this protocol
     * provider represents.
     *
     * @see net.java.sip.communicator.service.protocol.AccountID
     */
    protected void initialize(
            String userID,
            AccountID accountID)
    {
        synchronized(initializationLock)
        {
            this.accountID = accountID;
            
            //initialize the presence operationset
            OperationSetPersistentPresenceSSHImpl persistentPresence =
                    new OperationSetPersistentPresenceSSHImpl(this);
            
            supportedOperationSets.put(
                    OperationSetPersistentPresence.class.getName(),
                    persistentPresence);
            
            //register it once again for those that simply need presence and
            //won't be smart enough to check for a persistent presence
            //alternative
            supportedOperationSets.put(
                    OperationSetPresence.class.getName(),
                    persistentPresence);
            
            //initialize the IM operation set
            basicInstantMessaging = new 
                OperationSetBasicInstantMessagingSSHImpl(
                    this);
            
            supportedOperationSets.put(
                    OperationSetBasicInstantMessaging.class.getName(),
                    basicInstantMessaging);
            
            //initialze the file transfer operation set
            fileTranfer = new OperationSetFileTransferSSHImpl(
                    this);
            
            supportedOperationSets.put(
                    OperationSetFileTransfer.class.getName(),
                    fileTranfer);
            
            isInitialized = true;
        }
    }
    
    /**
     * Determines whether a vaild session exists for the contact of remote
     * machine.
     *
     * @param sshContact ID of SSH Contact
     *
     * @return <tt>true</tt> if the session is connected
     *         <tt>false</tt> otherwise
     */
    public boolean isSessionValid(ContactSSH sshContact)
    {
        Session sshSession = sshContact.getSSHSession();
        if( sshSession != null)
            if(sshSession.isConnected())
                return true;
        
        // remove reference to an unconnected SSH Session, if any
        sshContact.setSSHSession(null);
        return false;
    }
    
    /**
     * Determines whether the contact is connected to shell of remote machine
     * as a precheck for any further operation
     *
     * @param sshContact ID of SSH Contact
     *
     * @return <tt>true</tt> if the contact is connected
     *         <tt>false</tt> if the contact is not connected
     */
    public boolean isShellConnected(ContactSSH sshContact)
    {
        // a test command may also be run here
        
        if(isSessionValid(sshContact))
        {
            return(sshContact.getShellChannel() != null);
        }
        
        /*
         * Above should be return(sshContact.getShellChannel() != null
         *                     && sshContact.getShellChannel().isConnected());
         *
         * but incorrect reply from stack for isConnected()
         */
        
        return false;
    }
    
    /**
     * Creates a shell channel to the remote machine
     * a new jsch session is also created if the current one is invalid
     *
     * @param sshContact the contact of the remote machine
     * @param firstMessage the first message
     */
    public void connectShell(
            final ContactSSH sshContact,
            final Message firstMessage)
    {
        sshContact.setConnectionInProgress(true);
        
        final UIService uiService = this.uiService;
        
        final Thread newConnection = new Thread((new Runnable()
        {
            public void run()
            {
                OperationSetPersistentPresenceSSHImpl persistentPresence
                        = (OperationSetPersistentPresenceSSHImpl)sshContact
                        .getParentPresenceOperationSet();
                
                persistentPresence.changeContactPresenceStatus(
                        sshContact,
                        SSHStatusEnum.CONNECTING);
                
                try
                {
                    if(!isSessionValid(sshContact))
                        createSSHSessionAndLogin(sshContact);
                    
                    createShellChannel(sshContact);
                    
                    //initalizing the reader and writers of ssh contact
                    
                    persistentPresence.changeContactPresenceStatus(
                            sshContact,
                            SSHStatusEnum.CONNECTED);
                    
                    showWelcomeMessage(sshContact);
                    
                    sshContact.setMessageType(sshContact
                            .CONVERSATION_MESSAGE_RECEIVED);
                    
                    sshContact.setConnectionInProgress(false);
                    
                    Thread.sleep(1500);
                    
                    sshContact.setCommandSent(true);
                    
                    basicInstantMessaging.sendInstantMessage(
                            sshContact,
                            firstMessage);
                }
                // rigoruos Exception Checking in future
                catch (Exception ex)
                {
                    persistentPresence.changeContactPresenceStatus(
                            sshContact,
                            SSHStatusEnum.NOT_AVAILABLE);
                    
                    ex.printStackTrace();
                }
                finally
                {
                    sshContact.setConnectionInProgress(false);
                }
            }
        }));
        
        newConnection.start();
    }
    
    /**
     * Creates a channel for shell type in the current session
     * channel types = shell, sftp, exec(X forwarding),
     *                 direct-tcpip(stream forwarding) etc
     *
     * @param sshContact ID of SSH Contact
     *
     */
    public void createShellChannel(ContactSSH sshContact)
    throws IOException
    {
        try
        {
            Channel shellChannel = sshContact.getSSHSession()
                .openChannel("shell");
            
            //initalizing the reader and writers of ssh contact
            sshContact.initializeShellIO(shellChannel.getInputStream(),
                    shellChannel.getOutputStream());
            
            ((ChannelShell)shellChannel).setPtyType(
                    sshContact.getSSHConfigurationForm().getTerminalType());
            
            //initializing the shell
            shellChannel.connect(1000);
            
            sshContact.setShellChannel(shellChannel);
            
            sshContact.sendLine("export PS1=");
        }
        catch (JSchException ex)
        {
            sshContact.setSSHSession(null);
            throw new IOException("Unable to create shell channel to remote" +
                    " server");
        }
    }
    
    /**
     * Closes the Shell channel are associated IO Streams
     *
     * @param sshContact ID of SSH Contact
     */
    public void closeShellChannel(ContactSSH sshContact) throws
            JSchException,
            IOException
    {
        sshContact.closeShellIO();
        sshContact.getShellChannel().disconnect();
        sshContact.setShellChannel(null);
    }
    
    /**
     * Creates a SSH Session with a remote machine and tries to login
     * according to the details specified by Contact
     * An appropriate message is shown to the end user in case the login fails
     *
     * @param sshContact ID of SSH Contact
     *
     * @throws JSchException if a JSch is unable to create a SSH Session with 
     * the remote machine
     * @throws InterruptedException if the thread is interrupted before session
     *         connected or is timed out
     * @throws OperationFailedException if not of above reasons :-)
     */
    public void createSSHSessionAndLogin(ContactSSH sshContact) throws
            JSchException,
            OperationFailedException,
            InterruptedException
    {
        logger.info("Creating a new SSH Session to "
                + sshContact.getHostName());
        
        // creating a new JSch Stack identifier for contact
        JSch jsch = new JSch();
        
        String knownHosts = (String)accountID
                .getAccountProperties().get("KNOWN_HOSTS_FILE");
        
        if(!knownHosts.equals("Optional"))
            jsch.setKnownHosts(knownHosts);
        
        String identitiyKey = (String)accountID
                .getAccountProperties().get("IDENTITY_FILE");
        
        String userName = sshContact.getUserName();
        
        // use the name of system user if the contact has not supplied SSH
        // details
        if(userName.equals(""))
            userName = System.getProperty("user.name");
        
        if(!identitiyKey.equals("Optional"))
            jsch.addIdentity(identitiyKey);

        // creating a new session for the contact
        Session session = jsch.getSession(
                userName,
                sshContact.getHostName(),
                sshContact.getSSHConfigurationForm().getPort());
        
        /**
         * Creating and associating User Info with the session
         * User Info passes authentication from sshContact to SSH Stack
         */
        SSHUserInfo sshUserInfo = new SSHUserInfo(sshContact);
        
        session.setUserInfo(sshUserInfo);
        
        /**
         * initializing the session
         */
        session.connect(connectionTimeout);
        
        int count = 0;
        
        // wait for session to get connected
        while(!session.isConnected() && count<=30000)
        {
            Thread.sleep(1000);
            count += 1000;
            logger.trace("SSH:" + sshContact.getHostName()
                    + ": Sleep zzz .. " );
        }
        
        // if timeout have exceeded
        if(count>30000)
        {
            sshContact.setSSHSession(null);
            JOptionPane.showMessageDialog(
                    null,
                    "SSH Connection attempt to "
                    + sshContact.getHostName() + " timed out");
            
            // error codes are not defined yet
            throw new OperationFailedException("SSH Connection attempt to " +
                    sshContact.getHostName() + " timed out", 2);
        }
        
        sshContact.setJSch(jsch);
        sshContact.setSSHSession(session);
        
        logger.info("A new SSH Session to " + sshContact.getHostName()
                + " Created");
    }
    
    /**
     * Closes the SSH Session associated with the contact
     *
     * @param sshContact ID of SSH Contact
     */
    void closeSSHSession(ContactSSH sshContact)
    {
        sshContact.getSSHSession().disconnect();
        sshContact.setSSHSession(null);
    }
    
    /**
     * Presents the login welcome message to user
     *
     * @param sshContact ID of SSH Contact
     */
    public void showWelcomeMessage(ContactSSH sshContact)
    throws IOException
    {
/*      //sending the command
        sshContact.sendLine(testCommand);

        String reply = "", line = "";

        // message is extracted until the test Command ie echoed back
        while(line.indexOf(testCommand) == -1)
        {
            reply += line + "\n";
            line = sshContact.getLine();
        }

        uiService.getPopupDialog().showMessagePopupDialog
                (reply,"Message from " + sshContact.getDisplayName(),
                uiService.getPopupDialog().INFORMATION_MESSAGE);

        if(line.startsWith(testCommand))
            while(!sshContact.getLine().contains(testCommand));

        //one line output of testCommand
        sshContact.getLine();
*/
        logger.debug("SSH: Welcome message shown");
    }
    
    /**
     * Returns a reference to UIServce for accessing UI related services
     *
     * @return uiService a reference to UIService
     */
    public static UIService getUIService()
    {
        return uiService;
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
    private void fireRegistrationStateChanged(
            RegistrationState oldState,
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
        return SSH_PROTOCOL_NAME;
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
        return SSH_PROTOCOL_NAME;
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
        
        //get a reference to UI Service via its Service Reference
        ppUIServiceRef = SSHActivator.getBundleContext()
            .getServiceReference(UIService.class.getName());
        
        uiService = (UIService)SSHActivator.getBundleContext()
            .getService(ppUIServiceRef);
        
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
        logger.trace("Killing the SSH Protocol Provider.");
        
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
    
    /**
     * Returns the ssh protocol icon.
     * @return the ssh protocol icon
     */
    public ProtocolIcon getProtocolIcon()
    {
        return sshIcon;
    }
}
