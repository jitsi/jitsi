package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;
import net.java.sip.communicator.util.swing.event.*;
import net.java.sip.communicator.util.swing.plaf.*;

/**
 * The field shown on the top of the main window, which allows the user to
 * search for users.
 * @author Yana Stamcheva
 */
public class SearchField
    extends SIPCommTextField
    implements  TextFieldChangeListener,
                FilterQueryListener
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
     * @param frame the main application window
     */
    public SearchField(MainFrame frame)
    {
        super(GuiActivator.getResources()
                .getI18NString("service.gui.ENTER_NAME_OR_NUMBER"));

        this.mainFrame = frame;

        SearchTextFieldUI textFieldUI = new SearchTextFieldUI();
        textFieldUI.setDeleteButtonEnabled(true);
        this.setUI(textFieldUI);
        this.setBorder(null);
        this.setOpaque(false);
        this.setPreferredSize(new Dimension(100, 22));

        this.setDragEnabled(true);
        this.addTextChangeListener(this);

        InputMap imap = getInputMap(JComponent.WHEN_FOCUSED);
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape");
        ActionMap amap = getActionMap();
        amap.put("escape", new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                setText("");

                SearchField.this.mainFrame.requestFocusInCenterPanel();
            }
        });
    }

    /**
     * Handles the change when a char has been inserted in the field.
     */
    public void textInserted()
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
     */
    public void textRemoved()
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
        TreeContactList contactList = GuiActivator.getContactList();

        if (filterString != null && filterString.length() > 0)
        {
            FilterQuery filterQuery
                = contactList.applyFilter(TreeContactList.searchFilter);

            if (filterQuery != null)
                filterQuery.setQueryListener(this);
        }
        else
        {
            contactList.applyDefaultFilter();

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

    /**
     * Indicates that the given <tt>query</tt> has finished with failure, i.e.
     * no results for the filter were found.
     * @param query the <tt>FilterQuery</tt>, where this listener is registered
     */
    public void filterQueryFailed(FilterQuery query)
    {
        /// If don't have matching contacts we enter the unknown contact
        // view.
        enableUnknownContactView(true);

        lastHasMatching = false;
        query.setQueryListener(null);
    }

    /**
     * Indicates that the given <tt>query</tt> has finished with success, i.e.
     * the filter has returned results.
     * @param query the <tt>FilterQuery</tt>, where this listener is registered
     */
    public void filterQuerySucceeded(FilterQuery query)
    {
        // If the unknown contact view was previously enabled, but we
        // have found matching contacts we enter the normal view.
        if (!lastHasMatching)
            enableUnknownContactView(false);

        GuiActivator.getContactList().selectFirstContact();

        lastHasMatching = true;
        query.setQueryListener(null);
    }
}
