/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.utils;

import java.awt.Component;
import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;

public class StringUtils {

	
	public static String replaceSpecialRegExpChars(String text){		
		return text.replaceAll("([.()^&$*|])", "\\\\$1");
	}
    
    public static int getStringWidth(Component c, String text){
        return SwingUtilities.computeStringWidth
                (c.getFontMetrics(Constants.FONT), text);
    }
}
