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
package net.java.sip.communicator.plugin.keybindingchooser;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.keybindingchooser.chooser.*;
import net.java.sip.communicator.plugin.keybindingchooser.globalchooser.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.keybindings.*;

import org.osgi.framework.*;

/**
 * The <tt>ConfigurationForm</tt> that would be added to the settings
 * configuration to configure the application keybindings.
 *
 * @author Damian Johnson
 * @author Lubomir Marinov
 */
public class KeybindingsConfigPanel
    extends TransparentPanel
{
    private static KeybindingsService getKeybindingsService()
    {
        BundleContext bundleContext =
            KeybindingChooserActivator.getBundleContext();
        ServiceReference keybindingRef =
            bundleContext.getServiceReference(KeybindingsService.class
                .getName());

        return (KeybindingsService) bundleContext.getService(keybindingRef);
    }

    private static final long serialVersionUID = 0;

    private final HashMap<KeybindingSet, SIPChooser> choosers =
        new HashMap<KeybindingSet, SIPChooser>();

    /**
     * Constructor.
     */
    public KeybindingsConfigPanel()
    {
        super(new BorderLayout());

        KeybindingsService service = getKeybindingsService();

        setFocusable(true);

        JTabbedPane chooserPanes = new SIPCommTabbedPane();

        // deselects entries awaiting input when focus is lost
        this.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusLost(FocusEvent event)
            {
                for (SIPChooser chooser : choosers.values())
                {
                    chooser.setSelected(null);
                }
            }
        });

        // global shortcut
        GlobalShortcutConfigForm globalBindingPanel =
            new GlobalShortcutConfigForm();
        chooserPanes.addTab(KeybindingChooserActivator.getResources()
            .getI18NString("plugin.keybindings.GLOBAL"), globalBindingPanel);

        for (KeybindingSet.Category category : KeybindingSet.Category.values())
        {
            KeybindingSet bindingSet = service.getBindings(category);
            if (bindingSet == null)
                continue; // defaults failed to load

            SIPChooser newChooser = new SIPChooser();
            newChooser.putAllBindings(bindingSet);

            JPanel chooserWrapper = new TransparentPanel(new BorderLayout());
            chooserWrapper.add(newChooser, BorderLayout.NORTH);
            JScrollPane scroller = new JScrollPane(chooserWrapper);

            // adds listener that receives events to set bindings
            this.addKeyListener(newChooser.makeAdaptor());

            chooserPanes.addTab(KeybindingChooserActivator.getResources()
                .getI18NString("plugin.keybindings." + category.toString()),
                scroller);
            this.choosers.put(bindingSet, newChooser);
        }



        add(chooserPanes);
    }

    /**
     * Keybinding chooser with customized appearance and functionality for the
     * SIP Communicator.
     */
    private class SIPChooser
        extends BindingChooser
    {
        private static final long serialVersionUID = 0;

        // Provides mapping of UI labels to internal action names
        private HashMap<String, String> actionLabels =
            new HashMap<String, String>();

        // Calls focus to the form so keyboard events are received
        @Override
        protected void onClick(MouseEvent event, BindingEntry entry,
            BindingEntry.Field field)
        {
            super.onClick(event, entry, field);
            KeybindingsConfigPanel.this.requestFocus();
        }

        @Override
        public boolean putBinding(BindingEntry newEntry, int index)
        {
            // Converts to I18N strings for UI
            String actionInternal = newEntry.getAction();
            String actionLabel = getI18NString(actionInternal);
            this.actionLabels.put(actionLabel, actionInternal);
            newEntry.setAction(actionLabel);

            // Overwrites the default entry layout to stretch shortcut field
            newEntry.removeAll();
            newEntry.setLayout(new BorderLayout());

            JPanel left =
                new TransparentPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            left.add(newEntry.getField(BindingEntry.Field.INDENT));
            left.add(newEntry.getField(BindingEntry.Field.ACTION));
            newEntry.add(left, BorderLayout.WEST);
            newEntry.add(newEntry.getField(BindingEntry.Field.SHORTCUT));

            return super.putBinding(newEntry, index);
        }

        /**
         * Gets the internationalized string corresponding to a specific key
         * given in its plugin-specific format. The key is translated to the
         * global format of the ReouseceManagementService and the translated key
         * is used to retrieve the string from the resource files.
         *
         * @param key the key of the string to be retrieved given in its
         *            plugin-specific format
         * @return the internationalized string corresponding to a specific key
         *         given in its plugin-specific format
         */
        private String getI18NString(String key)
        {
            StringBuilder newKey = new StringBuilder();

            newKey.append("plugin.keybindings.");
            for (char keyChar : key.toCharArray())
            {
                if (Character.isLowerCase(keyChar))
                    newKey.append(Character.toUpperCase(keyChar));
                else if (Character.isUpperCase(keyChar))
                {
                    newKey.append('_');
                    newKey.append(keyChar);
                }
                else
                    newKey.append('_');
            }

            return
                KeybindingChooserActivator.getResources().getI18NString(
                    newKey.toString());
        }

        @Override
        public LinkedHashMap<KeyStroke, String> getBindingMap()
        {
            // Translates I18N strings back to internal action labels
            LinkedHashMap<KeyStroke, String> bindings =
                new LinkedHashMap<KeyStroke, String>();
            for (BindingEntry entry : super.getBindings())
            {
                bindings.put(entry.getShortcut(), this.actionLabels.get(entry
                    .getAction()));
            }

            return bindings;
        }
    }
}
