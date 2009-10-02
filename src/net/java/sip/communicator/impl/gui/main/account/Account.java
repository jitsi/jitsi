/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.account;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Represents an account in the account list.
 * 
 * @author Yana Stamcheva
 */
public class Account
{
    private final ProtocolProviderService protocolProvider;

    private final String name;

    private final ImageIcon icon;

    /**
     * Creates an <tt>Account</tt> instance from the given
     * <tt>protocolProvider</tt>.
     * @param protocolProvider the protocol provider on which this account is
     * based
     */
    public Account(ProtocolProviderService protocolProvider)
    {
        this.protocolProvider = protocolProvider;

        this.name = protocolProvider.getAccountID().getDisplayName();

        this.icon = this.getProtocolIcon();
    }

    /**
     * Returns the protocol provider, on which this account is based.
     * @return the protocol provider, on which this account is based
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return protocolProvider;
    }

    /**
     * Returns the account name.
     * @return the account name
     */
    public String getName()
    {
        return name;
    }

    /**
     * The icon of the account.
     * @return the icon of the account
     */
    public ImageIcon getIcon()
    {
        return icon;
    }

    /**
     * Returns the status name.
     * @return the status name
     */
    public String getStatusName()
    {
        return getAccountStatus(protocolProvider);
    }

    /**
     * Returns the status icon of this account.
     * @return the status icon of this account
     */
    public ImageIcon getStatusIcon()
    {
        return ImageLoader.getAccountStatusImage(protocolProvider);
    }

    /**
     * Returns the current presence status of the given protocol provider.
     * 
     * @param protocolProvider the protocol provider which status we're looking
     * for.
     * @return the current presence status of the given protocol provider.
     */
    private String getAccountStatus(ProtocolProviderService protocolProvider)
    {
        String status;

        OperationSetPresence presence
            = protocolProvider.getOperationSet(OperationSetPresence.class);

        if (presence != null)
        {
            status = presence.getPresenceStatus().getStatusName();
        }
        else
        {
            status
                = GuiActivator
                    .getResources()
                        .getI18NString(
                            protocolProvider.isRegistered()
                                ? "service.gui.ONLINE"
                                : "service.gui.OFFLINE");
        }

        return status;
    }

    /**
     * Returns the protocol icon. If an icon 32x32 is available, returns it,
     * otherwise tries to scale a bigger icon if available. If we didn't find
     * a bigger icon to scale, we return null.
     *
     * @return the protocol icon
     */
    private ImageIcon getProtocolIcon()
    {
        ProtocolIcon protocolIcon = protocolProvider.getProtocolIcon();
        Image protocolImage
            = ImageLoader
                .getBytesInImage(
                    protocolIcon.getIcon(ProtocolIcon.ICON_SIZE_32x32));

        if (protocolImage != null)
        {
            return new ImageIcon(protocolImage);
        }
        else
        {
            protocolImage
                = ImageLoader
                    .getBytesInImage(
                        protocolIcon.getIcon(ProtocolIcon.ICON_SIZE_48x48));
            if (protocolImage == null)
                protocolImage
                    = ImageLoader
                        .getBytesInImage(
                            protocolIcon.getIcon(ProtocolIcon.ICON_SIZE_64x64));

            if (protocolImage != null)
                return ImageUtils.scaleIconWithinBounds(protocolImage, 32, 32);
        }

        return null;
    }
}
