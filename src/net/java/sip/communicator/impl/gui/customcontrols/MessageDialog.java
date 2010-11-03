/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.customcontrols;

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>MessageDialog</tt> is a <tt>JDialog</tt> that contains a question
 * message, two buttons to confirm or cancel the question and a check box that
 * allows user to choose to not be questioned any more over this subject.
 * <p>
 * The message and the name of the "OK" button could be configured.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class MessageDialog
    extends SIPCommDialog
    implements  ActionListener,
                Skinnable
{
    private static final long serialVersionUID = 1L;

    private JButton cancelButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.CANCEL"));

    private JButton okButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.OK"));

    private JCheckBox doNotAskAgain = new SIPCommCheckBox(
        GuiActivator.getResources()
            .getI18NString("service.gui.DO_NOT_ASK_AGAIN"));

    private JLabel iconLabel = new JLabel(new ImageIcon(ImageLoader
            .getImage(ImageLoader.WARNING_ICON)));

    private StyledHTMLEditorPane messageArea = new StyledHTMLEditorPane();

    private TransparentPanel buttonsPanel
        = new TransparentPanel(new FlowLayout(FlowLayout.CENTER));

    private TransparentPanel checkBoxPanel
        = new TransparentPanel(new FlowLayout(FlowLayout.LEADING));

    private TransparentPanel messagePanel
        = new TransparentPanel(new BorderLayout(5, 5));

    private boolean isConfirmationEnabled = true;

    private int returnCode;

    /**
     * Indicates that the OK button is pressed.
     */
    public static final int OK_RETURN_CODE = 0;

    /**
     * Indicates that the Cancel button is pressed.
     */
    public static final int CANCEL_RETURN_CODE = 1;

    /**
     * Indicates that the OK button is pressed and the Don't ask check box is
     * checked.
     */
    public static final int OK_DONT_ASK_CODE = 2;

    /**
     * The maximum width that we allow message dialogs to have.
     */
    private static final int MAX_MSG_PANE_WIDTH = 600;

    /**
     * The maximum height that we allow message dialogs to have.
     */
    private static final int MAX_MSG_PANE_HEIGHT = 800;

    /**
     * Creates an instance of <tt>MessageDialog</tt> by specifying the
     * owner window.
     * @param owner This dialog owner.
     */
    public MessageDialog(Frame owner)
    {
        super(owner, false);

        this.getContentPane().setLayout(new BorderLayout(5, 5));

        this.messageArea.setOpaque(false);
        this.messageArea.setEditable(false);
        this.messageArea.setContentType("text/html");

        this.messagePanel.setBorder(
            BorderFactory.createEmptyBorder(10, 10, 0, 10));
        this.checkBoxPanel.setBorder(
            BorderFactory.createEmptyBorder(0, 10, 10, 10));

        this.init();
    }

    /**
     * Creates an instance of <tt>MessageDialog</tt> by specifying the
     * owner window and the message to be displayed.
     * @param owner the dialog owner
     * @param title the title of the message
     * @param message the message to be displayed
     */
    public MessageDialog(Frame owner, String title, String message)
    {
        this(owner);

        this.setTitle(title);

        setMessage(message);
    }

    /**
     * Creates an instance of <tt>MessageDialog</tt> by specifying the
     * owner window and the message to be displayed.
     * @param owner the dialog owner
     * @param title the title of the message
     * @param message the message to be displayed
     * @param okButtonName ok button name
     */
    public MessageDialog(   Frame owner,
                            String title,
                            String message,
                            String okButtonName)
    {
        this(owner, title, message);

        this.okButton.setText(okButtonName);
        this.okButton.setMnemonic(okButtonName.charAt(0));
    }

    /**
     * Creates an instance of <tt>MessageDialog</tt> by specifying the
     * owner window and the message to be displayed.
     * @param owner the dialog owner
     * @param title the title of the message
     * @param message the message to be displayed
     * @param okButtonName ok button name
     * @param isConfirmationEnabled indicates whether the "Do not ask again"
     * button should be enabled or not
     */
    public MessageDialog(   Frame owner,
                            String title,
                            String message,
                            String okButtonName,
                            boolean isConfirmationEnabled)
    {
        this(owner, title, message);

        this.okButton.setText(okButtonName);
        this.okButton.setMnemonic(okButtonName.charAt(0));

        this.isConfirmationEnabled = isConfirmationEnabled;
    }

    /**
     * Creates an instance of <tt>MessageDialog</tt> by specifying the
     * owner window and the message to be displayed.
     * @param owner the dialog owner
     * @param title the title of the message
     * @param message the message to be displayed
     * @param isCancelButtonEnabled <code>true</code> to show the Cancel button,
     * <code>false</code> - otherwise
     */
    public MessageDialog(   Frame owner,
                            String title,
                            String message,
                            boolean isCancelButtonEnabled)
    {
        this(owner, title, message);

        if(!isCancelButtonEnabled)
        {
            doNotAskAgain.setText(GuiActivator.getResources()
                .getI18NString("service.gui.DO_NOT_SHOW_AGAIN"));

            buttonsPanel.remove(cancelButton);
        }
    }

    /**
     * Initializes this dialog.
     */
    private void init()
    {
        this.getRootPane().setDefaultButton(okButton);

        if(isConfirmationEnabled)
            this.checkBoxPanel.add(doNotAskAgain);

        this.buttonsPanel.add(okButton);
        this.buttonsPanel.add(cancelButton);

        this.okButton.addActionListener(this);
        this.cancelButton.addActionListener(this);

        this.cancelButton.setMnemonic(cancelButton.getText().charAt(0));
        this.messagePanel.add(iconLabel, BorderLayout.WEST);
        this.messagePanel.add(messageArea, BorderLayout.CENTER);

        this.getContentPane().add(messagePanel, BorderLayout.NORTH);
        this.getContentPane().add(checkBoxPanel, BorderLayout.CENTER);
        this.getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
    }

    /**
     * Sets the message to be displayed.
     * @param message The message to be displayed.
     */
    public void setMessage(String message)
    {
        this.messageArea.setText(message);

        //try to reevaluate the preferred size of the message pane.
        //(this is definitely not a neat way to do it ... but it works).
        this.messageArea.setSize(
                        new Dimension(MAX_MSG_PANE_WIDTH, MAX_MSG_PANE_HEIGHT));
        int height = this.messageArea.getPreferredSize().height;
        this.messageArea.setPreferredSize(new Dimension(600, height));
    }

    /**
     * Shows the dialog.
     * @return The return code that should indicate what was the choice of
     * the user. If the user chooses cancel, the return code is the
     * CANCEL_RETURN_CODE.
     */
    public int showDialog()
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            final int[] returnCodes = new int[1];
            Exception exception = null;
            try
            {
                SwingUtilities.invokeAndWait(new Runnable()
                {
                    public void run()
                    {
                        returnCodes[0] = showDialog();
                    }
                });
            }
            catch (InterruptedException ex)
            {
                exception = ex;
            }
            catch (InvocationTargetException ex)
            {
                exception = ex;
            }
            if (exception != null)
                throw new UndeclaredThrowableException(exception);
            return returnCodes[0];
        }

        pack();

        setModal(true);
        setVisible(true);

        return returnCode;
    }

    /**
     * Handles the <tt>ActionEvent</tt>. Depending on the user choice sets
     * the return code to the appropriate value.
     *
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        JButton button = (JButton)e.getSource();

        if(button.equals(okButton))
        {
            if (doNotAskAgain.isSelected())
            {
                this.returnCode = OK_DONT_ASK_CODE;
            }
            else
            {
                this.returnCode = OK_RETURN_CODE;
            }
        }
        else
        {
            this.returnCode = CANCEL_RETURN_CODE;
        }

        this.dispose();
    }

    /**
     * Visually clicks the cancel button on close.
     *
     * @param isEscaped indicates if the window was close by pressing the escape
     * button
     */
    protected void close(boolean isEscaped)
    {
        this.cancelButton.doClick();
    }

    /**
     * Reloads icon.
     */
    public void loadSkin()
    {
        iconLabel.setIcon(new ImageIcon(ImageLoader
            .getImage(ImageLoader.WARNING_ICON)));
    }
}
