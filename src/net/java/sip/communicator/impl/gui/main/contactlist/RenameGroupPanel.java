/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;
import javax.swing.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.swing.*;

/**
 * The <tt>RenameGroupPanel</tt> is where the user could change the name of a
 * meta contact group.
 * 
 * @author Yana Stamcheva
 */
public class RenameGroupPanel
    extends TransparentPanel
{
    private JLabel uinLabel
        = new JLabel(Messages.getI18NString("newName").getText());

    private JTextField textField = new JTextField();

    private TransparentPanel dataPanel
        = new TransparentPanel(new BorderLayout(5, 5));

    private SIPCommMsgTextArea infoLabel
        = new SIPCommMsgTextArea(
            Messages.getI18NString("renameGroupInfo").getText());

    private JLabel infoTitleLabel
        = new JLabel(Messages.getI18NString("renameGroup").getText());

    private JLabel iconLabel
        = new JLabel(new ImageIcon(ImageLoader.getImage(
            ImageLoader.RENAME_DIALOG_ICON)));

    private TransparentPanel labelsPanel
        = new TransparentPanel(new GridLayout(0, 1));

    private TransparentPanel rightPanel
        = new TransparentPanel(new BorderLayout());

    /**
     * Creates an instance of <tt>RenameGroupPanel</tt> and initializes it.
     */
    public RenameGroupPanel(String groupName)
    {
        super(new BorderLayout());

        this.textField.setText(groupName);
        this.textField.select(0, groupName.length());

        this.setPreferredSize(new Dimension(500, 200));

        this.iconLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 10));

        this.infoLabel.setEditable(false);

        this.dataPanel.add(uinLabel, BorderLayout.WEST);

        this.dataPanel.add(textField, BorderLayout.CENTER);

        this.infoTitleLabel.setHorizontalAlignment(JLabel.CENTER);
        this.infoTitleLabel.setFont(Constants.FONT.deriveFont(Font.BOLD, 18));

        this.labelsPanel.add(infoTitleLabel);
        this.labelsPanel.add(infoLabel);
        this.labelsPanel.add(dataPanel);

        this.rightPanel.add(labelsPanel, BorderLayout.NORTH);

        this.add(iconLabel, BorderLayout.WEST);
        this.add(rightPanel, BorderLayout.CENTER);
    }

    /**
     * Returns the new name entered by the user.
     * 
     * @return the new name entered by the user.
     */
    public String getNewName()
    {
        return textField.getText();
    }

    /**
     * Requests the focus in the text field.
     */
    public void requestFocusInField()
    {
        this.textField.requestFocus();
    }
}
