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
package net.java.sip.communicator.impl.gui;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.service.contacteventhandler.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>DefaultContactEventHandler</tt> provides the default behavior on a
 * contact event.
 *
 * @author Yana Stamcheva
 */
public class DefaultContactEventHandler
    implements ContactEventHandler
{
    private final MainFrame mainFrame;

    /**
     * Creates an instance of <tt>DefaultContactEventHandler</tt>.
     *
     * @param mainFrame the main application window
     */
    public DefaultContactEventHandler(MainFrame mainFrame)
    {
        this.mainFrame = mainFrame;
    }

    /**
     * Indicates that a contact has been clicked.
     *
     * @param contact selected contact
     * @param clickCount count of user clicks
     */
    public void contactClicked(Contact contact, int clickCount)
    {
        MetaContact metaContact
            = GuiActivator.getContactListService()
                .findMetaContactByContact(contact);

        SwingUtilities
            .invokeLater(new RunMessageWindow(metaContact, contact));
    }

    /**
     * Runs the chat window for the specified contact. We examine different
     * cases here, depending on the chat window mode.
     *
     * In mode "Open messages in new window" a new window is opened for the
     * given <tt>MetaContact</tt> if there's no opened window for it,
     * otherwise the existing chat window is made visible and focused.
     *
     * In mode "Group messages in one chat window" a JTabbedPane is used to show
     * chats for different contacts in ona window. A new tab is opened for the
     * given <tt>MetaContact</tt> if there's no opened tab for it, otherwise
     * the existing chat tab is selected and focused.
     *
     * @author Yana Stamcheva
     */
    public static class RunMessageWindow
        implements Runnable
    {
        private final MetaContact metaContact;

        private final Contact protocolContact;

        /**
         * Creates a chat window
         *
         * @param metaContact
         * @param protocolContact
         */
        public RunMessageWindow(MetaContact metaContact,
            Contact protocolContact)
        {
            this.metaContact = metaContact;
            this.protocolContact = protocolContact;
        }

        /**
         * Opens a chat window
         */
        public void run()
        {
            ChatWindowManager chatWindowManager
                = GuiActivator.getUIService().getChatWindowManager();
            ChatPanel chatPanel
                = chatWindowManager
                    .getContactChat(metaContact, protocolContact);

            chatWindowManager.openChat(chatPanel, true);
        }
    }
}
