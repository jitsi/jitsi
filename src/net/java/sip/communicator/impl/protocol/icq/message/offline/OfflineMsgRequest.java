/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.icq.message.offline;

import java.io.*;

import net.java.sip.communicator.impl.protocol.icq.message.common.*;

/**
 * Sending Request for retreiving
 * all offline messages if any
 *
 * @author Damian Minkov
 */
public class OfflineMsgRequest
    extends IcqCommand
{
    public OfflineMsgRequest()
    {
        super(AbstractIcqCmd.CMD_OFFLINE_MSG_REQ);
    }

    public void writeIcqData(OutputStream out) throws IOException
    {
        // no specific data
    }
}
