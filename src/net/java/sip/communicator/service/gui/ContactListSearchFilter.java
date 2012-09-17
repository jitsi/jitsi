/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui;

/**
 * The <tt>ContactListSearchFilter</tt> is a <tt>ContactListFilter</tt> specific
 * for string search.
 */
public interface ContactListSearchFilter
    extends ContactListFilter
{
    /**
     * Creates the <tt>SearchFilter</tt> by specifying the string used for
     * filtering.
     * @param filter the String used for filtering
     */
    public void setFilterString(String filter);
}
