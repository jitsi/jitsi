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
package net.java.sip.communicator.plugin.desktoputil.presence.avatar;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.presence.avatar.imagepicker.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.GenericDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.ImageDetail;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;

/**
 * The dialog used as menu.
 *
 * @author Damian Minkov
 */
public class SelectAvatarMenu
    extends SIPCommPopupMenu
    implements ActionListener
{
    /**
     * Logger for this class.
     */
    private static final Logger logger =
            Logger.getLogger(SelectAvatarMenu.class);

    /**
     * Name of choose button.
     */
    private static final String CHOSE_BUTTON_NAME = "chooseButton";

    /**
     * Name of remove button.
     */
    private static final String REMOVE_BUTTON_NAME = "removeButton";

    /**
     * Name of clear button.
     */
    private static final String CLEAR_BUTTON_NAME = "clearButton";

    /**
     * Images shown as history.
     */
    private static final int MAX_STORED_IMAGES = 8;

    /**
     * Ordered in columns.
     */
    private static final int IMAGES_PER_COLUMN = 4;

    /**
     * Thumbnail width.
     */
    private static final int THUMB_WIDTH = 48;

    /**
     * Thumbnail height.
     */
    private static final int THUMB_HEIGHT = 48;

    /**
     * Buttons corresponding to images.
     */
    private SIPCommButton recentImagesButtons[] =
        new SIPCommButton[MAX_STORED_IMAGES];

    /**
     * Next free image index number.
     */
    private int nextImageIndex = 0;

    /**
     * The parent button using us.
     */
    private FramedImageWithMenu avatarImage;

    /**
     * The AccountID that we want to select avatar for. Could be null if
     * we want to select a global avatar.
     */
    private AccountID accountID;

    /**
     * Creates the dialog.
     * @param avatarImage the button that will trigger this menu.
     */
    public SelectAvatarMenu(FramedImageWithMenu avatarImage)
    {
        this.avatarImage = avatarImage;

        init();

        this.pack();
    }

    public void setAccountID(AccountID accountID)
    {
        this.accountID = accountID;
    }

    /**
     * Init visible components.
     */
    private void init()
    {
        TransparentPanel panel = new TransparentPanel(new BorderLayout());

        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Title label
        JLabel titleLabel = new JLabel(DesktopUtilActivator.getResources()
                .getI18NString("service.gui.avatar.RECENT_ICONS"));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));

        // fix for displaying text in menu
        // when using a dark OS theme (as default one in ubuntu)
        titleLabel.setForeground(new JMenuItem().getForeground());

        panel.add(titleLabel, BorderLayout.NORTH);


        // Init recent images grid
        TransparentPanel recentImagesGrid = new TransparentPanel();
        recentImagesGrid.setLayout(new GridLayout(0, IMAGES_PER_COLUMN));

        Dimension thumbsize = new Dimension(THUMB_WIDTH, THUMB_HEIGHT);
        for (int i=0; i < MAX_STORED_IMAGES; i++)
        {
            this.recentImagesButtons[i] = new SIPCommButton(null);
            this.recentImagesButtons[i].setBorder(BorderFactory.createEtchedBorder());
            this.recentImagesButtons[i].setMaximumSize(thumbsize);
            this.recentImagesButtons[i].setMinimumSize(thumbsize);
            this.recentImagesButtons[i].setPreferredSize(thumbsize);
            this.recentImagesButtons[i].addActionListener(this);
            this.recentImagesButtons[i].setName("" + i);
            recentImagesGrid.add(this.recentImagesButtons[i]);
        }

        panel.add(recentImagesGrid, BorderLayout.CENTER);

        // Action buttons
        TransparentPanel buttonsPanel = new TransparentPanel();
        buttonsPanel.setLayout(new GridLayout(0, 1));

        // we use this menu item just to get its foreground color.
        Color linkColor = new JMenuItem().getForeground();

        addActionButton(buttonsPanel, this,
            DesktopUtilActivator.getResources().getI18NString(
                    "service.gui.avatar.CHOOSE_ICON"),
                CHOSE_BUTTON_NAME,
                linkColor);
        addActionButton(buttonsPanel, this,
            DesktopUtilActivator.getResources().getI18NString(
                    "service.gui.avatar.REMOVE_ICON"),
                REMOVE_BUTTON_NAME,
                linkColor);
        addActionButton(buttonsPanel, this,
            DesktopUtilActivator.getResources().getI18NString(
                    "service.gui.avatar.CLEAR_RECENT"),
                CLEAR_BUTTON_NAME,
                linkColor);

        panel.add(buttonsPanel, BorderLayout.SOUTH);

        this.setLayout(new BorderLayout());
        this.add(panel, BorderLayout.CENTER);
    }

    /**
     * Adds action buttons.
     * @param buttonsPanel the panel to add to.
     * @param listener the listener for actions
     * @param text the text on the button.
     * @param name name of the button.
     * @param linkColor the color of the link.
     */
    private static void addActionButton(
            TransparentPanel buttonsPanel, ActionListener listener,
            String text, String name, Color linkColor)
    {
        SIPCommLinkButton button = new SIPCommLinkButton(text);
        button.setName(name);
        button.addActionListener(listener);
        button.setOpaque(false);
        button.setLinkColor(linkColor);

        TransparentPanel panel = new TransparentPanel(new BorderLayout());
        panel.add(button, BorderLayout.WEST);
        buttonsPanel.add(panel);
    }

    @Override
    public void setVisible(boolean b)
    {
        refreshRecentImages();
        super.setVisible(b);
    }

    /**
     * Refresh images with those stored locally.
     */
    public void refreshRecentImages()
    {
        int i;

        for (i = 0; i < MAX_STORED_IMAGES; i++)
        {
            BufferedImage image = AvatarStackManager.loadImage(i);
            if (image == null)
                break;

            this.recentImagesButtons[i].setImage(createThumbnail(image));
            this.recentImagesButtons[i].setEnabled(true);
        }

        if (i < MAX_STORED_IMAGES)
        {
            this.nextImageIndex = i;

            for (; i < MAX_STORED_IMAGES; i++)
            {
                this.recentImagesButtons[i].setImage(null);
                this.recentImagesButtons[i].setEnabled(false);
            }
        }
        else
            this.nextImageIndex = MAX_STORED_IMAGES;
    }

    /**
     * Create thumbnail for the image.
     * @param image to scale.
     * @return the thumbnail image.
     */
    private static BufferedImage createThumbnail(BufferedImage image)
    {
        int width = image.getWidth();
        int height = image.getHeight();

        // Image smaller than the thumbnail size
        if (width < THUMB_WIDTH && height < THUMB_HEIGHT)
            return image;

        Image i;

        if (width > height)
            i = image.getScaledInstance(THUMB_WIDTH, -1, Image.SCALE_SMOOTH);
        else
            i = image.getScaledInstance(-1, THUMB_HEIGHT, Image.SCALE_SMOOTH);

        return ImageUtils.getBufferedImage(i);
    }

    /**
     * Here is all the action. Stores the selected image into protocols and if
     * needed update it ina AccountStatusPanel.
     *
     * @param image the new image.
     */
    private void setNewImage(final BufferedImage image)
    {
        // Use separate thread to be sure we don't block UI thread.
        new Thread()
        {
            @Override
            public void run()
            {
                AccountManager accountManager
                        = DesktopUtilActivator.getAccountManager();

                for (AccountID accountID : accountManager.getStoredAccounts())
                {
                    if (accountManager.isAccountLoaded(accountID))
                    {
                        ProtocolProviderService protocolProvider
                            = AccountUtils.getRegisteredProviderForAccount(
                                accountID);

                        if(protocolProvider != null
                           && protocolProvider.isRegistered())
                        {
                            // If account id is set this means that we want to
                            // edit our current account image, not the global
                            // avatar. Hence, we might not want to save this
                            // account image on the server yet. For example: in
                            // the account info plugin the user might set a new
                            // avatar and then click the cancel button.
                            if (SelectAvatarMenu.this.accountID != null)
                            {
                                if (accountID.equals(
                                    SelectAvatarMenu.this.accountID))
                                {
                                    OperationSetServerStoredAccountInfo opSet =
                                        protocolProvider.getOperationSet(
                                            OperationSetServerStoredAccountInfo.class);
                                    if (opSet != null)
                                    {
                                        byte[] imageByte = null;
                                        if (image != null)
                                        {
                                            imageByte =
                                                ImageUtils.toByteArray(image);
                                        }
                                        avatarImage.setImageIcon(imageByte);
                                        ImageDetail newDetail =
                                            new ImageDetail(
                                                "avatar", imageByte);

                                        Iterator<GenericDetail> oldDetail =
                                            opSet.getDetails(ImageDetail.class);
                                        try
                                        {
                                            if (oldDetail.hasNext())
                                            {
                                                opSet.replaceDetail(
                                                    oldDetail.next(),
                                                    newDetail);
                                            }
                                            else
                                                opSet.addDetail(newDetail);
                                        }
                                        catch (Throwable t)
                                        {
                                            logger.error(
                                                "Error setting image", t);
                                        }
                                    }
                                }
                            }
                            else
                            {
                                OperationSetAvatar opSetAvatar
                                    = protocolProvider
                                        .getOperationSet(OperationSetAvatar.class);

                                if(opSetAvatar != null)
                                {
                                    byte[] imageByte = null;
                                    // Sets new avatar if not null. Otherwise, the
                                    // opSetAvatar.setAvatar(null) will removes the
                                    // current one.
                                    if(image != null)
                                    {
                                        imageByte = ImageUtils.toByteArray(image);
                                    }
                                    try
                                    {
                                        opSetAvatar.setAvatar(imageByte);
                                    }
                                    catch(Throwable t)
                                    {
                                        logger.error("Error setting image", t);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }.start();
    }

    /**
     * Clear stored images.
     */
    private void clearRecentImages()
    {
        for (int i=0; i < MAX_STORED_IMAGES; i++)
        {
            this.recentImagesButtons[i].setImage(null);
            this.recentImagesButtons[i].setEnabled(false);
            AvatarStackManager.deleteImage(i);
        }

        this.nextImageIndex = 0;
    }

    /**
     * Action performed on various action links(buttons).
     *
     * @param e the action.
     */
    public void actionPerformed(ActionEvent e)
    {
        JButton src = (JButton) e.getSource();

        if (src instanceof SIPCommButton)
        {
            // Load image
            int index = Integer.parseInt(src.getName());
            BufferedImage image = AvatarStackManager.loadImage(index);

            // Set the new image
            setNewImage(image);
        }
        else if (src.getName().equals("chooseButton"))
        {
            // Open the image picker
            Image currentImage = this.avatarImage.getAvatar();

            ImagePickerDialog dialog = new ImagePickerDialog(96, 96);

            byte[] bimage = dialog.showDialog(currentImage);

            if(bimage == null)
                return;

            // New image
            BufferedImage image = ImageUtils.getBufferedImage(
                    new ImageIcon(bimage).getImage());

            // Store image
            if (this.nextImageIndex == MAX_STORED_IMAGES)
            {
                // No more place to store images
                // Pop the first element (index 0)
                AvatarStackManager.popFirstImage(MAX_STORED_IMAGES);

                this.nextImageIndex = MAX_STORED_IMAGES - 1;
            }

            // Store the new image on hard drive
            AvatarStackManager.storeImage(image, this.nextImageIndex);

            // Inform protocols about the new image
            setNewImage(image);
        }
        else if (src.getName().equals("removeButton"))
        {
            // Removes the current photo.
            setNewImage(null);
        }
        else if (src.getName().equals("clearButton"))
        {
            clearRecentImages();
        }

        setVisible(false);
    }
}
