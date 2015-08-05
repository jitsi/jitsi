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

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.keybindings.*;

/**
 * Implementation of the BindingPanel that provides configuring functionality
 * for the keystroke component of key bindings. Methods provide a means of
 * producing predefined, sweeping changes in the display. This defaults to a
 * light blue color scheme with an index indent style.<br>
 * Though display elements are still accessible, manual changes are not
 * particularly recommended unless automated changes to the appearance (the
 * indentation style and color scheme) are disabled since they may be
 * unexpectedly reverted or clash any alterations made.
 *
 * @author Damian Johnson (atagar1@gmail.com)
 * @version September 1, 2007
 */
public class BindingChooser
    extends BindingPanel
{
    private static final long serialVersionUID = 0;

    private IndentStyle indentStyle = IndentStyle.INDEX;

    private boolean isShortcutEditable = true; // Determines if shortcut fields

    // can be selected
    private BindingEntry selectedEntry = null; // None selected when null

    private String selectedText = "Press shortcut...";

    /**
     * Keybinding set.
     */
    private KeybindingSet set = null;

    /**
     * Displays a dialog allowing the user to redefine the keystroke component
     * of key bindings. The top has light blue labels describing the fields and
     * the bottom provides an 'OK' and 'Cancel' option. This uses the default
     * color scheme and indent style. If no entries are selected then the enter
     * key is equivalent to pressing 'OK' and escape is the same as 'Cancel'.
     *
     * @param parent frame to which to apply modal property and center within
     *            (centers within screen if null)
     * @param bindings initial mapping of keystrokes to their actions
     * @return redefined mapping of keystrokes to their actions, null if cancel
     *         is pressed
     */
    public static LinkedHashMap<KeyStroke, String> showDialog(Component parent,
        Map<KeyStroke, String> bindings)
    {
        BindingChooser display = new BindingChooser();
        display.putAllBindings(bindings);
        return showDialog(parent, display, "Key Bindings", true, display
            .makeAdaptor());
    }

    /**
     * Adds a collection of new key binding mappings to the end of the listing.
     * If any shortcuts are already contained then the previous entries are
     * replaced (not triggering the onUpdate method). Disabled shortcuts trigger
     * replacement on duplicate actions instead.
     *
     * @param set mapping between keystrokes and actions to be added
     */
    public void putAllBindings(KeybindingSet set)
    {
        this.set = set;
        putAllBindings(set.getBindings());
    }

    /**
     * Displays a dialog allowing the user to redefine the keystroke component
     * of key bindings. The bottom provides an 'OK' and 'Cancel' option. If no
     * entries are selected then the enter key is equivalent to pressing 'OK'
     * and escape is the same as 'Cancel'. Label and button backgrounds try to
     * match color scheme if set.<br>
     * Including focusable elements in the display will prevent user input from
     * setting the selected shortcut field. Also note that labels use the
     * default entry size and should be omitted if using content with custom
     * dimensions.
     *
     * @param parent frame to which to apply modal property and center within
     *            (centers within screen if null)
     * @param display body of the display, containing current bindings and
     *            appearance properties
     * @param dialogTitle title of the displayed dialog
     * @param showLabels if true the top has labels describing the fields,
     *            otherwise they are omitted
     * @param adaptor adaptor used to provide configuring functionality
     * @return redefined mapping of keystrokes to their actions, null if cancel
     *         is pressed
     */
    public static LinkedHashMap<KeyStroke, String> showDialog(Component parent,
        final BindingChooser display, String dialogTitle, boolean showLabels,
        BindingAdaptor adaptor)
    {
        final JDialog dialog = new JDialog();
        dialog.setTitle(dialogTitle);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setResizable(false);

        if (showLabels)
            dialog.add(display.makeLabels(), BorderLayout.NORTH);
        dialog.add(display);

        // Bottom controls
        JPanel controlSection = new TransparentPanel(new GridLayout(1, 0));

        // HACK: Uses button's name as a mutable value to determine if pressed
        final String PRESSED_STATE = "pressed";
        final JButton okButton = new JButton("OK");
        okButton.setName("not " + PRESSED_STATE);
        okButton.setPreferredSize(new Dimension(
            okButton.getPreferredSize().width, 25));
        okButton.setFocusable(false);
        okButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                okButton.setName(PRESSED_STATE);
                dialog.dispose();
            }
        });
        controlSection.add(okButton);

        final JButton cancelButton = new JButton("Cancel");
        cancelButton.setPreferredSize(new Dimension(cancelButton
            .getPreferredSize().width, 25));
        cancelButton.setFocusable(false);
        cancelButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                dialog.dispose();
            }
        });
        controlSection.add(cancelButton);
        dialog.add(controlSection, BorderLayout.SOUTH);

        // Adds listener that closes dialog when pressing 'enter' or 'escape'
        dialog.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent event)
            {
                if (display.selectedEntry == null)
                {
                    if (event.getKeyCode() == KeyEvent.VK_ENTER)
                    {
                        okButton.doClick();
                    }
                    else if (event.getKeyCode() == KeyEvent.VK_ESCAPE)
                    {
                        cancelButton.doClick();
                    }
                }
            }
        });
        dialog.addKeyListener(adaptor); // Listener for shortcut field input
        dialog.pack();

        dialog.setVisible(true);
        if (okButton.getName().equals(PRESSED_STATE))
            return display.getBindingMap();
        else
            return null;
    }

    /**
     * This is called upon:
     * Component reordering (inherited functionality from BindingPanel)
     * Visual changes to the entry
     * Component validation
     */
    @Override
    protected void onUpdate(int index, BindingEntry entry, boolean isNew)
    {
        this.indentStyle.apply(entry, index);
    }

    /**
     * Invoked on click.
     */
    @Override
    protected void onClick(MouseEvent event, BindingEntry entry,
        BindingEntry.Field field)
    {
        // Selects shortcut fields when they're clicked
        if (field == BindingEntry.Field.SHORTCUT)
        {
            // Deselects if already selected
            if (entry.equals(this.selectedEntry))
                setSelected(null);
            else
                setSelected(entry);
        }
    }

    /**
     * Sets if the shortcut fields of entries can be selected to provide editing
     * functionality or not. If false, any selected entry is deselected.
     *
     * @param editable if true shortcut fields may be selected to have their
     *            values changed, otherwise user input and calls to the
     *            setSelected method are ignored
     */
    public void setEditable(boolean editable)
    {
        if (!editable && this.selectedEntry != null)
        {
            setSelected(null); // Deselects current selection
        }
        this.isShortcutEditable = editable;
    }

    /**
     * Provides the indent style used by the chooser.
     *
     * @return type of content in the indent field
     */
    public IndentStyle getIndentStyle()
    {
        return this.indentStyle;
    }

    /**
     * Sets content display in the indent field of entries. This will prompt an
     * onUpdate on all entries unless setting the style to NONE.
     *
     * @param style type of content displayed in entry's indent field
     */
    public void setIndentStyle(IndentStyle style)
    {
        this.indentStyle = style;

        if (style == IndentStyle.NONE)
            return;
        ArrayList<BindingEntry> bindings = getBindings();
        for (int i = 0; i < bindings.size(); ++i)
        {
            onUpdate(i, bindings.get(i), false);
        }
    }

    /**
     * Sets the message of the selected shortcut field when awaiting user input.
     * By default this is "Press shortcut...".
     *
     * @param message prompt for user input
     */
    public void setSelectedText(String message)
    {
        if (this.selectedEntry != null)
        {
            this.selectedEntry.getField(BindingEntry.Field.SHORTCUT).setText(
                message);
        }
        this.selectedText = message;
    }

    /**
     * Returns if a binding is currently awaiting input or not.
     *
     * @return true if a binding is awaiting input, false otherwise
     */
    public boolean isBindingSelected()
    {
        return this.selectedEntry != null;
    }

    /**
     * Provides the currently selected entry if awaiting input.
     *
     * @return entry currently awaiting input, if one exists
     */
    public BindingEntry getSelected()
    {
        return this.selectedEntry;
    }

    /**
     * Sets the shortcut field of an entry to prompt user input. The next call
     * to doInput sets set its shortcut field and deselects the entry. Any other
     * currently selected entry is deselected. If null, then this simply reverts
     * any selections (leaving no entry selected). The onUpdate method is called
     * whenever an entry is either selected or deselected.
     *
     * @param entry binding entry awaiting input for its shortcut field
     * @throws IllegalArgumentException if entry is not contained in chooser
     */
    public void setSelected(BindingEntry entry)
    {
        if (!this.isShortcutEditable)
            return; // Selection can't be changed
        if (entry != null && entry.equals(this.selectedEntry))
            return; // Entry is already selected
        if (entry != null && !getBindings().contains(entry))
        {
            throw new IllegalArgumentException(
                "BindingEntry not contained in display.");
        }

        BindingEntry previousSelection = this.selectedEntry;
        this.selectedEntry = entry;

        // Reverts previously selected field's attributes
        if (previousSelection != null)
        {
            onUpdate(getBindingIndex(previousSelection), previousSelection,
                false);
            previousSelection.setShortcut(previousSelection.getShortcut()); // Reverts
            // text
        }

        // Sets the new selection
        if (this.selectedEntry != null)
        {
            onUpdate(getBindingIndex(this.selectedEntry), this.selectedEntry,
                false);
            this.selectedEntry.getField(BindingEntry.Field.SHORTCUT).setText(
                " " + this.selectedText);
        }
    }

    /**
     * Provides a key adaptor that can provide editing functionality for the
     * selected entry.
     *
     * @return binding adaptor configured to this chooser
     */
    public BindingAdaptor makeAdaptor()
    {
        return new BindingAdaptor(this);
    }

    /**
     * Provides the labels naming the fields. These are based on the settings
     * when constructed and aren't updated when the display changes. Labels use
     * the default entry dimensions.
     *
     * @return labels used in dialog
     */
    public BindingEntry makeLabels()
    {
        BindingEntry labels = new BindingEntry(null, "");

        labels.setOpaque(false);

        for (BindingEntry.Field field : BindingEntry.Field.values())
        {
            JLabel fieldLabel = labels.getField(field);

            if (field == BindingEntry.Field.INDENT)
            {
                // Removes indent field if omitted from the rest of the display.
                fieldLabel.setVisible(indentStyle != IndentStyle.EMPTY);
            }
            else if (field == BindingEntry.Field.ACTION)
            {
                fieldLabel.setText(" Action:");
            }
            else if (field == BindingEntry.Field.SHORTCUT)
            {
                fieldLabel.setText(" Shortcut:");
            }
            else
            {
                // BindingEntry.Field has changed and this should be updated
                // accordingly
                assert false : BindingChooser.class.getName()
                    + " doesn't recognize the '" + field + "' field.";
            }
        }
        return labels;
    }

    /**
     * Emulates keyboard input, setting the selected entry's shortcut if an
     * entry's currently awaiting input.
     *
     * @param input keystroke input for selected entry
     */
    void doInput(KeyStroke input)
    {
        if (isBindingSelected())
        {
            this.selectedEntry.setShortcut(input);
            //apply configuration
            set.setBindings(this.getBindingMap());

            // TYPE indent can change according to the shortcut
            // this.indentStyle.apply(this.selectedEntry,
            // getBindingIndex(this.selectedEntry));
            setSelected(null); // Deselects shortcut field
        }
    }

    @Override
    public void validate()
    {
        super.validate();

        ArrayList<BindingEntry> bindings = getBindings();
        for (int i = 0; i < bindings.size(); ++i)
        {
            onUpdate(i, bindings.get(i), false);
        }
    }

    /**
     * Supported appearances of the indent field, which includes:<br>
     * NONE- No actions are taken to change the indent field's appearance.<br>
     * EMPTY- Indent field is set to be invisible (effectively removing it from
     * the display).<br>
     * SPACER- Blank field that occupies its currently set dimensions.<br>
     * TYPE- Displays Unicode arrows according to the shortcut's event type
     * (down for KEY_PRESSED, up for KEY_RELEASED, bidirectional for KEY_TYPED,
     * and an 'X' if disabled).<br>
     * INDEX- Displays the field's index from the top (starting with one).
     */
    public static enum IndentStyle
    {
        NONE, EMPTY, SPACER, TYPE, INDEX;

        /**
         * Returns the enum representation of a string. This is case sensitive.
         *
         * @param str toString representation of this enum
         * @return enum associated with a string
         * @throws IllegalArgumentException if argument is not represented by
         *             this enum.
         */
        public static IndentStyle fromString(String str)
        {
            for (IndentStyle type : IndentStyle.values())
            {
                if (str.equals(type.toString()))
                    return type;
            }
            throw new IllegalArgumentException();
        }

        // Applies this style to the indent field of an entry. This uses a zero
        // based index.
        private void apply(BindingEntry entry, int index)
        {
            if (this == NONE)
                return;
            JLabel indentField = entry.getField(BindingEntry.Field.INDENT);
            indentField.setVisible(this != EMPTY);
            indentField.setIcon(null);

            String fieldText = "";
            if (this == TYPE)
            {
                if (entry.getShortcut() == BindingEntry.DISABLED)
                {
                    fieldText = "  X";
                }
                else
                {
                    int type = entry.getShortcut().getKeyEventType();
                    if (type == KeyEvent.KEY_PRESSED)
                        fieldText = "  \u2193";
                    else if (type == KeyEvent.KEY_RELEASED)
                        fieldText = "  \u2191";
                    else if (type == KeyEvent.KEY_TYPED)
                        fieldText = "  \u2195";
                    else
                    {
                        // Should be unreachable according to the AWTKeyStroke
                        // class
                        assert false : "Unrecognized key type: " + type;
                        fieldText = "";
                    }
                }
            }
            else if (this == INDEX)
            {
                fieldText = " " + (index + 1) + ".";
            }
            indentField.setText(fieldText);
        }

        @Override
        public String toString()
        {
            if (this == TYPE)
                return "Event Type";
            return getReadableConstant(this.name());
        }
    }

    /**
     * Provides a more readable version of constant names. Spaces replace
     * underscores and this changes the input to lowercase except the first
     * letter of each word. For instance, "RARE_CARDS" would become
     * "Rare Cards".
     *
     * @param input string to be converted
     * @return reader friendly variant of constant name
     */
    public static String getReadableConstant(String input)
    {
        char[] name = input.toCharArray();

        boolean isStartOfWord = true;
        for (int i = 0; i < name.length; ++i)
        {
            char chr = name[i];
            if (chr == '_')
                name[i] = ' ';
            else if (isStartOfWord)
                name[i] = Character.toUpperCase(chr);
            else
                name[i] = Character.toLowerCase(chr);
            isStartOfWord = chr == '_';
        }

        return new String(name);
    }
}
