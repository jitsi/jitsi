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
    /**
     * The logger used by this class.
     */
    private final Logger logger = Logger.getLogger(SearchField.class);

    /**
     * The main application window.
     */
    private final MainFrame mainFrame;

    /**
     * Creates the <tt>SearchField</tt>.
     * @param frame the main application window
     */
    public SearchField(MainFrame frame)
    {
        super(GuiActivator.getResources()
                .getI18NString("service.gui.ENTER_NAME_OR_NUMBER"));

        this.mainFrame = frame;

        SearchFieldUI textFieldUI = new SearchFieldUI();
        textFieldUI.setDeleteButtonEnabled(true);
        this.setUI(textFieldUI);
        this.setBorder(null);
        this.setOpaque(false);
        this.setPreferredSize(new Dimension(100, 22));

        this.setDragEnabled(true);
        this.addTextChangeListener(this);

        InputMap imap
            = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
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

        updateContactListView();
    }

    /**
     * Handles the change when a char has been removed from the field.
     */
    public void textRemoved()
    {
        updateContactListView();
    }

    /**
     * Do not need this for the moment.
     * @param e the <tt>DocumentEvent</tt> that notified us
     */
    public void changedUpdate(DocumentEvent e) {}

    /**
     * Schedules an update if necessary.
     */
    private void updateContactListView()
    {
        String filterString = getText();

        FilterQuery filterQuery = null;

        if (filterString != null && filterString.length() > 0)
        {
            TreeContactList.searchFilter
                .setFilterString(filterString);

            filterQuery = GuiActivator.getContactList()
                .applyFilter(TreeContactList.searchFilter);
        }
        else
        {
            filterQuery = GuiActivator.getContactList().applyDefaultFilter();
        }

        if (logger.isDebugEnabled())
            logger.debug("Filter query for string "
                + filterString + " : " + filterQuery);

        if (filterQuery != null && !filterQuery.isCanceled())
        {
            // If we already have a result here we update the interface.
            if (filterQuery.isSucceeded())
                enableUnknownContactView(false);
            else
                // Otherwise we will listen for events for changes in status
                // of this query.
                filterQuery.setQueryListener(this);
        }
        else
            // If the query is null or is canceled, we would simply check the
            // contact list content.
            enableUnknownContactView(GuiActivator.getContactList().isEmpty());
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
        enableUnknownContactView(false);

        GuiActivator.getContactList().selectFirstContact();

        query.setQueryListener(null);
    }
}
