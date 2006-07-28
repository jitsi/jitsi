package net.java.sip.communicator.impl.protocol.icq.message.imicbm;

import java.io.*;

import net.java.sip.communicator.impl.protocol.icq.message.common.*;
import net.kano.joscar.*;
import net.kano.joscar.flapcmd.*;
import net.kano.joscar.snaccmd.*;
import net.kano.joscar.snaccmd.icbm.*;
import net.kano.joscar.tlv.*;

/**
 * Parses incoming data from the messages of channel 4
 *
 * @author Damian Minkov
 */
public class IcbmChannelFourCommand
    extends AbstractImIcbm
{
    /** A TLV type containing the text of the Instant Message. */
    private static final int TYPE_MESSAGE_DATA = 0x0005;

    /** Message type - Authorization Request */
    public static final int MTYPE_AUTHREQ = 0x06;
    /** Message type - Authorization Denied */
    public static final int MTYPE_AUTHDENY = 0x07;
    /** Message type - Authorization Accepted */
    public static final int MTYPE_AUTHOK = 0x08;
    /** Message type - Buddy has added you to his contact list */
    public static final int MTYPE_ADDED = 0x0c;

    /** Message type - Plain text (simple) message */
    public static final int MTYPE_PLAIN = 0x01;
    /** Message type - Chat request message */
    public static final int MTYPE_CHAT = 0x02;
    /** Message type - File request / file ok message */
    public static final int MTYPE_FILEREQ = 0x03;
    /** Message type - URL message (0xFE formatted) */
    public static final int MTYPE_URL = 0x04;
    /** Message type - Message from OSCAR server (0xFE formatted) */
    public static final int MTYPE_SERVER = 0x09;
    /** Message type - Web pager message (0xFE formatted) */
    public static final int MTYPE_WWP = 0x0D;
    /** Message type - Email express message (0xFE formatted) */
    public static final int MTYPE_EEXPRESS = 0x0E;
    /** Message type - Contact list message */
    public static final int MTYPE_CONTACTS = 0x13;
    /** Message type - Plugin message described by text string */
    public static final int MTYPE_PLUGIN = 0x1A;
    /** Message type - Auto away message */
    public static final int MTYPE_AUTOAWAY = 0xE8;
    /** Message type - Auto occupied message */
    public static final int MTYPE_AUTOBUSY = 0xE9;
    /** Message type - Auto not available message */
    public static final int MTYPE_AUTONA = 0xEA;
    /** Message type - Auto do not disturb message */
    public static final int MTYPE_AUTODND = 0xEB;
    /** Message type - Auto free for chat message */
    public static final int MTYPE_AUTOFFC = 0xEC;

    private int messageType = -1;

    /** Information about the sender of this IM. */
    private final FullUserInfo userInfo;

    private long sender;
    private String reason;

    private long requestID = -1;

    public IcbmChannelFourCommand(SnacPacket packet)
    {
        super(IcbmCommand.CMD_ICBM, packet);

        requestID = packet.getReqid();

        DefensiveTools.checkNull(packet, "packet");

        ByteBlock snacData = getChannelData();

        userInfo = FullUserInfo.readUserInfo(snacData);

        ByteBlock tlvBlock = snacData.subBlock(userInfo.getTotalSize());

        TlvChain chain = TlvTools.readChain(tlvBlock);

        Tlv messageDataTlv = chain.getLastTlv(TYPE_MESSAGE_DATA);
        ByteBlock messageData = messageDataTlv.getData();

        sender = LEBinaryTools.getUInt(messageData, 0);

        messageType = LEBinaryTools.getUByte(messageData, 4);

        short msgFlags = LEBinaryTools.getUByte(messageData, 5);

        int textlen = LEBinaryTools.getUShort(messageData, 6) - 1;
        ByteBlock field = messageData.subBlock(8, textlen);


        // 0 is the charset code for ASCII in joastim
        // the encoding params will check for special system set encodings
        ImEncodingParams encoding = new ImEncodingParams(0);
        reason = ImEncodedString.readImEncodedString(encoding, field);
    }

    protected void writeChannelData(OutputStream out) throws IOException
    {
    }

    public String getReason()
    {
        return reason;
    }

    public long getSender()
    {
        return sender;
    }

    public FullUserInfo getUserInfo()
    {
        return userInfo;
    }

    public int getMessageType()
    {
        return messageType;
    }

    public long getRequestID()
    {
        return requestID;
    }
}
