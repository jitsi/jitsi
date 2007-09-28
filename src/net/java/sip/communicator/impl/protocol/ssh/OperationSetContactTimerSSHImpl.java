/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging clitent.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 *
 * OperationSetContactTimerSSHImpl.java
 *
 * SSH Suport in SIP Communicator - GSoC' 07 Project
 */

package net.java.sip.communicator.impl.protocol.ssh;

import java.net.*;
import java.util.*;
import net.java.sip.communicator.util.*;

/**
 * Timer Task to update the reachability status of SSH Contact in contact list
 * The timer is started at either of the two places
 * - A new contact - OperationSetPersistentPresenceSSHImpl
 *                                                  .createUnresolvedContact
 * - Existing Contact - OperationSetPersistentPresenceSSHImpl.subscribe
 *
 * @author Shobhit Jindal
 */
public class OperationSetContactTimerSSHImpl
        extends TimerTask
{
    private static final Logger logger
            = Logger.getLogger(OperationSetFileTransferSSHImpl.class);
    
    /**
     * The contact ID of the remote machine
     */
    private ContactSSH sshContact;
    
    /**
     * PersistentPresence Identifer assoiciated with SSH Contact
     */
    private OperationSetPersistentPresenceSSHImpl persistentPresence;
    
    /**
     * The method which is called at regular intervals to update the status
     * of remote machines
     *
     * Presently only ONLINE and OFFILINE status are checked
     */
    public void run()
    {
        try
        {
/*            InetAddress remoteMachine = InetAddress.getByName(
                    sshContact.getSSHConfigurationForm().getHostName());

            //check if machine is reachable
            if(remoteMachine.isReachable(
                    sshContact.getSSHConfigurationForm().getUpdateInterval()))
            {
                if(
                        ! sshContact.getPresenceStatus().equals(SSHStatusEnum
                                                                .ONLINE)
                        &&
                        ! sshContact.getPresenceStatus().equals(SSHStatusEnum
                                                                .CONNECTING)
                        &&
                        ! sshContact.getPresenceStatus().equals(SSHStatusEnum
                                                                .CONNECTED)
                   )
*/
            
            Socket socket = new Socket(
                    sshContact.getSSHConfigurationForm().getHostName(),
                    sshContact.getSSHConfigurationForm().getPort());
            
            
            socket.close();
            
            if (sshContact.getPresenceStatus().equals(SSHStatusEnum.OFFLINE)
                || sshContact.getPresenceStatus().equals(SSHStatusEnum
                                                                .NOT_AVAILABLE))
            {
                // change status to online
                persistentPresence.changeContactPresenceStatus(
                        sshContact, SSHStatusEnum.ONLINE);
                
                logger.debug("SSH Host " + sshContact.getSSHConfigurationForm()
                        .getHostName() + ": Online");
            }
        }
        catch (Exception ex)
        {
            if (sshContact.getPresenceStatus().equals(SSHStatusEnum.ONLINE)
                || sshContact.getPresenceStatus().equals(SSHStatusEnum
                                                                .NOT_AVAILABLE))
            {
                persistentPresence.changeContactPresenceStatus(
                        sshContact, SSHStatusEnum.OFFLINE);
                
                logger.debug("SSH Host " + sshContact.getSSHConfigurationForm()
                        .getHostName() + ": Offline");
            }
        }
    }
    /**
     * Creates a new instance of OperationSetContactTimerSSHImpl
     */
    public OperationSetContactTimerSSHImpl(ContactSSH sshContact)
    {
        super();
        this.sshContact = sshContact;
        this.persistentPresence = (OperationSetPersistentPresenceSSHImpl)
            sshContact.getParentPresenceOperationSet();
    }
    
}
