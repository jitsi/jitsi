package net.java.sip.communicator.service.protocol;

/**
 * Provides additional information on the transport on which Basic Instant
 * Messaging communication is built. Note that this refers to the
 * characteristics of the instant messaging protocol, not to the underlying TCP
 * or UDP transport layer.
 *
 * This interface defines methods that provide information on the transport
 * facilities that are used by the Basic Instant Messaging protocol
 * implementation. Methods can be used to query the transport channel for
 * information such as maximum message sizes and allowed number of consecutive
 * messages.
 *
 * @author Danny van Heumen
 */
public interface OperationSetBasicInstantMessagingTransport extends OperationSet
{
    /**
     * Constant value indicating unlimited size or number.
     */
    int UNLIMITED = -1;

    /**
     * Compute the maximum message size for a messaging being sent to the
     * provided contact.
     *
     * <p>
     * If there is no limit to the message size, please use constant
     * {@link #UNLIMITED}.
     * </p>
     *
     * @param contact the contact to which the message will be sent
     * @return returns the maximum size of the message or UNLIMITED if there is
     *         no limit
     */
    int getMaxMessageSize(Contact contact);

    /**
     * Compute the maximum number of consecutive messages allowed to be sent to
     * this contact.
     *
     * <p>
     * If there is no limit to the number of messages, please use constant
     * {@link #UNLIMITED}.
     * </p>
     *
     * @param contact the contact to which the messages are sent
     * @return returns the maximum number of messages to send
     */
    int getMaxNumberOfMessages(Contact contact);
}
