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

import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.event.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The field shown on the top of the main window, which allows the user to
 * search for users.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 * @author Marin Dzhigarov
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
    private static final String uiClassID
        = SearchField.class.getName() +  "FieldUI";

    /**
     * Adds the ui class to UIDefaults.
     */
    static
    {
        UIManager.getDefaults().put(
                uiClassID,
                ContactSearchFieldUI.class.getName());
    }

    /**
     * The contact list on which we apply the filter.
     */
    private ContactList contactList;

    /**
     * The current filter query.
     */
    private FilterQuery currentFilterQuery = null;

    /**
     * The main application window.
     */
    private final MainFrame mainFrame;

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
     * @param isSMSButtonEnabled indicates if the sms button should be
     * enabled in this search field
     */
    public SearchField( MainFrame frame,
                        ContactListSearchFilter searchFilter,
                        boolean isCallButtonEnabled,
                        boolean isSMSButtonEnabled)
    {
        super(GuiActivator.getResources()
                .getI18NString("service.gui.ENTER_NAME_OR_NUMBER"));

        this.mainFrame = frame;
        this.searchFilter = searchFilter;

        if (getUI() instanceof ContactSearchFieldUI)
        {
            ((ContactSearchFieldUI) getUI()).setupListeners();
            ((ContactSearchFieldUI) getUI()).setDeleteButtonEnabled(true);
            ((ContactSearchFieldUI) getUI())
                .setCallButtonEnabled(isCallButtonEnabled);
            ((ContactSearchFieldUI) getUI())
                .setSMSButtonEnabled(isSMSButtonEnabled);
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
     * Do not need this for the moment.
     * @param e the <tt>DocumentEvent</tt> that notified us
     */
    @Override
    public void changedUpdate(DocumentEvent e) {}

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
     * Indicates that the given <tt>query</tt> has finished with failure, i.e.
     * no results for the filter were found.
     *
     * @param query the <tt>FilterQuery</tt>, where this listener is registered
     */
    public void filterQueryFailed(FilterQuery query)
    {
        // If the query has failed then we don't have results.
        if (currentFilterQuery.equals(query))
            filterQueryFinished(query, false);
    }

    /**
     * Performs all needed updates when a filter query has finished.
     *
     * @param query the query that has finished
     * @param hasResults indicates if the query has results
     */
    private void filterQueryFinished(FilterQuery query, boolean hasResults)
    {
        // If the unknown contact view was previously enabled, but we
        // have found matching contacts we enter the normal view.
        enableUnknownContactView(!hasResults);

        query.setQueryListener(null);
    }

    /**
     * Indicates that the given <tt>query</tt> has finished with success, i.e.
     * the filter has returned results.
     *
     * @param query the <tt>FilterQuery</tt>, where this listener is registered
     */
    public void filterQuerySucceeded(FilterQuery query)
    {
        // If the query has succeeded then we have results.
        if (currentFilterQuery.equals(query))
            filterQueryFinished(query, true);
    }

    /**
     * Returns the name of the L&F class that renders this component.
     *
     * @return the string "TreeUI"
     * @see JComponent#getUIClassID
     * @see UIDefaults#getUI
     */
    @Override
    public String getUIClassID()
    {
        return uiClassID;
    }

    /**
     * Reloads text field UI defs.
     */
    public void loadSkin()
    {
        if (getUI() instanceof ContactSearchFieldUI)
            ((ContactSearchFieldUI) getUI()).loadSkin();
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
     * Schedules an update if necessary.
     */
    private void updateContactListView()
    {
        String filterString = getText();

        boolean isDefaultFilter = false;

        searchFilter.setFilterString(filterString.trim());

        // First finish the last filter.
        if (currentFilterQuery != null)
            filterQueryFinished(currentFilterQuery, true);

        if (filterString != null && filterString.length() > 0)
        {
            currentFilterQuery = contactList.applyFilter(searchFilter);
        }
        else
        {
            currentFilterQuery = contactList.applyDefaultFilter();
            isDefaultFilter = true;
        }

        if (currentFilterQuery != null && !currentFilterQuery.isCanceled())
        {
            // If we already have a result here we update the interface.
            // In the case of default filter we don't need to know if the
            // query has succeeded, as event if it isn't we would like to
            // remove the unknown contact view.
            if (isDefaultFilter || currentFilterQuery.isSucceeded())
                enableUnknownContactView(false);
            else
                // Otherwise we will listen for events for changes in status
                // of this query.
                currentFilterQuery.setQueryListener(this);
        }
        else
        {
            // If the query is null or is canceled, we would simply check the
            // contact list content.
            filterQueryFinished(currentFilterQuery, !contactList.isEmpty());
        }
    }

    public void dispose()
    {
        if(getUI() instanceof ContactSearchFieldUI)
        {
            ((ContactSearchFieldUI)getUI()).removeListeners();
        }
    }
}
