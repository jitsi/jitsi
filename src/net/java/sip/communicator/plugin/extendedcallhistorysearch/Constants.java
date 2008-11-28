/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.plugin.extendedcallhistorysearch;

import java.awt.*;

import net.java.sip.communicator.util.*;

/**
 * All look and feel related constants are stored here.
 *
 * @author Yana Stamcheva
 */

public class Constants {

    private static Logger logger = Logger.getLogger(Constants.class);

     /*
     * ===================================================================
     * ---------------------- CALLTYPE CONSTANTS -------------------------
     * ===================================================================
     */
    
    /**
     *  The incoming call flag.
     */
    public static final int INCOMING_CALL = 1;

    /**
     * The outgoing call flag.
     */
    public static final int OUTGOING_CALL = 2;

    /**
     * The Incoming & outcoming flag.
     */
    public static final int INOUT_CALL = 3;
    
    /*
     * ======================================================================
     * -------------------- FONTS AND COLOR CONSTANTS ------------------------
     * ======================================================================
     */
    /**
     * The color used to paint the background of an incoming call history
     * record.
     */
    public static final Color HISTORY_IN_CALL_COLOR
        = new Color(ExtendedCallHistorySearchActivator.getResources()
                .getColor("service.gui.HISTORY_INCOMING_CALL_BACKGROUND"));

    /**
     * The color used to paint the background of an outgoing call history
     * record.
     */
    public static final Color HISTORY_OUT_CALL_COLOR
        = new Color(ExtendedCallHistorySearchActivator.getResources()
                .getColor("service.gui.HISTORY_OUTGOING_CALL_BACKGROUND"));

    /**
     * The end color used to paint a gradient selected background of some
     * components.
     */
    public static final Color SELECTED_COLOR
        = new Color(ExtendedCallHistorySearchActivator.getResources()
                .getColor("service.gui.LIST_SELECTION_COLOR"));

    /**
     * The start color used to paint a gradient mouse over background of some
     * components.
     */
    public static final Color GRADIENT_DARK_COLOR
        = new Color(ExtendedCallHistorySearchActivator.getResources()
                .getColor("service.gui.GRADIENT_DARK_COLOR"));

    /**
     * The end color used to paint a gradient mouse over background of some
     * components.
     */
    public static final Color GRADIENT_LIGHT_COLOR
        = new Color(ExtendedCallHistorySearchActivator.getResources()
                .getColor("service.gui.GRADIENT_LIGHT_COLOR"));

    /**
     * A color between blue and gray used to paint some borders.
     */
    public static final Color BORDER_COLOR
        = new Color(ExtendedCallHistorySearchActivator.getResources()
                .getColor("service.gui.BORDER_COLOR"));

    /**
     * A color between blue and gray (darker than the other one), used to paint
     * some borders.
     */
    public static final Color LIST_SELECTION_BORDER_COLOR
        = new Color(ExtendedCallHistorySearchActivator.getResources()
                .getColor("service.gui.LIST_SELECTION_BORDER_COLOR"));

    /*
     * ======================================================================
     * --------------------------- FONT CONSTANTS ---------------------------
     * ======================================================================
     */

    /**
     * The name of the font used in this ui implementation.
     */
    public static final String FONT_NAME = "Verdana";

    /**
     * The size of the font used in this ui implementation.
     */
    public static final String FONT_SIZE = "12";

    /**
     * The default <tt>Font</tt> object used through this ui implementation.
     */
    public static final Font FONT = new Font(Constants.FONT_NAME,
            Font.PLAIN, new Integer(Constants.FONT_SIZE).intValue());
 
 
}
