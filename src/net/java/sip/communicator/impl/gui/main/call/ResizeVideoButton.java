/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The button responsible to resize video for the associated participant.
 *
 * @author Yana Stamcheva
 */
public class ResizeVideoButton
    extends AbstractCallToggleButton
{
    /**
     * Initializes a new <tt>DesktopSharingButton</tt> instance which is meant
     * to be used to initiate a desktop sharing during a call.
     *
     * @param call the <tt>Call</tt> to be associated with the desktop sharing
     * button instance
     */
    public ResizeVideoButton(Call call)
    {
        this(call, false, false);
    }

    /**
     * Initializes a new <tt>HoldButton</tt> instance which is to put a specific
     * <tt>CallPeer</tt> on/off hold.
     *
     * @param call  the <tt>Call</tt> to be associated with the new instance and
     * to be put on/off hold upon performing its action
     * @param fullScreen <tt>true</tt> if the new instance is to be used in
     * full-screen UI; otherwise, <tt>false</tt>
     * @param selected <tt>true</tt> if the new toggle button is to be initially
     * selected; otherwise, <tt>false</tt>
     */
    public ResizeVideoButton(Call call, boolean fullScreen, boolean selected)
    {
        super(  call,
                fullScreen,
                selected,
                ImageLoader.SD_VIDEO_BUTTON,
                "service.gui.CHANGE_VIDEO_QUALITY");
    }

    /**
     * Shares the desktop with the peers in the current call.
     */
    public void buttonPressed()
    {
        if (call != null)
        {
            //We'll select the button once the desktop sharing  has been
            // established.
            setSelected(false);

            JPopupMenu resizeMenu = createResizeVideoMenu();

            Point location = new Point(getX(), getY() + getHeight());

            SwingUtilities.convertPointToScreen(location, getParent());

            resizeMenu.setLocation(location);
            resizeMenu.setVisible(true);
        }
    }

    /**
     * Creates the menu responsible for desktop sharing when a single desktop
     * sharing contact is available.
     *
     * @return the created popup menu
     */
    private JPopupMenu createResizeVideoMenu()
    {
        final JPopupMenu popupMenu = new JPopupMenu(
            GuiActivator.getResources().getI18NString(
                "service.gui.CHANGE_VIDEO_QUALITY"));

        popupMenu.setInvoker(this);
        popupMenu.setFocusable(true);

        JMenuItem lowQuality = new JMenuItem(
            GuiActivator.getResources()
                .getI18NString("service.gui.LOW_QUALITY"),
            GuiActivator.getResources()
                .getImage("service.gui.icons.LO_VIDEO_ICON"));

        JMenuItem normalQuality = new JMenuItem(GuiActivator.getResources()
                .getI18NString("service.gui.SD_QUALITY"),
            GuiActivator.getResources()
                .getImage("service.gui.icons.SD_VIDEO_ICON"));

        JMenuItem hdQuality = new JMenuItem(GuiActivator.getResources()
                .getI18NString("service.gui.HD_QUALITY"),
            GuiActivator.getResources()
                .getImage("service.gui.icons.HD_VIDEO_ICON"));

        JLabel titleLabel = new JLabel(
            GuiActivator.getResources().getI18NString(
                "service.gui.CHANGE_VIDEO_QUALITY"));

        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        popupMenu.add(titleLabel);
        popupMenu.addSeparator();
        popupMenu.add(hdQuality);
        popupMenu.add(normalQuality);
        popupMenu.add(lowQuality);

        lowQuality.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                popupMenu.setVisible(false);

                setIconImageID(ImageLoader.LO_VIDEO_BUTTON);
                // TODO: set low quality.
            }
        });

        normalQuality.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                popupMenu.setVisible(false);

                setIconImageID(ImageLoader.SD_VIDEO_BUTTON);
                // TODO: set normal quality.
            }
        });

        hdQuality.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                popupMenu.setVisible(false);

                setIconImageID(ImageLoader.HD_VIDEO_BUTTON);
                // TODO: set hd quality.
            }
        });

        return popupMenu;
    }
}
