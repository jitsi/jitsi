/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.utils;

import net.java.sip.communicator.impl.gui.*;

/**
 * Manages the access to the properties file containing all sounds paths.
 * 
 * @author Yana Stamcheva
 */
public class SoundProperties
{
    public static String INCOMING_MESSAGE
        = GuiActivator.getResources().getSoundPath("INCOMING_MESSAGE");

    public static String OUTGOING_CALL
        = GuiActivator.getResources().getSoundPath("OUTGOING_CALL");

    public static String INCOMING_CALL
        = GuiActivator.getResources().getSoundPath("INCOMING_CALL");

    public static String DIAL_ZERO
        = GuiActivator.getResources().getSoundPath("DIAL_ZERO");

    public static String DIAL_ONE
        = GuiActivator.getResources().getSoundPath("DIAL_ONE");

    public static String DIAL_TWO
        = GuiActivator.getResources().getSoundPath("DIAL_TWO");

    public static String DIAL_THREE
        = GuiActivator.getResources().getSoundPath("DIAL_THREE");

    public static String DIAL_FOUR
        = GuiActivator.getResources().getSoundPath("DIAL_FOUR");

    public static String DIAL_FIVE
        = GuiActivator.getResources().getSoundPath("DIAL_FIVE");

    public static String DIAL_SIX
        = GuiActivator.getResources().getSoundPath("DIAL_SIX");

    public static String DIAL_SEVEN
        = GuiActivator.getResources().getSoundPath("DIAL_SEVEN");

    public static String DIAL_EIGHT
        = GuiActivator.getResources().getSoundPath("DIAL_EIGHT");

    public static String DIAL_NINE
        = GuiActivator.getResources().getSoundPath("DIAL_NINE");

    public static String DIAL_DIEZ
        = GuiActivator.getResources().getSoundPath("DIAL_DIEZ");

    public static String DIAL_STAR
        = GuiActivator.getResources().getSoundPath("DIAL_STAR");

    public static String BUSY
        = GuiActivator.getResources().getSoundPath("BUSY");

    private static final String BUNDLE_NAME 
        = "net.java.sip.communicator.impl.gui.utils.sounds";

    private SoundProperties() {
    }
}
