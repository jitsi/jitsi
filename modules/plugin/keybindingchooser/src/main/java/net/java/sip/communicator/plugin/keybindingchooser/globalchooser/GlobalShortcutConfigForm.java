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
package net.java.sip.communicator.plugin.keybindingchooser.globalchooser;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.plugin.keybindingchooser.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.globalshortcut.*;
import net.java.sip.communicator.service.keybindings.*;
import net.java.sip.communicator.util.*;

/**
 * This ConfigurationForm shows the list of global shortcut
 *
 * @author Sebastien Vincent
 */
public class GlobalShortcutConfigForm
    extends TransparentPanel
    implements ListSelectionListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The logger for this class.
     */
    private static Logger logger = Logger.getLogger(
        GlobalShortcutConfigForm.class);

    /**
     * Displays the registered shortcuts.
     */
    private JTable shortcutsTable = new JTable();

    /**
     * Contains the shortcutsTable.
     */
    private JScrollPane scrollPane = new JScrollPane();

    /**
     * Contains listPanel.
     */
    private JPanel mainPanel = this;

    /**
     * Model for the shortcutsTable
     */
    private GlobalShortcutTableModel tableModel =
        new GlobalShortcutTableModel();

    /**
     * Constructor
     */
    public GlobalShortcutConfigForm()
    {
        super(new BorderLayout());
        logger.trace("New global shortcut configuration form.");
        this.initComponents();
    }

    /**
     * Initialize the swing components.
     */
    private void initComponents()
    {
        shortcutsTable.setRowHeight(22);
        shortcutsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        shortcutsTable.setShowHorizontalLines(false);
        shortcutsTable.setShowVerticalLines(false);
        shortcutsTable.setModel(tableModel);
        shortcutsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        shortcutsTable.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                if(e.getClickCount() >= 2)
                {
                    int row = GlobalShortcutConfigForm.this.shortcutsTable.
                        getSelectedRow();
                    GlobalShortcutEntry en =
                        GlobalShortcutConfigForm.this.tableModel.
                            getEntryAt(row);
                    List<AWTKeyStroke> kss = new ArrayList<AWTKeyStroke>();

                    GlobalShortcutDialog dialog =
                        new GlobalShortcutDialog((Dialog)
                            GlobalShortcutConfigForm.this.getTopLevelAncestor(),
                            en);

                    kss.add(en.getShortcut());
                    kss.add(en.getShortcut2());

                    KeybindingChooserActivator.getGlobalShortcutService().
                        setEnable(false);
                    int ret = dialog.showDialog();

                    if(ret == 1)
                    {
                        // ok button clicked
                        kss = new ArrayList<AWTKeyStroke>();
                        List<GlobalShortcutEntry> lst =
                            tableModel.getEntries();

                        for(GlobalShortcutEntry ee : lst)
                        {
                            boolean isEntry = (ee == en);
                            AWTKeyStroke s1 = isEntry ? null :
                                    ee.getShortcut();
                            AWTKeyStroke s2 = isEntry ? null :
                                    ee.getShortcut2();

                            if(s1 != null && en.getShortcut() != null &&
                                s1.getKeyCode() == en.getShortcut().
                                    getKeyCode() &&
                                s1.getModifiers() == en.getShortcut().
                                    getModifiers())
                            {
                                kss.add(null);
                                kss.add(ee.getShortcut2());
                                ee.setShortcuts(kss);
                                break;
                            }
                            else if(s2 != null && en.getShortcut2() != null &&
                                s2.getKeyCode() == en.getShortcut2().
                                    getKeyCode() &&
                                s2.getModifiers() == en.getShortcut2().
                                    getModifiers())
                            {
                                kss.add(ee.getShortcut());
                                kss.add(null);
                                ee.setShortcuts(kss);
                                break;
                            }
                        }

                        KeybindingChooserActivator.getGlobalShortcutService().
                        setEnable(true);
                        GlobalShortcutConfigForm.this.saveConfig();
                        GlobalShortcutConfigForm.this.refresh();
                        KeybindingChooserActivator.getGlobalShortcutService().
                            setEnable(true);
                    }
                    else
                    {
                        en.setShortcuts(kss);
                    }
                }
            }
        });

        scrollPane.getViewport().add(this.shortcutsTable);
        mainPanel.add(this.scrollPane,  BorderLayout.CENTER);
        mainPanel.setPreferredSize(new Dimension(500, 400));
        shortcutsTable.getSelectionModel().addListSelectionListener(this);
        loadConfig();
    }

    /**
     * Loads configuration.
     */
    private void loadConfig()
    {
        KeybindingsService keybindingService =
            KeybindingChooserActivator.getKeybindingsService();

        GlobalKeybindingSet set = keybindingService.getGlobalBindings();

        for(Map.Entry<String, List<AWTKeyStroke>> entry :
            set.getBindings().entrySet())
        {
            String key = entry.getKey();
            List<AWTKeyStroke> kss = entry.getValue();
            GlobalShortcutEntry gke = null;
            String desc = null;

            if(key.equals("answer"))
            {
                desc = Resources.getString(
                    "plugin.keybindings.globalchooser.ANSWER_CALL");
            }
            else if(key.equals("hangup"))
            {
                desc = Resources.getString(
                    "plugin.keybindings.globalchooser.HANGUP_CALL");
            }
            else if(key.equals("answer_hangup"))
            {
                desc = Resources.getString(
                    "plugin.keybindings.globalchooser.ANSWER_HANGUP_CALL");
            }
            else if(key.equals("contactlist"))
            {
                desc = Resources.getString(
                    "plugin.keybindings.globalchooser.SHOW_CONTACTLIST");
            }
            else if(key.equals("mute"))
            {
                desc = Resources.getString(
                    "plugin.keybindings.globalchooser.MUTE_CALLS");
            }
            else if(key.equals("push_to_talk"))
            {
                desc = Resources.getString(
                    "plugin.keybindings.globalchooser.PUSH_TO_TALK");
            }
            else
                continue;

            gke = new GlobalShortcutEntry(desc, kss);

            tableModel.addEntry(gke);
        }
        refresh();
    }

    /**
     * Save configuration.
     */
    public void saveConfig()
    {
        KeybindingsService keybindingService =
            KeybindingChooserActivator.getKeybindingsService();
        GlobalShortcutService globalShortcutService =
            KeybindingChooserActivator.getGlobalShortcutService();
        GlobalKeybindingSet globalBindingSet =
            keybindingService.getGlobalBindings();
        Map<String, List<AWTKeyStroke>> gBindings =
            globalBindingSet.getBindings();
        List<GlobalShortcutEntry> entries = tableModel.getEntries();
        List<AWTKeyStroke> kss = null;

        for(GlobalShortcutEntry entry : entries)
        {
            String desc = null;

            if(entry.getAction().equals(Resources.getString(
                "plugin.keybindings.globalchooser.ANSWER_CALL")))
            {
                desc = "answer";
            }
            else if(entry.getAction().equals(Resources.getString(
                "plugin.keybindings.globalchooser.HANGUP_CALL")))
            {
                desc = "hangup";
            }
            else if(entry.getAction().equals(Resources.getString(
                "plugin.keybindings.globalchooser.ANSWER_HANGUP_CALL")))
            {
                desc = "answer_hangup";
            }
            else if(entry.getAction().equals(Resources.getString(
                "plugin.keybindings.globalchooser.SHOW_CONTACTLIST")))
            {
                desc = "contactlist";
            }
            else if(entry.getAction().equals(Resources.getString(
                "plugin.keybindings.globalchooser.MUTE_CALLS")))
            {
                desc = "mute";
            }
            else if(entry.getAction().equals(Resources.getString(
                "plugin.keybindings.globalchooser.PUSH_TO_TALK")))
            {
                desc = "push_to_talk";
            }
            else
                continue;

            kss = gBindings.get(desc);
            kss.clear();
            kss.add(entry.getShortcut());
            kss.add(entry.getShortcut2());
            gBindings.put(desc, kss);
        }

        // save in configuration and reload the global shortcuts
        keybindingService.saveGlobalShortcutFromConfiguration();
        globalShortcutService.reloadGlobalShortcuts();
    }

    /**
     * Required by ListSelectionListener.
     *
     * @param e event triggered
     */
    public void valueChanged(ListSelectionEvent e)
    {
    }

    /**
     * refreshes the table display
     */
    private void refresh()
    {
        tableModel.fireTableStructureChanged();
    }

    /**
     * Indicates if this is an advanced configuration form.
     * @return <tt>true</tt> if this is an advanced configuration form,
     * otherwise it returns <tt>false</tt>
     */
    public boolean isAdvanced()
    {
        return true;
    }
}

