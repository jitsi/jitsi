/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>ChatConversationComponent</tt> is a component that can be added to
 * the conversation area of the chat window in order to display any special
 * events.
 * 
 * @author Yana Stamcheva
 */
public class ChatConversationComponent
    extends JPanel
{
    protected final GridBagConstraints constraints = new GridBagConstraints();

    private static final Color defaultColor
        = new Color(GuiActivator.getResources()
            .getColor("service.gui.CHAT_CONVERSATION_COMPONENT"));

    private static final Color warningColor
        = new Color(GuiActivator.getResources()
            .getColor("service.gui.CHAT_CONVERSATION_WARNING_COMPONENT"));

    private Color backgroundColor = defaultColor;

    /**
     * Creates a <tt>ChatConversationComponent</tt>.
     */
    public ChatConversationComponent()
    {
        this.setLayout(new GridBagLayout());
        this.setOpaque(false);
    }

    /**
     * A specially customized button to fit better chat conversation component
     * look and feel.
     */
    protected class ChatConversationButton extends JButton
    {
        public ChatConversationButton()
        {
            init();
        }

        /**
         * Create a new RolloverButton.
         *
         * @param text the button text.
         * @param icon the button icon.
         */
        public ChatConversationButton(String text, Icon icon)
        {
            super(text, icon);
            init();
        }

        /**
         * Decorates the button with the appropriate UI configurations.
         */
        private void init()
        {
            int color = GuiActivator.getResources()
                .getColor("service.gui.CHAT_LINK_COLOR");

            setForeground(new Color(color));
            setFont(getFont().deriveFont(Font.BOLD, 11f));
            setBorder(BorderFactory.createEmptyBorder());
            setBorderPainted(false);
            setOpaque(true);

            setContentAreaFilled(false);

            this.addMouseListener(new MouseAdapter()
            {
                public void mouseEntered(MouseEvent e)
                {
                    setCursor(new Cursor(Cursor.HAND_CURSOR));
                }

                public void mouseExited(MouseEvent e)
                {
                    setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
            });
        }
    }

    /**
     * Updates the background color to catch user attention if anything
     * unexpected has happened.
     * 
     * @param isWarningStyle <code>true</code> to indicate that the warning
     * style should be set, <code>false</code> - otherwise.
     */
    protected void setWarningStyle(boolean isWarningStyle)
    {
        if (isWarningStyle)
            backgroundColor = warningColor;
        else
            backgroundColor = defaultColor;
    }

    /**
     * Call a custom internal paint.
     */
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        g = g.create();
        try
        {
            internalPaintComponent(g);
        }
        finally
        {
            g.dispose();
        }
    }

    /**
     * Paints a round background for this component.
     * 
     * @param g the Graphics object
     */
    private void internalPaintComponent(Graphics g)
    {
        AntialiasingManager.activateAntialiasing(g);

        Graphics2D g2 = (Graphics2D) g;

        g2.setColor(backgroundColor);
        g2.fillRoundRect(
            1, 1, this.getWidth() - 1, this.getHeight() -1, 15, 15);
    }
}
