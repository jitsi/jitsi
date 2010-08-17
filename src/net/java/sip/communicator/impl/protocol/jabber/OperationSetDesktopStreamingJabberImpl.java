/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * Implements all desktop streaming related functions for XMPP.
 *
 * @author Sebastien Vincent
 */
public class OperationSetDesktopStreamingJabberImpl
    extends OperationSetVideoTelephonyJabberImpl
    implements OperationSetDesktopStreaming
{
    /**
     * Initializes a new <tt>OperationSetDesktopStreamingJabberImpl</tt>
     * instance which builds upon the telephony-related functionality of a
     * specific <tt>OperationSetBasicTelephonyJabberImpl</tt>.
     *
     * @param basicTelephony the <tt>OperationSetBasicTelephonyJabberImpl</tt>
     * the new extension should build upon
     */
    public OperationSetDesktopStreamingJabberImpl(
            OperationSetBasicTelephonyJabberImpl basicTelephony)
    {
        super(basicTelephony);
    }

    /**
     * Get the <tt>MediaUseCase</tt> of a desktop streaming operation set.
     *
     * @return <tt>MediaUseCase.DESKTOP</tt>
     */
    @Override
    public MediaUseCase getMediaUseCase()
    {
        return MediaUseCase.DESKTOP;
    }
}
