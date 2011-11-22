/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist.contactsource;

import java.util.*;
import java.util.regex.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;

import javax.swing.*;

/**
 * The <tt>MetaContactListSource</tt> is an abstraction of the
 * <tt>MetaContactListService</tt>, which makes the correspondence between a
 * <tt>MetaContact</tt> and an <tt>UIContact</tt> and between a
 * <tt>MetaContactGroup</tt> and an <tt>UIGroup</tt>. It is also responsible
 * for filtering of the <tt>MetaContactListService</tt> through a given pattern.
 *
 * @author Yana Stamcheva
 */
public class MetaContactListSource
{
    /**
     * The data key of the MetaContactDescriptor object used to store a
     * reference to this object in its corresponding MetaContact.
     */
    public static final String UI_CONTACT_DATA_KEY
        = MetaUIContact.class.getName() + ".uiContactDescriptor";

    /**
     * The data key of the MetaGroupDescriptor object used to store a
     * reference to this object in its corresponding MetaContactGroup.
     */
    public static final String UI_GROUP_DATA_KEY
        = MetaUIGroup.class.getName() + ".uiGroupDescriptor";

    /**
     * The initial result count below which we insert all filter results
     * directly to the contact list without firing events.
     */
    private final int INITIAL_CONTACT_COUNT = 30;

    /**
     * Returns the <tt>UIContact</tt> corresponding to the given
     * <tt>MetaContact</tt>.
     * @param metaContact the <tt>MetaContact</tt>, which corresponding UI
     * contact we're looking for
     * @return the <tt>UIContact</tt> corresponding to the given
     * <tt>MetaContact</tt>
     */
    public static UIContact getUIContact(MetaContact metaContact)
    {
        return (UIContact) metaContact.getData(UI_CONTACT_DATA_KEY);
    }

    /**
     * Returns the <tt>UIGroup</tt> corresponding to the given
     * <tt>MetaContactGroup</tt>.
     * @param metaGroup the <tt>MetaContactGroup</tt>, which UI group we're
     * looking for
     * @return the <tt>UIGroup</tt> corresponding to the given
     * <tt>MetaContactGroup</tt>
     */
    public static UIGroup getUIGroup(MetaContactGroup metaGroup)
    {
        return (UIGroup) metaGroup.getData(UI_GROUP_DATA_KEY);
    }

    /**
     * Creates a <tt>UIContact</tt> for the given <tt>metaContact</tt>.
     * @param metaContact the <tt>MetaContact</tt> for which we would like to
     * create an <tt>UIContact</tt>
     * @return an <tt>UIContact</tt> for the given <tt>metaContact</tt>
     */
    public static UIContact createUIContact(final MetaContact metaContact)
    {
        final MetaUIContact descriptor
            = new MetaUIContact(metaContact);

        metaContact.setData(UI_CONTACT_DATA_KEY, descriptor);

        return descriptor;
    }

    /**
     * Removes the <tt>UIContact</tt> from the given <tt>metaContact</tt>.
     * @param metaContact the <tt>MetaContact</tt>, which corresponding UI
     * contact we would like to remove
     */
    public static void removeUIContact(MetaContact metaContact)
    {
        metaContact.setData(UI_CONTACT_DATA_KEY, null);
    }

    /**
     * Creates a <tt>UIGroupDescriptor</tt> for the given <tt>metaGroup</tt>.
     * @param metaGroup the <tt>MetaContactGroup</tt> for which we would like to
     * create an <tt>UIContact</tt>
     * @return a <tt>UIGroup</tt> for the given <tt>metaGroup</tt>
     */
    public static UIGroup createUIGroup(MetaContactGroup metaGroup)
    {
        MetaUIGroup descriptor = new MetaUIGroup(metaGroup);
        metaGroup.setData(UI_GROUP_DATA_KEY, descriptor);

        return descriptor;
    }

    /**
     * Removes the descriptor from the given <tt>metaGroup</tt>.
     * @param metaGroup the <tt>MetaContactGroup</tt>, which descriptor we
     * would like to remove
     */
    public static void removeUIGroup(
        MetaContactGroup metaGroup)
    {
        metaGroup.setData(UI_GROUP_DATA_KEY, null);
    }

    /**
     * Indicates if the given <tt>MetaContactGroup</tt> is the root group.
     * @param group the <tt>MetaContactGroup</tt> to check
     * @return <tt>true</tt> if the given <tt>group</tt> is the root group,
     * <tt>false</tt> - otherwise
     */
    public static boolean isRootGroup(MetaContactGroup group)
    {
        return group.equals(GuiActivator.getContactListService().getRoot());
    }

