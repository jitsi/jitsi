/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.icq.message;

import java.util.*;

import net.java.sip.communicator.impl.protocol.icq.message.common.*;
import net.java.sip.communicator.impl.protocol.icq.message.offline.*;
import net.java.sip.communicator.util.*;
import net.kano.joscar.*;
import net.kano.joscar.flapcmd.*;
import net.kano.joscar.snac.*;

/**
 * Registers the new commands implemented in this package.
 * So they can be handled from the stack.
 *
 * @author Damian Minkov
 */
public class DefaultCmdFactory
	implements SnacCmdFactory
{
	private static final Logger logger =
		Logger.getLogger(DefaultCmdFactory.class);

	protected static final List SUPPORTED_TYPES =
		DefensiveTools.asUnmodifiableList(new CmdType[]
										  {new CmdType(21, 3)});

	/**
	 * Attempts to convert the given SNAC packet to a
	 * <code>SnacCommand</code>.
	 *
	 * @param packet the packet to use for generation of a
	 *   <code>SnacCommand</code>
	 * @return an appropriate <code>SnacCommand</code> for representing the
	 *   given <code>SnacPacket</code>, or <code>null</code> if no such
	 *   object can be created
	 */
	public SnacCommand genSnacCommand(SnacPacket packet)
	{
		FromIcqCmd fromICQCmd = new FromIcqCmd(packet);

		if(fromICQCmd.getType().equals(AbstractIcqCmd.CMD_META_SHORT_INFO_CMD))
		{
			return null;
//				new MetaShortInfoCmd(fromICQCmd);
		}
		else
//		if(FullInfoCmd.isOfType(fromICQCmd.getType()))
//		{
//			return new FullInfoCmd(fromICQCmd);
//		}
//		else
		if(fromICQCmd.getType().equals(AbstractIcqCmd.CMD_OFFLINE_MSG) ||
		   fromICQCmd.getType().equals(AbstractIcqCmd.CMD_OFFLINE_MSG_DONE))
		{
			return new OfflineMsgCmd(fromICQCmd);
		}
		else
		{
			logger.debug("Packet Received we don't know about! " + packet);

			return null;
		}
	}

	/**
	 * Returns a list of the SNAC command types this factory can possibly
	 * convert to <code>SnacCommand</code>s.
	 *
	 * @return a list of command types that can be passed to
	 *   <code>genSnacCommand</code>
	 */
	public List getSupportedTypes()
	{
		return SUPPORTED_TYPES;
	}
}
