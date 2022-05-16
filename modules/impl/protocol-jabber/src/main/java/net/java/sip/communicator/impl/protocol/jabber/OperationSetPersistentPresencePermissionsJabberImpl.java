/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import net.java.sip.communicator.service.protocol.*;

import java.util.*;

/**
 * Implements group edit permissions.
 *
 * @author Damian Minkov
 */
public class OperationSetPersistentPresencePermissionsJabberImpl
    implements OperationSetPersistentPresencePermissions
{
    /**
     * Will indicate everything is readonly.
     */
    private static final String ALL_GROUPS_STR = "all";

    /**
     * Can be added to mark root group as readonly.
     */
    private static final String ROOT_GROUP_STR = "root";

    /**
     * List of group names to be considered as readonly.
     */
    private List<String> readonlyGroups = new ArrayList<String>();

    /**
     * The parent provider.
     */
    private final ProtocolProviderServiceJabberImpl provider;

    OperationSetPersistentPresencePermissionsJabberImpl(
        ProtocolProviderServiceJabberImpl provider)
    {
        this.provider = provider;

        String readOnlyGroupsStr = provider.getAccountID()
            .getAccountPropertyString(
                ProtocolProviderFactory.ACCOUNT_READ_ONLY_GROUPS);

        if(readOnlyGroupsStr == null)
            return;

        StringTokenizer tokenizer = new StringTokenizer(readOnlyGroupsStr, ",");
        while(tokenizer.hasMoreTokens())
        {
            readonlyGroups.add(tokenizer.nextToken().trim());
        }
    }

    /**
     * Is the whole contact list for the current provider readonly.
     * @return <tt>true</tt> if the whole contact list is readonly, otherwise
     * <tt>false</tt>.
     */
    @Override
    public boolean isReadOnly()
    {
        if(readonlyGroups.contains(ALL_GROUPS_STR))
            return true;

        List<String> groupsList = new ArrayList<String>();
        groupsList.add(ROOT_GROUP_STR);
        Iterator<ContactGroup> groupsIter = provider
            .getOperationSet(OperationSetPersistentPresence.class)
            .getServerStoredContactListRoot().subgroups();
        while(groupsIter.hasNext())
        {
            groupsList.add(groupsIter.next().getGroupName());
        }

        if(groupsList.size() > readonlyGroups.size())
            return false;

        groupsList.removeAll(readonlyGroups);

        if(groupsList.size() > 0)
            return false;

        return true;
    }

    /**
     * Checks whether the <tt>contact</tt> can be edited, removed, moved. If
     * the parent group is readonly.
     * @param contact the contact to check.
     * @return <tt>true</tt> if the contact is readonly, otherwise
     * <tt>false</tt>.
     */
    @Override
    public boolean isReadOnly(Contact contact)
    {
        return isReadOnly(contact.getParentContactGroup());
    }

    /**
     * Checks whether the <tt>group</tt> is readonly.
     * @param group the group to check.
     * @return <tt>true</tt> if the group is readonly, otherwise
     * <tt>false</tt>.
     */
    @Override
    public boolean isReadOnly(ContactGroup group)
    {
        if(isReadOnly())
            return true;

        if(group instanceof RootContactGroupJabberImpl)
            return readonlyGroups.contains(ROOT_GROUP_STR);
        else
            return readonlyGroups.contains(group.getGroupName());
    }
}
