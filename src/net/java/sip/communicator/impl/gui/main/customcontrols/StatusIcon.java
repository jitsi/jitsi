package net.java.sip.communicator.impl.gui.main.customcontrols;

import java.awt.Image;
import java.awt.image.BufferedImage;

public class StatusIcon extends BufferedImage {
	
	private Image bgImage;
	private Image iconImage;
	
		
	public StatusIcon (Image bgImage, Image iconImage) {		
		super(	bgImage.getWidth(null),
				bgImage.getHeight(null),
				BufferedImage.TYPE_4BYTE_ABGR);
		
		this.bgImage = bgImage;
		this.iconImage = iconImage;
				
		this.getGraphics().drawImage (this.bgImage, 0, 0, null);
		
		if (this.iconImage != null)
			this.getGraphics().drawImage (this.iconImage,
					(this.bgImage.getWidth(null) - 
					this.iconImage.getWidth(null)) / 2,
					(this.bgImage.getHeight(null) - 
					this.iconImage.getHeight(null)) / 2, null);

	}
	
	public StatusIcon (Image image) {		
		super(	image.getWidth(null),
				image.getHeight(null),
				BufferedImage.TYPE_4BYTE_ABGR);
		
		this.bgImage = image;		
		
		this.getGraphics().drawImage (this.bgImage, 0, 0, null);
	}	
}