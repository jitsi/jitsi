/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.swing;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

/**
 * The <tt>SIPCommTextField</tt> is a <tt>JTextField</tt> that offers the
 * possibility to specify a default (tip) text that explains what is the
 * required data.
 * @author Yana Stamcheva
 */
public class SIPCommTextField
    extends JTextField
    implements  MouseListener,
                FocusListener
{
    private final String defaultText;

    /**
     * Creates an instance of <tt>SIPCommTextField</tt> by specifying the text
     * we would like to show by default in it.
     * @param text the text we would like to enter by default
     */
    public SIPCommTextField(String text)
    {
        super(text);

        this.defaultText = text;

        this.setFont(getFont().deriveFont(10f));
        this.setForeground(Color.GRAY);

        this.addMouseListener(this);
        this.addFocusListener(this);
    }

    /**
     * Indicates that the mouse button was pressed on this component. Hides
     * the default text when user clicks on the text field.
     * @param e the <tt>MouseEvent</tt> that notified us
     */
    public void mousePressed(MouseEvent e)
    {
        if (getText() == null)
        {
            clearDefaultText();
        }
    }

    public void mouseClicked(MouseEvent e) {}

    public void mouseEntered(MouseEvent e) {}

    public void mouseExited(MouseEvent e) {}

    public void mouseReleased(MouseEvent e) {}

    /**
     * Selects the user text when this text field gains the focus.
     * @param e the <tt>FocusEvent</tt> that notified us
     */
    public void focusGained(FocusEvent e)
    {
        clearDefaultText();
    }

    /**
     * Sets the default text when the field looses focus.
     * @param e the <tt>FocusEvent</tt> that notified us
     */
    public void focusLost(FocusEvent e)
    {
        if (getText() == null || getText().length() == 0)
        {
            setDefaultText();
        }
    }

    /**
     * Returns the text contained in this field.
     * @return the text contained in this field
     */
    public String getText()
    {
        if (!super.getText().equals(defaultText))
            return super.getText();

        return null;
    }

    /**
     * Sets the text of this text field.
     * @param text the text to show in this text field
     */
    public void setText(String text)
    {
        if ((text == null || text.length() == 0) && !isFocusOwner())
            setDefaultText();
        else
        {
            this.setForeground(Color.BLACK);
            super.setText(text);
        }
    }

    /**
     * Sets the default text.
     */
    private void setDefaultText()
    {
        super.setText(defaultText);
        this.setForeground(Color.GRAY);
    }

    /**
     * Clears the default text.
     */
    private void clearDefaultText()
    {
        if (super.getText().equals(defaultText))
        {
            super.setText("");
            this.setForeground(Color.BLACK);
        }
    }
}
