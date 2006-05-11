package net.java.sip.communicator.impl.gui.lookandfeel;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicBorders;
import javax.swing.text.JTextComponent;


public class SIPCommBorders {
    
    private static Border textFieldBorder;

    /**
     * Returns a border instance for a JTextField.
     */ 
    public static Border getTextFieldBorder() {        
        if (textFieldBorder == null 
            || !(textFieldBorder instanceof SIPCommBorders.TextFieldBorder)) {            
            textFieldBorder =  new SIPCommBorders.TextFieldBorder();
        }
        return textFieldBorder;
    }

    public static class TextFieldBorder extends AbstractBorder implements UIResource{

        private static final Insets insets = new Insets(2, 2, 2, 2);

        public void paintBorder(Component c, Graphics g, int x, int y,
              int w, int h) {
            g.setColor(Color.GRAY);
            if (c.isEnabled()) {
                g.drawRoundRect(x, y, w, h, 5, 5);
            } else {
                g.drawRoundRect(x, y, w, h, 5, 5);
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
}
