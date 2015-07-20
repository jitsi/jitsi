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

import java.awt.event.*;

import javax.swing.*;

/**
 * Adaptor that uses keyboard input to set the selected shortcut field of a
 * BindingChooser. This can be added to focused components to provide editing
 * functionality for this chooser. This prevents duplicate entries and varies
 * how it captures input according to the type of key event its set to capture.
 *
 * @author Damian Johnson (atagar1@gmail.com)
 * @version August 19, 2007
 */
public class BindingAdaptor
    extends KeyAdapter
{
    private int inputEventType = KeyEvent.KEY_PRESSED; // Type of key event
                                                       // registered by input

    private int disablingKeyCode = KeyEvent.VK_ESCAPE; // Input keycode that
                                                       // gives disabled status

    private boolean isDisablingEnabled = false;

    private final BindingChooser chooser;

    private KeyEvent buffer = null;

    BindingAdaptor(BindingChooser chooser)
    {
        this.chooser = chooser;
    }

    /**
     * Provides if bindings are currently disableable via generated key adaptors
     * or not.
     *
     * @return true if input can disable bindings, false otherwise.
     */
    public boolean isBindingDisablingEnabled()
    {
        return this.isDisablingEnabled;
    }

    /**
     * Sets if bindings can be disabled via generated key adaptors with the
     * disabling key code. By default this is false.
     *
     * @param enable if true then input can disable bindings, otherwise bindings
     *            may not be disabled
     */
    public void setBindingsDisableable(boolean enable)
    {
        this.isDisablingEnabled = enable;
    }

    /**
     * Provides the keycode that can be input to generated key adaptors to
     * disable key bindings.
     *
     * @return keycode of disabling input
     */
    public int getDisablingKeyCode()
    {
        return this.disablingKeyCode;
    }

    /**
     * Sets the keycode used to disable individual key bindings (removing it
     * from returned mappings) via generated key adaptors. This only works if
     * the set event type is KEY_PRESSED or KEY_RELEASED since KEY_TYPED events
     * fail to provide keycodes. By default this is VK_ESCAPE.
     *
     * @param keycode keycode that sets selected entry to a disabled state
     */
    public void setDisablingKeyCode(int keycode)
    {
        this.disablingKeyCode = keycode;
    }

    /**
     * Provides the type of keystroke registered by input via generated key
     * adaptors.
     *
     * @return type of input detected by generated key adaptors
     */
    public int getInputEventType()
    {
        return this.inputEventType;
    }

    /**
     * Sets the type of keystroke registered by input via generated key adaptors
     * (by default KeyEvent.KEY_PRESSED). This must be a valid type of key event
     * which includes: KEY_PRESSED, KEY_RELEASED, or KEY_TYPED.
     *
     * @param type type of keystroke registered by input
     * @throws IllegalArgumentException if type doesn't match a valid key event
     */
    public void setInputEventType(int type)
    {
        int[] validTypes = new int[]
        { KeyEvent.KEY_PRESSED, KeyEvent.KEY_RELEASED, KeyEvent.KEY_TYPED };
        boolean isValid = false;
        for (int validType : validTypes)
        {
            isValid |= type == validType;
        }

        if (!isValid)
        {
            StringBuilder message = new StringBuilder();
            message.append("Unrecognized event type: ");
            message.append(type);
            message
                .append(" (must match KEY_PRESSED, KEY_RELEASED, or KEY_TYPED)");
            throw new IllegalArgumentException(message.toString());
        }

        this.inputEventType = type;
    }

    @Override
    public void keyPressed(KeyEvent event)
    {
        this.buffer = event; // Reports KEY_PRESSED events on release to support
                             // modifiers
    }

    @Override
    public void keyReleased(KeyEvent event)
    {
        if (this.inputEventType == KeyEvent.KEY_PRESSED && this.buffer != null)
        {
            setShortcut(this.buffer);
        }
        else if (this.inputEventType == KeyEvent.KEY_RELEASED)
        {
            setShortcut(event);
        }
    }

    @Override
    public void keyTyped(KeyEvent event)
    {
        if (this.inputEventType == KeyEvent.KEY_TYPED)
            setShortcut(event);
    }

    // Sets keystroke component of currently selected entry
    private void setShortcut(KeyEvent event)
    {
        if (this.chooser.isBindingSelected())
        {
            KeyStroke input = KeyStroke.getKeyStrokeForEvent(event);

            if (this.isDisablingEnabled
                && input.getKeyCode() == this.disablingKeyCode)
            {
                String action = this.chooser.getSelected().getAction();
                if (this.chooser.getBindings().contains(
                    new BindingEntry(BindingEntry.DISABLED, action)))
                {
                    this.chooser.setSelected(null); // This would cause a
                                                    // duplicate mapping
                }
                else
                {
                    this.chooser.doInput(BindingEntry.DISABLED);
                }
            }
            else if (!this.chooser.contains(input)
                || this.chooser.getSelected().getShortcut().equals(input))
            {
                this.chooser.doInput(input);
            }

            this.buffer = null;
        }
    }
}
