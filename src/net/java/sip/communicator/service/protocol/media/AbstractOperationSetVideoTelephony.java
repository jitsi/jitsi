/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.media;

import net.java.sip.communicator.service.protocol.*;



/**
 * Represents a default implementation of <tt>OperationSetVideoTelephony</tt> in
 * order to make it easier for implementers to provide complete solutions while
 * focusing on implementation-specific details.
 *
 * @param <T> the implementation specific telephony operation set class like for
 * example <tt>OperationSetBasicTelephonySipImpl</tt>.
 * @param <U> the implementation specific provider class like for example
 * <tt>ProtocolProviderServiceSipImpl</tt>.
 *
 * @author Emil Ivov
 */
public abstract class AbstractOperationSetVideoTelephony<
                                    T extends OperationSetBasicTelephony<U>,
                                    U extends ProtocolProviderService>
    implements OperationSetVideoTelephony
{
    /**
     * The SIP <tt>ProtocolProviderService</tt> implementation which created
     * this instance and for which telephony conferencing services are being
     * provided by this instance.
     */
    private final U parentProvider;

    /**
     * The telephony-related functionality this extension builds upon.
     */
    private final T basicTelephony;

    /**
     * Initializes a new <tt>AbstractOperationSetVideoTelephony</tt> instance
     * which builds upon the telephony-related functionality of a specific
     * <tt>OperationSetBasicTelephony</tt> implementation.
     *
     * @param basicTelephony the <tt>OperationSetBasicTelephony</tt>
     * the new extension should build upon
     */
    public AbstractOperationSetVideoTelephony(T basicTelephony)
    {
        this.basicTelephony = basicTelephony;
        this.parentProvider = basicTelephony.getProtocolProvider();
    }


}
