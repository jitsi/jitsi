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
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>MetaContactListManager</tt> is the class through which we make
 * operations with the <tt>MetaContactList</tt>. All methods in this class are
 * static.
 *
 * @author Yana Stamcheva
 */
public class MetaContactListManager
{
    /**
     * Moves the given <tt>srcContact</tt> to the <tt>destMetaContact</tt>.
     * @param srcContact the <tt>Contact</tt> to move
     * @param destMetaContact the destination <tt>MetaContact</tt> to move to
     */
    public static void moveContactToMetaContact(Contact srcContact,
                                                MetaContact destMetaContact)
    {
        new MoveContactToMetaContactThread(srcContact, destMetaContact).start();
    }


    /**
     * Moves the given <tt>srcMetaContact</tt> to the <tt>destMetaContact</tt>.
     * @param srcMetaContact the <tt>MetaContact</tt> to move
     * @param destMetaContact the destination <tt>MetaContact</tt> to move to
     */
    public static void moveMetaContactToMetaContact(MetaContact srcMetaContact,
                                                    MetaContact destMetaContact)
    {
        new MoveMetaContactToMetaContactThread(
            srcMetaContact, destMetaContact).start();
    }

    /**
     * Moves the given <tt>srcContact</tt> to the <tt>destGroup</tt>.
     * @param srcContact the <tt>Contact</tt> to move
     * @param destGroup the destination <tt>MetaContactGroup</tt> to move to
     */
    public static void moveContactToGroup(  Contact srcContact,
                                            MetaContactGroup destGroup)
    {
        new MoveContactToGroupThread(srcContact, destGroup).start();
    }

    /**
     * Moves the given <tt>srcContact</tt> to the <tt>destGroup</tt>.
     * @param srcContact the <tt>MetaContact</tt> to move
     * @param group the destination <tt>MetaContactGroup</tt> to move to
     */
    public static void moveMetaContactToGroup(  MetaContact srcContact,
                                                MetaContactGroup group)
    {
        new MoveMetaContactThread(srcContact, group).start();
    }

    /**
     * Moves the given <tt>srcContact</tt> to the <tt>destGroup</tt>.
     * @param srcContact the <tt>MetaContact</tt> to move
     * @param groupID the identifier of the destination <tt>MetaContactGroup</tt>
     * to move to
     */
    public static void moveMetaContactToGroup(  MetaContact srcContact,
                                                String groupID)
    {
        new MoveMetaContactThread(srcContact, getGroupByID(groupID)).start();
    }

    /**
     * Removes the given <tt>Contact</tt> from its <tt>MetaContact</tt>.
     * @param contact the <tt>Contact</tt> to remove
     */
    public static void removeContact(Contact contact)
    {
        new RemoveContactThread(contact).start();
    }

    /**
     * Removes the given <tt>MetaContact</tt> from the list.
     * @param metaContact the <tt>MetaContact</tt> to remove
     */
    public static void removeMetaContact(MetaContact metaContact)
    {
        new RemoveMetaContactThread(metaContact).start();
    }

    /**
     * Removes the given <tt>MetaContactGroup</tt> from the list.
     * @param group the <tt>MetaContactGroup</tt> to remove
     */
    public static void removeMetaContactGroup(MetaContactGroup group)
    {
        new RemoveGroupThread(group).start();
    }

    /**
     * Returns the Meta Contact Group corresponding to the given MetaUID.
     *
     * @param metaUID An identifier of a group.
     * @return The Meta Contact Group corresponding to the given MetaUID.
     */
    private static MetaContactGroup getGroupByID(String metaUID)
    {
        return GuiActivator.getContactListService()
            .findMetaContactGroupByMetaUID(metaUID);
    }

    /**
     * Moves the given <tt>Contact</tt> to the given <tt>MetaContact</tt> and
     * asks user for confirmation.
     */
    private static class MoveContactToMetaContactThread extends Thread
    {
        private final Contact srcContact;
        private final MetaContact destMetaContact;

