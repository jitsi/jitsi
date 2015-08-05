/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;

import org.jitsi.service.resources.*;

/**
 * The <tt>ChatConversationComponent</tt> is a component that can be added to
 * the conversation area of the chat window in order to display any special
 * events.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public abstract class ChatConversationComponent
    extends JPanel
    implements Skinnable
{
    /**
     * The logger for this class.
     */
    private static final Logger logger
        = Logger.getLogger(ChatConversationComponent.class);

    /**
     * The constraints used to layout this component.
     */
    protected final GridBagConstraints constraints = new GridBagConstraints();

    /**
     * Chat conversation default background color.
     */
    private static Color defaultColor
        = new Color(GuiActivator.getResources()
            .getColor("service.gui.CHAT_CONVERSATION_COMPONENT"));

    /**
     * Chat conversation default warning background color.
     */
    private static Color warningColor
        = new Color(GuiActivator.getResources()
            .getColor("service.gui.CHAT_CONVERSATION_WARNING_COMPONENT"));

    /**
     * Initializes the background color with the default color.
     */
    private Color backgroundColor = defaultColor;

    /**
     * The service through which we access resources like colors.
     */
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
     * Reloads color information.
     */
    public void loadSkin()
    {
        boolean defaultCol = false;
        if(backgroundColor == defaultColor)
            defaultCol = true;

        defaultColor = new Color(GuiActivator.getResources()
            .getColor("service.gui.CHAT_CONVERSATION_COMPONENT"));

        warningColor = new Color(GuiActivator.getResources()
            .getColor("service.gui.CHAT_CONVERSATION_WARNING_COMPONENT"));

        if(defaultCol)
            backgroundColor = defaultColor;
        else
            backgroundColor = warningColor;
    }

    /**
     * A specially customized button to fit better chat conversation component
     * look and feel.
     */
    protected class ChatConversationButton
        extends JButton
        implements Skinnable
    {
        /**
         * Initializes the <tt>ChatConversationButton</tt>.
         */
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
            loadSkin();

            setFont(getFont().deriveFont(Font.BOLD, 11f));
            setBorder(BorderFactory.createEmptyBorder());
            setBorderPainted(false);
            setOpaque(true);

            setContentAreaFilled(false);

            this.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseEntered(MouseEvent e)
                {
                    setCursor(new Cursor(Cursor.HAND_CURSOR));
                }

                @Override
                public void mouseExited(MouseEvent e)
                {
                    setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
            });
        }

        /**
         * Reloads color information.
         */
        public void loadSkin()
        {
            int color = GuiActivator.getResources()
                .getColor("service.gui.CHAT_LINK_COLOR");

            setForeground(new Color(color));
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
     *
     * @param g the <tt>Graphics</tt> object used for painting
     */
    @Override
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
        return ChatHtmlUtils.getDateString(date)
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
