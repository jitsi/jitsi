package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;
import net.java.sip.communicator.util.swing.plaf.*;

/**
 * The field shown on the top of the main window, which allows the user to
 * search for users.
 * @author Yana Stamcheva
 */
public class SearchField
    extends SIPCommTextField
    implements DocumentListener
{
    private final Logger logger = Logger.getLogger(SearchField.class);

    private final MainFrame mainFrame;

    /**
     * We save the last status of hasMatching. By default we consider that
     * we'll find matching contacts.
     */
    private boolean lastHasMatching = true;

    private SearchThread searchThread = null;

    /**
     * Creates the <tt>SearchField</tt>.
     * @param mainFrame the main application window
     */
    public SearchField(MainFrame mainFrame)
    {
        super(GuiActivator.getResources()
                .getI18NString("service.gui.ENTER_NAME_OR_NUMBER"));

        this.mainFrame = mainFrame;

        SearchTextFieldUI textFieldUI = new SearchTextFieldUI();
        textFieldUI.setDeleteButtonEnabled(true);
        this.setUI(textFieldUI);
        this.setBorder(null);
        this.setOpaque(false);
        this.setPreferredSize(new Dimension(100, 22));

        this.setDragEnabled(true);
        this.getDocument().addDocumentListener(this);

        InputMap imap = getInputMap(JComponent.WHEN_FOCUSED);
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape");
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter");
        ActionMap amap = getActionMap();
        amap.put("escape", new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                setText("");

                SearchField.this.mainFrame.requestFocusInCenterPanel();
            }
        });
        amap.put("enter", new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (!lastHasMatching)
                    CallManager.createCall(getText());
                else
                    // Starts a chat with the currently selected contact.
                    GuiActivator.getContactList().startSelectedContactChat();
            }
        });
    }

    /**
     * Handles the change when a char has been inserted in the field.
     * @param e the <tt>DocumentEvent</tt> that notified us
     */
    public void insertUpdate(DocumentEvent e)
    {
        // Should explicitly check if there's a text, because the default text
        // triggers also an insertUpdate event.
        String filterString = this.getText();
        if (filterString == null || filterString.length() <= 0)
            return;

        scheduleUpdate();
    }

    /**
     * Handles the change when a char has been removed from the field.
     * @param e the <tt>DocumentEvent</tt> that notified us
     */
    public void removeUpdate(DocumentEvent e)
    {
        scheduleUpdate();
    }

    /**
     * Do not need this for the moment.
     * @param e the <tt>DocumentEvent</tt> that notified us
     */
    public void changedUpdate(DocumentEvent e) {}

    /**
     * Schedules an update if necessary.
     */
    private void scheduleUpdate()
    {
        GuiActivator.getContactList().stopFiltering();

        if (searchThread == null)
        {
            searchThread = new SearchThread();
            searchThread.start();
        }
        else
            synchronized (searchThread)
            {
                searchThread.notify();
            }
    }

    /**
     * The <tt>SearchThread</tt> is meant to launch the search in a separate
     * thread.
     */
    private class SearchThread extends Thread
    {
        public void run()
        {
            while (true)
            {
                String filterString = getText();

                if (filterString != null && filterString.length() > 0)
                {
                    TreeContactList.searchFilter
                        .setFilterString(filterString);
                }

                updateContactListView(filterString);

                synchronized (this)
                {
                    try
                    {
                        if (filterString == getText() //both are null or equal
                            || (filterString != null
                                && filterString.equals(getText())))
                        {
                            //filter still has the same value as the one
                            //we did a search for, so we can wait for a
                            //while
                            this.wait();

                            filterString = getText();
                        }
                    }
                    catch (InterruptedException e)
                    {
                        logger.debug("Search thread was interrupted.", e);
                    }
                }
            }
        }
    }

    /**
     * Updates the current contact list view to match the given
     * <tt>filterString</tt>. If the <tt>filterString</tt> is null or
     * empty we reset the presence filter.
     * @param filterString the current filter string entered in
     * this search field
     */
    public void updateContactListView(String filterString)
    {
        if (filterString != null && filterString.length() > 0)
        {
            TreeContactList contactList = GuiActivator.getContactList();

            boolean hasMatching
                = contactList.applyFilter(TreeContactList.searchFilter);

            // If don't have matching contacts we enter the unknown contact
            // view.
            if (!hasMatching)
            {
                enableUnknownContactView(true);
            }
            // If the unknown contact view was previously enabled, but we
            // have found matching contacts we enter the normal view.
            else
            {
                if (!lastHasMatching)
                    enableUnknownContactView(false);

                contactList.selectFirstContact();
            }

            lastHasMatching = hasMatching;
        }
        else
        {
            TreeContactList contactList = GuiActivator.getContactList();

            contactList.applyFilter(TreeContactList.presenceFilter);

            enableUnknownContactView(false);
        }
    }

    /**
     * Sets the unknown contact view to the main contact list window.
     * @param isEnabled indicates if the unknown contact view should be enabled
     * or disabled.
     */
    public void enableUnknownContactView(final boolean isEnabled)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                mainFrame.enableUnknownContactView(isEnabled);
            }
        });
    }
}
