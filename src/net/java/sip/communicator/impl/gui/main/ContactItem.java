package net.java.sip.communicator.impl.gui.main;

import java.awt.Image;

import javax.swing.Icon;

/**
 * @author Yana Stamcheva
 *  
 * The contact.
 * TODO: To be removed when the contact list service is ready. 
 */

public class ContactItem {

	private String 		nickname;
	private Image 		photo;
	private String[] 	protocolList;
	private String 		status;
	private Icon		userIcon;
	
	public ContactItem(String nickname){
		this.nickname = nickname;
	}
	
	public String getNickName() {
		return nickname;
	}
	
	public void setNickName(String nickname) {
		this.nickname = nickname;
	}
	
	public Image getPhoto() {
		return photo;
	}
	
	public void setPhoto(Image photo) {
		this.photo = photo;
	}
	
	public String[] getProtocolList() {
		return protocolList;
	}
	
	public void setProtocolList(String[] protocolList) {
		this.protocolList = protocolList;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Icon getUserIcon() {
		return userIcon;
	}

	public void setUserIcon(Icon userIcon) {
		this.userIcon = userIcon;
	}
}
