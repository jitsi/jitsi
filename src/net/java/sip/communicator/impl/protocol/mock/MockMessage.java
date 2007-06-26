/*
 * MockMessage.java
 *
 * Created on Jun 21, 2007, 3:10:21 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.java.sip.communicator.impl.protocol.mock;

import net.java.sip.communicator.service.protocol.*;

/**
 *  Message Impl.
 * @author Damian Minkov
 */
public class MockMessage
    implements Message
{    
    private String textContent = null;

    private String contentType = null;

    private String contentEncoding = null;

    private String messageUID = null;

    private String subject = null;

    MockMessage(String content,
                          String contentType,
                          String contentEncoding,
                          String subject)
    {
        this.textContent = content;
        this.contentType = contentType;
        this.contentEncoding = contentEncoding;
        this.subject = subject;

        //generate the uid
        this.messageUID = String.valueOf( System.currentTimeMillis())
                          + String.valueOf(hashCode());
    }
    
    MockMessage(String content)
    {
        this.textContent = content;
        this.contentType = 
            OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE;
        this.contentEncoding = 
            OperationSetBasicInstantMessaging.DEFAULT_MIME_ENCODING;
        this.subject = null;

        //generate the uid
        this.messageUID = String.valueOf( System.currentTimeMillis())
                          + String.valueOf(hashCode());
    }

    /**
     * Returns the content of this message if representable in text form or null
     * if this message does not contain text data.
     * @return a String containing the content of this message or null if the
     * message does not contain data representable in text form.
     */
    public String getContent()
    {
        return textContent;
    }

    /**
     * Returns the MIME type for the message content.
     * @return a String containing the mime type of the message contant.
     */
    public String getContentType()
    {
        return contentType;
    }

    /**
     * Returns the MIME content encoding of this message.
     * @return a String indicating the MIME encoding of this message.
     */
    public String getEncoding()
    {
        return contentEncoding;
    }

    /**
     * Returns a unique identifier of this message.
     * @return a String that uniquely represents this message in the scope of
     * this protocol.
     */
    public String getMessageUID()
    {
        return messageUID;
    }

    /**
     * Get the raw/binary content of an instant message.
     * @return a byte[] array containing message bytes.
     */
    public byte[] getRawData()
    {
        return getContent().getBytes();
    }

    /**
     * Returns the size of the content stored in this message.
     * @return an int indicating the number of bytes that this message contains.
     */
    public int getSize()
    {
        return getContent().length();
    }

    /**
     * Returns the subject of this message or null if the message contains no
     * subject.
     * @return the subject of this message or null if the message contains no
     * subject.
     */
    public String getSubject()
    {
        return subject;
    }
}
