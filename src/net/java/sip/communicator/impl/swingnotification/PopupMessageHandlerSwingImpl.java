/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.swingnotification;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

import javax.swing.Timer;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.service.systray.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * An implementation of <tt>PopupMessageHandler</tt> using swing.
 * @author Symphorien Wanko
 */
public class PopupMessageHandlerSwingImpl implements PopupMessageHandler
{
    /** logger for the <tt>PopupMessageHandlerSwingImpl</tt> class */
    private final Logger logger =
        Logger.getLogger(PopupMessageHandlerSwingImpl.class);

    /** The list of all added popup listeners */
    private final List<SystrayPopupMessageListener> popupMessageListeners =
        new Vector<SystrayPopupMessageListener>();

    /** An icon representing the contact from which the notification comes */
    private ImageIcon defaultIcon =
        SwingNotificationActivator.getResources().getImage(
        "service.gui.SIP_COMMUNICATOR_LOGO_45x45");;

    /**
     * Adds a listerner to receive popup events
     * @param listener the listener to add
     */
    public void addPopupMessageListener(SystrayPopupMessageListener listener)
    {
        synchronized (popupMessageListeners)
        {
            if (!popupMessageListeners.contains(listener))
                popupMessageListeners.add(listener);
        }
    }

    /**
     * Removes a listerner previously added with <tt>addPopupMessageListener</tt>
     * @param listener the listener to remove
     */
    public void removePopupMessageListener(SystrayPopupMessageListener listener)
    {
        synchronized (popupMessageListeners)
        {
            popupMessageListeners.remove(listener);
        }
    }

    /**
     * Implements <tt>PopupMessageHandler#showPopupMessage()</tt>
     *
     * @param popupMessage the message we will show
     */
    public void showPopupMessage(PopupMessage popupMessage)
    {
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

        notificationWindow.addMouseListener(new MouseAdapter()
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
        });

        if (popupMessage.getComponent() != null)
        {
            notificationWindow.add(popupMessage.getComponent());
        }
        else
        {
            notificationWindow.add(createPopup(
                popupMessage.getMessageTitle(),
                popupMessage.getMessage(),
                popupMessage.getIcon(),
                popupMessage.getTag()));
        }
        notificationWindow.setAlwaysOnTop(true);
        notificationWindow.pack();

        new Thread(new PopupLauncher(notificationWindow, graphicsConf)).start();
        popupTimer.start();
    }

    /**
     * builds the popup component with given informations.
     * 
     * @param title message title
     * @param message message content
     * @param icon message icon
     * @return
     */
    private JComponent createPopup( String titleString,
                                    String message,
                                    byte[] imageBytes,
                                    Object tag)
    {
        JLabel msgIcon = new JLabel(defaultIcon);

        if (imageBytes != null)
        {
            ImageIcon imageIcon
                = ImageUtils.getScaledRoundedIcon(imageBytes, 45, 45);

            msgIcon = new JLabel(imageIcon);
        }

        JLabel msgTitle = new JLabel(titleString);
        int msgTitleHeight
            = msgTitle.getFontMetrics(msgTitle.getFont()).getHeight();
        msgTitle.setPreferredSize(new Dimension(200, msgTitleHeight));
        msgTitle.setFont(msgTitle.getFont().deriveFont(Font.BOLD));

        String plainMessage = Html2Text.extractText(message);
        JTextArea msgContent = new JTextArea(plainMessage);
        msgContent.setLineWrap(true);
        msgContent.setWrapStyleWord(true);
        msgContent.setOpaque(false);
        msgContent.setAlignmentX(JTextArea.LEFT_ALIGNMENT);

        int msgContentHeight
            = getPopupMessageAreaHeight(msgContent, plainMessage);
        msgContent.setPreferredSize(new Dimension(200, msgContentHeight));

        TransparentPanel notificationBody = new TransparentPanel();
        notificationBody.setLayout(
            new BoxLayout(notificationBody, BoxLayout.Y_AXIS));
        notificationBody.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        notificationBody.add(msgTitle);
        notificationBody.add(msgContent);

        TransparentPanel notificationContent
            = new TransparentPanel();

        notificationContent.setLayout(new BorderLayout(5, 0));

        notificationContent.setBorder(
                BorderFactory.createEmptyBorder(5, 5, 5, 5));

        notificationContent.add(msgIcon, BorderLayout.WEST);
        notificationContent.add(notificationBody, BorderLayout.CENTER);

        return new PopupNotificationPanel(notificationContent, tag);
    }

    /**
     * Notifies all interested listeners that a <tt>SystrayPopupMessageEvent</tt>
     * occured.
     *
     * @param SystrayPopupMessageEvent the evt to send to listener.
     */
    private void firePopupMessageClicked(SystrayPopupMessageEvent evt)
    {
        logger.trace("Will dispatch the following popup event: " + evt);

        List<SystrayPopupMessageListener> listeners;
        synchronized (popupMessageListeners)
        {
            listeners =
                new ArrayList<SystrayPopupMessageListener>(
                popupMessageListeners);
        }

        for (SystrayPopupMessageListener listener : listeners)
            listener.popupMessageClicked(evt);
    }

    /**
     * Implements <tt>toString</tt> from <tt>PopupMessageHandler</tt>
     * @return a description of this handler
     */
    public String toString()
    {
        return SwingNotificationActivator.getResources()
            .getI18NString("impl.swingnotification.POPUP_MESSAGE_HANDLER");
    }

    /**
     * provide animation to hide a popup. The animation could be described
     * as an "inverse" of the one made by <tt>PopupLauncher</tt>.
     */
    class PopupDiscarder implements Runnable
    {

        private JWindow notificationWindow;

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
    class PopupLauncher implements Runnable
    {

        private final JWindow notificationWindow;

        private final int x;

        private final int y;

        PopupLauncher(JWindow notificationWindow,
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
        int stringWidth = GuiUtils.getStringWidth(c, message);

        int numberOfRows = 0;
        if (stringWidth/200 > 3)
            numberOfRows = 3;
        else
            numberOfRows = stringWidth/200;

        FontMetrics fontMetrics = c.getFontMetrics(c.getFont());

        return fontMetrics.getHeight()*numberOfRows;
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
