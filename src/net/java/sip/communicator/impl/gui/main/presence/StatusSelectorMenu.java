/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.presence;

import java.awt.*;
import java.awt.image.*;
import java.util.List;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;

import org.jitsi.service.configuration.*;

/**
 * A parent class for all status selector boxes.
 * 
 * @see SimpleStatusMenu
 * @see GlobalStatusSelectorBox
 * 
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public abstract class StatusSelectorMenu
    extends SIPCommMenu
    implements  ImageObserver,
                Skinnable
{
    /**
     * The connecting icon.
     */
    private Image connectingIcon;

    /**
     * Indicates if this menu is currently connecting.
     */
    private boolean isConnecting;

    /**
     * The <tt>ProtocolProviderService</tt> associated with this status menu.
     */
    protected ProtocolProviderService protocolProvider;

    /**
     * Creates a <tt>StatusSelectorMenu</tt>.
     */
    public StatusSelectorMenu()
    {
        super();
    }

    /**
     * Creates a <tt>StatusSelectorMenu</tt> by specifying the text and icon to
     * show.
     * @param text the text of the menu
     * @param defaultIcon the icon of the menu
     * @param protocolProvider the <tt>ProtocolProviderService</tt> associated
     * with this status menu
     */
    public StatusSelectorMenu(  String text,
                                Icon defaultIcon,
                                ProtocolProviderService protocolProvider)
    {
        super(text, defaultIcon);

        this.protocolProvider = protocolProvider;

        loadSkin();
    }

    /**
     * Updates the current status.
     */
    public void updateStatus(){}

    /**
     * Returns the account index (In case of more than one account for one and
     * the same protocol).
     * 
     * @return the account index
     */
    public int getAccountIndex(){return -1;}

    /**
     * Sets the account index.
     * 
     * @param index
     */
    public void setAccountIndex(int index){}

    /**
     * Paints this component. If the state of this menu is connecting, paints
     * the connecting icon.
     *
     * @param g the <tt>Graphics</tt> object used for painting
     */
    public void paintComponent(Graphics g)
    {
        AntialiasingManager.activateAntialiasing(g);
        super.paintComponent(g);

        if(isConnecting)
            g.drawImage(connectingIcon, 0, 3, this);
    }

    /**
     * Starts the connecting.
     */
    public void startConnecting()
    {
        setConnecting(true);
    }

    /**
     * Stops the connecting.
     */
    public void stopConnecting()
    {
        setConnecting(false);
    }

    /**
     * Sets the connecting state for this menu.
     * @param isConnecting indicates if this menu is currently connecting
     */
    private void setConnecting(boolean isConnecting)
    {
        this.isConnecting = isConnecting;
        this.repaint();
    }

    /**
     * Updates the menu icon.
     * @param img the image to update
     * @param infoflags the info flags
     * @param x the x coordinate
     * @param y the y coordinate
     * @param w the width
     * @param h the height
     * @return <tt>true</tt> if the image has been updated, <tt>false</tt>
     * otherwise
     */
    public boolean imageUpdate(Image img, int infoflags,
                                int x, int y,
                                int w, int h)
    {
        repaint();
        return true;
    }

    /**
     * Returns the protocol provider associated with this status menu.
     * @return the protocol provider associated with this status menu
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return protocolProvider;
    }

    /**
     * Load connecting icon.
     */
    public void loadSkin()
    {
        connectingIcon
            = GuiActivator.getResources()
                .getImage("service.gui.icons.CONNECTING").getImage();
    }
}
