/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.contactlist;

import java.util.Enumeration;

import javax.swing.DefaultListModel;

import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;

/**
 * The list model of the ContactList.
 * 
 * @author Yana Stamcheva
 *
 */
public class ContactListModel extends DefaultListModel {

    /**
     * Returns the ContactNode element in the ContactList that corresponds 
     * to the given MetaContact.
     * 
     * @param contact The MetaContact we are searching for. 
     * @return The ContactNode element corresponding to the given contact
     */
    public MetaContactNode getContactNodeByContact(MetaContact contact){
        
        MetaContactNode resultNode = null;
        
        Enumeration listEnum = this.elements();
        
        while (listEnum.hasMoreElements()){
            
            Object element = listEnum.nextElement();
            
            if(element instanceof MetaContactNode){
                
                MetaContactNode node = (MetaContactNode)element;
                
                if(node.getContact().equals(contact)){
                    resultNode = node;
                    break;
                }
            }
        }
        
        return resultNode;
    }
    
    public void contactStatusChanged(int index) {        
        fireContentsChanged(this, index, index);
    }
}
