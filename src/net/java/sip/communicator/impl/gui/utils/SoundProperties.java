/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.utils;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.resources.*;

/**
 * Manages the access to the properties file containing all sounds paths.
 * 
 * @author Yana Stamcheva
 */
public final class SoundProperties
{
    /**
     * The incoming message sound id.
     */
    public static final String INCOMING_MESSAGE;

    /**
     * The incoming file sound id.
     */
    public static final String INCOMING_FILE;

    /**
     * The outgoing call sound id.
     */
    public static final String OUTGOING_CALL;

    /**
     * The incoming call sound id.
     */
    public static final String INCOMING_CALL;

    /**
     * The zero tone sound id.
     */
    public static final String DIAL_ZERO;

    /**
     * The one tone sound id.
     */
    public static final String DIAL_ONE;

    /**
     * The two tone sound id.
     */
    public static final String DIAL_TWO;

    /**
     * The three tone sound id.
     */
    public static final String DIAL_THREE;

    /**
     * The four tone sound id.
     */
    public static final String DIAL_FOUR;

    /**
     * The five tone sound id.
     */
    public static final String DIAL_FIVE;

    /**
     * The six tone sound id.
     */
    public static final String DIAL_SIX;

    /**
     * The seven tone sound id.
     */
    public static final String DIAL_SEVEN;

    /**
     * The eight tone sound id.
     */
    public static final String DIAL_EIGHT;

    /**
     * The nine tone sound id.
     */
    public static final String DIAL_NINE;

    /**
     * The diez tone sound id.
     */
    public static final String DIAL_DIEZ;

    /**
     * The star tone sound id.
     */
    public static final String DIAL_STAR;

    /**
     * The busy sound id.
     */
    public static final String BUSY;

    /**
     * The dialing sound id.
     */
    public static final String DIALING;

    /**
     * The sound id of the sound played when call security is turned on.
     */
    public static final String CALL_SECURITY_ON;

    /**
     * The sound id of the sound played when a call security error occurs.
     */
    public static final String CALL_SECURITY_ERROR;

    /**
     * The hang up sound id.
     */
    public static final String HANG_UP;

    static
    {

        /*
         * Call GuiActivator.getResources() once because (1) it's not a trivial
         * getter, it caches the reference so it always checks whether the cache
         * has already been built and (2) accessing a local variable is supposed
         * to be faster than calling a method (even if the method is a trivial
         * getter and it's inlined at runtime, it's still supposed to be slower
         * because it will be accessing a field, not a local variable).
         */
        ResourceManagementService resources = GuiActivator.getResources();

        INCOMING_MESSAGE = resources.getSoundPath("INCOMING_MESSAGE");
        INCOMING_FILE = resources.getSoundPath("INCOMING_FILE");
        OUTGOING_CALL = resources.getSoundPath("OUTGOING_CALL");
        INCOMING_CALL = resources.getSoundPath("INCOMING_CALL");
        DIAL_ZERO = resources.getSoundPath("DIAL_ZERO");
        DIAL_ONE = resources.getSoundPath("DIAL_ONE");
        DIAL_TWO = resources.getSoundPath("DIAL_TWO");
        DIAL_THREE = resources.getSoundPath("DIAL_THREE");
        DIAL_FOUR = resources.getSoundPath("DIAL_FOUR");
        DIAL_FIVE = resources.getSoundPath("DIAL_FIVE");
        DIAL_SIX = resources.getSoundPath("DIAL_SIX");
        DIAL_SEVEN = resources.getSoundPath("DIAL_SEVEN");
        DIAL_EIGHT = resources.getSoundPath("DIAL_EIGHT");
        DIAL_NINE = resources.getSoundPath("DIAL_NINE");
        DIAL_DIEZ = resources.getSoundPath("DIAL_DIEZ");
        DIAL_STAR = resources.getSoundPath("DIAL_STAR");
        BUSY = resources.getSoundPath("BUSY");
        DIALING = resources.getSoundPath("DIAL");
        CALL_SECURITY_ON = resources.getSoundPath("CALL_SECURITY_ON");
        CALL_SECURITY_ERROR = resources.getSoundPath("CALL_SECURITY_ERROR");
        HANG_UP = resources.getSoundPath("HANG_UP");
    }

    private SoundProperties() {
    }
}
