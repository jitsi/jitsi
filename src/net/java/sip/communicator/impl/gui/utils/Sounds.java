/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.utils;

import java.util.*;

/**
 * Manages the access to the properties file containing all sounds paths.
 * 
 * @author Yana Stamcheva
 */
public class Sounds {
    
    public static String INCOMING_MESSAGE
        = "resources/sounds/incomingMessage.wav";
    
    public static String OUTGOING_CALL
        = "resources/sounds/ring.wav";
    public static String INCOMING_CALL
        = "resources/sounds/incomingCall.wav";
    
    public static String DIAL_ZERO
        = "resources/sounds/one_1.wav";
    
    public static String DIAL_ONE
        = "resources/sounds/one_1.wav";
    
    public static String DIAL_TWO
        = "resources/sounds/two_2.wav";
    
    public static String DIAL_THREE
        = "resources/sounds/three_3.wav";
    
    public static String DIAL_FOUR
        = "resources/sounds/four_4.wav";
    
    public static String DIAL_FIVE
        = "resources/sounds/five_5.wav";
    
    public static String DIAL_SIX
        = "resources/sounds/six_6.wav";
    
    public static String DIAL_SEVEN
        = "resources/sounds/seven_7.wav";
    
    public static String DIAL_EIGHT
        = "resources/sounds/eight_8.wav";
    
    public static String DIAL_NINE
        = "resources/sounds/nine_9.wav";
    
    public static String DIAL_DIEZ
        = "resources/sounds/one_1.wav";
    
    public static String DIAL_STAR
        = "resources/sounds/one_1.wav";
    
    public static String DIALING
        = "resources/sounds/dialing.wav";
    
    public static String BUSY
        = "resources/sounds/busy.wav";

    private static final String BUNDLE_NAME 
        = "net.java.sip.communicator.impl.gui.utils.sounds";

    private static final ResourceBundle RESOURCE_BUNDLE 
        = ResourceBundle.getBundle(BUNDLE_NAME);

    private Sounds() {
    }
}
