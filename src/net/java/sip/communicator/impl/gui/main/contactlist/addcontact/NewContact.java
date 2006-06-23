/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.contactlist.addcontact;

import java.util.ArrayList;

import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;

public class NewContact {

    private ArrayList protocolProviders = new ArrayList();
    
    private ArrayList groups = new ArrayList();
    
    private String uin = new String();
    
    public void addProtocolProvider(
            ProtocolProviderService protocolProvider) {
        protocolProviders.add(protocolProvider);
    }
    
    public void addGroup(MetaContactGroup group) {
        groups.add(group);
    }

    public String getUin() {
        return uin;
    }

    public void setUin(String uin) {
        this.uin = uin;
    }

    public ArrayList getGroups() {
        return groups;
    }

    public ArrayList getProtocolProviders() {
        return protocolProviders;
    }
    
    
}
