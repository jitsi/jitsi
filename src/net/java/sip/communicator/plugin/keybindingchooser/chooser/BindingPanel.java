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
package net.java.sip.communicator.plugin.keybindingchooser.chooser;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * Panel containing a listing of current keybinding mappings. This contains
 * methods that can be overwritten to provide easy editing functionality and
 * display logic. Note that this does not support the manual addition or removal
 * of BindingEntry components. However this is designed to tolerate the changing
 * of entry visibility (including individual fields) and the manual addition and
 * removal of extra components either to this panel or its BindingEntries.<br>
 * This represents a mapping of keystrokes to strings, and hence duplicate
 * shortcuts aren't supported. An exception is made in the case of disabled
 * shortcuts, but to keep mappings unique duplicate actions among disabled
 * entries are not permitted.
 *
 * @author Damian Johnson (atagar1@gmail.com)
 * @version September 1, 2007
 */
public abstract class BindingPanel
    extends TransparentPanel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private ArrayList<BindingEntry> contents = new ArrayList<BindingEntry>();

    /**
     * Method called whenever an entry is either added or shifts in the display.
     * For instance, if the second entry is removed then this is called on the
     * third to last elements.
     *
     * @param index newly assigned index of entry
     * @param entry entry that has been added or shifted
     * @param isNew if true the entry is new to the display, false otherwise
     */
    protected abstract void onUpdate(int index, BindingEntry entry,
        boolean isNew);

    /**
     * Method called upon any mouse clicks within a BindingEntry in the display.
     *
     * @param event fired mouse event that triggered method call
     * @param entry entry on which the click landed
     * @param field field of entry on which the click landed, null if not a
     *            recognized field
     */
    protected abstract void onClick(MouseEvent event, BindingEntry entry,
        BindingEntry.Field field);

    /**
     * Constructor.
     */
    public BindingPanel()
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        addMouseListener(new MouseTracker());
    }

    /**
     * Adds a new key binding mapping to the end of the listing. If this already
     * contains the shortcut then the previous entry is replaced instead (not
     * triggering the onUpdate method). Disabled shortcuts trigger replacement
     * on duplicate actions instead. This uses the normal parameters used to
     * generate key stokes, such as:
     *
     * <pre>
     * bindingPanel.putBinding('Y', 0, &quot;Confirm Selection&quot;);
     * bindingPanel.putBinding(KeyEvent.VK_DELETE, KeyEvent.CTRL_MASK
     *     | KeyEvent.ALT_MASK, &quot;Kill Process&quot;);
     * </pre>
     *
     * @param keyCode key code of keystroke component of mapping
     * @param modifier modifiers of keystroke component of mapping
     * @param action string component of mapping
     * @return true if contents did not already include shortcut
     */
    public boolean putBinding(int keyCode, int modifier, String action)
    {
        return putBinding(KeyStroke.getKeyStroke(keyCode, modifier), action);
    }

    /**
     * Adds a new key binding mapping to the end of the listing. If this already
     * contains the shortcut then the previous entry is replaced instead (not
     * triggering the onUpdate method). Disabled shortcuts trigger replacement
     * on duplicate actions instead.
     *
     * @param shortcut keystroke component of mapping
     * @param action string component of mapping
     * @return true if contents did not already include shortcut
     */
    public boolean putBinding(KeyStroke shortcut, String action)
    {
        return putBinding(shortcut, action, getComponentCount());
    }

    /**
     * Adds a new key binding mapping to a particular index of the listing. If
     * this already contains the shortcut then the previous entry is replaced
     * instead (not triggering the onUpdate method). Disabled shortcuts trigger
     * replacement on duplicate actions instead.
     *
     * @param shortcut keystroke component of mapping
     * @param action string component of mapping
     * @param index location in which to insert mapping
     * @return true if contents did not already include shortcut
     * @throws IndexOutOfBoundsException if index is out of range (index < 0 ||
     *             index > getBindingCount()).
     */
    public boolean putBinding(KeyStroke shortcut, String action, int index)
    {
        return putBinding(new BindingEntry(shortcut, action), index);
    }

    /**
     * Adds a new key binding mapping to a particular index of the listing. If
     * this already contains the shortcut then the previous entry is replaced
     * instead (not triggering the onUpdate method). Disabled shortcuts trigger
     * replacement on duplicate actions instead.
     *
     * @param newEntry entry to add to contents
     * @param index location in which to insert mapping
     * @return true if contents did not already include shortcut
     * @throws IndexOutOfBoundsException if index is out of range (index < 0 ||
     *             index > getBindingCount()).
     */
    public boolean putBinding(BindingEntry newEntry, int index)
    {
        if (index < 0 || index > getBindingCount())
        {
            String message = "Attempting to add to invalid index: " + index;
            throw new IndexOutOfBoundsException(message);
        }

        KeyStroke shortcut = newEntry.getShortcut();
        if (shortcut != BindingEntry.DISABLED)
        {
            // Checks for duplicate shortcut
            for (BindingEntry entry : this.contents)
            {
                if (shortcut.equals(entry.getShortcut()))
                {
                    entry.setAction(newEntry.getAction());
                    return false;
                }
            }
        }
        else
        {
            // Checks if this entry would be a duplicate
            if (this.contents.contains(newEntry))
                return false;
        }

        this.contents.add(index, newEntry);

        // Inserts into display, maintaining ordering of collection
        if (index > 0)
        {
            /*
             * Places the new entry after previously listed one, transversing
             * backward to support common case of adding to the end. This
             * depends on bindings being unique.
             */
            BindingEntry previous = getBinding(index - 1);
            for (int i = getComponentCount() - 1; i >= 0; --i)
            {
                if (getComponent(i).equals(previous))
                {
                    add(newEntry, i + 1);
                    break;
                }
                assert i != 0 : "Listing doesn't contain expected previous entry "
                    + "when adding to index " + index;
            }
        }
        else
        {
            add(newEntry, 0); // Adds to start
        }

        // Calls update on add entry and any shifted contents
        onUpdate(index, newEntry, true);
        for (int i = index + 1; i < getBindingCount(); ++i)
        {
            BindingEntry shifted = getBinding(i);
            onUpdate(i, shifted, false);
        }
        return true;
    }

    /**
     * Adds a collection of new key binding mappings to the end of the listing.
     * If any shortcuts are already contained then the previous entries are
     * replaced (not triggering the onUpdate method). Disabled shortcuts trigger
     * replacement on duplicate actions instead.
     *
     * @param bindings mapping between keystrokes and actions to be added
     */
    public void putAllBindings(Map<KeyStroke, String> bindings)
    {
        for (KeyStroke action : bindings.keySet())
        {
            putBinding(action, bindings.get(action));
        }
    }

    /**
     * Removes a particular binding from the contents.
     *
     * @param entry binding to be removed
     * @return true if binding was in the contents, false otherwise
     */
    public boolean removeBinding(BindingEntry entry)
    {
        int index = getBindingIndex(entry);
        if (index != -1)
            return removeBinding(index) != null;
        else
            return false;
    }

    /**
     * Removes the binding at a particular index of the listing.
     *
     * @param index from which to remove entry
     * @return the entry that was removed from the contents
     * @throws IndexOutOfBoundsException if index is out of range (index < 0 ||
     *             index > getBindingCount()).
     */
    public BindingEntry removeBinding(int index)
    {
        if (index < 0 || index > getBindingCount())
        {
            String message =
                "Attempting to remove from invalid index: " + index;
            throw new IndexOutOfBoundsException(message);
        }

        BindingEntry entry = this.contents.remove(index);
        remove(entry); // Removes from display

        // Calls update on shifted entries
        for (int i = index; i < getBindingCount(); ++i)
        {
            BindingEntry shifted = getBinding(i);
            onUpdate(i, shifted, false);
        }

        return entry;
    }

    /**
     * Removes all bindings from the panel.
     */
    public void clearBindings()
    {
        while (getBindingCount() > 0)
        {
            removeBinding(0);
        }
    }

    /**
     * Returns if a keystroke is in the panel's current contents. This provides
     * a preemptive means of checking if adding a non-disabled shortcut would
     * cause a replacement.
     *
     * @param shortcut keystroke to be checked against contents
     * @return true if contents includes the shortcut, false otherwise
     */
    public boolean contains(KeyStroke shortcut)
    {
        for (BindingEntry entry : this.contents)
        {
            if (shortcut == BindingEntry.DISABLED)
            {
                if (entry.isDisabled())
                    return true;
            }
            else
            {
                if (shortcut.equals(entry.getShortcut()))
                    return true;
            }
        }
        return false;
    }

    /**
     * Provides number of key bindings currently present.
     *
     * @return number of key bindings in the display
     */
    public int getBindingCount()
    {
        return this.contents.size();
    }

    /**
     * Provides the index of a particular entry.
     *
     * @param entry entry for which the index should be returned
     * @return entry index, -1 if not found
     */
    public int getBindingIndex(BindingEntry entry)
    {
        return this.contents.indexOf(entry);
    }

    /**
     * Provides a binding at a particular index.
     *
     * @param index index from which to retrieve binding.
     * @return the entry at the specified position in this list
     */
    public BindingEntry getBinding(int index)
    {
        return this.contents.get(index);
    }

    /**
     * Provides listing of the current keybinding entries.
     *
     * @return list of current entry contents
     */
    public ArrayList<BindingEntry> getBindings()
    {
        return new ArrayList<BindingEntry>(this.contents);
    }

    /**
     * Provides the mapping between keystrokes and actions represented by the
     * contents of the display. Disabled entries aren't included in the mapping.
     *
     * @return mapping between contained keystrokes and their associated actions
     */
    public LinkedHashMap<KeyStroke, String> getBindingMap()
    {
        LinkedHashMap<KeyStroke, String> mapping =
            new LinkedHashMap<KeyStroke, String>();
        for (BindingEntry entry : this.contents)
        {
            if (entry.isDisabled())
                continue;
            else
                mapping.put(entry.getShortcut(), entry.getAction());
        }
        return mapping;
    }

    /**
     * Provides an input map associating keystrokes to actions according to the
     * contents of the display. Disabled entries aren't included in the mapping.
     *
     * @return input mapping between contained keystrokes and their associated
     *         actions
     */
    public InputMap getBindingInputMap()
    {
        InputMap mapping = new InputMap();
        LinkedHashMap<KeyStroke, String> bindingMap = getBindingMap();
        for (KeyStroke keystroke : bindingMap.keySet())
        {
            mapping.put(keystroke, bindingMap.get(keystroke));
        }
        return mapping;
    }

    // Mouse listener for clicks within display
    private class MouseTracker
        extends MouseInputAdapter
    {
        @Override
        public void mousePressed(MouseEvent event)
        {
            Point loc = event.getPoint();
            Component comp = getComponentAt(loc);

            if (comp instanceof BindingEntry)
            {
                BindingEntry entry = (BindingEntry) comp;

                // Gets label within entry
                int x = loc.x - entry.getLocation().x;
                int y = loc.y - entry.getLocation().y;
                Component label = entry.findComponentAt(x, y);

                if (entry.getField(BindingEntry.Field.INDENT).equals(label))
                {
                    onClick(event, entry, BindingEntry.Field.INDENT);
                }
                else if (entry.getField(BindingEntry.Field.ACTION)
                    .equals(label))
                {
                    onClick(event, entry, BindingEntry.Field.ACTION);
                }
                else if (entry.getField(BindingEntry.Field.SHORTCUT).equals(
                    label))
                {
                    onClick(event, entry, BindingEntry.Field.SHORTCUT);
                }
                else
                {
                    onClick(event, entry, null); // Click fell on unrecognized
                                                 // component
                }
            }
        }
    }
}
