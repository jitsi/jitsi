/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.contactlist;

import javax.swing.ImageIcon;

import net.java.sip.communicator.impl.gui.main.utils.ImageLoader;
import net.java.sip.communicator.service.contactlist.MetaContact;

public class MetaContactNode {

    private MetaContact contact;
    
    private ImageIcon statusIcon 
        = new ImageIcon(ImageLoader.getImage(ImageLoader.USER_OFFLINE_ICON));
    
    public MetaContactNode(MetaContact contact){
        this.contact = contact;
    }

    public ImageIcon getStatusIcon() {
        return statusIcon;
    }

    public void setStatusIcon(ImageIcon statusIcon) {
        this.statusIcon = statusIcon;
    }

    public MetaContact getContact() {
        return contact;
    }
    
    public String toString(){
		return this.contact.getDisplayName();
    }
}
