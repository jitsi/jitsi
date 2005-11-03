package net.java.sip.communicator.impl.gui.main.customcontrols;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.SampleModel;

public class StatusIcon extends BufferedImage {
	
	private Image 	bgImage;
	private Image 	iconImage;
	private int 	rightShift = 0;
	
	public StatusIcon (Image bgImage, Image iconImage) {		
		this (bgImage, iconImage, 0);
	}
	
	public StatusIcon (	Image bgImage, 
						Image iconImage,
						int rightShift) {
		
		super(	bgImage.getWidth(null) + rightShift,
				bgImage.getHeight(null)  + rightShift,
				BufferedImage.TYPE_4BYTE_ABGR);
						
		this.bgImage = bgImage;
		this.iconImage = iconImage;
		this.rightShift = rightShift;
				
		this.getGraphics().drawImage (this.bgImage, 0, rightShift/2, null);
		
		if (this.iconImage != null){
			
			int x = (this.bgImage.getWidth(null) - 
						this.iconImage.getWidth(null)) / 2;
			
			int y = (this.bgImage.getHeight(null) - 
					this.iconImage.getHeight(null)) / 2;
						
			this.getGraphics().drawImage (	this.iconImage, 
											x + rightShift, 
											y + rightShift,			
											null);		
		}
			
	}
	
	public StatusIcon (Image image) {		
		
		super(	image.getWidth(null),
				image.getHeight(null),
				BufferedImage.TYPE_4BYTE_ABGR);
		
		this.bgImage = image;		
		
		this.getGraphics().drawImage (this.bgImage, 0, 0, null);
	}
	
	public StatusIcon (Image image, int rightShift) {
		
		super(	image.getWidth(null) + rightShift,
				image.getHeight(null) + rightShift,
				BufferedImage.TYPE_4BYTE_ABGR);
		
		this.bgImage = image;		
		
		this.getGraphics().drawImage (this.bgImage, 0, rightShift/2, null);
	}
}