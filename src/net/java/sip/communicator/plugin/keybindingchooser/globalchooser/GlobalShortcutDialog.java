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

import net.java.sip.communicator.plugin.keybindingchooser.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.plaf.*;
import net.java.sip.communicator.service.globalshortcut.*;
import net.java.sip.communicator.util.skin.*;

import org.jitsi.util.*;
// disambiguation

/**
 * Dialog to choose the shortcut.
 *
 * @author Sebastien Vincent
 */
public class GlobalShortcutDialog
    extends SIPCommDialog
    implements ActionListener,
               GlobalShortcutListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Text displayed when no shortcut is configured.
     */
    private static final String PRESS_TO_SETUP_SHORTCUT =
        Resources.getString("plugin.keybindings.globalchooser.PRESS_BTN");

    /**
     * The global shortcut entry.
     */
    private final GlobalShortcutEntry entry;

    /**
     * OK button.
     */
    private final JButton btnOK = new JButton(
        Resources.getString("service.gui.OK"));

    /**
     * Cancel button.
     */
    private final JButton btnCancel = new JButton(
        Resources.getString("service.gui.CANCEL"));

    /**
     * Enable or not special key for shortcut.
     */
    private final JCheckBox specialBox = new SIPCommCheckBox(
        Resources.getString("plugin.keybindings.globalchooser.ENABLE_SPECIAL"));

    /**
     * First shortcut field.
     */
    private final ShortcutField fldShortcut = new ShortcutField(
        PRESS_TO_SETUP_SHORTCUT);

    /**
     * Secondary shortcut field.
     */
    private final ShortcutField fldShortcut2 = new ShortcutField(
        PRESS_TO_SETUP_SHORTCUT);

    /**
     * Return code.
     */
    private int retCode = 0;

    /**
     * Constructor.
     *
     * @param dialog root dialog
     * @param entry the global shortcut entry
     */
    public GlobalShortcutDialog(Dialog dialog, GlobalShortcutEntry entry)
    {
        super(dialog);

        setModal(true);
        setTitle("Global shortcut: " + entry.getAction());
        this.entry = entry;
        init();
    }

    /**
     * Initialize components.
     */
    private void init()
    {
        TransparentPanel mainPanel = new TransparentPanel(new BorderLayout());
        JPanel btnPanel = new TransparentPanel(
            new FlowLayout(FlowLayout.RIGHT));
        JPanel shortcutPanel = new TransparentPanel(
            new GridLayout(0, 2, 0, 10));

        btnOK.addActionListener(this);
        btnCancel.addActionListener(this);

        KeyAdapter keyAdapter = new KeyAdapter()
        {
            private KeyEvent buffer = null;

            @Override
            public void keyPressed(KeyEvent event)
            {
                if(event.getKeyCode() == KeyEvent.VK_ESCAPE)
                {
                    SIPCommTextField field =
                        (SIPCommTextField)event.getSource();

                    AWTKeyStroke ks = null;

                    if(field == fldShortcut)
                    {
                        ks = entry.getShortcut();
                    }
                    else if(field == fldShortcut2)
                    {
                        ks = entry.getShortcut2();
                    }

                    if(ks == null)
                        field.setText(PRESS_TO_SETUP_SHORTCUT);
                    else
                    {

                        if(ks.getModifiers() ==
                            GlobalShortcutService.SPECIAL_KEY_MODIFIERS)
                        {
                            field.setText("Special");
                        }
                        else
                        {
                            field.setText(GlobalShortcutEntry.getShortcutText(
                                entry.getShortcut()));
                        }
                    }
                    btnOK.requestFocusInWindow();
                    return;
                }

                if(event.getKeyCode() == 0)
                    return;

                // Reports KEY_PRESSED events on release to support modifiers
                this.buffer = event;
            }

            @Override
            public void keyReleased(KeyEvent event)
            {
                if (buffer != null)
                {
                    SIPCommTextField field =
                        (SIPCommTextField)event.getSource();
                    AWTKeyStroke input = KeyStroke.getKeyStrokeForEvent(buffer);
                    buffer = null;

                    GlobalShortcutEntry en = entry;
                    List<AWTKeyStroke> kss = new ArrayList<AWTKeyStroke>();

                    if(field == fldShortcut)
                    {
                        kss.add(input);
                        kss.add(en.getShortcut2());
                    }
                    else if(field == fldShortcut2)
                    {
                        kss.add(en.getShortcut());
                        kss.add(input);
                    }

                    en.setShortcuts(kss);
                    en.setEditShortcut1(false);
                    en.setEditShortcut2(false);
                    field.setText(GlobalShortcutEntry.getShortcutText(
                        input));
                    btnOK.requestFocus();
                }
            }
        };

        AWTKeyStroke ks = entry.getShortcut();
        AWTKeyStroke ks2 = entry.getShortcut2();

        if(ks != null)
        {
            if(ks.getModifiers() != GlobalShortcutService.SPECIAL_KEY_MODIFIERS)
            {
                fldShortcut.setText(GlobalShortcutEntry.getShortcutText(ks));
            }
            else
            {
                fldShortcut.setText("Special");
            }
        }

        if(ks2 != null)
        {

            if(ks2.getModifiers() != GlobalShortcutService.SPECIAL_KEY_MODIFIERS)
            {
                fldShortcut2.setText(GlobalShortcutEntry.getShortcutText(ks2));
            }
            else
            {
                fldShortcut2.setText("Special");
            }
        }

        fldShortcut.addKeyListener(keyAdapter);
        fldShortcut2.addKeyListener(keyAdapter);

        specialBox.addItemListener(new ItemListener()
        {
            public void itemStateChanged(ItemEvent evt)
            {
                KeybindingChooserActivator.getGlobalShortcutService().
                    setSpecialKeyDetection(
                        (evt.getStateChange() == ItemEvent.SELECTED),
                        GlobalShortcutDialog.this);
            }
        });

        shortcutPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10,
            10));
        shortcutPanel.add(new JLabel("Primary shortcut"));
        shortcutPanel.add(fldShortcut);
        shortcutPanel.add(new JLabel("Secondary shortcut"));
        shortcutPanel.add(fldShortcut2);

        if(OSUtils.IS_WINDOWS)
        {
            shortcutPanel.add(new TransparentPanel());
            shortcutPanel.add(specialBox);
        }

        mainPanel.add(shortcutPanel, BorderLayout.CENTER);

        btnPanel.add(btnOK);
        btnPanel.add(btnCancel);
        mainPanel.add(btnPanel, BorderLayout.SOUTH);

        btnOK.requestFocus();

        getContentPane().add(mainPanel);
        pack();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void close(boolean isEscaped)
    {
        super.close(isEscaped);
        KeybindingChooserActivator.getGlobalShortcutService().
            setSpecialKeyDetection(false, this);
    }

    /**
     * Show the dialog and returns if the user has modified something (create
     * or modify entry).
     *
     * @return true if the user has modified something (create
     * or modify entry), false otherwise.
     */
    public int showDialog()
    {
        setVisible(true);

        // as the dialog is modal, wait for OK/Cancel button retCode
        setVisible(false);
        return retCode;
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent evt)
    {
        Object obj = evt.getSource();

        if(obj == btnOK)
        {
            retCode = 1;
            dispose();
        }
        else if(obj == btnCancel)
        {
            retCode = 0;
            dispose();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void shortcutReceived(GlobalShortcutEvent evt)
    {
        AWTKeyStroke ksr = evt.getKeyStroke();

        if(ksr.getModifiers() != GlobalShortcutService.SPECIAL_KEY_MODIFIERS)
        {
            return;
        }

        if(!fldShortcut.isFocusOwner() && !fldShortcut2.isFocusOwner())
            return;

        List<AWTKeyStroke> kss = new ArrayList<AWTKeyStroke>();

        if(fldShortcut.isFocusOwner())
        {
            kss.add(ksr);
            kss.add(entry.getShortcut2());
            fldShortcut.setText("Special");
        }
        else if(fldShortcut2.isFocusOwner())
        {
            kss.add(entry.getShortcut());
            kss.add(ksr);
            fldShortcut2.setText("Special");
        }
        entry.setShortcuts(kss);
        KeybindingChooserActivator.getGlobalShortcutService().
            setSpecialKeyDetection(false, this);
    }

    /**
     * Clear the text field.
     *
     * @param ui <tt>TextFieldUI</tt> to clear
     */
    public void clearTextField(SIPCommTextFieldUI ui)
    {
        List<AWTKeyStroke> kss = new ArrayList<AWTKeyStroke>();

        if(ui == fldShortcut.getUI())
        {
            kss.add(null);
            kss.add(entry.getShortcut2());
            entry.setShortcuts(kss);
            btnOK.requestFocusInWindow();
        }
        else if(ui == fldShortcut2.getUI())
        {
            kss.add(entry.getShortcut());
            kss.add(null);
            entry.setShortcuts(kss);
            btnOK.requestFocusInWindow();
        }
    }

    /**
     * A custom call field.
     */
    private class ShortcutField
        extends SIPCommTextField
        implements Skinnable
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        /**
         * The text field ui.
         */
        private ShortcutFieldUI textFieldUI;

        /**
         * Creates an instance of the <tt>CallField</tt>.
         *
         * @param text
         */
        public ShortcutField(String text)
        {
            super(text);

            textFieldUI = new ShortcutFieldUI();
            textFieldUI.setDeleteButtonEnabled(true);

            this.setPreferredSize(new Dimension(200, 23));
            this.setUI(textFieldUI);
            this.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            this.setOpaque(false);

            this.setDragEnabled(true);

            loadSkin();
        }

        /**
         * Reloads text field UI defs.
         */
        public void loadSkin()
        {
            textFieldUI.loadSkin();
        }
    }

    /**
     * A custom text field UI.
     */
    public class ShortcutFieldUI
        extends SIPCommTextFieldUI
        implements Skinnable
    {
        /**
         * Creates a <tt>SIPCommTextFieldUI</tt>.
         */
        public ShortcutFieldUI()
        {
            loadSkin();
        }

        /**
         * Adds the custom mouse listeners defined in this class to the installed
         * listeners.
         */
        @Override
        protected void installListeners()
        {
            super.installListeners();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void updateDeleteIcon(MouseEvent evt)
        {
            super.updateDeleteIcon(evt);

            Rectangle deleteRect = getDeleteButtonRect();
            if(deleteRect.contains(evt.getX(), evt.getY()) &&
                evt.getID() == MouseEvent.MOUSE_CLICKED)
            {
                clearTextField(this);
            }
        }

        /**
         * Implements parent paintSafely method and enables antialiasing.
         * @param g the <tt>Graphics</tt> object that notified us
         */
        @Override
        protected void paintSafely(Graphics g)
        {
            customPaintBackground(g);
            super.paintSafely(g);
        }

        /**
         * Paints the background of the associated component.
         * @param g the <tt>Graphics</tt> object used for painting
         */
        @Override
        protected void customPaintBackground(Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g.create();

            try
            {
                AntialiasingManager.activateAntialiasing(g2);
                super.customPaintBackground(g2);
            }
            finally
            {
                g2.dispose();
            }
        }
    }
}
