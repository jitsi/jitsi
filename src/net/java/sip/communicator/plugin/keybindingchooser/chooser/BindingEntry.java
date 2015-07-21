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

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * Display element for a single key binding.
 *
 * @author Damian Johnson (atagar1@gmail.com)
 * @version August 7, 2007
 */
public class BindingEntry
    extends TransparentPanel
{
    /**
     * Keystroke representing an unmapped entry.
     */
    public static final KeyStroke DISABLED = null;

    // Display dimensions
    private static final int LINE_HEIGHT = 25;

    private static final int INDENT_WIDTH = 25;

    private static final int ACTION_WIDTH = 150;

    private static final int SHORTCUT_WIDTH = 150;

    private static final long serialVersionUID = 0;

    private JLabel indentField = new JLabel();

    private JLabel actionField = new JLabel();

    private JLabel shortcutField = new JLabel();

    private KeyStroke shortcut;

    {
        this.indentField.setPreferredSize(new Dimension(INDENT_WIDTH,
            LINE_HEIGHT));
        this.indentField.setForeground(Color.BLACK);

        this.actionField.setPreferredSize(new Dimension(ACTION_WIDTH,
            LINE_HEIGHT));
        this.actionField.setForeground(Color.BLACK);

        this.shortcutField.setPreferredSize(new Dimension(SHORTCUT_WIDTH,
            LINE_HEIGHT));
        this.shortcutField.setForeground(Color.BLACK);
    }

    public BindingEntry(KeyStroke shortcut, String action)
    {
        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setAction(action);
        setShortcut(shortcut);

        add(this.indentField);
        add(this.actionField);
        add(this.shortcutField);
    }

    public KeyStroke getShortcut()
    {
        return this.shortcut;
    }

    public void setShortcut(KeyStroke shortcut)
    {
        this.shortcut = shortcut;

        // Sets shortcut label to representation of the keystroke
        if (this.shortcut == DISABLED)
        {
            this.shortcutField.setText(" Disabled");
        }
        else
        {
            StringBuffer buffer = new StringBuffer();

            if (this.shortcut.getKeyEventType() == KeyEvent.KEY_TYPED)
            {
                buffer.append(this.shortcut.getKeyChar());
            }
            else
            {
                int keycode = this.shortcut.getKeyCode();
                int modifiers = this.shortcut.getModifiers();

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

            this.shortcutField.setText(" " + buffer.toString());
        }
    }

    public String getAction()
    {
        return this.actionField.getText().substring(1);
    }

    public void setAction(String action)
    {
        this.actionField.setText(" " + action);
    }

    public boolean isDisabled()
    {
        return this.shortcut == DISABLED;
    }

    /**
     * Provides the label associated with a field.
     *
     * @param field element of display to be returned
     * @return label associated with field
     */
    public JLabel getField(Field field)
    {
        if (field == Field.INDENT)
            return this.indentField;
        else if (field == Field.ACTION)
            return this.actionField;
        else if (field == Field.SHORTCUT)
            return this.shortcutField;
        else
        {
            assert false : "Unrecognized field: " + field;
            return null;
        }
    }

    /**
     * Elements of the display (ordered left to right):<br>
     * Indent- Leading index label or icon.<br>
     * Action- String component of mapping.<br>
     * Shortcut- Keystroke component of mapping.
     */
    public enum Field
    {
        INDENT, ACTION, SHORTCUT;
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
        builder.append("BindingEntry (");

        if (isDisabled())
            builder.append("Disabled");
        else
            builder.append(getShortcut());
        builder.append(" \u2192 "); // Adds Unicode arrow pointing right
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
        else if (!(obj instanceof BindingEntry))
            return false;

        BindingEntry entry = (BindingEntry) obj;
        boolean equals = true;

        String action = this.getAction();
        if (action == null)
            equals &= entry.getAction() == null;
        else
            equals &= action.equals(entry.getAction());

        KeyStroke shortcut = this.getShortcut();
        if (shortcut == null)
            equals &= entry.getShortcut() == null;
        else
            equals &= shortcut.equals(entry.getShortcut());

        return equals;
    }

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
