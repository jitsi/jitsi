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
package net.java.sip.communicator.impl.protocol.zeroconf;

import java.io.*;
import java.net.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Class creating a thread responsible for handling the chat
 * with the remote user on the other end of the socket
 *
 * @author Christian Vincenot
 */
public class ClientThread
    extends Thread
{
    private static final Logger logger = Logger.getLogger(ClientThread.class);

    private OperationSetBasicInstantMessagingZeroconfImpl opSetBasicIM;
    private OperationSetTypingNotificationsZeroconfImpl opSetTyping;
    private Socket sock;
    private InetAddress remoteIPAddress;
    private OutputStream out;
    private DataInputStream in;
    private BonjourService bonjourService;
    private ContactZeroconfImpl contact=null;
    private boolean streamState = false;

    private String messagesQueue=null;

    /**
     * Sets the contact with which we're chatting in this ClientThread
     * @param contact Zeroconf contact with which we're chatting
     */
    protected void setContact(ContactZeroconfImpl contact)
    {
        this.contact = contact;
    }

    /**
     * Set the stream as opened. This means that the
     * conversation with the client is really opened
     * from now on (the XML greetings are over)
     */
    protected void setStreamOpen()
    {
        synchronized(this)
        {
            this.streamState = true;
        }
    }

    /**
     * Says if the stream between the local user and the remote user
     * is in an opened state (greetings are over and we can chat)
     * @return Returns true if the stream is "opened" (ie, ready for chat)
     */
    protected boolean isStreamOpened()
    {
        synchronized(this)
        {
            return this.streamState;
        }
    }

    /**
     * Creates a new instance of ClientThread reponsible
     * for handling the conversation with the remote user.
     * @param sock Socket created for chatting
     * @param bonjourService BonjourService which spawned this ClientThread
     */
    public ClientThread(Socket sock, BonjourService bonjourService)
    {
        this.sock = sock;
        this.remoteIPAddress = sock.getInetAddress();
        this.bonjourService = bonjourService;
        this.opSetBasicIM =
            (OperationSetBasicInstantMessagingZeroconfImpl) bonjourService
                .getPPS().getOperationSet(
                    OperationSetBasicInstantMessaging.class);

        this.opSetTyping =
            (OperationSetTypingNotificationsZeroconfImpl) bonjourService
                .getPPS()
                .getOperationSet(OperationSetTypingNotifications.class);
        this.setDaemon(true);

        try
        {
            out = sock.getOutputStream();
            in = new DataInputStream(sock.getInputStream());
        }
        catch (IOException e)
        {
            logger.error("Creating ClientThread: Couldn't get I/O for "
                    +"the connection", e);
            //System.exit(1);
            return;
        }

        this.start();
    }

    /*
     * Read a message from the socket.
     * TODO: clean the code a bit and optimize it.
     */
    private String readMessage()
    {
        String line;
        byte[] bytes = new byte[10];

        try
        {
            int i=0;

            while (i < 9)
            {
                i += in.read(bytes,0,9-i);
            }

            line = new String(bytes);
            bytes = new byte[1];
            if ((line.getBytes())[0] == '\n')
                line = line.substring(1);

            if (line.startsWith("<message"))
            {
                while (true)
                {
                    bytes[0] = in.readByte();
                    line += new String(bytes);

                    if ((line.endsWith("</message>"))
                        || (line.endsWith("stream>")))
                        return line;
                }
            }
            else
            {
                while (true)
                {
                    bytes[0] = in.readByte();
                    line += new String(bytes);
                    if ( ">".compareTo(new String(bytes)) == 0 )
                        return line;
                }
            }
        }
        catch (IOException e)
        {
            logger.error("Couldn't get I/O for the connection", e);
            //System.exit(1);
        }

        return null;
    }

    /*
     * Parse the payload and extract the information.
     * TODO: If needed, fill in the remaining fields of MessageZeroconfImpl
     * like the baloon icon color, color/size/font of the text.
     */
    private MessageZeroconfImpl parseMessage(String str)
    {
        if (str.startsWith("<?xml") || str.startsWith("<stream"))
            return new MessageZeroconfImpl
                (null, null, MessageZeroconfImpl.STREAM_OPEN);

        if (str.endsWith("stream>"))
            return new MessageZeroconfImpl
                (null, null, MessageZeroconfImpl.STREAM_CLOSE);

        if ((str.indexOf("<delivered/>") > 0) && (str.indexOf("<body>") < 0))
            return new MessageZeroconfImpl
                (null, null, MessageZeroconfImpl.DELIVERED);

        if (!str.startsWith("<message"))
            return new MessageZeroconfImpl
                (null, null, MessageZeroconfImpl.UNDEF);

        /* TODO: Parse Enconding (& contact id to be able to double-check
         * the source of a message)
         *
         * TODO: Check that the fields are outside of <body>..</body>
         */

        if ((str.indexOf("<body>") < 0) || (str.indexOf("</body>") < 0))
            return new MessageZeroconfImpl
                (null, null, MessageZeroconfImpl.UNDEF);

        String temp =
                str.substring(str.indexOf("<body>")+6, str.indexOf("</body>"));

        if (logger.isDebugEnabled())
            logger.debug("ZEROCONF: received message ["+temp+"]");

        int messageType = MessageZeroconfImpl.MESSAGE;

        if ((str.indexOf("<id>") >= 0) && (str.indexOf("</id>") >= 0))
            messageType = MessageZeroconfImpl.TYPING;

        MessageZeroconfImpl msg =
            new MessageZeroconfImpl(
                    temp,
                    null,
                    OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE,
                    messageType);

        return msg;
    }

    private int handleMessage(MessageZeroconfImpl msg)
    {

        switch(msg.getType())
        {
            /* STREAM INIT */
            case MessageZeroconfImpl.STREAM_OPEN:
                if (contact == null)
                    contact = bonjourService.getContact(null, remoteIPAddress);
                if (!isStreamOpened())
                {
                    sendHello();
                    setStreamOpen();
                }
                if (messagesQueue != null)
                {
                    write(messagesQueue);
                    messagesQueue = null;
                }
                break;

                /* ACK */
            case MessageZeroconfImpl.DELIVERED : break;

                /* NORMAL MESSAGE */
            case MessageZeroconfImpl.MESSAGE:
                if (!isStreamOpened())
                    if (logger.isDebugEnabled())
                        logger.debug("ZEROCONF: client on the other side "
                            +"isn't polite (sending messages without "
                            +"saying hello :P");
                if (contact == null)
                    //TODO: Parse contact id to double-check
                    contact = bonjourService.getContact(null, remoteIPAddress);

                /* TODO: If we want to implement invisible status, we'll have to
                 * make this test less restrictive to handle messages from
                 * unannounced clients.
                 */
                if (contact == null)
                {
                    logger.error("ZEROCONF: ERROR - Couldn't identify "
                            +"contact. Closing socket.");
                    return -1;
                }
                else if (contact.getClientThread() == null)
                    contact.setClientThread(this);

                opSetBasicIM.fireMessageReceived(msg, contact);

                opSetTyping.fireTypingNotificationsEvent(contact,
                    OperationSetTypingNotificationsZeroconfImpl.STATE_STOPPED);
                break;

            case MessageZeroconfImpl.TYPING:
                if (!isStreamOpened())
                    if (logger.isDebugEnabled())
                        logger.debug("ZEROCONF: client on the other side "
                            +"isn't polite (sending messages without "
                            +"saying hello :P");
                if (contact == null)
                    //TODO: Parse contact id to double-check
                    contact = bonjourService.getContact(null, remoteIPAddress);
                opSetTyping.fireTypingNotificationsEvent(contact,
                    OperationSetTypingNotificationsZeroconfImpl.STATE_TYPING);

                /* TODO: code a private runnable class to be used as timeout
                 * to set the typing state to STATE_PAUSED when a few seconds
                 * without news have passed.
                 */

                break;

            case MessageZeroconfImpl.STREAM_CLOSE:
                sendBye();
                contact.setClientThread(null);
                return 1;

            case MessageZeroconfImpl.UNDEF:
                logger.error("ZEROCONF: received strange message. SKIPPING!");
                break;
        }

        //System.out.println("RECEIVED MESSAGE "+ msg.getContent()+
        //" from "+contact.getAddress() + "!!!!!!!!!!!!!!");
        return 0;
    }


    private void write(String string)
    {
        //System.out.println("Writing " + string + "!!!!!!!!!");
        byte[] bytes = string.getBytes();
        try
        {
            out.write(bytes);
            out.flush();
        }
        catch (IOException e)
        {
            logger.error("Couldn't get I/O for the connection");
            if (contact != null)
            {
                contact.setClientThread(null);
            }

            try
            {
                sock.close();
            }
            catch (IOException ex)
            {
                logger.error(ex);
            }

        }
    }

    /**
     * Say hello :)
     */
    protected void sendHello()
    {
        switch(contact.getClientType())
        {
            case ContactZeroconfImpl.GAIM:
            case ContactZeroconfImpl.ICHAT:
            case ContactZeroconfImpl.SIPCOM:
                write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
                write("<stream:stream xmlns=\"jabber:client\" "
                        +"xmlns:stream=\"http://etherx.jabber.org/streams\">");
                break;
            case ContactZeroconfImpl.XMPP:
                write("<stream:stream"
                        +"xmlns='jabber:client'"
                        +"xmlns:stream='http://etherx.jabber.org/streams'"
                        +"from='"+bonjourService.getID()+"'"
                        +"to='"+contact.getAddress()+"'"
                        +"version='1.0'>\n");
                break;
        }

        /* Legacy: OLD XMPP (XEP-0174 Draft) */
        //write("<stream:stream to='"+sock.getInetAddress().getHostAddress()
        //+"' xmlns='jabber:client' stream='http://etherx.jabber.org/streams'>");
    }

    private void sendBye()
    {
        write("</stream:stream>\n");
    }

    private String toXHTML(MessageZeroconfImpl msg)
    {
        switch(contact.getClientType())
        {
            case ContactZeroconfImpl.XMPP:
                return new String("<message to='"
                        +contact.getAddress()+"' from='"
                        +bonjourService.getID()+"'>"
                        + "<body>"+msg.getContent()+"</body>"
                        + "</message>\n");

            case ContactZeroconfImpl.SIPCOM:

            case ContactZeroconfImpl.ICHAT:
                return new String(
                    "<message to='"+sock.getInetAddress().getHostAddress()
                    +"' type='chat' id='"+bonjourService.getID()+"'>"
                    + "<body>"+msg.getContent()+"</body>"
                    + "<html xmlns='http://www.w3.org/1999/xhtml'>"
                    + "<body ichatballooncolor='#7BB5EE' "
                    + "ichattextcolor='#000000'>"
                    + "<font face='Helvetica' ABSZ='12' color='#000000'>"
                    + msg.getContent()
                    + "</font>"
                    + "</body>"
                    + "</html>"
                    + "<x xmlns='jabber:x:event'>"
                    + "<offline/>"
                    + "<delivered/>"
                    + "<composing/>"
                    + (msg.getType()==MessageZeroconfImpl.TYPING?"<id></id>":"")
                    + "</x>"
                    + "</message>");

            case ContactZeroconfImpl.GAIM:
            default:
                return new String(
                    "<message to='"+contact.getAddress()
                    +"' from='"+bonjourService.getID()
                    + "' type='chat'><body>"+msg.getContent()+"</body>"
                    + "<html xmlns='http://www.w3.org/1999/xhtml'><body><font>"
                    + msg.getContent()
                    + "</font></body></html><x xmlns='jabber:x:event'><composing/>"
                    + (msg.getType()==MessageZeroconfImpl.TYPING?"<id></id>":"")
                    + "</x></message>\n");
        }
    }


    /**
     * Send a message to the remote user
     * @param msg Message to send
     */
    public void sendMessage(MessageZeroconfImpl msg)
    {
        if (logger.isDebugEnabled())
            logger.debug("ZEROCONF: Sending messag ["
                +msg.getContent()+"] to "
                + contact.getDisplayName());
        if (!isStreamOpened())
        {
            if (logger.isDebugEnabled())
                logger.debug("ZEROCONF: Stream not opened... "
                    +"will send the message later");
            messagesQueue += toXHTML(msg);
        }
        else write(toXHTML(msg));
    }

    /**
     * Walk?
     */
    @Override
    public void run()
    {
        if (logger.isDebugEnabled())
            logger.debug("Bonjour: NEW CONNEXION from "
                + sock.getInetAddress().getCanonicalHostName()
                +" / "+sock.getInetAddress().getHostAddress());
        String input;
        MessageZeroconfImpl msg=null;


        input = readMessage();
        msg = parseMessage(input);

        while (handleMessage(msg) == 0 && !sock.isClosed())
        {
            input = readMessage();
            msg = parseMessage(input);
        }

        if (logger.isDebugEnabled())
            logger.debug("ZEROCONF : OUT OF LOOP !! Closed chat.");
        cleanThread();
    }

    /**
     * Clean-up the thread to exit
     */
    public void cleanThread()
    {
        /* I wonder if that's ok... */
        if (sock != null && sock.isClosed() == false)
        {
            sendBye();
            try
            {
                sock.close();
            }
            catch (IOException ex)
            {
                logger.error(ex);
            }
        }
    }
}
