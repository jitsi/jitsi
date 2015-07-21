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
package net.java.sip.communicator.service.protocol;

/**
 * This interface is addition to the persistence presence operation set, meant
 * to provide per group permissions for modification of the contacts and groups.
 * Can make the contact list read only or only some groups in it.
 *
 * @author Damian Minkov
 */
public interface OperationSetPersistentPresencePermissions
    extends OperationSet
{
    /**
     * Is the whole contact list for the current provider readonly.
     * @return <tt>true</tt> if the whole contact list is readonly, otherwise
     * <tt>false</tt>.
     */
    public boolean isReadOnly();

    /**
     * Checks whether the <tt>contact</tt> can be edited, removed, moved. If
     * the parent group is readonly.
     * @param contact the contact to check.
     * @return <tt>true</tt> if the contact is readonly, otherwise
     * <tt>false</tt>.
     */
    public boolean isReadOnly(Contact contact);

    /**
     * Checks whether the <tt>group</tt> is readonly.
     * @param group the group to check.
     * @return <tt>true</tt> if the group is readonly, otherwise
     * <tt>false</tt>.
     */
    public boolean isReadOnly(ContactGroup group);
}
