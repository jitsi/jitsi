/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.contactlist.event;

import java.util.EventListener;

/**
 *
 * @author Emil Ivov
 */
public interface MetaContactListListener
    extends EventListener
{
    
    /**
     * Indicates that a MetaContact has been successfully added 
     * to the MetaContact list.
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    public void metaContactAdded(MetaContactEvent evt);
    
    /**
     * Indicates that a MetaContact has been removed from the MetaContact list.
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    public void metaContactRemoved(MetaContactEvent evt);
    
    /**
     * Indicates that a MetaContactGroup has been successfully added 
     * to the MetaContact list.
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    public void metaContactGroupAdded(MetaContactGroupEvent evt);
    
    /**
     * Indicates that a MetaContactGroup has been removed from the MetaContact list.
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    public void metaContactGroupRemoved(MetaContactGroupEvent evt);
    
}
