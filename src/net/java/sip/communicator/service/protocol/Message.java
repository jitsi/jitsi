/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

/**
 * Used to access the content of instant messages that are sent or received via
 * the instant messaging operation set.
 * <p>
 * This class provides easy access to the content and key fields of an instant
 * Message. Content types are represented using MIME types. [IETF RFC 2045-2048].
 * <p>
 * Messages are created through the <tt>OperationSetBaicInstanceMessaging</tt>
 * operation set.
 * <p>
 * All messages have message ids that allow the underlying implementation to
 * notify the user of their successful delivery.
 *
 * @author Emil Ivov
 */
public interface Message
{
    /**
     * Returns the content of this message if representable in text form or null
     * if this message does not contain text data.
     * @return a String containing the content of this message or null if the
     * message does not contain data representable in text form.
     */
    public String getContent();

    /**
     * Returns the MIME type for the message content.
     * @return a String containing the mime type of the message contant.
     */
    public String getContentType();

    /**
     * Get the raw/binary content of an instant message.
     * @return a byte[] array containing message bytes.
     */
    public byte[] getRawData();

    /**
     * Returns the MIME content encoding of this message.
     * @return a String indicating the MIME encoding of this message.
     */
    public String getEncoding();

    /**
     * Returns the subject of this message or null if the message contains no
     * subject.
     * @return the subject of this message or null if the message contains no
     * subject.
     */
    public String getSubject();

    /**
     * Returns the size of the content stored in this message.
     * @return an int indicating the number of bytes that this message contains.
     */
    public int getSize();

    /**
     * Returns a unique identifier of this message.
     * @return a String that uniquely represents this message in the scope of
     * this protocol.
     */
    public String getMessageUID();
}
