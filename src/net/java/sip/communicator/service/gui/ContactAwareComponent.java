/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui;

import net.java.sip.communicator.service.contactlist.*;

/**
 * The <tt>ContactAwareComponent</tt> is an interface meant to be implemented by
 * all plugin components that are interested of the current contact that they're
 * dealing with.
 * 
 * @author Yana Stamcheva
 */
public interface ContactAwareComponent
{
    /**
     * Sets the current meta contact. Meant to be used by plugin components that
     * are interested of the current contact. The current contact could be the
     * contact currently selected in the contact list or the contact for the
     * currently selected chat, etc. It depends on the container, where this
     * component is meant to be added.
     * 
     * @param metaContact the current meta contact
     */
    public void setCurrentContact(MetaContact metaContact);
    
    /**
     * Sets the current meta group. Meant to be used by plugin components that
     * are interested of the current meta group. The current group is always
     * the currently selected group in the contact list. If the group passed
     * here is null, this means that no group is selected.
     * 
     * @param metaGroup the current meta contact group
     */
    public void setCurrentContactGroup(MetaContactGroup metaGroup);
}
