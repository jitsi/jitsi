/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.contactlist.addcontact;

import java.util.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>NewContact</tt> is meant to be used from the "Add Contact" wizard
 * to collect all user choices through the wizard process.
 * 
 * @author Yana Stamcheva
 */
public class NewContact {

    private ArrayList<ProtocolProviderService> protocolProviders 
        = new ArrayList<ProtocolProviderService>();
    
    private MetaContactGroup group;
    
    private String uin;
    
    /**
     * Adds a protocol provider to the list of protocol providers, where this
     * contact will be added.
     * 
     * @param protocolProvider The <tt>ProtocolProviderService</tt> to add.
     */
    public void addProtocolProvider(
            ProtocolProviderService protocolProvider) {
        protocolProviders.add(protocolProvider);
    }
    
    /**
     * Adds a group to the list of meta contact groups, where this contact will
     * be added.
     * 
     * @param group The <tt>MetaContactGroup</tt> to add.
     */
    public void setGroup(MetaContactGroup group) {
        this.group = group;
    }

    /**
     * Sets the identifier of the contact.
     * @param uin The String identifier.
     */
    public void setUin(String uin) {
        this.uin = uin;
    }
    
    /**
     * Returns the identifier of the contact.
     * 
     * @return the identifier of the contact.
     */
    public String getUin() {
        return uin;
    }
    
    /**
     * Returns a list of meta contact groups, where this contact should be
     * added.
     * @return a list of meta contact groups, where this contact should be
     * added.
     */
    public MetaContactGroup getGroup() {
        return group;
    }

    /**
     * Returns a list of protocol providers, where this contact should be
     * added.
     * @return a list of protocol providers, where this contact should be
     * added.
     */
    public ArrayList<ProtocolProviderService> getProtocolProviders() {
        return protocolProviders;
    }
}