        public MoveContactToMetaContactThread(  Contact srcContact,
                                                MetaContact destMetaContact)
        {
            this.srcContact = srcContact;
            this.destMetaContact = destMetaContact;
        }

        @Override
        @SuppressWarnings("fallthrough") //intentional
        public void run()
        {
            if (!ConfigurationUtils.isMoveContactConfirmationRequested())
            {
                // we move the specified contact
                GuiActivator.getContactListService()
                    .moveContact(srcContact, destMetaContact);

                return;
            }

            String message = GuiActivator.getResources().getI18NString(
                "service.gui.MOVE_SUBCONTACT_QUESTION",
                new String[]{   srcContact.getDisplayName(),
                                destMetaContact.getDisplayName()});

            MessageDialog dialog = new MessageDialog(
                    null,
                    GuiActivator.getResources()
                        .getI18NString("service.gui.MOVE_CONTACT"),
                    message,
                    GuiActivator.getResources()
                        .getI18NString("service.gui.MOVE"));

            switch (dialog.showDialog())
            {
            case MessageDialog.OK_DONT_ASK_CODE:
                ConfigurationUtils.setMoveContactConfirmationRequested(false);
                // do fall through

            case MessageDialog.OK_RETURN_CODE:
                // we move the specified contact
                GuiActivator.getContactListService().moveContact(
                    srcContact, destMetaContact);
                break;
            }
        }
    }

    /**
     * Moves the given <tt>Contact</tt> to the given <tt>MetaContact</tt> and
     * asks user for confirmation.
     */
    private static class MoveMetaContactToMetaContactThread extends Thread
    {
        private final MetaContact srcMetaContact;
        private final MetaContact destMetaContact;

        public MoveMetaContactToMetaContactThread(  MetaContact srcContact,
                                                    MetaContact destMetaContact)
        {
            this.srcMetaContact = srcContact;
            this.destMetaContact = destMetaContact;
        }

        @Override
        @SuppressWarnings("fallthrough") //intentional
        public void run()
        {
            if (!ConfigurationUtils.isMoveContactConfirmationRequested())
            {
                // We move all subcontacts of the source MetaContact to the
                // destination MetaContact.
                this.moveAllSubcontacts();

                return;
            }

            String message = GuiActivator.getResources().getI18NString(
                "service.gui.MOVE_SUBCONTACT_QUESTION",
                new String[]{   srcMetaContact.getDisplayName(),
                                destMetaContact.getDisplayName()});

            MessageDialog dialog = new MessageDialog(
                    null,
                    GuiActivator.getResources()
                        .getI18NString("service.gui.MOVE_CONTACT"),
                    message,
                    GuiActivator.getResources()
                        .getI18NString("service.gui.MOVE"));

            switch (dialog.showDialog())
            {
            case MessageDialog.OK_DONT_ASK_CODE:
                ConfigurationUtils.setMoveContactConfirmationRequested(false);
                // do fall through

            case MessageDialog.OK_RETURN_CODE:
                // We move all subcontacts of the source MetaContact to the
                // destination MetaContact.
                this.moveAllSubcontacts();
                break;
            }
        }

        /**
         * Move all subcontacts of the <tt>srcMetaContact</tt> to
         * <tt>destMetaContact</tt>.
         */
        private void moveAllSubcontacts()
        {
            Iterator<Contact> contacts = srcMetaContact.getContacts();
            while(contacts.hasNext())
            {
                GuiActivator.getContactListService().moveContact(
                    contacts.next(), destMetaContact);
            }
        }
    }

    /**
     * Moves the given <tt>Contact</tt> to the given <tt>MetaContactGroup</tt>
     * and asks user for confirmation.
     */
    @SuppressWarnings("fallthrough")
    private static class MoveContactToGroupThread extends Thread
    {
        private final Contact srcContact;
        private final MetaContactGroup destGroup;

        public MoveContactToGroupThread(Contact srcContact,
                                        MetaContactGroup destGroup)
        {
            this.srcContact = srcContact;
            this.destGroup = destGroup;
        }

