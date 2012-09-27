/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;
import net.java.sip.communicator.util.swing.event.*;

/**
 * The field shown on the top of the main window, which allows the user to
 * search for users.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class SearchField
    extends SIPCommTextField
    implements  TextFieldChangeListener,
                FilterQueryListener,
                Skinnable
{
    /**
     * Class id key used in UIDefaults.
     */
    private static final String uiClassID =
        SearchField.class.getName() +  "FieldUI";

    /**
     * Adds the ui class to UIDefaults.
     */
    static
    {
        UIManager.getDefaults().put(uiClassID,
            SearchFieldUI.class.getName());
    }

    /**
     * The main application window.
     */
    private final MainFrame mainFrame;

    /**
     * The contact list on which we apply the filter.
     */
    private ContactList contactList;

    /**
     * The filter to apply on search.
     */
    private final ContactListSearchFilter searchFilter;

    /**
     * Creates the <tt>SearchField</tt>.
     *
     * @param frame the main application window
     * @param searchFilter the filter to apply on search
     * @param isCallButtonEnabled indicates if the call button should be
     * enabled in this search field
     */
    public SearchField( MainFrame frame,
                        ContactListSearchFilter searchFilter,
                        boolean isCallButtonEnabled)
    {
        super(GuiActivator.getResources()
                .getI18NString("service.gui.ENTER_NAME_OR_NUMBER"));

        this.mainFrame = frame;
        this.searchFilter = searchFilter;

        if(getUI() instanceof  SearchFieldUI)
        {
            ((SearchFieldUI)getUI()).setDeleteButtonEnabled(true);
            ((SearchFieldUI)getUI()).setCallButtonEnabled(isCallButtonEnabled);
        }

        this.setBorder(null);
        this.setOpaque(false);

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

                if (SearchField.this.mainFrame != null)
                    SearchField.this.mainFrame.requestFocusInContactList();
            }
        });

        loadSkin();
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

        boolean isDefaultFilter = false;

        searchFilter.setFilterString(filterString.trim());

        if (filterString != null && filterString.length() > 0)
        {
            filterQuery = contactList.applyFilter(searchFilter);
        }
        else
        {
            filterQuery = contactList.applyDefaultFilter();
            isDefaultFilter = true;
        }

        if (filterQuery != null && !filterQuery.isCanceled())
        {
            // If we already have a result here we update the interface.
            // In the case of default filter we don't need to know if the
            // query has succeeded, as event if it isn't we would like to
            // remove the unknown contact view.
            if (isDefaultFilter || filterQuery.isSucceeded())
                enableUnknownContactView(false);
            else
                // Otherwise we will listen for events for changes in status
                // of this query.
                filterQuery.setQueryListener(this);
        }
        else
        {
            // If the query is null or is canceled, we would simply check the
            // contact list content.
            closeFilterQuery(filterQuery, !contactList.isEmpty());
        }
    }

    /**
     * Sets the unknown contact view to the main contact list window.
     *
     * @param isEnabled indicates if the unknown contact view should be enabled
     * or disabled.
     */
    public void enableUnknownContactView(final boolean isEnabled)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                if (mainFrame != null)
                    mainFrame.enableUnknownContactView(isEnabled);
            }
        });
    }

    /**
     * Sets the contact list, in which the search is performed.
     *
     * @param contactList the contact list in which the search is performed
     */
    public void setContactList(ContactList contactList)
    {
        this.contactList = contactList;
    }

    /**
     * Indicates that the given <tt>query</tt> has finished with failure, i.e.
     * no results for the filter were found.
     *
     * @param query the <tt>FilterQuery</tt>, where this listener is registered
     */
    public void filterQueryFailed(FilterQuery query)
    {
        closeFilterQuery(query, !contactList.isEmpty());
    }

    /**
     * Indicates that the given <tt>query</tt> has finished with success, i.e.
     * the filter has returned results.
     *
     * @param query the <tt>FilterQuery</tt>, where this listener is registered
     */
    public void filterQuerySucceeded(FilterQuery query)
    {
        closeFilterQuery(query, !contactList.isEmpty());
    }

    /**
     * Reloads text field UI defs.
     */
    public void loadSkin()
    {
        if(getUI() instanceof  SearchFieldUI)
            ((SearchFieldUI)getUI()).loadSkin();
    }

    /**
     * Returns the name of the L&F class that renders this component.
     *
     * @return the string "TreeUI"
     * @see JComponent#getUIClassID
     * @see UIDefaults#getUI
     */
    public String getUIClassID()
    {
        return uiClassID;
    }

    private void closeFilterQuery(FilterQuery query, boolean hasResults)
    {
        // If the unknown contact view was previously enabled, but we
        // have found matching contacts we enter the normal view.
        enableUnknownContactView(!hasResults);

        if (hasResults)
            contactList.selectFirstContact();

        query.setQueryListener(null);
    }
}
