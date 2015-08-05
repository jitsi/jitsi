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

import net.java.sip.communicator.service.protocol.*;

import com.jcraft.jsch.*;

/**
 * This interface represents a Contact of SSH Type
 * As a SSH Session is specific to a contact, additional information needed
 * to maintain its state with the remote server is present here
 *
 * @author Shobhit Jindal
 */
interface ContactSSH
        extends Contact
{
    /**
     * An event type indicating that the message being received is a standard
     * conversation message sent by another contact.
     */
    public static final int CONVERSATION_MESSAGE_RECEIVED = 1;

    /**
     * An event type indicting that the message being received is a system
     * message being sent by the server or a system administrator.
     */
    public static final int SYSTEM_MESSAGE_RECEIVED = 2;

    //Following eight function declations to be moved to Contact

    /**
     * This method is only called when the contact is added to a new
     * <tt>ContactGroupSSHImpl</tt> by the
     * <tt>ContactGroupSSHImpl</tt> itself.
     *
     * @param newParentGroup the <tt>ContactGroupSSHImpl</tt> that is now
     * parent of this <tt>ContactSSHImpl</tt>
     */
    void setParentGroup (ContactGroupSSHImpl newParentGroup);

    /**
     * Sets <tt>sshPresenceStatus</tt> as the PresenceStatus that this
     * contact is currently in.
     * @param sshPresenceStatus the <tt>SSHPresenceStatus</tt>
     * currently valid for this contact.
     */
    public void setPresenceStatus (PresenceStatus sshPresenceStatus);

    /**
     * Returns the persistent presence operation set that this contact belongs
     * to.
     *
     * @return the <tt>OperationSetPersistentPresenceSSHImpl</tt> that
     * this contact belongs to.
     */
    public OperationSetPersistentPresence
            getParentPresenceOperationSet ();

    /**
     * Returns the BasicInstant Messaging operation set that this contact
     * belongs to.
     *
     * @return the <tt>OperationSetBasicInstantMessagingSSHImpl</tt> that
     * this contact belongs to.
     */
    public OperationSetBasicInstantMessaging
            getParentBasicInstantMessagingOperationSet ();

    /**
     * Returns the File Transfer operation set that this contact belongs
     * to.
     *
     * @return the <tt>OperationSetFileTransferSSHImpl</tt> that
     * this contact belongs to.
     */
    public OperationSetFileTransfer
            getFileTransferOperationSet ();

    /**
     * Return the type of message received from remote server
     *
     * @return messageType
     */
    public int getMessageType ();

    /**
     * Sets the type of message received from remote server
     *
     * @param messageType
     */
    public void setMessageType (int messageType);

    /**
     * Stores persistent data of the contact.
     *
     * @param persistentData of the contact
     */
    public void setPersistentData (String persistentData);

    /**
     * Makes the contact resolved or unresolved.
     *
     * @param resolved  true to make the contact resolved; false to
     *                  make it unresolved
     */
    public void setResolved (boolean resolved);

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
    public void setPersistent (boolean isPersistent);

    /**
     * Returns true if a command has been sent whos reply was not received yet
     * false otherwise
     *
     * @return commandSent
     */
    public boolean isCommandSent ();

    /**
     * Set the state of commandSent variable which determines whether a reply
     * to a command sent is awaited
     *
     * @param commandSent
     */
    public void setCommandSent (boolean commandSent);

    /**
     * Initializes the reader and writers associated with shell of this contact
     *
     * @param shellInputStream The InputStream of stack
     * @param shellOutputStream The OutputStream of stack
     */
    void initializeShellIO (InputStream shellInputStream,
            OutputStream shellOutputStream);

    /**
     * Closes the readers and writer associated with shell of this contact
     */
    void closeShellIO ();

    /**
     * Determines whether a connection to a remote server is already underway
     *
     * @return connectionInProgress
     */
    public boolean isConnectionInProgress ();

    /**
     * Sets the status of connection attempt to remote server
     *
     * @param connectionInProgress
     */
    public void setConnectionInProgress (boolean connectionInProgress);

//    /**
//     * Sets the PS1 prompt of the current shell of Contact
//     * This method is synchronized
//     *
//     * @param sshPrompt to be associated
//     */
//    public void setShellPrompt(String sshPrompt);
//
//    /**
//     * Returns the PS1 prompt of the current shell of Contact
//     *
//     * @return sshPrompt
//     */
//    public String getShellPrompt();


    /**
     * Saves the details of contact in persistentData
     */
    public void savePersistentDetails ();

    /*
     * Returns the SSHContactInfo associated with this contact
     *
     * @return sshConfigurationForm
     */
    public SSHContactInfo getSSHConfigurationForm ();

    /**
     * Returns the JSch Stack identified associated with this contact
     *
     * @return jsch
     */
    JSch getJSch ();

    /**
     * Starts the timer and its task to periodically update the status of
     * remote machine
     */
    void startTimerTask ();

    /**
     * Stops the timer and its task to stop updating the status of
     * remote machine
     */
    void stopTimerTask ();

    /**
     * Sets the JSch Stack identified associated with this contact
     *
     * @param jsch to be associated
     */
    void setJSch (JSch jsch);

    /**
     * Returns the Username associated with this contact
     *
     * @return userName
     */
    String getUserName ();

    /**
     * Returns the Hostname associated with this contact
     *
     * @return hostName
     */
    String getHostName ();

    /**
     * Returns the Password associated with this contact
     *
     * @return password
     */
    String getPassword ();

    /**
     * Sets the Password associated with this contact
     *
     * @param password
     */
    void setPassword (String password);

    /**
     * Returns the SSH Session associated with this contact
     *
     * @return sshSession
     */
    Session getSSHSession ();

    /**
     * Sets the SSH Session associated with this contact
     *
     * @param sshSession the newly created SSH Session to be associated
     */
    void setSSHSession (Session sshSession);

    /**
     * Returns the SSH Shell Channel associated with this contact
     *
     * @return shellChannel
     */
    Channel getShellChannel ();

    /**
     * Sets the SSH Shell channel associated with this contact
     *
     * @param shellChannel to be associated with SSH Session of this contact
     */
    void setShellChannel (Channel shellChannel);

    /**
     * Sends a message a line to remote machine via the Shell Writer
     *
     * @param message to be sent
     * @throws IOException if message failed to be sent
     */
    public void sendLine (String message)
        throws IOException;

//    /**
//     * Reads a line from the remote machine via the Shell Reader
//     *
//     * @return message read
//     */
//    public String getLine()
//        throws IOException;

    /**
     * Returns the Input Stream associated with SSH Channel of this contact
     *
     * @return shellInputStream associated with SSH Channel of this contact
     */
    public InputStream getShellInputStream ();

//    /**
//     * Sets the Input Stream associated with SSH Channel of this contact
//     *
//     * @param shellInputStream to be associated with SSH Channel of this
//     * contact
//     */
//    public void setShellInputStream(InputStream shellInputStream);

    /**
     * Returns the Output Stream associated with SSH Channel of this contact
     *
     * @return shellOutputStream associated with SSH Channel of this contact
     */
    public OutputStream getShellOutputStream ();

//    /**
//     * Sets the Output Stream associated with SSH Channel of this contact
//     *
//     * @param shellOutputStream to be associated with SSH Channel of this
//     * contact
//     */
//    public void setShellOutputStream(OutputStream shellOutputStream);
//
    /**
     * Returns the BufferedReader associated with SSH Channel of this contact
     *
     * @return shellReader associated with SSH Channel of this contact
     */
    public InputStreamReader getShellReader ();
//
//    /**
//     * Sets the BufferedReader associated with SSH Channel of this contact
//     *
//     * @param shellReader to be associated with SSH Channel of this contact
//     */
//    public void setShellReader(BufferedReader shellReader);

    /**
     * Returns the PrintWriter associated with SSH Channel of this contact
     *
     * @return shellWriter associated with SSH Channel of this contact
     */
    public PrintWriter getShellWriter ();

//    /**
//     * Sets the PrintWriter associated with SSH Channel of this contact
//     *
//     * @param shellWriter to be associated with SSH Channel of this contact
//     */
//    public void setShellWriter(PrintWriter shellWriter);
}
