/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.swing;

import java.awt.*;

/**
 * Through the <tt>AntialiasingManager</tt> the developer could activate the
 * antialiasing mechanism when painting. The method that do the job is
 * the <code>activateAntialiasing</code> method. It takes a <tt>Graphics</tt>
 * object and activates the antialiasing for it.
 * 
 * @author Yana Stamcheva
 */
public class AntialiasingManager {

    /**
     * Activates the antialiasing mechanism for the given <tt>Graphics</tt>
     * object.
     * @param g The <tt>Graphics</tt> object.
     */
    public static void activateAntialiasing(Graphics g)
    {
        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                        RenderingHints.VALUE_ANTIALIAS_ON);
    }
}
