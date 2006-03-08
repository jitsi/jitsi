/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.contactlist.event;

import java.util.EventListener;

/**
 * A MetaContactListListener can be registered with a MetaContactListService
 * so that it will receive any changes that have occurred in the contact list
 * layout.
 *
 * @author Yana Stamcheva
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
     * Indicates that a MetaContact has been modified.
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    public void metaContactModified(MetaContactEvent evt);


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
     * Indicates that a MetaContactGroup has been modified (e.g. a proto contact
     * group was removed).
     *
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    public void metaContactGroupModified(MetaContactGroupEvent evt);


    /**
     * Indicates that a MetaContactGroup has been removed from the MetaContact list.
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    public void metaContactGroupRemoved(MetaContactGroupEvent evt);

}
