package net.java.sip.communicator.impl.gui.main.customcontrols;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import net.java.sip.communicator.impl.gui.main.LookAndFeelConstants;

/**
 * @author Yana Stamcheva
 * 
 * The quick menu is composed of special buttons, which are specified here.
 */
public class SIPCommButton extends JButton {

	private Image bgImage;

	private Image bgRolloverImage;

	private Image iconImage;
	
	private int iconRightShift = 0;

	private int iconLeftShift = 0;
	
	public SIPCommButton() {
		super();

		this.bgImage = LookAndFeelConstants.QUICK_MENU_BUTTON_BG;
		this.bgRolloverImage = LookAndFeelConstants.QUICK_MENU_BUTTON_ROLLOVER_BG;
		this.setIcon(new ImageIcon(this.bgImage));

		this.setPreferredSize(new Dimension(this.bgImage.getWidth(null),
				this.bgImage.getHeight(null)));
	}

	public SIPCommButton(String text) {
		super(text);

		this.bgImage = LookAndFeelConstants.QUICK_MENU_BUTTON_BG;
		this.bgRolloverImage = LookAndFeelConstants.QUICK_MENU_BUTTON_ROLLOVER_BG;

		this.setPreferredSize(new Dimension(this.bgImage.getWidth(null),
				this.bgImage.getHeight(null)));
	}

	public SIPCommButton(Image iconImage) {
		super();

		this.iconImage = iconImage;
		this.bgImage = LookAndFeelConstants.QUICK_MENU_BUTTON_BG;
		this.bgRolloverImage = LookAndFeelConstants.QUICK_MENU_BUTTON_ROLLOVER_BG;

		this.setPreferredSize(new Dimension(this.bgImage.getWidth(null),
				this.bgImage.getHeight(null)));

		this.setIcon(new ImageIcon(this.bgImage));
	}

	public SIPCommButton(Image bgImage, Image rolloverImage, Image iconImage) {
		super();

		this.iconImage = iconImage;
		this.bgImage = bgImage;
		this.bgRolloverImage = rolloverImage;

		this.setPreferredSize(new Dimension(this.bgImage.getWidth(null),
				this.bgImage.getHeight(null)));

		this.setIcon(new ImageIcon(this.bgImage));
	}

	public void paint(Graphics g) {

		g.drawImage(this.bgImage, 0, 0, this);

		if (this.iconImage != null) {

			g.drawImage(this.iconImage,
							(this.bgImage.getWidth(null) - 
							this.iconImage.getWidth(null)) / 2 + 
							this.iconRightShift - 
							this.iconLeftShift,
							(this.bgImage.getHeight(null) - 
							this.iconImage.getHeight(null)) / 2, this);
		}

		if (this.getModel().isRollover()) {

			g.setColor(LookAndFeelConstants.CONTACTPANEL_LINES_COLOR);
			g.drawImage(this.bgRolloverImage, 0, 0, this);

			if (this.iconImage != null) {

				g.drawImage(this.iconImage,
								(this.bgImage.getWidth(null) - 
								this.iconImage.getWidth(null)) / 2 + 
								this.iconRightShift - 
								this.iconLeftShift, 
								(this.bgImage.getHeight(null) - 
								this.iconImage.getHeight(null)) / 2, this);
			}
		}
	}

	public Image getBgImage () {
		return bgImage;
	}

	public void setBgImage (Image bgImage) {
		this.bgImage = bgImage;
	}

	public Image getBgRolloverImage () {
		return bgRolloverImage;
	}

	public void setBgRolloverImage (Image bgRolloverImage) {
		this.bgRolloverImage = bgRolloverImage;
	}

	public Image getIconImage() {
		return iconImage;
	}

	public void setIconImage(Image iconImage) {
		this.iconImage = iconImage;
	}
	
	public void setIconRightShift (int iconRightShift) {
		this.iconRightShift = iconRightShift;
	}
	
	public void setIconLeftShift (int iconLeftShift) {
		this.iconLeftShift = iconLeftShift;
	}
	
	public int getIconRightShift (int iconRightShift) {
		return this.iconRightShift;
	}
	
	public int getIconLeftShift (int iconLeftShift) {
		return this.iconLeftShift;
	}
}
