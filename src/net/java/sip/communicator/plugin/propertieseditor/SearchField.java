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
package net.java.sip.communicator.plugin.propertieseditor;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.*;
import javax.swing.table.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.plaf.*;
import net.java.sip.communicator.util.skin.*;

import org.apache.commons.lang3.*;
import org.jitsi.service.configuration.*;

/**
 * The field used for searching in the properties table.
 * 
 * @author Marin Dzhigarov
 */
public class SearchField
    extends SIPCommTextField
    implements Skinnable
{
    /**
     * Serial Version UID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The number of milliseconds that {@link #filterThread} is allowed to stay
     * alive idle i.e. without performing any filtering. After the timeout
     * elapses, <tt>filterThread</tt> will commit suicide.
     */
    private static final long FILTER_THREAD_TIMEOUT = 5 * 60 * 1000;

    /**
     * Class id key used in UIDefaults.
     */
    private static final String uiClassID
        = SearchField.class.getName() + "FieldUI";

    /**
     * Adds the ui class to UIDefaults.
     */
    static
    {
        UIManager.getDefaults().put(uiClassID, SearchFieldUI.class.getName());
    }

    private final ConfigurationService confService
        = PropertiesEditorActivator.getConfigurationService();

    /**
     * The <tt>String</tt> that filters the <tt>ConfigurationService</tt>
     * properties to be displayed.
     */
    private String filter;

    /**
     * The <tt>Object</tt> which is to be used for synchronization purposes
     * related to <tt>ConfigurationService</tt> property filtering (e.g.
     * {@link #filter}, {@link #filterThread}).
     */
    private final Object filterSyncRoot = new Object();

    private Thread filterThread;

    /**
     * The table on which the search will be performed.
     */
    private final JTable table;

    /**
     * Creates an instance <tt>SearchField</tt>.
     * 
     * @param text the text we would like to enter by default
     * @param sorter the sorter which will be used for filtering.
     */
    public SearchField(String text, JTable table)
    {
        super(text);

        this.table = table;

        ComponentUI ui = getUI();

        if (ui instanceof SearchFieldUI)
        {
            SearchFieldUI searchFieldUI = (SearchFieldUI) ui;

            searchFieldUI.setCallButtonEnabled(false);
            searchFieldUI.setDeleteButtonEnabled(true);
        }

        setBorder(null);
        setDragEnabled(true);
        setOpaque(false);
        setPreferredSize(new Dimension(100, 25));

        InputMap imap
            = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape");

        ActionMap amap = getActionMap();
        @SuppressWarnings("serial")
        AbstractAction escapeAction
            = new AbstractAction()
            {
                public void actionPerformed(ActionEvent e)
                {
                    setText("");
                }
            };

        amap.put("escape", escapeAction);

        getDocument().addDocumentListener(
                new DocumentListener()
                {
                    public void changedUpdate(DocumentEvent e) {}

                    public void insertUpdate(DocumentEvent e)
                    {
                        filter(getText());
                    }

                    public void removeUpdate(DocumentEvent e)
                    {
                        filter(getText());
                    }
                });

        loadSkin();
    }

    /**
     * Schedules a filter to be applied to <tt>ConfigurationService</tt>
     * properties so that only the ones matching the specified <tt>filter</tt>
     * will be displayed.
     *
     * @param filter the <tt>String</tt> to filter the
     * <tt>ConfigurationService</tt> properties to be displayed
     */
    private void filter(String filter)
    {
        synchronized (filterSyncRoot)
        {
            this.filter = filter;

            if (filterThread == null)
            {
                filterThread
                    = new Thread()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                runInFilterThread();
                            }
                            finally
                            {
                                /*
                                 * XXX Making sure here that filterThread is
                                 * aware of its death is just a precaution for
                                 * the cases of abnormal execution of the method
                                 * runInFilterThread(); otherwise, it's
                                 * insufficient in terms of synchronization
                                 * because the lock on filterSyncRoot has been
                                 * relinquished and the method
                                 * runInFilterThread() should take care of
                                 * making sure that filterThread is aware of its
                                 * death while it holds filterSyncRoot and has
                                 * determined that filterThread has idled too
                                 * long.
                                 */
                                synchronized (filterSyncRoot)
                                {
                                    if (Thread.currentThread().equals(
                                            filterThread))
                                    {
                                        filterThread = null;
                                    }
                                }
                            }
                        }
                    };
                filterThread.setDaemon(true);
                filterThread.setName(
                        SearchField.class.getName() + ".filterThread");
                filterThread.start();
            }
            else
            {
                filterSyncRoot.notify();
            }
        }
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

    /**
     * Reloads text field UI defs.
     */
    public void loadSkin()
    {
        ComponentUI ui = getUI();

        if (ui instanceof SearchFieldUI)
            ((SearchFieldUI) ui).loadSkin();
    }

    /**
     * Runs in {@link #filterThread} to apply {@link #filter} to the displayed
     * <tt>ConfigurationService</tt> properties.
     */
    private void runInFilterThread()
    {
        String prevFilter = null;
        long prevFilterTime = 0;

        do
        {
            final String filter;

            synchronized (filterSyncRoot)
            {
                filter = this.filter;

                /*
                 * If the currentThread is idle for too long (which also means
                 * that the filter has not been changed), kill it because we do
                 * not want to keep it alive forever.
                 */
                if ((prevFilterTime != 0)
                        && StringUtils.equalsIgnoreCase(filter, prevFilter))
                {
                    long timeout
                        = FILTER_THREAD_TIMEOUT
                            - (System.currentTimeMillis() - prevFilterTime);

                    if (timeout > 0)
                    {
                        // The currentThread has been idle but not long enough.
                        try
                        {
                            filterSyncRoot.wait(timeout);
                            continue;
                        }
                        catch (InterruptedException ie)
                        {
                            // The currentThread will die bellow at the break.
                        }
                    }
                    // Commit suicide.
                    if (Thread.currentThread().equals(filterThread))
                        filterThread = null;
                    break;
                }
            }

            List<String> properties = confService.getAllPropertyNames();
            final List<Object[]> rows
                = new ArrayList<Object[]>(properties.size());

            for (String property : properties)
            {
                String value = (String) confService.getProperty(property);
                if ((filter == null)
                    || StringUtils.containsIgnoreCase(property, filter)
                    || StringUtils.containsIgnoreCase(value, filter))
                {
                    rows.add(
                            new Object[]
                                    {
                                        property,
                                        confService.getProperty(property)
                                    });
                }
            }

            // If in the meantime someone has changed the filter, we don't want
            // to update the GUI but filter the results again.
            if (StringUtils.equalsIgnoreCase(filter, this.filter))
            {
                LowPriorityEventQueue.invokeLater(
                        new Runnable()
                        {
                           public void run()
                           {
                               DefaultTableModel model
                                   = (DefaultTableModel) table.getModel();

                               model.setRowCount(0);
                               for (Object[] row : rows)
                               {
                                   model.addRow(row);
                                   if (filter != SearchField.this.filter)
                                       return;
                               }
                           }
                        });
            }

            prevFilter = filter;
            prevFilterTime = System.currentTimeMillis();
        }
        while (true);
    }
}