        @Override
        public void run()
        {
            if (!ConfigurationUtils.isMoveContactConfirmationRequested())
            {
                // we move the specified contact
                GuiActivator.getContactListService()
                    .moveContact(srcContact, destGroup);

                return;
            }

            String message = GuiActivator.getResources().getI18NString(
                "service.gui.MOVE_SUBCONTACT_QUESTION",
                new String[]{   srcContact.getDisplayName(),
                                destGroup.getGroupName()});

            MessageDialog dialog = new MessageDialog(
                    null,
                    GuiActivator.getResources()
                        .getI18NString("service.gui.MOVE_CONTACT"),
                    message,
                    GuiActivator.getResources()
                        .getI18NString("service.gui.MOVE"));

            switch (dialog.showDialog())
            {
            case MessageDialog.OK_DONT_ASK_CODE:
                ConfigurationUtils.setMoveContactConfirmationRequested(false);
                // do fall through

            case MessageDialog.OK_RETURN_CODE:
                // we move the specified contact
                GuiActivator.getContactListService()
                    .moveContact(srcContact, destGroup);
                break;
            }
        }
    }

    /**
     * Moves the given <tt>MetaContact</tt> to the given
     * <tt>MetaContactGroup</tt> and asks user for confirmation.
     */
    private static class MoveMetaContactThread
        extends Thread
    {
        private final MetaContact srcContact;
        private final MetaContactGroup destGroup;

        public MoveMetaContactThread(   MetaContact srcContact,
                                        MetaContactGroup destGroup)
        {
            this.srcContact = srcContact;
            this.destGroup = destGroup;
        }

        @Override
        @SuppressWarnings("fallthrough")
        public void run()
        {
            if (!ConfigurationUtils.isMoveContactConfirmationRequested())
            {
                // we move the specified contact
                try
                {
                    GuiActivator.getContactListService()
                        .moveMetaContact(srcContact, destGroup);
                }
                catch (MetaContactListException e)
                {

                }

                return;
            }

            String message = GuiActivator.getResources().getI18NString(
                "service.gui.MOVE_SUBCONTACT_QUESTION",
                new String[]{   srcContact.getDisplayName(),
                                destGroup.getGroupName()});

            MessageDialog dialog = new MessageDialog(
                    null,
                    GuiActivator.getResources()
                        .getI18NString("service.gui.MOVE_CONTACT"),
                    message,
                    GuiActivator.getResources()
                        .getI18NString("service.gui.MOVE"));

            switch (dialog.showDialog())
            {
            case MessageDialog.OK_DONT_ASK_CODE:
                ConfigurationUtils.setMoveContactConfirmationRequested(false);
                // do fall through

            case MessageDialog.OK_RETURN_CODE:
                // we move the specified contact
                GuiActivator.getContactListService()
                    .moveMetaContact(srcContact, destGroup);
                break;
            }
        }
    }

    /**
     * Removes a contact from a meta contact in a separate thread.
     */
    private static class RemoveContactThread extends Thread
    {
        private Contact contact;
        public RemoveContactThread(Contact contact)
        {
            this.contact = contact;
        }

