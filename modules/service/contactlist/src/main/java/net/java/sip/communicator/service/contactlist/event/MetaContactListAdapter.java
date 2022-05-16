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
package net.java.sip.communicator.service.contactlist.event;

/**
 * The MetaContactListAdapter provides a default implementation of the
 * MetaContactListListener, which can be registered with a
 * MetaContactListService so that it will receive any changes that have occurred
 * in the contact list layout.
 *
 * @author Yana Stamcheva
 */
public class MetaContactListAdapter
    implements MetaContactListListener
{
    /**
     * Indicates that a MetaContact has been successfully added
     * to the MetaContact list.
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    public void metaContactAdded(MetaContactEvent evt) {}

    /**
     * Indicates that a MetaContact has been modified.
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    public void metaContactRenamed(MetaContactRenamedEvent evt) {}

    /**
     * Indicates that a protocol specific <tt>Contact</tt> instance has been
     * added to the list of protocol specific buddies in this
     * <tt>MetaContact</tt>
     * @param evt a reference to the corresponding
     * <tt>ProtoContactEvent</tt>
     */
    public void protoContactAdded(ProtoContactEvent evt) {}

    /**
     * Indicates that one of the protocol specific <tt>Contact</tt> instances
     * encapsulated by this <tt>MetaContact</tt> has been modified in some way.
     * The event
     * added to the list of protocol specific buddies in this
     * <tt>MetaContact</tt>
     * @param evt a reference to the corresponding
     * <tt>ProtoContactEvent</tt>
     */
    public void protoContactModified(ProtoContactEvent evt) {}

    /**
     * Indicates that a protocol specific <tt>Contact</tt> instance has been
     * removed from the list of protocol specific buddies in this
     * <tt>MetaContact</tt>
     * @param evt a reference to the corresponding
     * <tt>ProtoContactEvent</tt>
     */
    public void protoContactRemoved(ProtoContactEvent evt) {}

    /**
     * Indicates that a protocol specific <tt>Contact</tt> instance has been
     * moved from within one <tt>MetaContact</tt> to another.
     * @param evt a reference to the <tt>ProtoContactMovedEvent</tt> instance.
     */
    public void protoContactMoved(ProtoContactEvent evt) {}

    /**
     * Indicates that a MetaContact has been removed from the MetaContact list.
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    public void metaContactRemoved(MetaContactEvent evt) {}

    /**
     * Indicates that a MetaContact has been moved inside the MetaContact list.
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    public void metaContactMoved(MetaContactMovedEvent evt) {}

    //-------------------- events on groups. ----------------------------------

    /**
     * Indicates that a MetaContactGroup has been successfully added
     * to the MetaContact list.
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    public void metaContactGroupAdded(MetaContactGroupEvent evt) {}

    /**
     * Indicates that a MetaContactGroup has been modified (e.g. a proto contact
     * group was removed).
     *
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    public void metaContactGroupModified(MetaContactGroupEvent evt) {}


    /**
     * Indicates that a MetaContactGroup has been removed from the MetaContact
     * list.
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    public void metaContactGroupRemoved(MetaContactGroupEvent evt) {}

    /**
     * Indicates that the order under which the child contacts were ordered
     * inside the source group has changed.
     * @param evt the <tt>MetaContactGroupEvent</tt> containing details of this
     * event.
     */
    public void childContactsReordered(MetaContactGroupEvent evt) {}

    /**
     * Indicates that a MetaContact has been modified.
     * @param evt the MetaContactModifiedEvent containing the corresponding
     * contact
     */
    public void metaContactModified(MetaContactModifiedEvent evt) {}

    /**
     * Indicates that a new avatar is available for a <tt>MetaContact</tt>.
     * @param evt the <tt>MetaContactAvatarUpdateEvent</tt> containing details
     * of this event
     */
    public void metaContactAvatarUpdated(MetaContactAvatarUpdateEvent evt) {}
}
