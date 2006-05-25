/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.icq.message.offline;

import java.io.*;

import net.kano.joscar.snaccmd.icbm.*;
import net.kano.joscar.tlv.*;

/**
 * Sending InstantMessages with TLV for delivering as offline message
 *
 * @author Damian Minkov
 */
public class SendOfflineIm
    extends SendImIcbm
{
    /**
     * A TLV type present if this message must be offline delivered.
     */
    private static final int TYPE_OFFLINE = 0x0006;

    public SendOfflineIm(String sn, String message)
    {
        super(sn, message);
    }

    protected void writeChannelData(OutputStream out) throws IOException
    {
        super.writeChannelData(out);
        new Tlv(TYPE_OFFLINE).write(out);
    }

}
