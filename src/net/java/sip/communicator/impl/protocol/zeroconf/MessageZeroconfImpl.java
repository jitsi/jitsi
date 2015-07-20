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

import net.java.sip.communicator.service.protocol.*;

/**
 * Very simple message implementation for the Zeroconf protocol.
 *
 * @author Christian Vincenot
 * @author Maxime Catelin
 * @author Jonathan Martin
 * @author Lubomir Marinov
 */
public class MessageZeroconfImpl
    extends AbstractMessage
{

    /**
     * Message Type.
     */
    private int type;

    /**
     * Message type indicating that a stream is being created
     */
    public static final int STREAM_OPEN = 0x1;

    /**
     * Normal chat message
     */
    public static final int MESSAGE = 0x2;

    /**
     * Typing notification
     */
    public static final int TYPING = 0x3;

    /**
     * Message indicating that the stream is being closed
     */
    public static final int STREAM_CLOSE = 0x4;

    /**
     * Message indicating that the previsous message was delivered successfully
     */
    public static final int DELIVERED = 0x5;

    /**
     * Undefined message
     */
    public static final int UNDEF = 0x6;

    /*
     * The Baloon Icon color. (we probably won't ever use it)
     */
    private int baloonColor = 0x7BB5EE;

    /*
     * The Text Color.
     */
    private int textColor = 0x000000;

    /*
     * The font of the message.
     */
    private String textFont = "Helvetica";

    /*
     * The size of the caracters composing the message.
     */
    private int textSize = 12;

    /*
     * The source contact id announced in the message. TODO: Could be set &
     * checked to identify more precisely the contact in case several users
     * would be sharing the same IP.
     */
    private String contactID;

    /**
     * Creates a message instance according to the specified parameters.
     *
     * @param content the message body
     * @param contentEncoding message encoding or null for UTF8
     * @param contentType of the message
     * @param type Type of message
     */
    public MessageZeroconfImpl(String content, String contentEncoding,
        String contentType, int type)
    {
        super(content, contentType, contentEncoding, null);

        this.type = type;
    }

    /**
     * Creates a message instance according to the specified parameters.
     *
     * @param type Type of message
     * @param content the message body
     * @param contentEncoding message encoding or null for UTF8
     */
    public MessageZeroconfImpl(String content, String contentEncoding, int type)
    {
        this(content, contentEncoding,
            OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE, type);
    }

    /**
     * Returns the type of message. Always text/plain for Zeroconf, so null.
     *
     * @return null
     */
    public int getType()
    {
        return type;
    }

    /**
     * Gets the baloon color declared in messages sent by iChat-like clients
     *
     * @return baloon color
     */
    public int getBaloonColor()
    {
        return baloonColor;
    }

    /**
     * Sets the baloon color declared in messages sent by iChat-like clients
     *
     * @param baloonColor baloon color
     */
    public void setBaloonColor(int baloonColor)
    {
        this.baloonColor = baloonColor;
    }

    /**
     * Returns the text color
     *
     * @return Text color
     */
    public int getTextColor()
    {
        return textColor;
    }

    /**
     * Sets the text color
     *
     * @param textColor Text color
     */
    public void setTextColor(int textColor)
    {
        this.textColor = textColor;
    }

    /**
     * Returns the text font
     *
     * @return Text font
     */
    public String getTextFont()
    {
        return textFont;
    }

    /**
     * Sets the text color
     *
     * @param textFont Text font
     */
    public void setTextFont(String textFont)
    {
        this.textFont = textFont;
    }

    /**
     * Returns the text size
     *
     * @return Text size
     */
    public int getTextSize()
    {
        return textSize;
    }

    /**
     * Sets the text size
     *
     * @param textSize Text size
     */
    public void setTextSize(int textSize)
    {
        this.textSize = textSize;
    }

    /**
     * Returns the contact's ID
     *
     * @return String representing the contact's ID
     */
    public String getContactID()
    {
        return contactID;
    }

    /**
     * Sets the contact's ID
     *
     * @param contactID String representing the contact's ID
     */
    public void setContactID(String contactID)
    {
        this.contactID = contactID;
    }
}
