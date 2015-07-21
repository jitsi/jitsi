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

/**
 *
 * @author Shobhit Jindal
 */
public class SSHReaderDaemon
        extends Thread
{

    /**
     * A Buffer to aggregate replies to be sent as one message
     */
    private StringBuffer replyBuffer;

    /**
     * The identifier of Contact representing the remote machine
     */
    private ContactSSHImpl sshContact;

    /**
     * The identifier of the message received from server
     */
    private String message;

    /**
     * An identifier representing the state of Reader Daemon
     */
    private boolean isActive = false;

    /**
     * This OperationSet delivers incoming message
     */
    private OperationSetBasicInstantMessagingSSHImpl instantMessaging;

    /**
     * Input Stream of remote user to be read
     */
    private InputStream shellInputStream;

    /**
     * Buffered Reader associated with above input stream
     */
    private InputStreamReader shellReader;

//    /**
//     * This OperationSet delivers incoming message
//     */
//    private OperationSetPersistentPresenceSSHImpl persistentPresence;

    /**
     * Bytes available in Input Stream before reading
     */
    private int bytesAvailable;

    private int bytesRead;

    int bufferCount;

    char buf;

    /**
     * Creates a new instance of SSHReaderDaemon
     */
    public SSHReaderDaemon(ContactSSH sshContact)
    {
        this.sshContact = (ContactSSHImpl)sshContact;
        instantMessaging =
            (OperationSetBasicInstantMessagingSSHImpl)
                sshContact
                    .getProtocolProvider()
                        .getOperationSet(
                            OperationSetBasicInstantMessaging.class);
    }

    /**
     * Reads the remote machine, updating the chat window as necessary
     * in a background thread
     */
    @Override
    public void run()
    {
        shellInputStream = sshContact.getShellInputStream();
        shellReader = sshContact.getShellReader();
        replyBuffer = new StringBuffer();


        try
        {
            do
            {
                bytesAvailable = shellInputStream.available();

                if(bytesAvailable == 0 )
                {
                    // wait if more data is available
                    // for a slower connection this value need to be raised
                    // to avoid splitting of messages
                    Thread.sleep(250);
                    continue;
                }

                bufferCount = 0;

//                if(replyBuffer > 0)

                do
                {
                    // store the responses in a buffer
                    storeMessage(replyBuffer);

                    Thread.sleep(250);

                    bytesAvailable = shellInputStream.available();

                }while(bytesAvailable > 0  && bufferCount<16384);

                message = replyBuffer.toString();

                if(sshContact.isCommandSent())
                {
                    // if the response is as a result of a command sent
                    sshContact.setMessageType(
                            ContactSSH.CONVERSATION_MESSAGE_RECEIVED);

                    message = message.substring(message.indexOf('\n') + 1);

                    sshContact.setCommandSent(false);
                }
                else
                {
                    // server sent an asynchronous message to the terminal
                    // display it as a system message
                    sshContact.setMessageType(
                            ContactSSH.SYSTEM_MESSAGE_RECEIVED);

                    //popup disabled
//                    JOptionPane.showMessageDialog(
//                            null,
//                            message,
//                            "Message from " + sshContact.getDisplayName(),
//                            JOptionPane.INFORMATION_MESSAGE);
                }

                instantMessaging.deliverMessage(
                        instantMessaging.createMessage(message),
                        sshContact);

                replyBuffer.delete(0, replyBuffer.length());

            }while(isActive);
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
    }

    /**
     * Stores the response from server in a temporary buffer
     * the bytes available are determined before the function is called
     *
     * @param replyBuffer to store the response from server
     *
     * @throws IOException if the network goes down
     */
    private void storeMessage(StringBuffer replyBuffer) throws IOException
    {
        do
        {
            buf = (char)shellInputStream.read();

//            System.out.println(String.valueOf(buf)+ " " + (int)buf);

            replyBuffer.append(String.valueOf(buf));

//                    logger.debug(shellReader.readLine());

            bufferCount++;

            bytesAvailable--;

        }while(bytesAvailable>0 && bufferCount<32700);
    }

    public void isActive(boolean isActive)
    {
        this.isActive = isActive;
    }
}
