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

import java.awt.*;
import java.awt.event.*;

import javax.swing.text.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.service.gui.*;

/**
 * The <tt>MainKeyDispatcher</tt> is added to pre-listen KeyEvents before
 * they're delivered to the current focus owner in order to introduce a
 * specific behavior for the <tt>SearchField</tt> on top of the contact
 * list.
 *
 * @author Yana Stamcheva
 */
public class ContactListSearchKeyDispatcher
    implements KeyEventDispatcher
{
    /**
     * The keyboard focus manager.
     */
    private KeyboardFocusManager keyManager;

    /**
     * The contact list on which this key dispatcher works.
     */
    private ContactList contactList;

    /**
     * The search field of this key dispatcher.
     */
    private final SearchField searchField;

    /**
     * The container of the contact list.
     */
    private final ContactListContainer contactListContainer;

    /**
     * Creates an instance of <tt>MainKeyDispatcher</tt>.
     * @param keyManager the parent <tt>KeyboardFocusManager</tt>
     */
    public ContactListSearchKeyDispatcher(  KeyboardFocusManager keyManager,
                                            SearchField searchField,
                                            ContactListContainer container)
    {
        this.keyManager = keyManager;
        this.searchField = searchField;
        this.contactListContainer = container;
    }

    /**
     * Sets the contact list.
     *
     * @param contactList the contact list to set
     */
    public void setContactList(ContactList contactList)
    {
        this.contactList = contactList;
    }

    /**
     * Dispatches the given <tt>KeyEvent</tt>.
     * @param e the <tt>KeyEvent</tt> to dispatch
     * @return <tt>true</tt> if the KeyboardFocusManager should take no
     * further action with regard to the KeyEvent; <tt>false</tt>
     * otherwise
     */
    public boolean dispatchKeyEvent(KeyEvent e)
    {
        // If this window is not the focus window or if the event is not of type
        // PRESSED, we have nothing more to do here. Also don't re-dispatch any
        // events if the menu is active.
        if (!contactListContainer.isFocused())
            return false;

        int id = e.getID();

        if (id != KeyEvent.KEY_PRESSED && id != KeyEvent.KEY_TYPED)
            return false;

        SingleWindowContainer singleWindowContainer
            = GuiActivator.getUIService().getSingleWindowContainer();

        if ((singleWindowContainer != null)
                && singleWindowContainer.containsFocusOwner())
            return false;

        Component focusOwner = keyManager.getFocusOwner();

        if (focusOwner != null
                && !searchField.isFocusOwner()
                && focusOwner instanceof JTextComponent)
            return false;
        if (contactListContainer.isMenuSelected())
            return false;

        // Ctrl-Enter || Cmd-Enter typed when this window is the focused
        // window.
        //
        // Tried to make this with key bindings first, but has a problem
        // with enter key binding. When the popup menu containing call
        // contacts was opened the default keyboard manager was prioritizing
        // the window ENTER key, which will open a chat and we wanted that
        // the enter starts a call with the selected contact from the menu.
        // This is why we need to do it here and to check if the
        // permanent focus owner is equal to the focus owner, which is not
        // the case when a popup menu is opened.
        int keyCode = e.getKeyCode();

        if (keyCode == KeyEvent.VK_ENTER
                && (e.isControlDown() || e.isMetaDown()))
        {
            contactListContainer.ctrlEnterKeyTyped();
            return false;
        }
        else if (keyCode == KeyEvent.VK_ENTER
                && focusOwner.equals(keyManager.getPermanentFocusOwner()))
        {
            contactListContainer.enterKeyTyped();
            return false;
        }

        // If the search field is the focus owner.
        if (searchField.isFocusOwner()
                && (keyCode == KeyEvent.VK_UP
                        || keyCode == KeyEvent.VK_DOWN
                        || keyCode == KeyEvent.VK_PAGE_UP
                        || keyCode == KeyEvent.VK_PAGE_DOWN))
        {
            contactList.selectFirstContact();
            if(contactList instanceof TreeContactList)
            {
                ((TreeContactList) contactList).setAutoSectionAllowed(false);
            }
            contactList.getComponent().requestFocus();
            return false;
        }

        // If the contact list is the focus owner.
        if (contactList.getComponent().isFocusOwner()
                && keyCode == KeyEvent.VK_ESCAPE)
        {
            // Removes all current selections.
            contactList.removeSelection();

            if(contactList instanceof TreeContactList)
            {
                ((TreeContactList) contactList).setAutoSectionAllowed(false);
            }
            if (searchField.getText() != null)
                searchField.requestFocus();

            return false;
        }

        char keyChar = e.getKeyChar();
        UIGroup selectedGroup = contactList.getSelectedGroup();

        // No matter who is the focus owner.
        if (keyChar == KeyEvent.CHAR_UNDEFINED
                || keyCode == KeyEvent.VK_ENTER
                || keyCode == KeyEvent.VK_DELETE
                || keyCode == KeyEvent.VK_BACK_SPACE
                || keyCode == KeyEvent.VK_TAB
                || e.getKeyChar() == '\t'
                || keyCode == KeyEvent.VK_SPACE
                || (selectedGroup != null
                        && (keyChar == '+' || keyChar == '-')))
        {
            return false;
        }

        boolean singleWindowRule
            = singleWindowContainer == null
                || contactList.getComponent().isFocusOwner();

        if (!searchField.isFocusOwner()
                && focusOwner != null
                && singleWindowRule
                && focusOwner.equals(keyManager.getPermanentFocusOwner()))
        {
            // Request the focus in the search field if a letter is typed.
            searchField.requestFocusInWindow();
            // We re-dispatch the event to search field.
            keyManager.redispatchEvent(searchField, e);
            // We don't want to dispatch further this event.
            return true;
        }

        return false;
    }
}
