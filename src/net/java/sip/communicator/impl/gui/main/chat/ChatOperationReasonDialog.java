/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;

/**
 * @author Yana Stamcheva
 * @author Valentin Martinet
 */
public class ChatOperationReasonDialog extends MessageDialog
{
    private static final long serialVersionUID = 3290030744711759011L;

    private final JLabel reasonLabel = new JLabel(
        GuiActivator.getResources()
            .getI18NString("service.gui.REASON") + ":");

    private final JTextField reasonField = new JTextField();

    /**
     * Creates an instance of <tt>ChatOperationReasonDialog</tt> using the
     * default title and message.
     */
    public ChatOperationReasonDialog()
    {
        this(null,
            GuiActivator.getResources().getI18NString(
            "service.gui.REASON"),
            GuiActivator.getResources().getI18NString(
            "service.gui.SPECIFY_REASON"),
            GuiActivator.getResources().getI18NString(
            "service.gui.OK"), true, false);
        
        
    }
    
    /**
     * Creates an instance of <tt>ChatOperationReasonDialog</tt> using the
     * default title and message.
     * 
     * @param disableOKIfReasonIsEmpty if true the OK button will be disabled if
     * the reason text is empty.
     */
    public ChatOperationReasonDialog(boolean disableOKIfReasonIsEmpty)
    {
        this(null,
            GuiActivator.getResources().getI18NString(
            "service.gui.REASON"),
            GuiActivator.getResources().getI18NString(
            "service.gui.SPECIFY_REASON"),
            GuiActivator.getResources().getI18NString(
            "service.gui.OK"), true, disableOKIfReasonIsEmpty);
        
        
    }

    /**
     * Creates an instance of <tt>ChatOperationReasonDialog</tt> by specifying
     * the title and the message shown in the dialog.
     * @param title the title of this dialog
     * @param message the message shown in this dialog
     */
    public ChatOperationReasonDialog(String title, String message)
    {
        this(null,
            title,
            message,
            GuiActivator.getResources().getI18NString("service.gui.OK"),
            true,
            false);
        
    }
    
    /**
     * Creates an instance of <tt>ChatOperationReasonDialog</tt> by specifying
     * the title and the message shown in the dialog.
     * @param title the title of this dialog
     * @param message the message shown in this dialog
     * @param disableOKIfReasonIsEmpty if true the OK button will be disabled if
     * the reason text is empty.
     * @param showReasonLabel specify if we want the "Reason:" label
     */
    public ChatOperationReasonDialog(String title, String message,
        boolean showReasonLabel,
        boolean disableOKIfReasonIsEmpty)
    {
        this(null,
            title,
            message,
            GuiActivator.getResources().getI18NString("service.gui.OK"),
            showReasonLabel,
            disableOKIfReasonIsEmpty);
        
    }

    /**
     * Creates an instance of <tt>ChatOperationReasonDialog</tt> by specifying
     * the parent window, the title and the message to show.
     * @param chatWindow the parent window
     * @param title the title of this dialog
     * @param message the message shown in this dialog
     * @param okButtonName the custom name of the ok button
     * @param showReasonLabel specify if we want the "Reason:" label
     */
    public ChatOperationReasonDialog(Frame chatWindow, String title,
        String message, String okButtonName, boolean showReasonLabel)
    {
        this(chatWindow,
            title,
            message,
            okButtonName,
            showReasonLabel,
            false);
    }
    
    /**
     * Creates an instance of <tt>ChatOperationReasonDialog</tt> by specifying
     * the parent window, the title and the message to show.
     * @param chatWindow the parent window
     * @param title the title of this dialog
     * @param message the message shown in this dialog
     * @param okButtonName the custom name of the ok button
     * @param showReasonLabel specify if we want the "Reason:" label
     * @param disableOKIfReasonIsEmpty if true the OK button will be disabled if
     * the reason text is empty.
     */
    public ChatOperationReasonDialog(Frame chatWindow, String title,
        String message, String okButtonName, boolean showReasonLabel, 
        boolean disableOKIfReasonIsEmpty)
    {
        super(chatWindow, title, message, okButtonName, false);

        JPanel reasonPanel = new JPanel(new BorderLayout());

        if(showReasonLabel)
        {
            reasonPanel.add(reasonLabel, BorderLayout.WEST);
        }
        else
            reasonPanel.add(new JLabel(""), BorderLayout.WEST);

        reasonPanel.add(new JLabel("          "), BorderLayout.EAST);

        JPanel reasonFieldPanel = new JPanel(new BorderLayout());
        reasonFieldPanel.add(reasonField, BorderLayout.NORTH);

        reasonPanel.add(reasonFieldPanel, BorderLayout.CENTER);
        reasonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        reasonPanel.setOpaque(false);

        replaceCheckBoxPanel(reasonPanel);
        
        if(disableOKIfReasonIsEmpty)
        {
            updateOKButtonState();
            this.reasonField.getDocument().addDocumentListener(new DocumentListener()
            {
                
                @Override
                public void removeUpdate(DocumentEvent arg0)
                {
                    updateOKButtonState();                
                }
                
                @Override
                public void insertUpdate(DocumentEvent arg0)
                {
                    updateOKButtonState();
                }
                
                @Override
                public void changedUpdate(DocumentEvent arg0)
                {
                    updateOKButtonState();
                }
                
               
            });
        }
        this.pack();
    }
    
    /**
     * Enables the OK button if reason field is not empty and disables it if the
     * reason field is empty.
     */
    private void updateOKButtonState()
    {
        okButton.setEnabled(!reasonField.getText().trim().equals(""));
    }

    /**
     * Returns the text entered in the reason field.
     * @return the text entered in the reason field
     */
    public String getReason()
    {
        return reasonField.getText();
    }

    /**
     * Sets a default value for the reason field.
     * @param value the text to set as default text for the reason field
     */
    public void setReasonFieldText(String value)
    {
        reasonField.setText(value);
    }

    /**
     * Sets the message to be displayed.
     * @param message The message to be displayed.
     */
    public void setMessage(String message)
    {
        super.setMessage(message);

        setMaxWidth(400);
    }
}