        @Override
        public void run()
        {
            if (!contact.getProtocolProvider().isRegistered())
            {
                new ErrorDialog(
                    GuiActivator.getUIService().getMainFrame(),
                    GuiActivator.getResources().getI18NString(
                    "service.gui.ADD_CONTACT_ERROR_TITLE"),
                    GuiActivator.getResources().getI18NString(
                            "service.gui.REMOVE_CONTACT_NOT_CONNECTED"),
                    ErrorDialog.WARNING)
                .showDialog();

                return;
            }

            try
            {
                if(Constants.REMOVE_CONTACT_ASK)
                {
                    String message = GuiActivator.getResources().getI18NString(
                        "service.gui.REMOVE_CONTACT_TEXT",
                        new String[]{contact.getDisplayName()});

                    MessageDialog dialog = new MessageDialog(
                        null,
                        GuiActivator.getResources()
                            .getI18NString("service.gui.REMOVE_CONTACT"),
                        message,
                        GuiActivator.getResources()
                            .getI18NString("service.gui.REMOVE"));

                    int returnCode = dialog.showDialog();

                    if (returnCode == MessageDialog.OK_RETURN_CODE)
                    {
                        GuiActivator.getContactListService()
                            .removeContact(contact);
                    }
                    else if (returnCode == MessageDialog.OK_DONT_ASK_CODE)
                    {
                        GuiActivator.getContactListService()
                            .removeContact(contact);

                        Constants.REMOVE_CONTACT_ASK = false;
                    }
                }
                else
                    GuiActivator.getContactListService().removeContact(contact);
            }
            catch (Exception ex)
            {
                new ErrorDialog(null,
                                GuiActivator.getResources().getI18NString(
                                "service.gui.REMOVE_CONTACT"),
                                ex.getMessage(),
                                ex)
                            .showDialog();
            }
        }
    }

    /**
     * Removes a contact from a meta contact in a separate thread.
     */
    private static class RemoveMetaContactThread extends Thread
    {
        private MetaContact metaContact;
        public RemoveMetaContactThread(MetaContact contact)
        {
            this.metaContact = contact;
        }

        @Override
        public void run()
        {
            if(Constants.REMOVE_CONTACT_ASK)
            {
                String message
                    = GuiActivator.getResources().getI18NString(
                        "service.gui.REMOVE_CONTACT_TEXT",
                        new String[]{metaContact.getDisplayName()});

                MessageDialog dialog = new MessageDialog(null,
                    GuiActivator.getResources().getI18NString(
                        "service.gui.REMOVE_CONTACT"),
                    message,
                    GuiActivator.getResources().getI18NString(
                        "service.gui.REMOVE"));

                int returnCode = dialog.showDialog();

                if (returnCode == MessageDialog.OK_RETURN_CODE)
                {
                    GuiActivator.getContactListService()
                        .removeMetaContact(metaContact);
                }
                else if (returnCode == MessageDialog.OK_DONT_ASK_CODE)
                {
                    GuiActivator.getContactListService()
                        .removeMetaContact(metaContact);

                    Constants.REMOVE_CONTACT_ASK = false;
                }
            }
            else
            {
                GuiActivator.getContactListService()
                    .removeMetaContact(metaContact);
            }
        }
    }

    /**
     * Removes a group from the contact list in a separate thread.
     */
    private static class RemoveGroupThread extends Thread
    {
        private MetaContactGroup group;

        public RemoveGroupThread(MetaContactGroup group)
        {
            this.group = group;
        }
        @Override
        public void run()
        {
            try
            {
                if(Constants.REMOVE_CONTACT_ASK) {
                    String message = GuiActivator.getResources().getI18NString(
                        "service.gui.REMOVE_CONTACT_TEXT",
                        new String[]{group.getGroupName()});

                    MessageDialog dialog = new MessageDialog(
                        null,
                        GuiActivator.getResources().getI18NString(
                            "service.gui.REMOVE_GROUP"),
                        message,
                        GuiActivator.getResources().getI18NString(
                            "service.gui.REMOVE"));

                    int returnCode = dialog.showDialog();

                    if (returnCode == MessageDialog.OK_RETURN_CODE)
                    {
                        GuiActivator.getContactListService()
                            .removeMetaContactGroup(group);
                    }
                    else if (returnCode == MessageDialog.OK_DONT_ASK_CODE)
                    {
                        GuiActivator.getContactListService()
                            .removeMetaContactGroup(group);

                        Constants.REMOVE_CONTACT_ASK = false;
                    }
                }
                else
                    GuiActivator.getContactListService()
                        .removeMetaContactGroup(group);
            }
            catch (Exception ex)
            {
                new ErrorDialog(null,
                                GuiActivator.getResources().getI18NString(
                                "service.gui.REMOVE_GROUP"),
                                ex.getMessage(),
                                ex)
                .showDialog();
            }
        }
    }
}
