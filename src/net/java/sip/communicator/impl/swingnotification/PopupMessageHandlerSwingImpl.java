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
package net.java.sip.communicator.impl.swingnotification;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.service.systray.event.*;
import net.java.sip.communicator.util.*;

/**
 * Implements <tt>PopupMessageHandler</tt> using Swing.
 *
 * @author Symphorien Wanko
 * @author Lubomir Marinov
 */
public class PopupMessageHandlerSwingImpl
    extends AbstractPopupMessageHandler
{
    /** logger for the <tt>PopupMessageHandlerSwingImpl</tt> class */
    private static final Logger logger
        = Logger.getLogger(PopupMessageHandlerSwingImpl.class);

    /**
     * Implements <tt>PopupMessageHandler#showPopupMessage()</tt>
     *
     * @param popupMessage the message we will show
     */
    public void showPopupMessage(final PopupMessage popupMessage)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    showPopupMessage(popupMessage);
                }
            });
            return;
        }

        final GraphicsConfiguration graphicsConf =
            GraphicsEnvironment.getLocalGraphicsEnvironment().
            getDefaultScreenDevice().
            getDefaultConfiguration();

        final JWindow notificationWindow = new JWindow(graphicsConf);

        final Timer popupTimer = new Timer(10000, new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (notificationWindow.isVisible())
                    new Thread(new PopupDiscarder(notificationWindow)).start();
            }
        });
        popupTimer.setRepeats(false);

        MouseAdapter adapter = new MouseAdapter()
        {
            @Override
            public void mouseEntered(MouseEvent e)
            {
                popupTimer.stop();
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                popupTimer.start();
            }

            @Override
            public void mouseClicked(MouseEvent e)
            {
                Container container = notificationWindow.getContentPane();
                PopupNotificationPanel notif =
                    (PopupNotificationPanel) container.getComponent(0);
                firePopupMessageClicked(
                    new SystrayPopupMessageEvent(e, notif.getTag()));
                notificationWindow.dispose();
            }
        };

        notificationWindow.addMouseListener(adapter);
        JComponent content = popupMessage.getComponent();
        if (content == null)
        {
            content = createPopup(
                popupMessage.getMessageTitle(),
                popupMessage.getMessage(),
                popupMessage.getIcon(),
                popupMessage.getTag());
        }
        registerMouseListener(content, adapter);
        notificationWindow.add(content);
        notificationWindow.setAlwaysOnTop(true);
        notificationWindow.pack();

        new Thread(new PopupLauncher(notificationWindow, graphicsConf)).start();
        popupTimer.start();
    }

    private void registerMouseListener(Component content, MouseAdapter adapter)
    {
        content.addMouseListener(adapter);
        if(content instanceof JComponent)
            for(Component c : ((JComponent) content).getComponents())
                registerMouseListener(c, adapter);
    }

    /**
     * Builds the popup component with given informations. Wraps the specified
     * <tt>message</tt> in HTML &lt;pre&gt; tags to ensure that text such as
     * full pathnames is displayed correctly after HTML is stripped from it.
     *
     * @param titleString message title
     * @param message message content
     * @param imageBytes message icon
     * @param tag
     * @return
     */
    private JComponent createPopup( String titleString,
                                    String message,
                                    byte[] imageBytes,
                                    Object tag)
    {
        JLabel msgIcon = null;
        if (imageBytes != null)
        {
            ImageIcon imageIcon
                = ImageUtils.getScaledRoundedIcon(imageBytes, 45, 45);

            msgIcon = new JLabel(imageIcon);
        }

        String plainMessage
            = Html2Text.extractText("<pre>" + message + "</pre>");
        JTextArea msgContent = new JTextArea(plainMessage);

        msgContent.setLineWrap(true);
        msgContent.setWrapStyleWord(true);
        msgContent.setOpaque(false);
        msgContent.setAlignmentX(JTextArea.LEFT_ALIGNMENT);

        int msgContentHeight
            = getPopupMessageAreaHeight(msgContent, plainMessage);
        msgContent.setPreferredSize(new Dimension(250, msgContentHeight));

        TransparentPanel notificationBody = new TransparentPanel();
        notificationBody.setLayout(
            new BoxLayout(notificationBody, BoxLayout.Y_AXIS));
        notificationBody.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        notificationBody.add(msgContent);

        TransparentPanel notificationContent
            = new TransparentPanel();

        notificationContent.setLayout(new BorderLayout(5, 0));

        notificationContent.setBorder(
                BorderFactory.createEmptyBorder(0, 5, 5, 5));

        if(msgIcon != null)
            notificationContent.add(msgIcon, BorderLayout.WEST);
        notificationContent.add(notificationBody, BorderLayout.CENTER);

        return new PopupNotificationPanel(titleString, notificationContent, tag);
    }

    /**
     * Implements <tt>toString</tt> from <tt>PopupMessageHandler</tt>
     * @return a description of this handler
     */
    @Override
    public String toString()
    {
        String applicationName
            = SwingNotificationActivator.getResources()
                .getSettingsString("service.gui.APPLICATION_NAME");

        return SwingNotificationActivator.getResources()
                .getI18NString("impl.swingnotification.POPUP_MESSAGE_HANDLER",
                    new String[]{applicationName});
    }

    /**
     * provide animation to hide a popup. The animation could be described
     * as an "inverse" of the one made by <tt>PopupLauncher</tt>.
     */
    private static class PopupDiscarder
        implements Runnable
    {
        private final JWindow notificationWindow;

        PopupDiscarder(JWindow notificationWindow)
        {
            this.notificationWindow = notificationWindow;
        }

        public void run()
        {
            int height = notificationWindow.getY();
            int x = notificationWindow.getX();
            do
            {
                notificationWindow.setLocation(
                    x,
                    notificationWindow.getY() + 2);
                try
                {
                    Thread.sleep(10);
                    height -= 2;
                } catch (InterruptedException ex)
                {
                    logger.warn("exception while discarding" +
                        " popup notification window :", ex);
                }
            } while (height > 0);
            notificationWindow.dispose();
        }
    }

    /**
     * provide animation to show a popup. The popup comes from the bottom of
     * screen and will stay in the bottom right corner.
     */
    private static class PopupLauncher
        implements Runnable
    {
        private final JWindow notificationWindow;

        private final int x;

        private final int y;

        PopupLauncher(
                JWindow notificationWindow,
                GraphicsConfiguration graphicsConf)
        {
            this.notificationWindow = notificationWindow;

            final Rectangle rec = graphicsConf.getBounds();

            final Insets ins =
                Toolkit.getDefaultToolkit().getScreenInsets(graphicsConf);

            x = rec.width + rec.x -
                ins.right - notificationWindow.getWidth() - 1;

            y = rec.height + rec.y -
                ins.bottom - notificationWindow.getHeight() - 1;

            notificationWindow.setLocation(x, rec.height);
            notificationWindow.setVisible(true);
        }

        public void run()
        {
            int height = y - notificationWindow.getY();
            do
            {
                notificationWindow.setLocation(
                    x,
                    notificationWindow.getY() - 2);
                try
                {
                    Thread.sleep(10);
                    height += 2;
                } catch (InterruptedException ex)
                {
                    logger.warn("exception while showing" +
                        " popup notification window :", ex);
                }
            } while (height < 0);
        }
    }

    /**
     * Returns the appropriate popup message height, according to the currently
     * used font and the size of the message.
     *
     * @param c the component used to show the message
     * @param message the message
     * @return the appropriate popup message height
     */
    private int getPopupMessageAreaHeight(Component c, String message)
    {
        int stringWidth = ComponentUtils.getStringWidth(c, message);

        int numberOfRows = 0;
        if (stringWidth/230 > 5)
            numberOfRows = 5;
        else
            numberOfRows = stringWidth/230 + 1;

        FontMetrics fontMetrics = c.getFontMetrics(c.getFont());

        return fontMetrics.getHeight()*Math.max(numberOfRows, 3)+5;
    }

    /**
     * Implements <tt>getPreferenceIndex</tt> from <tt>PopupMessageHandler</tt>.
     * This handler is able to show images, detect clicks, match a click to a
     * message, thus the preference index is 3.
     * @return a preference index
     */
    public int getPreferenceIndex()
    {
        return 3;
    }
}
