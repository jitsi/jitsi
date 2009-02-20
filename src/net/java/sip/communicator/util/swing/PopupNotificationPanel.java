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

    /** an object to distinguish this <tt>PopupNotificationPanel</tt> */
    private Object tag;

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

//        JLabel notifClose = new JLabel(
//                UtilActivator.getResources()
//                .getImage("service.gui.lookandfeel.CLOSE_TAB_ICON"));

        final SIPCommButton notifClose = new SIPCommButton(
                UtilActivator.getResources()
                .getImage("service.gui.lookandfeel.CLOSE_TAB_ICON").getImage());
        notifClose.setToolTipText(UtilActivator.getResources()
                .getI18NString("service.gui.CLOSE"));
        //notifClose.

        notifClose.addActionListener(new ActionListener()
        {
            //@Override
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

            public void actionPerformed(ActionEvent e)
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

        JPanel notificationWindowTitle =
            new JPanel(new BorderLayout(0, 2));
        notificationWindowTitle
                .setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
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
     * @param tag an object to distinguish this <tt>PopupNotificationPanel</tt>
     */
    public PopupNotificationPanel(JPanel notificationContent, Object tag)
    {
        this();
        add(notificationContent, BorderLayout.CENTER);
        this.tag = tag;
    }

    /**
     * @return the tag
     */
    public Object getTag()
    {
        return tag;
    }

    /**
     * @param tag the tag to set
     */
    public void setTag(Object tag)
    {
        this.tag = tag;
    }
}
