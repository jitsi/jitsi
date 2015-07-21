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
package net.java.sip.communicator.impl.gui.main.account;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.globalstatus.*;

/**
 * Represents an account in the account list.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class Account
{
    /**
     * The corresponding protocol provider.
     */
    private ProtocolProviderService protocolProvider;

    /**
     * The identifier of the account.
     */
    private final AccountID accountID;

    /**
     * The display name of the account
     */
    private final String name;

    /**
     * The icon of the image.
     */
    private ImageIcon icon;

    /**
     * Indicates if the account is enabled.
     */
    private boolean isEnabled;

    /**
     * The corresponding check box in the account list.
     */
    private JCheckBox enableCheckBox;

    /**
     * Creates an <tt>Account</tt> instance from the given
     * <tt>protocolProvider</tt>.
     * @param protocolProvider the protocol provider on which this account is
     * based
     */
    public Account(ProtocolProviderService protocolProvider)
    {
        this.protocolProvider = protocolProvider;

        this.accountID = protocolProvider.getAccountID();

        this.name = accountID.getDisplayName();

        this.icon = getProtocolIcon(protocolProvider);

        this.isEnabled = accountID.isEnabled();
    }

    /**
     * Creates an account object with the given <tt>accountName</tt> and
     * <tt>icon</tt>.
     * @param accountID the identifier of the account
     */
    public Account(AccountID accountID)
    {
        this.accountID = accountID;

        this.name = accountID.getDisplayName();

        String iconPath = accountID.getAccountPropertyString(
            ProtocolProviderFactory.ACCOUNT_ICON_PATH);

        if (iconPath != null)
            this.icon = ImageLoader.getImageForPath(iconPath);

        this.isEnabled = accountID.isEnabled();
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
     * Returns the account identifier.
     * @return the account identifier
     */
    public AccountID getAccountID()
    {
        return accountID;
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
     * Returns the protocol name of the account.
     * @return the protocol name of the account
     */
    public String getProtocolName()
    {
        return accountID.getProtocolDisplayName();
    }

    /**
     * The icon of the account.
     * @return the icon of the account
     */
    public Icon getIcon()
    {
        return icon;
    }

    /**
     * Returns the status name.
     * @return the status name
     */
    public String getStatusName()
    {
        if (protocolProvider != null)
            return getAccountStatus(protocolProvider);
        else
            return GlobalStatusEnum.OFFLINE_STATUS;
    }

    /**
     * Returns the status icon of this account.
     * @return the status icon of this account
     */
    public Icon getStatusIcon()
    {
        if (protocolProvider != null)
            return ImageLoader.getAccountStatusImage(protocolProvider);
        else if (icon != null)
        {
            icon = ImageLoader.getImageForPath(
                accountID.getAccountPropertyString(
                    ProtocolProviderFactory.ACCOUNT_ICON_PATH));

            Image scaledImage
                = ImageUtils.scaleImageWithinBounds(icon.getImage(), 16, 16);

            if (scaledImage != null)
                return new ImageIcon(
                    GrayFilter.createDisabledImage(scaledImage));
        }
        return null;
    }

    /**
     * Returns <tt>true</tt> to indicate that this account is enabled,
     * <tt>false</tt> - otherwise.
     * @return <tt>true</tt> to indicate that this account is enabled,
     * <tt>false</tt> - otherwise
     */
    public boolean isEnabled()
    {
        return isEnabled;
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

        // If our account doesn't have a registered protocol provider we return
        // offline.
        if (protocolProvider == null)
            return GuiActivator.getResources()
            .getI18NString("service.gui.OFFLINE");

        OperationSetPresence presence
            = protocolProvider.getOperationSet(OperationSetPresence.class);

        if (presence != null)
        {
            status = presence.getPresenceStatus().getStatusName();
        }
        else
        {
            status = GuiActivator.getResources()
                        .getI18NString( protocolProvider.isRegistered()
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
     * @param protocolProvider the protocol provider, which icon we're looking
     * for
     * @return the protocol icon
     */
    private ImageIcon getProtocolIcon(ProtocolProviderService protocolProvider)
    {
        ProtocolIcon protocolIcon = protocolProvider.getProtocolIcon();
        Image protocolImage
            = ImageUtils.getBytesInImage(
                    protocolIcon.getIcon(ProtocolIcon.ICON_SIZE_32x32));

        if (protocolImage != null)
        {
            return new ImageIcon(protocolImage);
        }
        else
        {
            protocolImage = ImageUtils.getBytesInImage(
                protocolIcon.getIcon(ProtocolIcon.ICON_SIZE_48x48));

            if (protocolImage == null)
                protocolImage
                    = ImageUtils.getBytesInImage(
                        protocolIcon.getIcon(ProtocolIcon.ICON_SIZE_64x64));

            if (protocolImage != null)
                return ImageUtils.scaleIconWithinBounds(protocolImage, 32, 32);
        }

        return null;
    }

    /**
     * Sets the given <tt>protocolProvider</tt> to this account.
     * @param protocolProvider the <tt>ProtocolProviderService</tt>
     * corresponding to this account
     */
    public void setProtocolProvider(ProtocolProviderService protocolProvider)
    {
        this.protocolProvider = protocolProvider;
    }

    /**
     * Sets the <tt>isDisabled</tt> property.
     * @param isEnabled indicates if this account is currently
     * <tt>disabled</tt>
     */
    public void setEnabled(boolean isEnabled)
    {
        this.isEnabled = isEnabled;
    }

    /**
     * Sets the enable check box.
     * @param enableCheckBox the enable check box corresponding to this account
     */
    public void setEnableCheckBox(JCheckBox enableCheckBox)
    {
        this.enableCheckBox = enableCheckBox;
    }

    /**
     * Returns the enable check box corresponding to this account.
     * @return the enable check box corresponding to this account
     */
    public JCheckBox getEnableCheckBox()
    {
        return enableCheckBox;
    }

    /**
     * Returns the string representation of this account.
     *
     * @return the string representation of this account
     */
    @Override
    public String toString()
    {
        return accountID.getDisplayName();
    }
}
