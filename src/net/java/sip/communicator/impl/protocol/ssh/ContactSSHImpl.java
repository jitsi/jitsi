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
package net.java.sip.communicator.impl.protocol.ssh;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.Base64; // disambiguation
import net.java.sip.communicator.util.Logger;

import com.jcraft.jsch.*;
// disambiguation

/**
 * A Contact of SSH Type
 *
 * @author Shobhit Jindal
 */
public class ContactSSHImpl
    extends AbstractContact
    implements ContactSSH
{
    private static final Logger logger
            = Logger.getLogger(ContactSSHImpl.class);

    /**
     * This acts as a separator between details stored in persistent data
     */
    private final String separator =
        Resources.getString("impl.protocol.ssh.DETAILS_SEPARATOR");

    /**
     * The identifier for SSH Stack
     * Java Secure Channel JSch
     */
    private JSch jsch;

    /**
     * Interface for user to provide details about machine
     */
    private SSHContactInfo sshConfigurationForm;

    /**
     * A Timer Daemon to update the status of this contact
     */
    private Timer timer = new Timer(true);

    /**
     * A Daemon to retrieve and fire messages received from remote machine
     */
    private SSHReaderDaemon contactSSHReaderDaemon;

    /**
     * The id of the contact.
     */
    private String contactID = null;

    /**
     * The persistentData of the contact.
     */
    private String persistentData = null;

//    /**
//     * This stores the prompt string of shell
//     */
//    private String sshPrompt;

    /**
     * The provider that created us.
     */
    private ProtocolProviderServiceSSHImpl parentProvider = null;

    /**
     * The identifier of the type of message received from server
     */
    private int messageType;



    /**
     * The identifier for SSH Session with the remote server
     */
    private Session sshSession = null;

    /**
     * The identifier for a sshShellChannel with the remote server is of type
     * shell - for an interactive SSH Session with the remote machine
     *
     * Other types
     * sftp - to tranfer files from/to the remote machine
     * exec - X forwarding
     * direct-tcpip - stream forwarding
     */
    private Channel sshShellChannel = null;

    /**
     * The identifier for the Shell Input Stream associated with SSH Sesion
     */
    private InputStream shellInputStream = null;

    /**
     * The identifier for the Shell Output Stream associated with SSH Sesion
     */
    private OutputStream shellOutputStream = null;

    /**
     * Higher wrapper for shellInputStream
     */
    private InputStreamReader shellReader = null;

    /**
     * Higher wrapper for shellOutputStream
     */
    private PrintWriter shellWriter = null;

    /**
     * The group that belong to.
     */
    private ContactGroupSSHImpl parentGroup = null;

    /**
     * The presence status of the contact.
     */
    private PresenceStatus presenceStatus = SSHStatusEnum.NOT_AVAILABLE;

    /**
     * Determines whether this contact is persistent, i.e. member of the contact
     * list or whether it is here only temporarily.
     */
    private boolean isPersistent = false;

    /**
     * Determines whether the contact has been resolved (i.e. we have a
     * confirmation that it is still on the server contact list).
     */
    private boolean isResolved = true;

    /**
     * Determines whether an connection attempt to remote server is already
     * underway
     */
    private boolean isConnectionInProgress = false;

    /**
     * Determines whether the message received from remote machine is as a
     * result of command sent to it
     */
    private boolean commandSent = false;

    /**
     * A lock to synchronize the access of commandSent boolean object
     * with the reader thread.
     */
    private final Object lock = new Object();

    /**
     * Creates an instance of a meta contact with the specified string used
     * as a name and identifier.
     *
     * @param id the identifier of this contact (also used as a name).
     * @param parentProvider the provider that created us.
     */
    public ContactSSHImpl(
            String id,
            ProtocolProviderServiceSSHImpl parentProvider)
    {
        this.contactID = id;
        this.parentProvider = parentProvider;

        this.sshConfigurationForm =
                new SSHContactInfo(this);

        this.savePersistentDetails();
    }

    /**
     * Initializes the reader and writers associated with shell of this contact
     *
     * @param shellInputStream The InputStream of stack
     * @param shellOutputStream The OutputStream of stack
     */
    public void initializeShellIO(
            InputStream shellInputStream,
            OutputStream shellOutputStream)
    {
        this.shellInputStream = shellInputStream;
        this.shellOutputStream = shellOutputStream;
        shellReader = new InputStreamReader(shellInputStream);
        shellWriter = new PrintWriter(shellOutputStream);

        contactSSHReaderDaemon = new SSHReaderDaemon(this);
        contactSSHReaderDaemon.setDaemon(true);
        contactSSHReaderDaemon.isActive(true);
        contactSSHReaderDaemon.start();
    }

    /**
     * Closes the readers and writer associated with shell of this contact
     */
    public void closeShellIO()
    {
        try
        {
            shellReader.close();
            shellInputStream.close();
        }
        catch(IOException ex)
        {}

        try
        {
            shellWriter.close();
            shellOutputStream.close();
        }
        catch(IOException ex)
        {}

        shellInputStream = null;

        shellReader = null;

        shellOutputStream = null;

        shellWriter = null;

        try
        {
            sshShellChannel.disconnect();
        }
        catch(Exception e)
        {}

        // Removing the reference to current channel
        // a new shell channel will be created for the next message
        sshShellChannel = null;

        // remove the reference of session if it were also disconnected
        // like in the case of exit command
        if(!sshSession.isConnected())
        {
            sshSession = null;
            jsch = null;
        }

        ((OperationSetPersistentPresenceSSHImpl)
        getParentPresenceOperationSet()).
                changeContactPresenceStatus(this, SSHStatusEnum.ONLINE);
    }

    /**
     * Sends a message a line to remote machine via the Shell Writer
     *
     * @param message to be sent
     */
    public void sendLine(String message)
    throws IOException
    {
//        logger.debug("SSH TO: " + this.contactID + ": " +  message);
        shellWriter.println(message);
        shellWriter.flush();
    }

    /**
     * Reads a line from the remote machine via the Shell Reader
     *
     * @return message read
     */
//    public String getLine()
//        throws IOException
//    {
//        String line = shellReader.readLine();
////        logger.debug("SSH FROM: " + this.contactID + ": " +  line);
//
//        // null is never returned normally, the reading attempt returs a
//        // string
//        // or blocks until one line is available
//        if(line == null)
//        {
//            sshShellChannel.disconnect();
//            sshShellChannel = null;
//            sshSession = null;
//            throw(new IOException("Unexpected Reply from remote Server"));
//        }
//        return line;
//    }

    /**
     * Starts the timer and its task to periodically update the status of
     * remote machine
     */
    public void startTimerTask()
    {
        timer.scheduleAtFixedRate(new ContactTimerSSHImpl(this),
                2000, sshConfigurationForm.getUpdateInterval()*1000);
    }

    /**
     * Stops the timer and its task to stop updating the status of
     * remote machine
     */
    public void stopTimerTask()
    {
        timer.cancel();
    }


    /**
     * Saves the details of contact in persistentData seperated by
     * separator
     * Passowrd is saved unsecurely using Base64 encoding
     */
    public void savePersistentDetails()
    {
        persistentData =
                this.sshConfigurationForm.getHostName() +
                separator +
                this.sshConfigurationForm.getUserName() +
                separator +
                new String(Base64.encode(this.sshConfigurationForm.getPassword()
                        .getBytes())) +
                separator + sshConfigurationForm.getPort() +
                separator +
                sshConfigurationForm.getTerminalType() +
                separator +
                sshConfigurationForm.getUpdateInterval();
    }

    /**
     * Stores persistent data in fields of the contact seperated by
     * separator.
     *
     * @param persistentData of the contact
     */
    public void setPersistentData(String persistentData)
    {
        try
        {
            this.persistentData = persistentData;
            int firstCommaIndex = this.persistentData.indexOf(separator);
            int secondCommaIndex = this.persistentData.indexOf(separator,
                    firstCommaIndex +1);
            int thirdCommaIndex = this.persistentData.indexOf(separator,
                    secondCommaIndex +1);
            int fourthCommaIndex = this.persistentData.indexOf(separator,
                    thirdCommaIndex +1);
            int fifthCommaIndex = this.persistentData.indexOf(separator,
                    fourthCommaIndex +1);

            if (logger.isDebugEnabled())
                logger.debug("Commas: " + firstCommaIndex + " " + secondCommaIndex + " "
                    + thirdCommaIndex + " " +fourthCommaIndex + " "
                    +fifthCommaIndex);

            this.sshConfigurationForm.setHostNameField(
                    this.persistentData.substring(0,firstCommaIndex));

            this.sshConfigurationForm.setUserNameField(
                    this.persistentData.substring(firstCommaIndex+1,
                            secondCommaIndex));

            if( (thirdCommaIndex - secondCommaIndex) > 1)
            {
                if(this.persistentData.substring(secondCommaIndex+1).length()>0)
                    this.sshConfigurationForm.setPasswordField(
                            new String(Base64.decode(this.persistentData
                            .substring(secondCommaIndex+1, thirdCommaIndex))));
            }


            this.sshConfigurationForm.setPort(
                    this.persistentData.substring(thirdCommaIndex + 1,
                            fourthCommaIndex));

            this.sshConfigurationForm.setTerminalType(
                    this.persistentData.substring(fourthCommaIndex + 1,
                            fifthCommaIndex));

            this.sshConfigurationForm.setUpdateInterval(
                Integer.parseInt(this.persistentData.substring(fifthCommaIndex+1)));
        }
        catch(Exception ex)
        {
            logger.error("Error setting persistent data!", ex);
        }
    }

    /**
     * Determines whether a connection to a remote server is already underway
     *
     * @return isConnectionInProgress
     */
    public boolean isConnectionInProgress()
    {
        return this.isConnectionInProgress;
    }

    /**
     * Sets the status of connection attempt to remote server
     * This method is synchronized
     *
     * @param isConnectionInProgress
     */
    public synchronized void setConnectionInProgress(
            boolean isConnectionInProgress)
    {
        this.isConnectionInProgress = isConnectionInProgress;
    }

    /**
     * Returns the SSHContactInfo associated with this contact
     *
     * @return sshConfigurationForm
     */
    public SSHContactInfo getSSHConfigurationForm()
    {
        return this.sshConfigurationForm;
    }

    /**
     * Returns the JSch Stack identified associated with this contact
     *
     * @return jsch
     */
    public JSch getJSch()
    {
        return this.jsch;
    }

    /**
     * Sets the JSch Stack identified associated with this contact
     *
     * @param jsch to be associated
     */
    public void setJSch(JSch jsch)
    {
        this.jsch = jsch;
    }

    /**
     * This method is only called when the contact is added to a new
     * <tt>ContactGroupSSHImpl</tt> by the
     * <tt>ContactGroupSSHImpl</tt> itself.
     *
     * @param newParentGroup the <tt>ContactGroupSSHImpl</tt> that is now
     * parent of this <tt>ContactSSHImpl</tt>
     */
    public void setParentGroup(ContactGroupSSHImpl newParentGroup)
    {
        this.parentGroup = newParentGroup;
    }

    /**
     * Returns the Hostname associated with this contact
     *
     * @return hostName
     */
    public String getHostName()
    {
        return sshConfigurationForm.getHostName();
    }

    /**
     * Returns a String that can be used for identifying the contact.
     *
     * @return a String id representing and uniquely identifying the contact.
     */
    public String getAddress()
    {
        return contactID;
    }

    /**
     * Returns a String that could be used by any user interacting modules
     * for referring to this contact.
     *
     * @return a String that can be used for referring to this contact when
     *   interacting with the user.
     */
    public String getDisplayName()
    {
        return contactID;
    }

    /**
     * Returns a byte array containing an image (most often a photo or an
     * avatar) that the contact uses as a representation.
     *
     * @return byte[] an image representing the contact.
     */
    public byte[] getImage()
    {
        return null;
    }

    /**
     * Returns true if a command has been sent whos reply was not received yet
     * false otherwise
     *
     * @return commandSent
     */
    public boolean isCommandSent()
    {
        return this.commandSent;
    }

    /**
     * Set the state of commandSent variable which determines whether a reply
     * to a command sent is awaited
     */
    public void setCommandSent(boolean commandSent)
    {
        synchronized(lock)
        {
            this.commandSent = commandSent;
        }
    }

    /**
     * Return the type of message received from remote server
     *
     * @return messageType
     */
    public int getMessageType()
    {
        return this.messageType;
    }

    /**
     * Sets the type of message received from remote server
     *
     * @param messageType
     */
    public void setMessageType(int messageType)
    {
        this.messageType = messageType;
    }

    /**
     * Returns the status of the contact.
     *
     * @return presenceStatus
     */
    public PresenceStatus getPresenceStatus()
    {
        return this.presenceStatus;
    }

    /**
     * Sets <tt>sshPresenceStatus</tt> as the PresenceStatus that this
     * contact is currently in.
     * @param sshPresenceStatus the <tt>SSHPresenceStatus</tt>
     * currently valid for this contact.
     */
    public void setPresenceStatus(PresenceStatus sshPresenceStatus)
    {
        this.presenceStatus = sshPresenceStatus;
    }

    /**
     * Returns a reference to the protocol provider that created the contact.
     *
     * @return a refererence to an instance of the ProtocolProviderService
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return parentProvider;
    }

    /**
     * Determines whether or not this contact represents our own identity.
     *
     * @return true
     */
    public boolean isLocal()
    {
        return true;
    }

    /**
     * Returns the group that contains this contact.
     * @return a reference to the <tt>ContactGroupSSHImpl</tt> that
     * contains this contact.
     */
    public ContactGroup getParentContactGroup()
    {
        return this.parentGroup;
    }

    /**
     * Returns a string representation of this contact, containing most of its
     * representative details.
     *
     * @return  a string representation of this contact.
     */
    @Override
    public String toString()
    {
        StringBuffer buff
                = new StringBuffer("ContactSSHImpl[ DisplayName=")
                .append(getDisplayName()).append("]");

        return buff.toString();
    }

    /**
     * Determines whether or not this contact is being stored by the server.
     * Non persistent contacts are common in the case of simple, non-persistent
     * presence operation sets. They could however also be seen in persistent
     * presence operation sets when for example we have received an event
     * from someone not on our contact list. Non persistent contacts are
     * volatile even when coming from a persistent presence op. set. They would
     * only exist until the application is closed and will not be there next
     * time it is loaded.
     *
     * @return true if the contact is persistent and false otherwise.
     */
    public boolean isPersistent()
    {
        return isPersistent;
    }

    /**
     * Specifies whether or not this contact is being stored by the server.
     * Non persistent contacts are common in the case of simple, non-persistent
     * presence operation sets. They could however also be seen in persistent
     * presence operation sets when for example we have received an event
     * from someone not on our contact list. Non persistent contacts are
     * volatile even when coming from a persistent presence op. set. They would
     * only exist until the application is closed and will not be there next
     * time it is loaded.
     *
     * @param isPersistent true if the contact is persistent and false
     * otherwise.
     */
    public void setPersistent(boolean isPersistent)
    {
        this.isPersistent = isPersistent;
    }


    /**
     * Returns persistent data of the contact.
     *
     * @return persistentData of the contact
     */
    public String getPersistentData()
    {
        return persistentData;
    }

    /**
     * Determines whether or not this contact has been resolved against the
     * server. Unresolved contacts are used when initially loading a contact
     * list that has been stored in a local file until the presence operation
     * set has managed to retrieve all the contact list from the server and has
     * properly mapped contacts to their on-line buddies.
     *
     * @return true if the contact has been resolved (mapped against a buddy)
     * and false otherwise.
     */
    public boolean isResolved()
    {
        return isResolved;
    }

    /**
     * Makes the contact resolved or unresolved.
     *
     * @param resolved  true to make the contact resolved; false to
     *                  make it unresolved
     */
    public void setResolved(boolean resolved)
    {
        this.isResolved = resolved;
    }

    /**
     * Returns the persistent presence operation set that this contact belongs
     * to.
     *
     * @return the <tt>OperationSetPersistentPresenceSSHImpl</tt> that
     * this contact belongs to.
     */
    public OperationSetPersistentPresence
            getParentPresenceOperationSet()
    {
        return
            parentProvider
                .getOperationSet(OperationSetPersistentPresence.class);
    }

    /**
     * Returns the BasicInstant Messaging operation set that this contact
     * belongs to.
     *
     * @return the <tt>OperationSetBasicInstantMessagingSSHImpl</tt> that
     * this contact belongs to.
     */
    public OperationSetBasicInstantMessaging
            getParentBasicInstantMessagingOperationSet()
    {
        return
            parentProvider
                .getOperationSet(OperationSetBasicInstantMessaging.class);
    }

    /**
     * Returns the File Transfer operation set that this contact belongs
     * to.
     *
     * @return the <tt>OperationSetFileTransferSSHImpl</tt> that
     * this contact belongs to.
     */
    public OperationSetFileTransfer
            getFileTransferOperationSet()
    {
        return parentProvider.getOperationSet(OperationSetFileTransfer.class);
    }


    /**
     * Returns the SSH Session associated with this contact
     *
     * @return sshSession
     */
    public Session getSSHSession()
    {
        return this.sshSession;
    }

    /**
     * Sets the SSH Session associated with this contact
     *
     * @param sshSession the newly created SSH Session to be associated
     */
    public void setSSHSession(Session sshSession)
    {
        this.sshSession = sshSession;
    }

    /**
     * Returns the SSH Shell Channel associated with this contact
     *
     * @return sshShellChannel
     */
    public Channel getShellChannel()
    {
        return this.sshShellChannel;
    }

    /**
     * Sets the SSH Shell channel associated with this contact
     *
     * @param sshShellChannel to be associated with SSH Session of this contact
     */
    public void setShellChannel(Channel sshShellChannel)
    {
        this.sshShellChannel = sshShellChannel;
    }

    /**
     * Returns the Input Stream associated with SSH Channel of this contact
     *
     * @return shellInputStream associated with SSH Channel of this contact
     */
    public InputStream getShellInputStream()
    {
        return this.shellInputStream;
    }

//    /**
//     * Sets the Input Stream associated with SSH Channel of this contact
//     *
//     * @param shellInputStream to be associated with SSH Channel of
//     * this contact
//     */
//    public void setShellInputStream(InputStream shellInputStream)
//    {
//        this.shellInputStream = shellInputStream;
//    }

    /**
     * Returns the Output Stream associated with SSH Channel of this contact
     *
     * @return shellOutputStream associated with SSH Channel of this contact
     */
    public OutputStream getShellOutputStream()
    {
        return this.shellOutputStream;
    }

//    /**
//     * Sets the Output Stream associated with SSH Channel of this contact
//     *
//     * @param shellOutputStream to be associated with SSH Channel of this contact
//     */
//    public void setShellOutputStream(OutputStream shellOutputStream)
//    {
//        this.shellOutputStream = shellOutputStream;
//    }

    /**
     * Returns the BufferedReader associated with SSH Channel of this contact
     *
     * @return shellReader associated with SSH Channel of this contact
     */
    public InputStreamReader getShellReader()
    {
        return this.shellReader;
    }

//    /**
//     * Sets the BufferedReader associated with SSH Channel of this contact
//     *
//     * @param shellReader to be associated with SSH Channel of this contact
//     */
//    public void setShellReader(BufferedReader shellReader)
//    {
//        this.shellReader = shellReader;
//    }

    /**
     * Returns the PrintWriter associated with SSH Channel of this contact
     *
     * @return shellWriter associated with SSH Channel of this contact
     */
    public PrintWriter getShellWriter()
    {
        return this.shellWriter;
    }

//    /**
//     * Sets the PrintWriter associated with SSH Channel of this contact
//     *
//     * @param shellWriter to be associated with SSH Channel of this contact
//     */
//    public void setShellWriter(PrintWriter shellWriter)
//    {
//        this.shellWriter = shellWriter;
//    }

    /**
     * Returns the userName associated with SSH Channel of this contact
     *
     * @return userName associated with SSH Channel of this contact
     */
    public String getUserName()
    {
        return sshConfigurationForm.getUserName();
    }

    /**
     * Returns the password associated with SSH Channel of this contact
     *
     * @return password associated with SSH Channel of this contact
     */
    public String getPassword()
    {
        return sshConfigurationForm.getPassword();
    }

    /**
     * Sets the Password associated with this contact
     *
     * @param password
     */
    public void setPassword(String password)
    {
        this.sshConfigurationForm.setPasswordField(password);
        savePersistentDetails();
    }

//    /**
//     * Sets the PS1 prompt of the current shell of Contact
//     *
//     * @param sshPrompt to be associated
//     */
//    public void setShellPrompt(String sshPrompt)
//    {
//        this.sshPrompt = sshPrompt;
//    }
//
//    /**
//     * Returns the PS1 prompt of the current shell of Contact
//     *
//     * @return sshPrompt
//     */
//    public String getShellPrompt()
//    {
//        return this.sshPrompt;
//    }

    /**
     * Return the current status message of this contact.
     *
     * @return the current status message
     */
    public String getStatusMessage()
    {
        return presenceStatus.getStatusName();
    }
}
