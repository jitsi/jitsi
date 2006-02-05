package net.java.sip.communicator.service.protocol;

/**
 * Used to access the content of instant messages that are sent or
 * received via the instant messaging operation set.
 * <p>
 * This class provides easy access to the content and key fields of an instant
 * Message. Content types are represented using MIME types. [IETF RFC 2045-2048].
 * </p>
 * <p>
 * Messages are created through the
 * <tt>OperationSetBaicInstanceMessaging</tt> operation set.
 * </p>
 * <p>
 * </p>
 * <p>
 * All messages have message ids that allow the underlying implementation to
 * notify the user of their succesful delivery.
 * </p>
 *
 * @author Emil Ivov
 */
public interface Message
{
}
