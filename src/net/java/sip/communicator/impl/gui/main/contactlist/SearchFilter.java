/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.util.*;
import java.util.regex.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>SearchFilter</tt> is a <tt>ContactListFilter</tt> that filters the
 * contact list content by a filter string.
 *
 * @author Yana Stamcheva
 */
public class SearchFilter
    implements ContactListFilter
{
    private Pattern filterPattern;

    /**
     * Creates the <tt>SearchFilter</tt> by specifying the string used for
     * filtering.
     * @param filterString the String used for filtering
     */
    public void setFilterString(String filterString)
    {
        // First escape all special characters from the given filter string.
        filterString = Pattern.quote(filterString);

        // Then create the pattern.
        // By default, case-insensitive matching assumes that only characters
        // in the US-ASCII charset are being matched, that's why we use
        // the UNICODE_CASE flag to enable unicode case-insensitive matching.
        // Sun Bug ID: 6486934 "RegEx case_insensitive match is broken"
        this.filterPattern = Pattern.compile(
            filterString, Pattern.MULTILINE
                            | Pattern.CASE_INSENSITIVE
                            | Pattern.UNICODE_CASE);
    }

    /**
     * Checks if the given <tt>metaContact</tt> is matching the current filter.
     * A <tt>MetaContact</tt> would be matching the filter if one of the
     * following is true:<br>
     * - its display name contains the filter string
     * - at least one of its child protocol contacts has a display name or an
     * address that contains the filter string.
     * @param metaContact the <tt>MetaContact</tt> to check
     * @return <tt>true</tt> to indicate that the given <tt>metaContact</tt> is
     * matching the current filter, otherwise returns <tt>false</tt>
     */
    public boolean isMatching(MetaContact metaContact)
    {
        Matcher matcher = filterPattern.matcher(metaContact.getDisplayName());

        if(matcher.find())
            return true;

        Iterator<Contact> contacts = metaContact.getContacts();
        while (contacts.hasNext())
        {
            Contact contact = contacts.next();

            matcher = filterPattern.matcher(contact.getDisplayName());

            if (matcher.find())
                return true;

            matcher = filterPattern.matcher(contact.getAddress());

            if (matcher.find())
                return true;
        }
        return false;
    }

    /**
     * Checks if the given <tt>metaGroup</tt> is matching the current filter. A
     * group is matching the current filter only if it contains at least one
     * child <tt>MetaContact</tt>, which is matching the current filter.
     * @param metaGroup the <tt>MetaContactGroup</tt> to check
     * @return <tt>true</tt> to indicate that the given <tt>metaGroup</tt> is
     * matching the current filter, otherwise returns <tt>false</tt>
     */
    public boolean isMatching(MetaContactGroup metaGroup)
    {
        Iterator<MetaContact> contacts = metaGroup.getChildContacts();

        while (contacts.hasNext())
        {
            MetaContact metaContact = contacts.next();

            if (isMatching(metaContact))
                return true;
        }
        return false;
    }
}
