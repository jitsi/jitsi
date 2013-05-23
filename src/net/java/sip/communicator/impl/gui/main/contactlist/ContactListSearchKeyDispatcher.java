/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;
import java.awt.event.*;

import javax.swing.text.*;

import net.java.sip.communicator.impl.gui.*;
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
        Component focusOwner = keyManager.getFocusOwner();

        // If this window is not the focus window  or if the event is not
        // of type PRESSED we have nothing more to do here.
        // Also don't re-dispatch any events if the menu is active.
        if (!contactListContainer.isFocused()
            || (e.getID() != KeyEvent.KEY_PRESSED
                && e.getID() != KeyEvent.KEY_TYPED)
            || (GuiActivator.getUIService()
                    .getSingleWindowContainer() != null)
                && GuiActivator.getUIService()
                    .getSingleWindowContainer().containsFocus()
            || (focusOwner != null
                && !searchField.isFocusOwner()
                && focusOwner instanceof JTextComponent)
            || contactListContainer.isMenuSelected())
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
        if (e.getKeyCode() == KeyEvent.VK_ENTER
            && (e.isControlDown() || e.isMetaDown()))
        {
            contactListContainer.ctrlEnterKeyTyped();
            return false;
        }
        else if (e.getKeyCode() == KeyEvent.VK_ENTER
            && keyManager.getFocusOwner()
            .equals(keyManager.getPermanentFocusOwner()))
        {
            contactListContainer.enterKeyTyped();
            return false;
        }

        // If the search field is the focus owner.
        if (searchField.isFocusOwner()
            && (e.getKeyCode() == KeyEvent.VK_UP
                || e.getKeyCode() == KeyEvent.VK_DOWN
                || e.getKeyCode() == KeyEvent.VK_PAGE_UP
                || e.getKeyCode() == KeyEvent.VK_PAGE_DOWN))
        {
            contactList.selectFirstContact();
            contactList.getComponent().requestFocus();
            return false;
        }

        // If the contact list is the focus owner.
        if (contactList.getComponent().isFocusOwner()
            && e.getKeyCode() == KeyEvent.VK_ESCAPE)
        {
            // Removes all current selections.
            contactList.removeSelection();

            if (searchField.getText() != null)
            {
                searchField.requestFocus();
            }
            return false;
        }

        UIGroup selectedGroup = contactList.getSelectedGroup();

        // No matter who is the focus owner.
        if (e.getKeyChar() == KeyEvent.CHAR_UNDEFINED
            || e.getKeyCode() == KeyEvent.VK_ENTER
            || e.getKeyCode() == KeyEvent.VK_DELETE
            || e.getKeyCode() == KeyEvent.VK_BACK_SPACE
            || e.getKeyCode() == KeyEvent.VK_TAB
            || e.getKeyChar() == '\t'
            || e.getKeyCode() == KeyEvent.VK_SPACE
            || (selectedGroup != null
                && (e.getKeyChar() == '+'
                    || e.getKeyChar() == '-')))
        {
            return false;
        }

        boolean singleWindowRule
            = GuiActivator.getUIService().getSingleWindowContainer() == null
                || contactList.getComponent().isFocusOwner();

        if (!searchField.isFocusOwner()
            && keyManager.getFocusOwner() != null
            && singleWindowRule
            && keyManager.getFocusOwner()
                .equals(keyManager.getPermanentFocusOwner()))
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
