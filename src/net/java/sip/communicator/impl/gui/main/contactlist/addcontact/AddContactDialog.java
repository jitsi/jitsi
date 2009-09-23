/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist.addcontact;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>AddContactDialog</tt> is the dialog containing the form for adding
 * a contact. It is different from the "Add Contact" wizard opened from the
 * quick menu button. The <tt>AddContactDialog</tt> is used when a new contact
 * is added to an already existing meta contact or when adding a meta contact
 * to an already existing meta contact group.
 * 
 * @author Yana Stamcheva
 */
public class AddContactDialog
    extends SIPCommDialog
    implements ActionListener
{
    private Logger logger = Logger.getLogger(AddContactDialog.class.getName());
    
    private AddContactPanel addContactPanel = new AddContactPanel();
    
    private JButton addButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.ADD"));
    
    private JButton cancelButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.CANCEL"));
    
    private TransparentPanel buttonsPanel
        = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));
    
    private TransparentPanel mainPanel
        = new TransparentPanel(new BorderLayout());
    
    private MetaContactListService clist;
    
    private MainFrame mainFrame;
    
    private MetaContact metaContact;
    
    private MetaContactGroup group;
    
    private ProtocolProviderService pps;
    
    /**
     * Creates an instance of <tt>AddContactDialog</tt> that represents a dialog
     * that adds a new contact to an already existing meta contact.
     * 
     * @param mainFrame The <tt>MainFrame</tt> parent window.
     * @param metaContact The <tt>MetaContact</tt> that would contain the
     * newly created contact.
     * @param pps The <tt>ProtocolProviderService</tt>.
     */
    public AddContactDialog(MainFrame mainFrame,
            MetaContact metaContact,
            ProtocolProviderService pps)
    {
        super(mainFrame);
        
        this.mainFrame = mainFrame;
        this.clist = mainFrame.getContactList();
        this.metaContact = metaContact;
        this.pps = pps;
        
        this.init();
    }

    /**
     * Creates an instance of <tt>AddContactDialog</tt> that represents a dialog
     * that adds a new meta contact to an already existing meta contact group.
     * 
     * @param mainFrame The <tt>MainFrame</tt> parent window.
     * @param group The <tt>MetaContactGroup</tt> that would contain the
     * newly created meta contact.
     * @param pps The <tt>ProtocolProviderService</tt>.
     */
    public AddContactDialog(MainFrame mainFrame,
            MetaContactGroup group,
            ProtocolProviderService pps)
    {
        super(mainFrame);
        
        this.mainFrame = mainFrame;
        this.clist = mainFrame.getContactList();
        this.group = group;
        this.pps = pps;
        
        this.init();
    }

    /**
     * Initializes the dialog.
     */
    private void init()
    {
        this.setTitle(GuiActivator.getResources()
            .getI18NString("service.gui.ADD_CONTACT"));

        this.setSize(520, 250);

        this.getRootPane().setDefaultButton(addButton);
        this.addButton.setName("add");
        this.cancelButton.setName("cancel");

        this.addButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.ADD"));

        this.cancelButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.CANCEL"));
        
        this.addButton.addActionListener(this);
        this.cancelButton.addActionListener(this);
        
        this.buttonsPanel.add(addButton);
        this.buttonsPanel.add(cancelButton);
        
        this.mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10));
        
        this.mainPanel.add(addContactPanel, BorderLayout.CENTER);
        this.mainPanel.add(buttonsPanel, BorderLayout.SOUTH);
        
        this.getContentPane().add(mainPanel);
    }
    
    /**
     * 
     */
    public void actionPerformed(ActionEvent e)
    {
        JButton button = (JButton)e.getSource();
        String name = button.getName();
        
        if (name.equals("add"))
        {
            if (metaContact != null)
            {
                new Thread()
                {
                    public void run()
                    {
                        clist.addNewContactToMetaContact(pps, metaContact,
                            addContactPanel.getUIN());
                    }
                }.start();
            }
            else if (group != null)
            {
                new Thread()
                {
                    public void run()
                    {
                        String uin = addContactPanel.getUIN();
                        try
                        {
                            clist.createMetaContact(
                                pps, group, uin);
                        }
                        catch (MetaContactListException ex)
                        {
                            logger.error(ex);
                            ex.printStackTrace();
                            int errorCode = ex.getErrorCode();

                            if (errorCode
                                    == MetaContactListException
                                        .CODE_CONTACT_ALREADY_EXISTS_ERROR)
                            {
                                new ErrorDialog(mainFrame,
                                    GuiActivator.getResources().getI18NString(
                                    "addContactErrorTitle"),
                                    GuiActivator.getResources().getI18NString(
                                            "service.gui.ADD_CONTACT_EXIST_ERROR",
                                            new String[]{uin}),
                                    ex)
                                .showDialog();
                            }
                            else if (errorCode
                                    == MetaContactListException
                                        .CODE_NETWORK_ERROR)
                            {
                                new ErrorDialog(mainFrame,
                                    GuiActivator.getResources().getI18NString(
                                    "addContactErrorTitle"),
                                    GuiActivator.getResources().getI18NString(
                                        "service.gui.ADD_CONTACT_NETWORK_ERROR",
                                        new String[]{uin}),
                                    ex)
                                .showDialog();
                            }
                            else
                            {
                                new ErrorDialog(mainFrame,
                                    GuiActivator.getResources().getI18NString(
                                    "addContactErrorTitle"),
                                    GuiActivator.getResources().getI18NString(
                                            "service.gui.ADD_CONTACT_ERROR",
                                            new String[]{uin}),
                                    ex)
                                .showDialog();
                            }
                        }
                       ConfigurationManager.
                           setLastContactParent(group.getGroupName());
                    }
                }.start();
            }
            this.dispose();
        }
        else
        {
            this.dispose();
        }
    }

    protected void close(boolean isEscaped)
    {
        this.cancelButton.doClick();
    }

    /**
     * Shows this dialog and requests the default focus in the name field.
     */
    public void showDialog()
    {
        this.setVisible(true);
        
        this.addContactPanel.requestFocusInField();
    }
}
