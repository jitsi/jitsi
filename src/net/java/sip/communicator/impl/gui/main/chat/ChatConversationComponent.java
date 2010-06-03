/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>ChatConversationComponent</tt> is a component that can be added to
 * the conversation area of the chat window in order to display any special
 * events.
 * 
 * @author Yana Stamcheva
 */
public abstract class ChatConversationComponent
    extends JPanel
{
    private static final Logger logger
        = Logger.getLogger(ChatConversationComponent.class);

    protected final GridBagConstraints constraints = new GridBagConstraints();

    private static final Color defaultColor
        = new Color(GuiActivator.getResources()
            .getColor("service.gui.CHAT_CONVERSATION_COMPONENT"));

    private static final Color warningColor
        = new Color(GuiActivator.getResources()
            .getColor("service.gui.CHAT_CONVERSATION_WARNING_COMPONENT"));

    private Color backgroundColor = defaultColor;

    protected static final ResourceManagementService resources
        = GuiActivator.getResources();

    /**
     * Creates a <tt>ChatConversationComponent</tt>.
     */
    public ChatConversationComponent()
    {
        this.setLayout(new GridBagLayout());
        this.setOpaque(false);
        this.setCursor(Cursor.getDefaultCursor());
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

        this.repaint();
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

    /**
     * Opens the given file through the <tt>DesktopService</tt>.
     * 
     * @param downloadFile the file to open
     */
    protected void openFile(File downloadFile)
    {
        try
        {
            GuiActivator.getDesktopService().open(downloadFile);
        }
        catch (IllegalArgumentException e)
        {
            if (logger.isDebugEnabled())
                logger.debug("Unable to open file.", e);

            this.showErrorMessage(
                resources.getI18NString(
                    "service.gui.FILE_DOES_NOT_EXIST"));
        }
        catch (NullPointerException e)
        {
            if (logger.isDebugEnabled())
                logger.debug("Unable to open file.", e);

            this.showErrorMessage(
                resources.getI18NString(
                    "service.gui.FILE_DOES_NOT_EXIST"));
        }
        catch (UnsupportedOperationException e)
        {
            if (logger.isDebugEnabled())
                logger.debug("Unable to open file.", e);

            this.showErrorMessage(
                resources.getI18NString(
                    "service.gui.FILE_OPEN_NOT_SUPPORTED"));
        }
        catch (SecurityException e)
        {
            if (logger.isDebugEnabled())
                logger.debug("Unable to open file.", e);

            this.showErrorMessage(
                resources.getI18NString(
                    "service.gui.FILE_OPEN_NO_PERMISSION"));
        }
        catch (IOException e)
        {
            if (logger.isDebugEnabled())
                logger.debug("Unable to open file.", e);

            this.showErrorMessage(
                resources.getI18NString(
                    "service.gui.FILE_OPEN_NO_APPLICATION"));
        }
        catch (Exception e)
        {
            if (logger.isDebugEnabled())
                logger.debug("Unable to open file.", e);

            this.showErrorMessage(
                resources.getI18NString(
                    "service.gui.FILE_OPEN_FAILED"));
        }
    }

    /**
     * Returns the date string to be used in order to show date and time in the
     * chat conversation component.
     * @param date the date to format
     * @return the date string to be used in order to show date and time in the
     * chat conversation component
     */
    public String getDateString(Date date)
    {
        return ChatConversationPanel.getDateString(date.getTime())
                + GuiUtils.formatTime(date)
                + " ";
    }

    /**
     * Returns the date of the component event.
     * 
     * @return the date of the component event
     */
    public abstract Date getDate();

    /**
     * Shows the given error message to the user. This method is made abstract
     * in order to allow extension classes to provide custom implementations
     * of how errors are shown to the users.
     * 
     * @param errorMessage the error message to show
     */
    protected abstract void showErrorMessage(String errorMessage);
}
