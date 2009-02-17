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

import net.java.sip.communicator.util.*;

/**
 * A custom panel to handle systray popup notification
 *
 * @author Symphorien Wanko
 */
public class PopupNotificationPanel extends SIPCommFrame.MainContentPane
{
    /** logger for this class */
    private final Logger logger = Logger.getLogger(SIPCommFrame.class);

    /**
     * Creates a new <tt>PopupNotificationPanel</tt> with a customized panel title
     */
    private PopupNotificationPanel()
    {
        JLabel notifTitle = new JLabel(
                UtilActivator.getResources().getSettingsString(
                "service.gui.APPLICATION_NAME"),
                UtilActivator.getResources().getImage(
                "service.gui.SIP_COMMUNICATOR_LOGO"),
                SwingConstants.LEFT);

        final JLabel notifClose = new JLabel(
                UtilActivator.getResources()
                .getImage("service.gui.lookandfeel.CLOSE_TAB_ICON"));
        notifClose.setToolTipText(UtilActivator.getResources()
                .getI18NString("service.gui.CLOSE"));

        notifClose.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                try
                {
                    // TODO : that is pretty ugly. It will be nice if
                    // it is possible to reach the top window in a better way
                    JWindow jw = (JWindow) notifClose
                            .getParent().getParent().getParent()
                            .getParent().getParent().getParent();
                    jw.dispose();
                }
                catch (Exception ex)
                {
                    // should never happens : if the user clicks on the close
                    // icon, it means that the popup window were visible
                    logger.warn("error while getting the popup window :"
                            , ex);
                }
            }
        });

        BorderLayout borderLayout = new BorderLayout();
        borderLayout.setVgap(5);

        JPanel notificationWindowTitle = new JPanel(borderLayout);
        notificationWindowTitle
                .setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        notificationWindowTitle.setOpaque(false);

        notificationWindowTitle.add(notifTitle, BorderLayout.WEST);
        notificationWindowTitle.add(notifClose, BorderLayout.EAST);

        JSeparator jSep = new JSeparator();

        notificationWindowTitle.add(jSep, BorderLayout.SOUTH);
        add(notificationWindowTitle, BorderLayout.NORTH);
        setBorder(BorderFactory.createLineBorder(Color.GRAY));
    }

    /**
     * Creates a new notifcation panel with <tt>notificationContent</tt> as
     * the component to put in that panel
     *
     * @param notificationContent content to add in the new created
     * <tt>PopupNotificationPanel</tt>
     */
    public PopupNotificationPanel(JPanel notificationContent)
    {
        this();
        add(notificationContent, BorderLayout.CENTER);
    }
}
