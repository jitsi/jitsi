/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.icq.message.usrinfo;

import java.io.*;

import net.java.sip.communicator.impl.protocol.icq.message.common.*;
import net.kano.joscar.flapcmd.*;

/**
 * Result for client change full-info tlv-based request.
 * If success byte equal 0x0A - operation was finished succesfully,
 * if not - database error.
 * Request was sent by SNAC(15,02)/07D0/0C3A.
 *
 * @author Damian Minkov
 */
public class FullInfoAck
    extends SnacCommand
{
    public static final IcqType SET_FULLINFO_ACK = new IcqType(0x07DA, 0x0C3F);
    private static final int SUCCESS_BYTE = 0x0A;

    private boolean isSuccess = false;

    public FullInfoAck(FromIcqCmd packet)
    {
        super(21, 3);

        byte[] result = packet.getIcqData().toByteArray();

        if(result.length == 1 &&
           result[0] == SUCCESS_BYTE)
        {
            this.isSuccess = true;
        }
    }

    public void writeData(OutputStream out) throws IOException
    {
        // nothing to write
    }

    public boolean isCommandSuccesful()
    {
        return isSuccess;
    }
}
