/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.history.records;

import net.java.sip.communicator.util.EnumerationBase;

/**
 * @author Alexander Pelov
 */
public final class TextType extends EnumerationBase {
	
	public static final TextType SHORT = new TextType("SHORT");
	public static final TextType LONG = new TextType("LONG");

	protected TextType(String description) {
		super(description);
	}
	
}
