/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

/**
 * The <tt>ContactListFilter</tt> is an interface meant to be implemented by
 * modules interested in filtering the contact list. An implementation of this
 * interface should be able to answer if an <tt>UIContact</tt> or an
 * <tt>UIGroup</tt> is matching the corresponding filter.
 *
 * @author Yana Stamcheva
 */
public interface ContactListFilter
{
    /**
     * Indicates if the given <tt>uiGroup</tt> is matching the current filter.
     * @param uiContact the <tt>UIContact</tt> to check
     * @return <tt>true</tt> to indicate that the given <tt>uiContact</tt>
     * matches this filter, <tt>false</tt> - otherwise
     */
    public boolean isMatching(UIContact uiContact);

    /**
     * Indicates if the given <tt>uiGroup</tt> is matching the current filter.
     * @param uiGroup the <tt>UIGroup</tt> to check
     * @return <tt>true</tt> to indicate that the given <tt>uiGroup</tt>
     * matches this filter, <tt>false</tt> - otherwise
     */
    public boolean isMatching(UIGroup uiGroup);

    /**
     * Applies this filter to any interested sources
     * @param filterQuery the <tt>FilterQuery</tt> that tracks the results of
     * this filtering
     */
    public void applyFilter(FilterQuery filterQuery);
}
