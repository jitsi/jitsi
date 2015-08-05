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
import java.net.*;
import java.util.*;

import net.java.sip.communicator.util.*;

/**
 * Timer Task to update the reachability status of SSH Contact in contact list.
 * (Reachability of remote machine from user's machine)
 * The timer is started at either of the two places
 * - A new contact - OperationSetPersistentPresenceSSHImpl
 *                                                  .createUnresolvedContact
 * - Existing Contact - OperationSetPersistentPresenceSSHImpl.subscribe
 *
 * @author Shobhit Jindal
 */
public class ContactTimerSSHImpl
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
    @Override
    public void run()
    {
        try
        {
            InetAddress remoteMachine = InetAddress.getByName(
                    sshContact.getSSHConfigurationForm().getHostName());

            //check if machine is reachable
            if(remoteMachine.isReachable(
                    sshContact.getSSHConfigurationForm().getUpdateInterval()))
            {
                if (sshContact.getPresenceStatus().equals(SSHStatusEnum.OFFLINE)
                || sshContact.getPresenceStatus().equals(SSHStatusEnum
                        .NOT_AVAILABLE))
                {
                    // change status to online
                    persistentPresence.changeContactPresenceStatus(
                            sshContact, SSHStatusEnum.ONLINE);

                    if (logger.isDebugEnabled())
                        logger.debug("SSH Host " + sshContact
                        .getSSHConfigurationForm().getHostName() + ": Online");
                }

            }
            else throw new IOException();

        }
        catch (IOException ex)
        {
            if (sshContact.getPresenceStatus().equals(SSHStatusEnum.ONLINE)
            || sshContact.getPresenceStatus().equals(
                    SSHStatusEnum.NOT_AVAILABLE))
            {
                persistentPresence.changeContactPresenceStatus(
                        sshContact, SSHStatusEnum.OFFLINE);

                if (logger.isDebugEnabled())
                    logger.debug("SSH Host " + sshContact.getSSHConfigurationForm()
                .getHostName() + ": Offline");
            }
        }
    }
    /**
     * Creates a new instance of ContactTimerSSHImpl
     *
     * @param sshContact the <tt>Contact</tt>
     */
    public ContactTimerSSHImpl(ContactSSH sshContact)
    {
        super();
        this.sshContact = sshContact;
        this.persistentPresence = (OperationSetPersistentPresenceSSHImpl)
            sshContact.getParentPresenceOperationSet();
    }

}
