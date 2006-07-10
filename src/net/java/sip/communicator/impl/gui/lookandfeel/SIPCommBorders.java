/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.lookandfeel;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicBorders;
import javax.swing.text.JTextComponent;

/**
 * SIPCommBorders is where all component borders used in the SIPComm L&F
 * are drawn.
 * 
 * @author Yana Stamcheva
 */
public class SIPCommBorders {

    /**
     * The RoundBorder is common border which is used throughout the 
     * SIPComm L&F.
     */
    public static class RoundBorder extends AbstractBorder
        implements UIResource{

        private static final Insets insets = new Insets(2, 2, 2, 2);

        public void paintBorder(Component c, Graphics g, int x, int y,
              int w, int h) {
            if (c.isEnabled()) {
                SIPCommLFUtils.drawRoundBorder(g, x, y, w, h, 5, 5);
            } else {
                SIPCommLFUtils.drawRoundDisabledBorder(g, x, y, w, h, 5, 5);
            }
        }
        public Insets getBorderInsets(Component c)       {
            return insets;
        }
        public Insets getBorderInsets(Component c, Insets newInsets) {
            newInsets.top = insets.top;
            newInsets.left = insets.left;
            newInsets.bottom = insets.bottom;
            newInsets.right = insets.right;
            
            return newInsets;
        }       
    }
    
    private static Border textFieldBorder;

    /**
     * Returns a border instance for a JTextField.
     */ 
    public static Border getTextFieldBorder() {
        if (textFieldBorder == null 
            || !(textFieldBorder instanceof SIPCommBorders.TextFieldBorder)) {
            textFieldBorder =  new BorderUIResource.CompoundBorderUIResource(
                    new SIPCommBorders.TextFieldBorder(),
                    new BasicBorders.MarginBorder());
        }
        return textFieldBorder;
    }

   
    /**
     * The TextField border which is used in SIPComm L&F for all text fields.
     */
    public static class TextFieldBorder extends RoundBorder {

        public void paintBorder(Component c, Graphics g, int x, int y,
                    int w, int h) {

          if (!(c instanceof JTextComponent)) {                
                if (c.isEnabled()) {
                    SIPCommLFUtils.drawRoundBorder(g, x, y, w, h, 5, 5);
                } else {
                    SIPCommLFUtils.drawRoundDisabledBorder(g, x, y, w, h, 5, 5);
                }
                return;
          }

          if (c.isEnabled() && ((JTextComponent)c).isEditable()) {
              SIPCommLFUtils.drawRoundBorder(g, x, y, w, h, 5, 5);
          } else {
              SIPCommLFUtils.drawRoundDisabledBorder(g, x, y, w, h, 5, 5);
          }
        }
    }
}
