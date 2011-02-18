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
 * The button responsible to start(the <tt>Call</tt> of) an associated
 * <tt>CallPariticant</tt>.
 *
 * @author Yana Stamcheva
 */
public class DesktopSharingButton
    extends AbstractCallToggleButton
{
    /**
     * Initializes a new <tt>DesktopSharingButton</tt> instance which is meant
     * to be used to initiate a desktop sharing during a call.
     *
     * @param call the <tt>Call</tt> to be associated with the desktop sharing
     * button instance
     */
    public DesktopSharingButton(Call call)
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
    public DesktopSharingButton(Call call, boolean fullScreen, boolean selected)
    {
        super(  call,
                fullScreen,
                selected,
                ImageLoader.CALL_DESKTOP_BUTTON,
                "service.gui.SHARE_DESKTOP_WITH_CONTACT");
    }

    /**
     * Shares the desktop with the peers in the current call.
     */
    public void buttonPressed()
    {
        if (call != null)
        {
            // If it's already enabled, we disable it.
            if (CallManager.isDesktopSharingEnabled(call))
            {
                CallManager.enableDesktopSharing(call, false);
            }
            // Otherwise we enable the desktop sharing.
            else
            {
                //We'll select the button once the desktop sharing  has been
                // established.
                setSelected(false);

                JPopupMenu sharingMenu = createDesktopSharingMenu();

                Point location = new Point(getX(), getY() + getHeight());

                SwingUtilities.convertPointToScreen(location, getParent());

                sharingMenu.setLocation(location);
                sharingMenu.setVisible(true);
            }
        }
    }

    /**
     * Creates the menu responsible for desktop sharing when a single desktop
     * sharing contact is available.
     *
     * @return the created popup menu
     */
    private JPopupMenu createDesktopSharingMenu()
    {
        final JPopupMenu popupMenu = new JPopupMenu(
            GuiActivator.getResources().getI18NString(
                "service.gui.SHARE_DESKTOP"));

        popupMenu.setInvoker(this);
        popupMenu.setFocusable(true);

        JMenuItem shareFullScreen = new JMenuItem(GuiActivator.getResources()
            .getI18NString("service.gui.SHARE_FULL_SCREEN"));

        JMenuItem shareRegion = new JMenuItem(GuiActivator.getResources()
            .getI18NString("service.gui.SHARE_REGION"));

        popupMenu.add(shareFullScreen);
        popupMenu.add(shareRegion);

        shareFullScreen.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                popupMenu.setVisible(false);
                CallManager.enableDesktopSharing(call, true);
            }
        });

        shareRegion.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                popupMenu.setVisible(false);

                CallManager.enableRegionDesktopSharing(call, true);
            }
        });

        return popupMenu;
    }
}
