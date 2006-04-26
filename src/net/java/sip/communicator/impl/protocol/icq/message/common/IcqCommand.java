/*
 *  Copyright (c) 2003, The Joust Project
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *  - Neither the name of the Joust Project nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *  FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *  COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *  ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 *
 *  File created by jkohen @ Oct 12, 2003
 *
 */

package net.java.sip.communicator.impl.protocol.icq.message.common;

import java.io.IOException;
import java.io.OutputStream;

import net.kano.joscar.DefensiveTools;
import net.kano.joscar.snaccmd.conn.SnacFamilyInfo;

/**
 * A base class for commands in the "old ICQ" <code>0x15</code> SNAC
 * family. This is used for those parts of the protocol that are ICQ-specific.
 */
public abstract class IcqCommand
{
	/** The SNAC family code of this family. */
	public static final int FAMILY_ICQ = 0x0015;

	/** A SNAC family info block for this family. */
	public static final SnacFamilyInfo FAMILY_INFO
		= new SnacFamilyInfo(FAMILY_ICQ, 0x0001, 0x0110, 0x08e4);

	/** Send a command to the old ICQ server. */
	public static final int CMD_TO_ICQ = 0x0002;
	/** A message from the old ICQ server. */
	public static final int CMD_FROM_ICQ = 0x0003;

	/** The command subtype. */
	private IcqType type;

	/**
	 * Creates a new <code>RvCommand</code> with properties read from the given
	 * incoming <code>RecvRvIcbm</code>.
	 *
	 * @param icbm an incoming RV ICBM command
	 */
	protected IcqCommand(FromIcqCmd cmd)
	{
		DefensiveTools.checkNull(cmd, "cmd");

		type = cmd.getType();
	}

	/**
	 * Creates a command object in the buddy status family with the given
	 * command subtype.
	 *
	 * @param command this command's SNAC command subtype
	 * @param type1 the old ICQ command subtype
	 */
	protected IcqCommand(IcqType type)
	{
		this.type = type;
	}

	/**
	 * Returns this ICQ-specific command's subtype.
	 *
	 * @return the old ICQ command subtype.
	 */
	public final IcqType getType()
	{
		return type;
	}

	/**
	 * Writes this ICQ command's "data block" to the given stream.
	 *
	 * @param out the stream to which to write
	 *
	 * @throws IOException if an I/O error occurs
	 */
	public abstract void writeIcqData(OutputStream out)
		throws IOException;
}
