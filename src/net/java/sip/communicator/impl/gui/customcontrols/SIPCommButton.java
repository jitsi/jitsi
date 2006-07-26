/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.customcontrols;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import net.java.sip.communicator.impl.gui.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.impl.gui.utils.LightGrayFilter;

/**
 * The <tt>SIPCommButton</tt> is a very flexible <tt>JButton</tt> that allows
 * to configure its background, its icon, the look when a mouse is over it, etc.
 * 
 * @author Yana Stamcheva
 */
public class SIPCommButton extends JButton {

    public static final String LEFT_ICON_LAYOUT = "left";

    public static final String CENTER_ICON_LAYOUT = "center";

    public static final String RIGHT_ICON_LAYOUT = "right";

    private Image bgImage;

    private Image bgRolloverImage;

    private Image iconImage;

    private Image pressedImage;

    private String iconLayout = SIPCommButton.CENTER_ICON_LAYOUT;

    /**
     * Creates a button with icon.
     * 
     * @param iconImage The button icon image.
     * @param iconLayout The layout of the icon. One of the LEFT_ICON_LAYOUT,
     * CENTER_ICON_LAYOUT and RIGHT_ICON_LAYOUT.
     */
    public SIPCommButton(Image iconImage, String iconLayout) {
        super();

        this.iconLayout = iconLayout;

        this.setIcon(new ImageIcon(iconImage));
    }

    /**
     * Creates a button with custom background image, rollover image and
     * icon image.
     * 
     * @param bgImage       The background image.
     * @param rolloverImage The rollover image.
     * @param iconImage     The icon.
     */
    public SIPCommButton(Image bgImage, Image rolloverImage, Image iconImage) {
        super();

        this.bgImage = bgImage;
        this.bgRolloverImage = rolloverImage;
        this.iconImage = iconImage;

        this.setPreferredSize(new Dimension(this.bgImage.getWidth(null),
                this.bgImage.getHeight(null)));

        this.setIcon(new ImageIcon(this.bgImage));
    }

    /**
     * Creates a button with custom background image and rollover image.
     * 
     * @param bgImage The background button image.
     * @param rolloverImage The rollover button image.
     */
    public SIPCommButton(Image bgImage, Image rolloverImage) {
        super();

        this.bgImage = bgImage;
        this.bgRolloverImage = rolloverImage;

        this.setPreferredSize(new Dimension(this.bgImage.getWidth(null),
                this.bgImage.getHeight(null)));
    }

