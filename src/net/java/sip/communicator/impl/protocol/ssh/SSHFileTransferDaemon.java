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

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.Logger;

import com.jcraft.jsch.*;

/**
 * @author Shobhit Jindal
 */
public class SSHFileTransferDaemon
        extends Thread
{
    private static final Logger logger =
            Logger.getLogger(SSHFileTransferDaemon .class);

    /**
     * The contact of the remote machine
     */
    private ContactSSH sshContact;

    /**
     * The currently valid ssh protocol provider
     */
    private ProtocolProviderServiceSSHImpl ppService;

    /**
     * JSch Channel to be used for file transfer
     */
    private Channel fileTransferChannel;

    /**
     * The identifier for the Input Stream associated with SCP Channel
     */
    private InputStream scpInputStream = null;

    /**
     * The identifier for the Output Stream associated with SCP Channel
     */
    private OutputStream scpOutputStream = null;

    /**
     * Identifier of local file
     */
    private String localPath;

    /**
     * Identifier of remote file
     */
    private String remotePath;

    /**
     * File to be uploaded or saved
     */
    private File file;

    /**
     * The file input stream associated with the file to be uploaded
     */
    private FileInputStream fileInputStream;

    /**
     * The file output stream associated with the file to be uploaded
     */
    private FileOutputStream fileOutputStream;

    /**
     * The boolean which determines whether we are uploading or downloading
     * files
     */
    private boolean uploadFile;

    /**
     * The currently valid ssh persistent presence operation set
     */
    private OperationSetPersistentPresenceSSHImpl opSetPersPresence = null;

    /**
     * The currently valid ssh instant messaging operation set
     */
    private OperationSetBasicInstantMessagingSSHImpl instantMessaging = null;

    /**
     * Creates a new instance of SSHFileTransferDaemon
     *
     *
     * @param sshContact The contact of the remote machine
     * @param ppService  The current ssh protocol provider
     */
    public SSHFileTransferDaemon(
            ContactSSH sshContact,
            ProtocolProviderServiceSSHImpl ppService)
    {
        super();
        this.sshContact = sshContact;
        this.opSetPersPresence = (OperationSetPersistentPresenceSSHImpl)
                ppService.getOperationSet(OperationSetPersistentPresence.class);
        this.instantMessaging = (OperationSetBasicInstantMessagingSSHImpl)
                ppService.getOperationSet(
                    OperationSetBasicInstantMessaging.class);
        this.ppService = ppService;
    }

    /**
     * This method is called when file is to be transfered from local machine
     * to remote machine
     *
     * @param remotePath - the identifier for the remote file
     * @param localPath - the identifier for the local file
     */
    public void uploadFile(
            String remotePath,
            String localPath)
    {
        this.uploadFile = true;
        this.remotePath = remotePath;
        this.localPath = localPath;

        file = new File(localPath);

        start();
    }

    /**
     * This method is called when a file is to be downloaded from remote machine
     * to local machine
     *
     * @param remotePath - the identifier for the remote file
     * @param localPath - the identifier for the local file
     */
    public void downloadFile(
            String remotePath,
            String localPath)
    {
        this.uploadFile = false;
        this.remotePath = remotePath;
        this.localPath = localPath;

        file = new File(localPath);

        start();
    }

    /**
     * Background thread for the file transfer
     */
    @Override
    public void run()
    {
        //oldStatus to be resumed earlier
        PresenceStatus oldStatus = sshContact.getPresenceStatus();

        opSetPersPresence.changeContactPresenceStatus(
                sshContact,
                SSHStatusEnum.CONNECTING);

        try
        {
            //create a new JSch session if current is invalid
            if( !ppService.isSessionValid(sshContact))
                ppService.createSSHSessionAndLogin(sshContact);

            fileTransferChannel = sshContact.getSSHSession()
                .openChannel("exec");
            String command;

            // -p = Preserves modification times, access times, and modes from
            // the original file
            if(uploadFile)
                command = "scp -p -t " + remotePath;
            else
                command = "scp -f " + remotePath;

            //the command to be executed on the remote terminal
            ((ChannelExec)fileTransferChannel).setCommand(command);

            scpInputStream = fileTransferChannel.getInputStream();
            scpOutputStream = fileTransferChannel.getOutputStream();

            fileTransferChannel.connect();

            //file transfer is setup
            opSetPersPresence.changeContactPresenceStatus(
                    sshContact,
                    SSHStatusEnum.FILE_TRANSFER);

            if(uploadFile)
            {
                instantMessaging.deliverMessage(
                        instantMessaging.createMessage(
                        "Uploading " + file.getName() + " to server"),
                        sshContact);

                upload();
            }
            else
            {
                instantMessaging.deliverMessage(
                        instantMessaging.createMessage(
                        "Downloading " + file.getName() + " from server"),
                        sshContact);

                download();
            }

        }
        catch(Exception ex)
        {
            //presently errors(any type) are directly logged directly in chat
            instantMessaging.deliverMessage(
                    instantMessaging.createMessage(ex.getMessage()),
                    sshContact);

            logger.error(ex.getMessage());

            try
            {
                if(fileInputStream!=null)
                {
                    fileInputStream.close();
                }

                if(fileOutputStream!=null)
                {
                    fileOutputStream.close();
                }
            }
            catch(Exception e)
            {}
        }

        // restore old status
        opSetPersPresence.changeContactPresenceStatus(
                sshContact,
                oldStatus);
    }

    /**
     * Check for error in reading stream of remote machine
     *
     * @return 0 for success, 1 for error, 2 for fatal error, -1 otherwise
     * @throws IOException when the network goes down
     */
    private int checkAck(InputStream inputStream)
        throws IOException
    {
        int result = inputStream.read();

        // read error message
        if(result==1 || result==2)
        {
            StringBuffer buffer = new StringBuffer();

            int ch;

            do
            {
                //read a line of message
                ch = inputStream.read();
                buffer.append((char)ch);

            }while(ch != '\n');

            ProtocolProviderServiceSSHImpl
                .getUIService()
                    .getPopupDialog()
                        .showMessagePopupDialog(
                            buffer.toString(),
                            "File Transfer Error: "
                                + sshContact.getDisplayName(),
                            PopupDialog.ERROR_MESSAGE);

            logger.error(buffer.toString());
        }

        return result;
    }

    /**
     * Uploads the file to the remote server
     *
     * @throws IOException when the network goes down
     * @throws OperationFailedException when server behaves unexpectedly
     */
    private void upload()
    throws  IOException,
            OperationFailedException
    {
        fileInputStream = new FileInputStream(file);

        byte[] buffer = new byte[1024];
        int result, bytesRead;

        if( (result = checkAck(scpInputStream)) !=0)
            throw new OperationFailedException("Error in Ack", result);

        // send "C0644 filesize filename", where filename should not include '/'
        long filesize= file.length();
        String command = "C0644 " + filesize + " ";

//        if(lfile.lastIndexOf('/')>0)
//        {
//            command+=lfile.substring(lfile.lastIndexOf('/')+1);
//        }
//        else
//        {
//            command+=lfile;
//        }

        command += file.getName() + "\n";
        if (logger.isTraceEnabled())
            logger.trace(command);
        scpOutputStream.write(command.getBytes());
        scpOutputStream.flush();

        if( (result = checkAck(scpInputStream)) !=0)
            throw new OperationFailedException("Error in Ack", result);

        while(true)
        {
            bytesRead = fileInputStream.read(buffer, 0, buffer.length);
            if(bytesRead <= 0)
                break;

            scpOutputStream.write(buffer, 0, bytesRead); //out.flush();
        }
        fileInputStream.close();
        fileInputStream = null;

        // send '\0'
        buffer[0]=0; scpOutputStream.write(buffer, 0, 1);
        scpOutputStream.flush();

        if( (result = checkAck(scpInputStream)) !=0)
            throw new OperationFailedException("Error in Ack", result);

        scpInputStream.close();
        scpOutputStream.close();

        fileTransferChannel.disconnect();

        instantMessaging.deliverMessage(
                instantMessaging.createMessage(file.getName()
                        + " uploaded to Server"),
                sshContact);
    }

    /**
     * Downloads a file from the remote machine
     *
     * @throws IOException when the network goes down
     * @throws OperationFailedException when server behaves unexpectedly
     */
    private void download()
    throws  IOException,
            OperationFailedException
    {
        fileOutputStream = new FileOutputStream(file);

        int result;

        byte[] buffer = new byte[1024];

        // send '\0'
        buffer[0]=0;

        scpOutputStream.write(buffer, 0, 1);
        scpOutputStream.flush();

        int ch = checkAck(scpInputStream);

        if(ch!='C')
        {
            throw new OperationFailedException("Invalid reply from server", 12);
        }

        // read '0644 '
        scpInputStream.read(buffer, 0, 5);

        long filesize=0L;
        while(true)
        {
            if(scpInputStream.read(buffer, 0, 1) < 0)
            {
                // error
                break;
            }
            if(buffer[0]==' ')break;
            filesize=filesize*10L+buffer[0]-'0';
        }

        String file=null;
        for(int i=0;true;i++)
        {
            scpInputStream.read(buffer, i, 1);
            if(buffer[i]==(byte)0x0a)
            {
                file=new String(buffer, 0, i);
                break;
            }
        }

        //System.out.println("filesize="+filesize+", file="+file);

        // send '\0'
        buffer[0]=0;
        scpOutputStream.write(buffer, 0, 1);
        scpOutputStream.flush();

        // read a content of lfile
        int foo;
        while(true)
        {
            if(buffer.length<filesize)
                foo=buffer.length;
            else
                foo=(int)filesize;

            foo = scpInputStream.read(buffer, 0, foo);
            if(foo<0)
                break;

            fileOutputStream.write(buffer, 0, foo);
            filesize-=foo;
            if(filesize==0L) break;
        }
        fileOutputStream.close();
        fileOutputStream=null;

        if( (result = checkAck(scpInputStream)) !=0)
            throw new OperationFailedException("Error in Ack", result);

        // send '\0'
        buffer[0]=0;
        scpOutputStream.write(buffer, 0, 1);
        scpOutputStream.flush();

        scpInputStream.close();
        scpOutputStream.close();

        fileTransferChannel.disconnect();

        instantMessaging.deliverMessage(
                instantMessaging.createMessage(
                this.file.getName() + " downloaded from Server"),
                sshContact);
    }
}
