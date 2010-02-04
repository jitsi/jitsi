/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import net.java.sip.communicator.service.contactlist.*;

/**
 * The <tt>ContactListFilter</tt> is an interface meant to be implemented by
 * modules interested in filtering the contact list. An implementation of this
 * interface should be able answer if a <tt>MetaContact</tt> or a
 * <tt>MetaContactGroup</tt> is matching the corresponding filter.
 *
 * @author Yana Stamcheva
 */
public interface ContactListFilter
{
    /**
     * Returns <tt>true</tt> if the given <tt>metaContact</tt> is matching this
     * filter, otherwise returns <tt>false</tt>
     * @param metaContact the <tt>MetaContact</tt> to check
     * @return <tt>true</tt> if the given <tt>metaContact</tt> is matching this
     * filter, otherwise returns <tt>false</tt>
     */
    public boolean isMatching(MetaContact metaContact);

    /**
     * Returns <tt>true</tt> if the given <tt>metaGroup</tt> is matching this
     * filter, otherwise returns <tt>false</tt>
     * @param metaGroup the <tt>MetaContactGroup</tt> to check
     * @return <tt>true</tt> if the given <tt>metaGroup</tt> is matching this
     * filter, otherwise returns <tt>false</tt>
     */
    public boolean isMatching(MetaContactGroup metaGroup);
}
