/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.customcontrols;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.plaf.IconUIResource;

import net.java.sip.communicator.impl.gui.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;
import net.java.sip.communicator.impl.gui.utils.LightGrayFilter;

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
	 * Button with icon.
	 * 
	 * @param iconImage
	 */
	public SIPCommButton(Image iconImage, String iconLayout) {
		super();
		
		this.iconLayout = iconLayout;
		
		this.setIcon(new ImageIcon(iconImage));
	}
	
	/**
	 * Custom button.
	 * 
	 * @param bgImage The background image.
	 * @param rolloverImage The rollover image
	 * @param iconImage The icon.
	 */
	public SIPCommButton ( Image bgImage, 
                           Image rolloverImage, 
                           Image iconImage) {
		super();

		this.bgImage = bgImage;
		this.bgRolloverImage = rolloverImage;
        this.iconImage = iconImage;

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
	}
		
	/**
	 * Paint the SIPCommButton.
	 */	
	public void paintComponent(Graphics g) {	
		AntialiasingManager.activateAntialiasing(g);
		
        if(this.bgImage != null){
            // If there's no icon, we make grey the backgroundImage
            // when disabled.
            if(this.iconImage == null && !isEnabled()){
                Image disabledImage = new ImageIcon(
                    LightGrayFilter.createDisabledImage(bgImage)).getImage();
                
                g.drawImage(disabledImage, 0, 0, this);
            }
            else
                g.drawImage(this.bgImage, 0, 0, this);
        }

        if (this.iconImage != null) {            
            if(!isEnabled()){
                Image disabledImage = new ImageIcon(LightGrayFilter
                        .createDisabledImage(iconImage)).getImage();
                
//              draw the button icon depending the current button layout
                if (this.iconLayout.equals(SIPCommButton.CENTER_ICON_LAYOUT))
                    g.drawImage(disabledImage,
                                (this.bgImage.getWidth(null) - 
                                disabledImage.getWidth(null)) / 2,
                                (this.bgImage.getHeight(null) - 
                                disabledImage.getHeight(null)) / 2, this);
                
                else if (this.iconLayout.equals(SIPCommButton.LEFT_ICON_LAYOUT))
                    g.drawImage(disabledImage,
                                7, 
                                (this.bgImage.getHeight(null) - 
                                disabledImage.getHeight(null)) / 2, 
                                this);
                
                else if (this.iconLayout.equals(SIPCommButton.LEFT_ICON_LAYOUT))
                    g.drawImage(disabledImage,
                            this.bgImage.getWidth(null) - 3, 
                            (this.bgImage.getHeight(null) - 
                            disabledImage.getHeight(null)) / 2, 
                            this);
            }
            else{
//              draw the button icon depending the current button layout
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

		if (this.bgRolloverImage != null && this.getModel().isRollover()) {

			g.setColor(Constants.CONTACTPANEL_LINES_COLOR);
			g.drawImage(this.bgRolloverImage, 0, 0, this);

			if (this.iconImage != null) {

				if (this.iconLayout
                            .equals(SIPCommButton.CENTER_ICON_LAYOUT))
					g.drawImage(this.iconImage,
									(this.bgImage.getWidth(null) - 
									this.iconImage.getWidth(null)) / 2,
									(this.bgImage.getHeight(null) - 
									this.iconImage.getHeight(null)) / 2, this);
				
				else if (this.iconLayout
                                .equals(SIPCommButton.LEFT_ICON_LAYOUT))
					g.drawImage(this.iconImage,
								7, 
								(this.bgImage.getHeight(null) - 
								this.iconImage.getHeight(null)) / 2, 
								this);
				
				else if (this.iconLayout
                                .equals(SIPCommButton.LEFT_ICON_LAYOUT))
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
					
					if (this.iconLayout
                            .equals(SIPCommButton.CENTER_ICON_LAYOUT))
						g.drawImage(this.iconImage,
										(this.bgImage.getWidth(null) - 
										this.iconImage.getWidth(null)) / 2 + 1,
										(this.bgImage.getHeight(null) - 
										this.iconImage.getHeight(null)) / 2 + 1, this);
					
					else if (this.iconLayout
                                    .equals(SIPCommButton.LEFT_ICON_LAYOUT))
						g.drawImage(this.iconImage,
									7 + 1, 
									(this.bgImage.getHeight(null) - 
									this.iconImage.getHeight(null)) / 2 + 1, 
									this);
					
					else if (this.iconLayout
                                    .equals(SIPCommButton.LEFT_ICON_LAYOUT))
						g.drawImage(this.iconImage,
								this.bgImage.getWidth(null) - 3 + 1, 
								(this.bgImage.getHeight(null) - 
								this.iconImage.getHeight(null)) / 2 + 1, 
								this);
				}
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
}
