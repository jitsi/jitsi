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

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;
import net.java.sip.communicator.util.skin.*;

/**
 * @author Damian Minkov
 */
public class ReadonlyStatusItem
    extends JMenuItem
    implements  StatusEntry,
                Skinnable
{
    /**
     * Our logger.
     */
    private final Logger logger = Logger.getLogger(ReadonlyStatusItem.class);

    /**
     * The connecting icon.
     */
    private Image connectingIcon;

    /**
     * Indicates if this menu is currently connecting.
     */
    private boolean isConnecting;

    /**
     * The offline status.
     */
    private PresenceStatus offlineStatus;

    /**
     * The online status.
     */
    private PresenceStatus onlineStatus;

    /**
     * The <tt>ProtocolProviderService</tt> associated with this status menu.
     */
    protected ProtocolProviderService protocolProvider;

    /**
     * Constructs the item with default values.
     *
     * @param protocolProvider the provider to represent.
     */
    public ReadonlyStatusItem(ProtocolProviderService protocolProvider)
    {
        super(protocolProvider.getAccountID().getDisplayName(),
            ImageLoader.getAccountStatusImage(protocolProvider));
        this.protocolProvider = protocolProvider;

        loadSkin();

        String tooltip =
            "<html><b>" + protocolProvider.getAccountID().getDisplayName()
                + "</b><br>Connecting</html>";

        this.setToolTipText(tooltip);

        this.offlineStatus
            = AccountStatusUtils.getOfflineStatus(protocolProvider);
        this.onlineStatus
            = AccountStatusUtils.getOnlineStatus(protocolProvider);

        this.setSelectedStatus(offlineStatus);
        updateStatus(offlineStatus);
    }

    /**
     * Selects the given status in the status menu.
     *
     * @param status the status to select
     */
    public void setSelectedStatus(PresenceStatus status)
    {
        Icon statusImage = ImageLoader.getAccountStatusImage(protocolProvider);

        this.setIcon(statusImage);

        String tooltip = this.getToolTipText();

        tooltip = tooltip.substring(0, tooltip.lastIndexOf("<br>"));

        if(status != null)
            this.setToolTipText(
                    tooltip.concat("<br>" + status.getStatusName()));
    }

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
     * @return the connecting icon, if missing create it.
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
    @Override
    public void loadSkin()
    {
        clearConnectingIcon();
        this.setIcon(ImageLoader.getAccountStatusImage(protocolProvider));
    }

    /**
     * Clears resources.
     */
    public void dispose()
    {
        protocolProvider = null;
        clearConnectingIcon();
        onlineStatus = null;
        offlineStatus = null;
    }

    /**
     * The component of this entry.
     * @return the component used to add to global status
     */
    public JMenuItem getEntryComponent()
    {
        return this;
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
     * Selects a specific <tt>PresenceStatus</tt> in this instance and the
     * <tt>ProtocolProviderService</tt> it depicts.
     *
     * @param presenceStatus the <tt>PresenceStatus</tt> to be selected in this
     * instance and the <tt>ProtocolProviderService</tt> it depicts
     */
    public void updateStatus(PresenceStatus presenceStatus)
    {
        if (logger.isTraceEnabled())
            logger.trace("Update status for provider: "
                + protocolProvider.getAccountID().getAccountAddress()
                + ". The new status will be: " + presenceStatus.getStatusName());

        this.setSelectedStatus(presenceStatus);
    }
}
