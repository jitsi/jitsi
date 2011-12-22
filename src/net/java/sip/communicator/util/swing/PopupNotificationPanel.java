/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.util.swing;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;

/**
 * A custom panel to handle systray popup notification
 *
 * @author Symphorien Wanko
 * @author Adam Netocny
 */
public class PopupNotificationPanel
    extends SIPCommFrame.MainContentPane
    implements Skinnable
{
    /**
     * Logger for this class.
     **/
    private final Logger logger = Logger.getLogger(SIPCommFrame.class);

    /**
     * An object to distinguish this <tt>PopupNotificationPanel</tt>
     */
    private Object tag;

    /**
     * Close button.
     */
    private final SIPCommButton notifClose;

    /**
     * Notification title.
     */
    private JLabel notifTitle;

    /**
     * Creates a new <tt>PopupNotificationPanel</tt> with a customized panel
     * title.
     * @param titleString The title of the popup
     */
    private PopupNotificationPanel(String titleString)
    {
        notifTitle = new JLabel(
                UtilActivator.getResources().getSettingsString(
                    "service.gui.APPLICATION_NAME")
                    + (StringUtils.isNullOrEmpty(titleString, true)
                        ? ""
                        : ": " + titleString),
                SwingConstants.LEFT);

        notifClose = new SIPCommButton();

        notifClose.setToolTipText(UtilActivator.getResources()
                .getI18NString("service.gui.CLOSE"));

        notifClose.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    Window parentWindow
                        = SwingUtilities.getWindowAncestor(
                            PopupNotificationPanel.this);

                    parentWindow.dispose();
                }
                catch (Exception ex)
                {
                    // should never happens : if the user clicks on the close
                    // icon, it means that the popup window were visible
                    logger.warn("Error while getting the popup window :", ex);
                }
            }
        });

        JPanel notificationWindowTitle
            = new JPanel(new BorderLayout(0, 2));
        notificationWindowTitle
                .setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        notificationWindowTitle.setOpaque(false);
        notificationWindowTitle.add(notifTitle, BorderLayout.WEST);
        notificationWindowTitle.add(notifClose, BorderLayout.EAST);

        JSeparator jSep = new JSeparator();

        notificationWindowTitle.add(jSep, BorderLayout.SOUTH);

        add(notificationWindowTitle, BorderLayout.NORTH);
        setBorder(BorderFactory.createLineBorder(Color.GRAY));

        // All items are now instantiated and could safely load the skin.
        loadSkin();
    }

    /**
     * Creates a new notification panel with <tt>notificationContent</tt> as
     * the component to put in that panel
     * 
     * @param titleString The title of the popup
     * @param notificationContent content to add in the new created
     * <tt>PopupNotificationPanel</tt>
     * @param tag an object to distinguish this <tt>PopupNotificationPanel</tt>
     */
    public PopupNotificationPanel(String titleString,
        JPanel notificationContent, Object tag)
    {
        this(titleString);
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

    /**
     * Reloads resources for this component.
     */
    public void loadSkin()
    {
        notifTitle.setIcon(UtilActivator.getResources().getImage(
                "service.gui.SIP_COMMUNICATOR_LOGO"));
        notifClose.setBackgroundImage(UtilActivator.getResources()
                .getImage("service.gui.lookandfeel.CLOSE_TAB_ICON").getImage());
    }
}
