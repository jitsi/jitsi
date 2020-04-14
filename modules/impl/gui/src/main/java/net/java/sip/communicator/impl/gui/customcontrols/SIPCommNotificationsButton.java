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
package net.java.sip.communicator.impl.gui.customcontrols;

import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;

import javax.swing.*;
import java.awt.*;

/**
 * @author Damian Minkov
 */
public class SIPCommNotificationsButton
    extends SIPCommTextButton
{
    /**
     * The default icon.
     */
    protected Image defaultImage;

    /**
     * The pressed icon.
     */
    protected Image pressedImage;

    /**
     * The notification image.
     */
    protected Image notificationImage;

    /**
     * Indicates if this button currently shows the number of unread
     * notifications or the just the icon.
     */
    private boolean hasNotifications = false;

    /**
     * Indicates if the default is visible.
     * Used when toggling some view with this button.
     */
    private boolean defaultViewVisible = false;

    /**
     * Disables toggling of the button.
     */
    private boolean toggleDisabled = false;

    /**
     * Creates a <tt>SIPCommTextButton</tt>
     * @param text the text of the button
     */
    public SIPCommNotificationsButton(String text)
    {
        super(text);

        // All items are now instantiated and could safely load the skin.
        loadSkin();

        this.setForeground(Color.WHITE);
        this.setBorder(BorderFactory.createEmptyBorder());
        this.setFont(getFont().deriveFont(Font.BOLD, 10f));
        this.setBackground(new Color(255, 255, 255, 160));
    }

    /**
     * Loads images and sets history view.
     */
    public void loadSkin()
    {
        notificationImage
            = ImageLoader.getImage(
                ImageLoader.CALL_HISTORY_BUTTON_NOTIFICATION);

        this.setPreferredSize(new Dimension(defaultImage.getWidth(this),
            defaultImage.getHeight(this)));
    }

    public void setToggleDisabled(boolean disableToggle)
    {
        this.toggleDisabled = disableToggle;
    }

    public boolean isToggleDisabled()
    {
        return toggleDisabled;
    }

    public boolean hasNotifications()
    {
        return hasNotifications;
    }

    public void setHasNotifications(boolean hasNotifications)
    {
        this.hasNotifications = hasNotifications;
    }

    public boolean isDefaultViewVisible()
    {
        return defaultViewVisible;
    }

    public void setDefaultViewVisible(boolean defaultViewVisible)
    {
        this.defaultViewVisible = defaultViewVisible;
    }

    public void setNotifications(int count)
    {
        setHasNotifications(true);

        this.setBackground(new Color(200, 0, 0));
        this.setVerticalTextPosition(SwingConstants.TOP);

        Image iconImage = ImageLoader.getImage(notificationImage,
            new Integer(count).toString(), this);

        if (isDefaultViewVisible())
        {
            setBgImage(ImageLoader.getImage(
                pressedImage,
                iconImage,
                pressedImage.getWidth(null)/2
                    - notificationImage.getWidth(null)/2,
                0));
        }
        else
        {
            setBgImage(ImageLoader.getImage(
                defaultImage,
                iconImage,
                pressedImage.getWidth(null)/2
                    - notificationImage.getWidth(null)/2,
                0));
        }
    }

    public void clearNotifications()
    {
        if (hasNotifications())
            setHasNotifications(false);
        else
            setIcon(null);

        if (!isToggleDisabled() && isDefaultViewVisible())
        {
            setBgImage(pressedImage);
        }
        else
        {
            setBgImage(defaultImage);
        }
        setText("");
    }
}
