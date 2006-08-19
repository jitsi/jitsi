/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.lookandfeel;

import java.awt.*;

import net.java.sip.communicator.impl.gui.utils.*;
/**
 * 
 * @author Yana Stamcheva
 */
public class SIPCommLFUtils {

    /**
     * Draws the "Round Border" which is used throughout the SIPComm L&F
     */
   static void drawRoundBorder(Graphics g, int x, int y, int w, int h, 
           int r1, int r2) {
       AntialiasingManager.activateAntialiasing(g);
       
       Graphics2D g2 = (Graphics2D) g;
       
       g2.translate(x, y);
       
       g2.setStroke(new BasicStroke(1.0f));
       g2.setColor(SIPCommLookAndFeel.getControlDarkShadow());
       
       g2.drawRoundRect( 0, 0, w-1, h-1 , r1, r2);
                     
       g2.translate(-x, -y);
   }
   
   /**
    * Draws the "Round Disabled Border" which is used throughout the SIPComm L&F
    */
   static void drawRoundDisabledBorder(Graphics g, int x, int y, int w, int h,
           int r1, int r2) {
       AntialiasingManager.activateAntialiasing(g);
       
       Graphics2D g2 = (Graphics2D) g;
       
       g2.translate(x, y);
       g2.setColor(SIPCommLookAndFeel.getControlShadow());
       g2.setStroke(new BasicStroke(1.0f));
       
       g2.drawRoundRect(0, 0, w-1, h-1, r1, r2);
       g2.translate(-x, -y);
   }
   
   
   /**
    * Draws the "Bold Round Disabled Border" which is used throughout the SIPComm L&F
    */
   static void drawBoldRoundBorder(Graphics g, int x, int y, int w, int h,
           int r1, int r2) {
       AntialiasingManager.activateAntialiasing(g);

       Graphics2D g2 = (Graphics2D) g;

       g2.setColor(Constants.BLUE_GRAY_BORDER_COLOR);
       g2.setStroke(new BasicStroke(1.5f));

       g2.drawRoundRect(x, y, w - 1, h - 1, r1, r2);

   }   
}
