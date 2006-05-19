/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.icq.message.auth;

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
public class AuthOldMsgCmd
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

    private int messageType = -1;

	/** Information about the sender of this IM. */
    private final FullUserInfo userInfo;

	private long sender;
	private String reason;

    public AuthOldMsgCmd(SnacPacket packet)
	{
		super(IcbmCommand.CMD_ICBM, packet);

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
		reason = OscarTools.getString(field, "US-ASCII");

		/** @todo
			here we must fire some event because
			this is not common request response
			because response may come some days after ,
			after several opened and closed sessions
		 */
		if(messageType == MTYPE_AUTHREQ)
		{
			// this is a authorisation request with or without reason

			// unknown why the reason starts with some bytes offset ?
			// in the spec I found it must be like the auth deny
			System.out.println("user " + sender + " asks you for authorisation");

			//send deny
//			ICQClient.conn.getInfoService().sendSnac(
//				 new AuthReplyCmd(String.valueOf(sender), "Some reason!", false)
//				);
			// send accept
//			ICQClient.conn.getInfoService().sendSnac(
//				 new AuthReplyCmd(String.valueOf(sender), "Some reason!", true)
//				);

//            authorizationHandler.processAuthorisationRequest()
		}
		else
			if(messageType == MTYPE_AUTHDENY)
			{
				// this is authorisation reply deny
				// with or without reason
				System.out.println("user " + sender + " has denied your authorization request\n" +
								   "with reason : " + reason);
			}
			else
				if(messageType == MTYPE_AUTHOK)
				{
					// this is authorization reply with accept
					// with reason == null
					System.out.println("user " + sender + " has denied your authorization request");
				}
			else
				if(messageType == MTYPE_ADDED)
				{
					System.out.println("user " + sender + " has added you to contact list!");
				}
				else
				{
					System.out.println("unknown message for channel 4 of type 0x" +
									   Integer.toHexString(messageType));
				}
	}

	protected void writeChannelData(OutputStream out)
		throws IOException
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
}
