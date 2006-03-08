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

public class ContactListModel extends DefaultListModel {

    public MetaContactNode getContactNodeByContact(MetaContact contact){

        MetaContactNode resultNode = null;

        Enumeration enumeration = this.elements();

        while (enumeration.hasMoreElements()){
            MetaContactNode node = (MetaContactNode)enumeration.nextElement();

            if(node.getContact().equals(contact)){
                resultNode = node;
                break;
            }
        }

        return resultNode;
    }

}
