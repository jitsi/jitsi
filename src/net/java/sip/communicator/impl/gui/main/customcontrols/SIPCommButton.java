package net.java.sip.communicator.impl.gui.main.customcontrols;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import net.java.sip.communicator.impl.gui.main.utils.Constants;
import net.java.sip.communicator.impl.gui.main.utils.AntialiasingManager;

/**
 * @author Yana Stamcheva
 * 
 * The quick menu is composed of special buttons, which are specified here.
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
	 * Default constructor
	 */
	
	public SIPCommButton() {
		super();

		this.bgImage = Constants.BUTTON_BG;
		this.bgRolloverImage = Constants.BUTTON_ROLLOVER_BG;
		this.setIcon(new ImageIcon(this.bgImage));

		this.setPreferredSize(new Dimension(this.bgImage.getWidth(null),
				this.bgImage.getHeight(null)));
	}

	/**
	 * Button with text.
	 * 
	 * @param text
	 */
	public SIPCommButton(String text) {
		super(text);

		this.bgImage = Constants.BUTTON_BG;
		this.bgRolloverImage = Constants.BUTTON_ROLLOVER_BG;

		this.setPreferredSize(new Dimension(this.bgImage.getWidth(null),
				this.bgImage.getHeight(null)));
	}
	
	
	/**
	 * Button with icon.
	 * 
	 * @param iconImage
	 */
	public SIPCommButton(Image iconImage) {
		super();

		this.iconImage = iconImage;
		this.bgImage = Constants.BUTTON_BG;
		this.bgRolloverImage = Constants.BUTTON_ROLLOVER_BG;
		
		this.setPreferredSize(new Dimension(this.bgImage.getWidth(null),
				this.bgImage.getHeight(null)));

		this.setIcon(new ImageIcon(this.bgImage));
	}

	/**
	 * Button with icon.
	 * 
	 * @param iconImage
	 */
	public SIPCommButton(Image iconImage, String iconLayout) {
		super();

		this.iconImage = iconImage;
		this.iconLayout = iconLayout;
		this.bgImage = Constants.BUTTON_BG;
		this.bgRolloverImage = Constants.BUTTON_ROLLOVER_BG;
		
		this.setPreferredSize(new Dimension(this.bgImage.getWidth(null),
				this.bgImage.getHeight(null)));

		this.setIcon(new ImageIcon(this.bgImage));
	}
	
	/**
	 * Custom button.
	 * 
	 * @param bgImage The background image.
	 * @param rolloverImage The rollover image
	 * @param iconImage The icon.
	 */
	public SIPCommButton (Image bgImage, Image rolloverImage, Image iconImage) {
		super();

		this.iconImage = iconImage;
		this.bgImage = bgImage;
		this.bgRolloverImage = rolloverImage;

		this.setPreferredSize(new Dimension(this.bgImage.getWidth(null),
				this.bgImage.getHeight(null)));

		this.setIcon(new ImageIcon(this.bgImage));
	}

	/**
	 * Custom button.
	 * 
	 * @param bgImage 		The background image.
	 * @param rolloverImage The rollover image
	 * @param iconImage 	The button icon.
	 * @param pressedImage 	The image when button is pressed.
	 */
	public SIPCommButton (	Image bgImage, 
							Image rolloverImage,
							Image pressedImage,
							Image iconImage) {
		super();

		this.iconImage = iconImage;
		this.bgImage = bgImage;
		this.bgRolloverImage = rolloverImage;
		this.pressedImage = pressedImage;
		
		this.setPreferredSize(new Dimension(this.bgImage.getWidth(null),
				this.bgImage.getHeight(null)));

		this.setIcon(new ImageIcon(this.bgImage));
	}
	
	public SIPCommButton(Image bgImage, Image rolloverImage) {
		super();
		
		this.bgImage = bgImage;
		this.bgRolloverImage = rolloverImage;
		
		this.setPreferredSize(new Dimension(this.bgImage.getWidth(null),
				this.bgImage.getHeight(null)));

		this.setIcon(new ImageIcon(this.bgImage));
	}
	
	
	public SIPCommButton(String text, Image bgImage, Image rolloverImage) {
		super(text);
		
		this.bgImage = bgImage;
		this.bgRolloverImage = rolloverImage;
		
		this.setPreferredSize(new Dimension(this.bgImage.getWidth(null) + 80,
				this.bgImage.getHeight(null)));
	}
	
	/**
	 * Paint the SIPCommButton.
	 */	
	public void paint(Graphics g) {	
		AntialiasingManager.activateAntialiasing(g);
		
		g.drawImage(this.bgImage, 0, 0, this);

		if (this.iconImage != null) {

			//draw the button icon depending the current layout
			if (this.iconLayout.equals(SIPCommButton.CENTER_ICON_LAYOUT))
				g.drawImage(this.iconImage,
								(this.bgImage.getWidth(null) - 
								this.iconImage.getWidth(null)) / 2,
								(this.bgImage.getHeight(null) - 
								this.iconImage.getHeight(null)) / 2, this);
			
			else if (this.iconLayout.equals(SIPCommButton.LEFT_ICON_LAYOUT))
				g.drawImage(this.iconImage,
							7, 
							(this.bgImage.getHeight(null) - 
							this.iconImage.getHeight(null)) / 2, 
							this);
			
			else if (this.iconLayout.equals(SIPCommButton.LEFT_ICON_LAYOUT))
				g.drawImage(this.iconImage,
						this.bgImage.getWidth(null) - 3, 
						(this.bgImage.getHeight(null) - 
						this.iconImage.getHeight(null)) / 2, 
						this);
		}

		if (this.getModel().isRollover()) {

			g.setColor(Constants.CONTACTPANEL_LINES_COLOR);
			g.drawImage(this.bgRolloverImage, 0, 0, this);

			if (this.iconImage != null) {

				if (this.iconLayout.equals(SIPCommButton.CENTER_ICON_LAYOUT))
					g.drawImage(this.iconImage,
									(this.bgImage.getWidth(null) - 
									this.iconImage.getWidth(null)) / 2,
									(this.bgImage.getHeight(null) - 
									this.iconImage.getHeight(null)) / 2, this);
				
				else if (this.iconLayout.equals(SIPCommButton.LEFT_ICON_LAYOUT))
					g.drawImage(this.iconImage,
								7, 
								(this.bgImage.getHeight(null) - 
								this.iconImage.getHeight(null)) / 2, 
								this);
				
				else if (this.iconLayout.equals(SIPCommButton.LEFT_ICON_LAYOUT))
					g.drawImage(this.iconImage,
							this.bgImage.getWidth(null) - 3, 
							(this.bgImage.getHeight(null) - 
							this.iconImage.getHeight(null)) / 2, 
							this);
			}
		}
		
		if (this.getModel().isPressed()) {
			
			if(this.pressedImage != null) {
				g.drawImage(this.pressedImage, 0, 0, this);
			}
			else {
				g.setColor(Constants.CONTACTPANEL_LINES_COLOR);
				g.drawImage(this.bgRolloverImage, 0, 0, this);
				
				if (this.iconImage != null) {
					
					if (this.iconLayout.equals(SIPCommButton.CENTER_ICON_LAYOUT))
						g.drawImage(this.iconImage,
										(this.bgImage.getWidth(null) - 
										this.iconImage.getWidth(null)) / 2 + 1,
										(this.bgImage.getHeight(null) - 
										this.iconImage.getHeight(null)) / 2 + 1, this);
					
					else if (this.iconLayout.equals(SIPCommButton.LEFT_ICON_LAYOUT))
						g.drawImage(this.iconImage,
									7 + 1, 
									(this.bgImage.getHeight(null) - 
									this.iconImage.getHeight(null)) / 2 + 1, 
									this);
					
					else if (this.iconLayout.equals(SIPCommButton.LEFT_ICON_LAYOUT))
						g.drawImage(this.iconImage,
								this.bgImage.getWidth(null) - 3 + 1, 
								(this.bgImage.getHeight(null) - 
								this.iconImage.getHeight(null)) / 2 + 1, 
								this);
				}
			}
		}
		
		if(this.getText() != null)
			g.drawString(this.getText(), 12, 10);
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
}
