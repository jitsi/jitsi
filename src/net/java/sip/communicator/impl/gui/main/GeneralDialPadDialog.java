/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;
import net.java.sip.communicator.util.swing.plaf.*;

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
        if (OSUtils.IS_MAC)
        {
            HudWindow window = new HudWindow();

            dialPadDialog = window.getJDialog();
        }
        else
        {
            dialPadDialog = new SIPCommDialog(false);
        }

        callField = new CallField(GuiActivator.getResources()
            .getI18NString("service.gui.ENTER_NAME_OR_NUMBER"));

        dialPadDialog.setTitle(
            GuiActivator.getResources().getI18NString("service.gui.DIALPAD"));

        JPanel mainPanel = new TransparentPanel(new BorderLayout());

        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        mainPanel.add(callField, BorderLayout.NORTH);
        DTMFHandler dtmfHandler = new DTMFHandler();
        dtmfHandler.addParent(dialPadDialog);
        mainPanel.add(new GeneralDialPanel(this, dtmfHandler));
        mainPanel.add(createCallPanel(), BorderLayout.SOUTH);

        dialPadDialog.add(mainPanel);
        dialPadDialog.pack();

        dialPadDialog.addWindowListener(new WindowAdapter()
        {
            public void windowOpened(WindowEvent e)
            {
                if (keyManager == null)
                {
                    keyManager
                        = KeyboardFocusManager.getCurrentKeyboardFocusManager();

                    keyDispatcher = new MainKeyDispatcher(keyManager);
                }

                keyManager.addKeyEventDispatcher(
                    new MainKeyDispatcher(keyManager));
            }

            @Override
            public void windowClosed(WindowEvent e)
            {
                if (keyManager != null)
                    keyManager.removeKeyEventDispatcher(keyDispatcher);

                keyManager = null;
                keyDispatcher = null;
            }
        });
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
        callField.setText(callField.getText() + s);
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
        private KeyboardFocusManager keyManager;

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
            if (!dialPadDialog.isFocused()
                || (e.getID() != KeyEvent.KEY_TYPED))
                return false;

            if (e.getKeyChar() == KeyEvent.CHAR_UNDEFINED
                || e.getKeyCode() == KeyEvent.VK_ENTER
                || e.getKeyCode() == KeyEvent.VK_DELETE
                || e.getKeyCode() == KeyEvent.VK_BACK_SPACE
                || e.getKeyCode() == KeyEvent.VK_TAB
                || e.getKeyCode() == KeyEvent.VK_SPACE)
            {
                return false;
            }

            if (!callField.isFocusOwner()
                && keyManager.getFocusOwner() != null)
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
    private class CallField
        extends SIPCommTextField
        implements Skinnable
    {
        /**
         * The text field ui.
         */
        private SIPCommTextFieldUI textFieldUI;

        /**
         * Creates an instance of the <tt>CallField</tt>.
         */
        public CallField(String text)
        {
            super(text);

            textFieldUI = new DialPadFieldUI();
            textFieldUI.setDeleteButtonEnabled(true);

            this.setPreferredSize(new Dimension(200, 23));
            this.setUI(textFieldUI);
            this.setBorder(null);
            this.setOpaque(false);

            this.setDragEnabled(true);

            InputMap imap
                = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
            imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape");
            ActionMap amap = getActionMap();
            amap.put("escape", new AbstractAction()
            {
                public void actionPerformed(ActionEvent e)
                {
                    setText("");
                }
            });

            loadSkin();
        }

        /**
         * Reloads text field UI defs.
         */
        public void loadSkin()
        {
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
    }
}
