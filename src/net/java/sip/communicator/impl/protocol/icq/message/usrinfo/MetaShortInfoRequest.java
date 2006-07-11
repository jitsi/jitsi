/*
 * Created on 20/10/2003
 */
package net.java.sip.communicator.impl.protocol.icq.message.usrinfo;

import java.io.*;

import net.java.sip.communicator.impl.protocol.icq.message.common.*;

/**
 * @author jkohen
 */
public class MetaShortInfoRequest
    extends IcqCommand
{
    private long uin;

//    public MetaShortInfoRequest(FromIcqCmd cmd)
//    {
//        super(cmd);
//
//        ByteBlock block = cmd.getIcqData();
//
//        uin = LEBinaryTools.getUInt(block, 0);
//    }

    /**
     * Creates a new instance of this command given the specified values.
     *
     * @param uin the UIN of the sender.
     */
    public MetaShortInfoRequest(long uin)
    {
        super(AbstractIcqCmd.CMD_META_SHORT_INFO_REQ);

        this.uin = uin;
    }

    public void writeIcqData(OutputStream out) throws IOException
    {
        LEBinaryTools.writeUInt(out, uin);
    }

    /**
     * Returns the UIN of the sender of this message.
     *
     * @return the UIN of the sender.
     */
    public long getUin()
    {
        return uin;
    }
}