    /**
     * Overrides the <code>paintComponent</code> method of <tt>JButton</tt>
     * to paint the button background and icon, and all additional effects
     * of this configururable button.
     * 
     * @param g The Graphics object.
     */
    public void paintComponent(Graphics g) {
        AntialiasingManager.activateAntialiasing(g);

        if (this.bgImage != null) {
            // If there's no icon, we make grey the backgroundImage
            // when disabled.
            if (this.iconImage == null && !isEnabled()) {
                Image disabledImage = new ImageIcon(LightGrayFilter
                        .createDisabledImage(bgImage)).getImage();

                g.drawImage(disabledImage, 0, 0, this);
            } 
            else {
                g.drawImage(this.bgImage, 0, 0, this);
            }
        }

        if (this.iconImage != null) {
            if (!isEnabled()) {
                Image disabledImage = new ImageIcon(LightGrayFilter
                        .createDisabledImage(iconImage)).getImage();

                // draw the button icon depending the current button layout
                if (this.iconLayout
                            .equals(SIPCommButton.CENTER_ICON_LAYOUT)) {
                    g.drawImage(disabledImage,
                            (this.bgImage.getWidth(null) - disabledImage
                                    .getWidth(null)) / 2, (this.bgImage
                                    .getHeight(null) - disabledImage
                                    .getHeight(null)) / 2, this);
                }
                else if (this.iconLayout
                            .equals(SIPCommButton.LEFT_ICON_LAYOUT)) {
                    g.drawImage(disabledImage, 7,
                            (this.bgImage.getHeight(null) - disabledImage
                                    .getHeight(null)) / 2, this);
                }
                else if (this.iconLayout
                            .equals(SIPCommButton.LEFT_ICON_LAYOUT)) {
                    g.drawImage(disabledImage, this.bgImage.getWidth(null) - 3,
                            (this.bgImage.getHeight(null) - disabledImage
                                    .getHeight(null)) / 2, this);
                }
            } else {
                // draw the button icon depending the current button layout
                if (this.iconLayout
                            .equals(SIPCommButton.CENTER_ICON_LAYOUT)) {
                    g.drawImage(this.iconImage,
                            (this.bgImage.getWidth(null) - this.iconImage
                                    .getWidth(null)) / 2, (this.bgImage
                                    .getHeight(null) - this.iconImage
                                    .getHeight(null)) / 2, this);
                }
                else if (this.iconLayout
                            .equals(SIPCommButton.LEFT_ICON_LAYOUT)) {
                    g.drawImage(this.iconImage, 7,
                            (this.bgImage.getHeight(null) - this.iconImage
                                    .getHeight(null)) / 2, this);
                }
                else if (this.iconLayout
                            .equals(SIPCommButton.LEFT_ICON_LAYOUT)) {
                    g.drawImage(this.iconImage,
                            this.bgImage.getWidth(null) - 3, (this.bgImage
                                    .getHeight(null) - this.iconImage
                                    .getHeight(null)) / 2, this);
                }
            }

        }

        if (this.bgRolloverImage != null && this.getModel().isRollover()) {

            g.setColor(Constants.GRAY_COLOR);
            g.drawImage(this.bgRolloverImage, 0, 0, this);

            if (this.iconImage != null) {

                if (this.iconLayout
                            .equals(SIPCommButton.CENTER_ICON_LAYOUT)) {
                    g.drawImage(this.iconImage,
                            (this.bgImage.getWidth(null) - this.iconImage
                                    .getWidth(null)) / 2, (this.bgImage
                                    .getHeight(null) - this.iconImage
                                    .getHeight(null)) / 2, this);
                }
                else if (this.iconLayout
                            .equals(SIPCommButton.LEFT_ICON_LAYOUT)) {
                    g.drawImage(this.iconImage, 7,
                            (this.bgImage.getHeight(null) - this.iconImage
                                    .getHeight(null)) / 2, this);
                }
                else if (this.iconLayout
                            .equals(SIPCommButton.LEFT_ICON_LAYOUT)) {
                    g.drawImage(this.iconImage,
                            this.bgImage.getWidth(null) - 3, (this.bgImage
                                    .getHeight(null) - this.iconImage
                                    .getHeight(null)) / 2, this);
                }
            }
        }

        if (this.getModel().isPressed()) {

            if (this.pressedImage != null) {
                g.drawImage(this.pressedImage, 0, 0, this);
            } else {
                g.setColor(Constants.GRAY_COLOR);
                g.drawImage(this.bgRolloverImage, 0, 0, this);

                if (this.iconImage != null) {

                    if (this.iconLayout
                            .equals(SIPCommButton.CENTER_ICON_LAYOUT)) {
                        g.drawImage(this.iconImage,
                                (this.bgImage.getWidth(null) - this.iconImage
                                        .getWidth(null)) / 2 + 1, (this.bgImage
                                        .getHeight(null) - this.iconImage
                                        .getHeight(null)) / 2 + 1, this);
                    }
                    else if (this.iconLayout
                            .equals(SIPCommButton.LEFT_ICON_LAYOUT)) {
                        g.drawImage(this.iconImage, 7 + 1, (this.bgImage
                                .getHeight(null) - this.iconImage
                                .getHeight(null)) / 2 + 1, this);
                    }
                    else if (this.iconLayout
                            .equals(SIPCommButton.LEFT_ICON_LAYOUT)) {
                        g.drawImage(this.iconImage,
                                this.bgImage.getWidth(null) - 3 + 1,
                                (this.bgImage.getHeight(null) - this.iconImage
                                        .getHeight(null)) / 2 + 1, this);
                    }
                }
            }
        }
    }

    /**
     * Returns the background image of this button.
     * @return the background image of this button.
     */
    public Image getBgImage() {
        return bgImage;
    }

    /**
     * Sets the background image to this button.
     * @param bgImage The background image to set.
     */
    public void setBgImage(Image bgImage) {
        this.bgImage = bgImage;
    }

    /**
     * Returns the background rollover image of this button.
     * @return the background rollover image of this button.
     */
    public Image getBgRolloverImage() {
        return bgRolloverImage;
    }

    /**
     * Sets the background rollover image to this button.
     * @param bgRolloverImage The background rollover image to set.
     */
    public void setBgRolloverImage(Image bgRolloverImage) {
        this.bgRolloverImage = bgRolloverImage;
    }

    /**
     * Returns the icon image of this button.
     * @return the icon image of this button.
     */
    public Image getIconImage() {
        return iconImage;
    }

    /**
     * Sets the icon image to this button.
     * @param iconImage The icon image to set.
     */
    public void setIconImage(Image iconImage) {
        this.iconImage = iconImage;
    }
}
