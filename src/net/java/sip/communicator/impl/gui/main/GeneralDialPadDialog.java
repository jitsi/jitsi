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
package net.java.sip.communicator.impl.gui.main;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.plaf.*;
import net.java.sip.communicator.util.skin.*;

import org.jitsi.util.*;

import com.explodingpixels.macwidgets.*;

/**
 * The <tt>DialpadDialog</tt> is a popup dialog containing a dialpad.
 *
 * @author Yana Stamcheva
 */
public class GeneralDialPadDialog
{
    /**
     * The call field, where the dialed number is typed.
     */
    private final JTextField callField;

    /**
     * The actual dial pad dialog.
     */
    private final JDialog dialPadDialog;

    /**
     * The call button.
     */
    private JButton callButton;

    /**
     * A keyboard manager, where we register our own key dispatcher.
     */
    private KeyboardFocusManager keyManager;

    /**
     * A key dispatcher that redirects all key events to call field.
     */
    private KeyEventDispatcher keyDispatcher;

    /**
     * Creates an instance of <tt>GeneralDialPadDialog</tt>.
     */
    public GeneralDialPadDialog()
    {
        dialPadDialog
            = OSUtils.IS_MAC
                ? new HudWindow().getJDialog()
                : new SIPCommDialog(false);
        dialPadDialog.setTitle(
                GuiActivator.getResources().getI18NString(
                        "service.gui.DIALPAD"));

        callField
            = new CallField(
                    GuiActivator.getResources().getI18NString(
                            "service.gui.ENTER_NAME_OR_NUMBER"));

        initInputMap();

        JPanel mainPanel = new TransparentPanel(new BorderLayout());

        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(callField, BorderLayout.NORTH);

        final DTMFHandler dtmfHandler = new DTMFHandler();

        mainPanel.add(new GeneralDialPanel(this, dtmfHandler));
        mainPanel.add(createCallPanel(), BorderLayout.SOUTH);

        dialPadDialog.add(mainPanel);
        dialPadDialog.pack();

        dialPadDialog.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowOpened(WindowEvent e)
            {
                if (keyManager == null)
                {
                    keyManager
                        = KeyboardFocusManager.getCurrentKeyboardFocusManager();
                }
                if (keyDispatcher == null)
                    keyDispatcher = new MainKeyDispatcher(keyManager);
                keyManager.addKeyEventDispatcher(keyDispatcher);

                dtmfHandler.addParent(dialPadDialog);
            }

            @Override
            public void windowClosed(WindowEvent e)
            {
                try
                {
                    if (keyManager != null)
                    {
                        keyManager.removeKeyEventDispatcher(keyDispatcher);
                        keyManager = null;
                    }
                    keyDispatcher = null;
                }
                finally
                {
                    dtmfHandler.removeParent(dialPadDialog);
                }
            }
        });
    }

    /**
     * Initializes the input map.
     */
    private void initInputMap()
    {
        InputMap imap = dialPadDialog.getRootPane().getInputMap(
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape");

        ActionMap amap = dialPadDialog.getRootPane().getActionMap();

        amap.put("escape", new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                String text = callField.getText();

                // If the text area is empty we close the dialpad.
                if (text == null || text.length() <= 0)
                    dialPadDialog.setVisible(false);
                else
                    callField.setText("");
            }
        });

        // put the defaults for macosx
        if(OSUtils.IS_MAC)
        {
            imap.put(
                KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.META_DOWN_MASK),
                "close");
            imap.put(
                KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK),
                "close");

            amap.put("close", new AbstractAction()
            {
                public void actionPerformed(ActionEvent e)
                {
                    dialPadDialog.setVisible(false);
                }
            });
        }
    }

    /**
     * Creates the call panel.
     *
     * @return the created call panel.
     */
    private JComponent createCallPanel()
    {
        JPanel buttonsPanel
            = new TransparentPanel(new FlowLayout(FlowLayout.CENTER));

        Image callButtonImage
            = ImageLoader.getImage(ImageLoader.DIAL_PAD_CALL_BUTTON_BG);

        callButton = new SIPCommTextButton(
            GuiActivator.getResources().getI18NString("service.gui.CALL"),
            callButtonImage);

        callButton.setPreferredSize(new Dimension(
            callButtonImage.getWidth(null),
            callButtonImage.getHeight(null)));

        callButton.setForeground(Color.WHITE);

        callButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                String callNumber = callField.getText();

                if (callNumber != null && callNumber.length() > 0)
                    CallManager.createCall(callField.getText(), callButton,
                        new CallInterfaceListener()
                        {
                            public void callInterfaceStarted()
                            {
                                dialPadDialog.setVisible(false);
                            }
                        });
            }
        });

        buttonsPanel.add(callButton);

        dialPadDialog.getRootPane().setDefaultButton(callButton);

        return buttonsPanel;
    }

    /**
     * Indicates that a dial button was pressed.
     *
     * @param s the string corresponding to a number to add to the call field
     */
    public void dialButtonPressed(String s)
    {
        String currentText = callField.getText();

        if (currentText == null)
            currentText = "";
        callField.setText(currentText + s);
    }

    /**
     * Shows/hides the dial pad dialog.
     *
     * @param visible indicates if the dial pad should be shown or hidden.
     */
    public void setVisible(boolean visible)
    {
        dialPadDialog.setLocationRelativeTo(
            GuiActivator.getUIService().getMainFrame());
        dialPadDialog.setVisible(visible);
        callField.requestFocus();
    }

    /**
     * Clears the call field.
     */
    public void clear()
    {
        callField.setText("");
    }

    /**
     * The <tt>MainKeyDispatcher</tt> is added to pre-listen KeyEvents before
     * they're delivered to the current focus owner in order to introduce a
     * specific behavior for the <tt>CallField</tt> on top of the dial pad.
     */
    private class MainKeyDispatcher implements KeyEventDispatcher
    {
        private final KeyboardFocusManager keyManager;

        /**
         * Creates an instance of <tt>MainKeyDispatcher</tt>.
         * @param keyManager the parent <tt>KeyboardFocusManager</tt>
         */
        public MainKeyDispatcher(KeyboardFocusManager keyManager)
        {
            this.keyManager = keyManager;
        }

        /**
         * Dispatches the given <tt>KeyEvent</tt>.
         * @param e the <tt>KeyEvent</tt> to dispatch
         * @return <tt>true</tt> if the KeyboardFocusManager should take no
         * further action with regard to the KeyEvent; <tt>false</tt>
         * otherwise
         */
        public boolean dispatchKeyEvent(KeyEvent e)
        {
            // If this window is not the focus window  or if the event is not
            // of type PRESSED we have nothing more to do here.
            if (!dialPadDialog.isFocused() || (e.getID() != KeyEvent.KEY_TYPED))
                return false;

            switch (e.getKeyChar())
            {
            case KeyEvent.CHAR_UNDEFINED:
            case KeyEvent.VK_ENTER:
            case KeyEvent.VK_DELETE:
            case KeyEvent.VK_BACK_SPACE:
            case KeyEvent.VK_TAB:
            case KeyEvent.VK_SPACE:
                return false;
            }

            if (!callField.isFocusOwner()
                    && (keyManager.getFocusOwner() != null))
            {
                // Request the focus in the call field if a letter is typed.
                callField.requestFocusInWindow();

                // We re-dispatch the event to call field.
                keyManager.redispatchEvent(callField, e);

                // We don't want to dispatch further this event.
                return true;
            }

            return false;
        }
    }

    /**
     * A custom call field.
     */
    private static class CallField
        extends SIPCommTextField
        implements Skinnable
    {
        /**
         * Class id key used in UIDefaults.
         */
        private static final String uiClassID =
            CallField.class.getName() +  "FieldUI";

        /**
         * Adds the ui class to UIDefaults.
         */
        static
        {
            UIManager.getDefaults().put(uiClassID,
                DialPadFieldUI.class.getName());
        }

        /**
         * The text field ui.
         */
        private SIPCommTextFieldUI textFieldUI;

        /**
         * Creates an instance of the <tt>CallField</tt>.
         *
         * @param text
         */
        public CallField(String text)
        {
            super(text);

            if(getUI() instanceof DialPadFieldUI)
            {
                ((DialPadFieldUI)getUI()).setDeleteButtonEnabled(true);
            }

            this.setPreferredSize(new Dimension(200, 23));
            this.setBorder(null);
            this.setOpaque(false);

            this.setDragEnabled(true);

            loadSkin();
        }

        /**
         * Reloads text field UI defs.
         */
        public void loadSkin()
        {
            if(getUI() instanceof SIPCommTextFieldUI)
                textFieldUI = (SIPCommTextFieldUI)getUI();
            else
                return;

            textFieldUI.loadSkin();

            if (OSUtils.IS_MAC)
            {
                textFieldUI.setBgStartColor(Color.BLACK);
                textFieldUI.setBgEndColor(Color.BLACK);
                textFieldUI.setBgBorderStartColor(Color.DARK_GRAY);
                textFieldUI.setBgBorderEndColor(Color.GRAY);

                setForegroundColor(Color.WHITE);
                setDefaultTextColor(Color.GRAY);

                setCaretColor(Color.WHITE);
            }
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
    }
}
