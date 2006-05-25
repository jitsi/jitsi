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
 * Request send to the server to delete all offline messages
 * as we have already retreived them.
 *
 * @author Damian Minkov
 */
public class OfflineMsgDeleteRequest
    extends IcqCommand
{
    public OfflineMsgDeleteRequest()
    {
        super(AbstractIcqCmd.CMD_OFFLINE_MSG_ACK);
    }

    public void writeIcqData(OutputStream out) throws IOException
    {
        // no specific data
    }
}
