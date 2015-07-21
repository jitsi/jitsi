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
package net.java.sip.communicator.impl.gui.main.presence;

import java.awt.*;
import java.awt.image.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.account.*;
import net.java.sip.communicator.util.skin.*;

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
                StatusEntry,
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
     * The offline status if presence is supported, <tt>null</tt> otherwise.
     */
    private PresenceStatus offlineStatus;

    /**
     * The online status if presence is supported, <tt>null</tt> otherwise.
     */
    private PresenceStatus onlineStatus;

    /**
     * The presence if supported, <tt>null</tt> otherwise.
     */
    protected OperationSetPresence presence;

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

        this.presence
            = protocolProvider.getOperationSet(OperationSetPresence.class);

        this.onlineStatus
            = AccountStatusUtils.getOnlineStatus(protocolProvider);
        this.offlineStatus
            = AccountStatusUtils.getOfflineStatus(protocolProvider);

        loadSkin();
    }

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
    @Override
    public void paintComponent(Graphics g)
    {
        AntialiasingManager.activateAntialiasing(g);
        super.paintComponent(g);

        if(isConnecting)
            g.drawImage(getConnectingIcon(), 0, 3, this);
    }

    /**
     * Returns the connecting icon, if missing create it.
     * @return the connecting i con, if missing create it.
     */
    private Image getConnectingIcon()
    {
        if(connectingIcon == null)
        {
            connectingIcon
                = GuiActivator.getResources()
                    .getImage("service.gui.icons.CONNECTING").getImage();
        }

        return connectingIcon;
    }

    /**
     * Clears the connecting icon.
     */
    private void clearConnectingIcon()
    {
        if(connectingIcon != null)
        {
            connectingIcon.flush();
            connectingIcon = null;
        }
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
        clearConnectingIcon();

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
    @Override
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
     * Returns the Offline status in this selector box.
     *
     * @return the Offline status in this selector box
     */
    public PresenceStatus getOfflineStatus()
    {
        return offlineStatus;
    }

    /**
     * Returns the Online status in this selector box.
     *
     * @return the Online status in this selector box
     */
    public PresenceStatus getOnlineStatus()
    {
        return onlineStatus;
    }

    /**
     * Load connecting icon.
     */
    public void loadSkin()
    {
        clearConnectingIcon();
    }

    /**
     * Clears resources.
     */
    public void dispose()
    {
        protocolProvider = null;
        clearConnectingIcon();
        offlineStatus = null;
        onlineStatus = null;
    }

    /**
     * The component of this entry.
     * @return the component used to add to global status
     */
    public JMenuItem getEntryComponent()
    {
        return this;
    }
}