    /**
     * Filters the <tt>MetaContactListService</tt> to match the given
     * <tt>filterPattern</tt> and stores the result in the given
     * <tt>treeModel</tt>.
     * @param filterPattern the pattern to filter through
     * @return the created <tt>MetaContactQuery</tt> corresponding to the
     * query this method does
     */
    public MetaContactQuery queryMetaContactSource(final Pattern filterPattern)
    {
        final MetaContactQuery query = new MetaContactQuery();

        new Thread()
        {
            public void run()
            {
                int resultCount = 0;
                queryMetaContactSource( filterPattern,
                        GuiActivator.getContactListService().getRoot(),
                        query,
                        resultCount);

                if (!query.isCanceled())
                    query.fireQueryEvent(
                        MetaContactQueryStatusEvent.QUERY_COMPLETED);
                else
                    query.fireQueryEvent(
                        MetaContactQueryStatusEvent.QUERY_CANCELED);
            }
        }.start();

        return query;
    }

    /**
     * Filters the children in the given <tt>MetaContactGroup</tt> to match the
     * given <tt>filterPattern</tt> and stores the result in the given
     * <tt>treeModel</tt>.
     * @param filterPattern the pattern to filter through
     * @param parentGroup the <tt>MetaContactGroup</tt> to filter
     * @param query the object that tracks the query
     * @param resultCount the initial result count we would insert directly to
     * the contact list without firing events
     */
    private void queryMetaContactSource(Pattern filterPattern,
                                        MetaContactGroup parentGroup,
                                        MetaContactQuery query,
                                        int resultCount)
    {
        Iterator<MetaContact> childContacts = parentGroup.getChildContacts();

        while (childContacts.hasNext() && !query.isCanceled())
        {
            MetaContact metaContact = childContacts.next();

            if (isMatching(filterPattern, metaContact))
            {
                resultCount++;

                if (resultCount <= INITIAL_CONTACT_COUNT)
                {
                    UIGroup uiGroup = null;
                    if (!MetaContactListSource.isRootGroup(parentGroup))
                    {
                        uiGroup = MetaContactListSource
                            .getUIGroup(parentGroup);

                        if (uiGroup == null)
                            uiGroup = MetaContactListSource
                                .createUIGroup(parentGroup);
                    }

                    GuiActivator.getContactList().addContact(
                            MetaContactListSource.createUIContact(metaContact),
                            uiGroup,
                            true,
                            true);

                    query.setInitialResultCount(resultCount);
                }
                else
                    query.fireQueryEvent(metaContact);
            }
        }

        // If in the meantime the query is canceled we return here.
        if(query.isCanceled())
            return;

        Iterator<MetaContactGroup> subgroups = parentGroup.getSubgroups();
        while (subgroups.hasNext() && !query.isCanceled())
        {
            MetaContactGroup subgroup = subgroups.next();

            queryMetaContactSource(filterPattern, subgroup, query, resultCount);
        }
    }

    /**
     * Checks if the given <tt>metaContact</tt> is matching the given
     * <tt>filterPattern</tt>.
     * A <tt>MetaContact</tt> would be matching the filter if one of the
     * following is true:<br>
     * - its display name contains the filter string
     * - at least one of its child protocol contacts has a display name or an
     * address that contains the filter string.
     * @param filterPattern the filter pattern to check for matches
     * @param metaContact the <tt>MetaContact</tt> to check
     * @return <tt>true</tt> to indicate that the given <tt>metaContact</tt> is
     * matching the current filter, otherwise returns <tt>false</tt>
     */
    private boolean isMatching(Pattern filterPattern, MetaContact metaContact)
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
     * @param filterPattern the filter pattern to check for matches
     * @param metaGroup the <tt>MetaContactGroup</tt> to check
     * @return <tt>true</tt> to indicate that the given <tt>metaGroup</tt> is
     * matching the current filter, otherwise returns <tt>false</tt>
     */
    public boolean isMatching(Pattern filterPattern, MetaContactGroup metaGroup)
    {
        Iterator<MetaContact> contacts = metaGroup.getChildContacts();

        while (contacts.hasNext())
        {
            MetaContact metaContact = contacts.next();

            if (isMatching(filterPattern, metaContact))
                return true;
        }
        return false;
    }
}
