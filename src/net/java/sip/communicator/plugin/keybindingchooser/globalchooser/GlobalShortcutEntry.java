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
import java.util.List;

import net.java.sip.communicator.service.globalshortcut.*;
//disambiguation

/**
 * Entry for a global shortcut.
 *
 * @author Sebastien Vincent
 */
public class GlobalShortcutEntry
{
    /**
     * Disabled keystroke.
     */
    private static final AWTKeyStroke DISABLED = null;

    /**
     * Action name.
     */
    private String action = null;

    /**
     * Primary shortcut.
     */
    private AWTKeyStroke shortcut = null;

    /**
     * Second shortcut.
     */
    private AWTKeyStroke shortcut2 = null;

    /**
     * If it is editable, display "key press".
     */
    private boolean editableShortcut1 = false;

    /**
     * If it is editable, display "key press".
     */
    private boolean editableShortcut2 = false;

    /**
     * Constructor.
     *
     * @param action action
     * @param shortcuts list of shortcut for this action
     */
    public GlobalShortcutEntry(String action, List<AWTKeyStroke> shortcuts)
    {
        setAction(action);
        setShortcuts(shortcuts);
    }

    /**
     * Returns primary shortcut if it exists.
     *
     * @return primary shortcut if it exists.
     */
    public AWTKeyStroke getShortcut()
    {
        return this.shortcut;
    }

    /**
     * Returns second shortcut if it exists.
     *
     * @return second shortcut if it exists.
     */
    public AWTKeyStroke getShortcut2()
    {
        return this.shortcut2;
    }

    /**
     * Set the shortcut keystroke and field.
     *
     * @param shortcut <tt>AWTKeyStroke</tt>
     * @return string representation of the keystroke
     */
    public static String getShortcutText(AWTKeyStroke shortcut)
    {
        if (shortcut == DISABLED)
        {
            return "Disabled";
        }
        else
        {
            StringBuffer buffer = new StringBuffer();

            if (shortcut.getKeyEventType() == KeyEvent.KEY_TYPED)
            {
                buffer.append(shortcut.getKeyChar());
            }
            else
            {
                int keycode = shortcut.getKeyCode();
                int modifiers = shortcut.getModifiers();

                if(modifiers == GlobalShortcutService.SPECIAL_KEY_MODIFIERS)
                {
                    return "Special";
                }

                // Indicates modifiers of the keystroke
                boolean shiftMask = (modifiers & InputEvent.SHIFT_MASK) != 0;
                boolean ctrlMask = (modifiers & InputEvent.CTRL_MASK) != 0;
                boolean metaMask = (modifiers & InputEvent.META_MASK) != 0;
                boolean altMask = (modifiers & InputEvent.ALT_MASK) != 0;
                if (shiftMask && keycode != KeyEvent.VK_SHIFT)
                    buffer.append("Shift + ");
                if (ctrlMask && keycode != KeyEvent.VK_CONTROL)
                    buffer.append("Ctrl + ");
                if (metaMask && keycode != KeyEvent.VK_META)
                    buffer.append("Meta + ");
                if (altMask && keycode != KeyEvent.VK_ALT)
                    buffer.append("Alt + ");

                buffer.append(KeyEvent.getKeyText(keycode));
            }
            return buffer.toString();
        }
    }

    /**
     * Set the shortcuts for this action.
     *
     * @param shortcuts list of shortcuts
     */
    public void setShortcuts(List<AWTKeyStroke> shortcuts)
    {
        if(shortcuts.size() > 0)
        {
            this.shortcut = shortcuts.get(0);
        }
        if(shortcuts.size() > 1)
        {
            this.shortcut2 = shortcuts.get(1);
        }
    }

    /**
     * Returns action string.
     *
     * @return action
     */
    public String getAction()
    {
        return action;
    }

    /**
     * Set action string
     *
     * @param action action
     */
    public void setAction(String action)
    {
        this.action = action;
    }

    /**
     * If this global keybindings is disabled.
     *
     * @return true if this global keybinding is disabled
     */
    public boolean isDisabled()
    {
        return this.shortcut == DISABLED && this.shortcut2 == DISABLED;
    }

    /**
     * Provides the string representation of this mapping. The exact details of
     * the representation are unspecified and subject to change but the
     * following format can be considered to be typical:<br>
     * "BindingEntry (" + Shortcut + " \u2192 " + Action + ")"
     *
     * @return string representation of entry
     */
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("GlobalBindingEntry (");

        if (isDisabled())
            builder.append("Disabled");
        else
            builder.append(getShortcut());
        builder.append(" \u2192 "); // arrow pointing right
        builder.append(getAction());
        builder.append(")");
        return builder.toString();
    }

    /**
     * Checks if argument is an instance of this class with the same shortcut
     * and associated action. It does not compare aspects of the display
     * elements.
     *
     * @param obj element with which to be compared
     * @return true if argument is an instance of this class with matching
     *         shortcut and action, false otherwise
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
            return true;
        else if (!(obj instanceof GlobalShortcutEntry))
            return false;

        GlobalShortcutEntry entry = (GlobalShortcutEntry) obj;
        boolean equals = true;

        String action = this.getAction();
        if (action == null)
            equals &= entry.getAction() == null;
        else
            equals &= action.equals(entry.getAction());

        AWTKeyStroke shortcut = this.getShortcut();
        if (shortcut == null)
            equals &= entry.getShortcut() == null;
        else
            equals &= shortcut.equals(entry.getShortcut());

        shortcut = this.getShortcut2();
        if (shortcut == null)
            equals &= entry.getShortcut() == null;
        else
            equals &= shortcut.equals(entry.getShortcut2());

        return equals;
    }

    /**
     * Set editable for primary shortcut.
     *
     * @param value value to set
     */
    public void setEditShortcut1(boolean value)
    {
        editableShortcut1 = value;
    }

    /**
     * Set editable for secondary shortcut.
     *
     * @param value value to set
     */
    public void setEditShortcut2(boolean value)
    {
        editableShortcut2 = value;
    }

    /**
     * Get editable for primary shortcut.
     *
     * @return value value to set
     */
    public boolean getEditShortcut1()
    {
        return editableShortcut1;
    }

    /**
     * Get editable for secondary shortcut.
     *
     * @return value
     */
    public boolean getEditShortcut2()
    {
        return editableShortcut2;
    }

    /**
     * Returns hashcode for this instance.
     *
     * @return hashcode for this instance
     */
    @Override
    public int hashCode()
    {
        int hash = 17;
        hash = 37 * hash + (getAction() == null ? 0 : getAction().hashCode());
        hash =
            37 * hash + (getShortcut() == null ? 0 : getShortcut().hashCode());
        return hash;
    }
}
