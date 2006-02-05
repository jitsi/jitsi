/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.utils;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Images {
	
	private static final String BUNDLE_NAME = "net.java.sip.communicator.impl.gui.main.utils.images";

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
			.getBundle(BUNDLE_NAME);

	private Images() {
	}

	public static String getString(String key) {
		
		try {
			return RESOURCE_BUNDLE.getString(key);
			
		} catch (MissingResourceException e) {
			
			return '!' + key + '!';
		}
	}
}
