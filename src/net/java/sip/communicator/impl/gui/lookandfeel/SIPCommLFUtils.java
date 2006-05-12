/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.lookandfeel;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.plaf.metal.MetalLookAndFeel;

import net.java.sip.communicator.impl.gui.utils.AntialiasingManager;

/**
 * 
 * @author Yana Stamcheva
 */
public class SIPCommLFUtils {

    /**
     * This draws the "Round Border" which is used throughout the SIPComm L&F
     */
   static void drawRoundBorder(Graphics g, int x, int y, int w, int h, 
           int r1, int r2) {
       AntialiasingManager.activateAntialiasing(g);
       
       g.translate(x, y);
       
       g.setColor(SIPCommLookAndFeel.getControlDarkShadow());
       g.drawRoundRect( 0, 0, w-1, h-1 , r1, r2);
                     
       g.translate(-x, -y);
   }
   
   /**
    * This draws the "Round Disabled Border" which is used throughout the SIPComm L&F
    */
   static void drawRoundDisabledBorder(Graphics g, int x, int y, int w, int h, 
           int r1, int r2) {
       AntialiasingManager.activateAntialiasing(g);
       
       g.translate(x, y);
       g.setColor(SIPCommLookAndFeel.getControlShadow());
       
       g.drawRoundRect(0, 0, w-1, h-1, r1, r2);
       g.translate(-x, -y);
   }
}
