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
package net.java.sip.communicator.impl.gui.main.contactlist.addgroup;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>CreateGroupDialog</tt> is the dialog containing the form for creating
 * a group.
 * @author Yana Stamcheva
 */
public class CreateGroupDialog
    extends SIPCommDialog
    implements ActionListener, WindowFocusListener
{
    private final Logger logger
        = Logger.getLogger(CreateGroupDialog.class.getName());

    private CreateGroupPanel groupPanel = new CreateGroupPanel();

    private JButton addButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.CREATE"));

    private JButton cancelButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.CANCEL"));

    private TransparentPanel buttonsPanel
        = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));

    private TransparentPanel mainPanel
        = new TransparentPanel(new BorderLayout());

    private MetaContactListService clist;

    /**
     * The newly created <tt>MetaContactGroup</tt>.
     */
    private MetaContactGroup newMetaGroup;

    /**
     * Creates an instance of <tt>CreateGroupDialog</tt> that represents a dialog
     * that creates a new contact group.
     *
     * @param parentWindow the parent dialog
     */
    public CreateGroupDialog(Dialog parentWindow)
    {
        this(parentWindow, true);
    }

    /**
     * Creates an instance of <tt>CreateGroupDialog</tt> that represents a dialog
     * that creates a new contact group.
     *
     * @param parentWindow the parent frame
     */
    public CreateGroupDialog(Frame parentWindow)
    {
        this(parentWindow, true);
    }

    /**
     * Creates an instance of <tt>CreateGroupDialog</tt> that represents a dialog
     * that creates a new contact group.
     *
     * @param parentWindow the parent dialog
     * @param isSaveSizeAndLocation indicates if this dialog should remember its
     * size and location
     */
    public CreateGroupDialog(Dialog parentWindow, boolean isSaveSizeAndLocation)
    {
        super(parentWindow, isSaveSizeAndLocation);

        this.clist = GuiActivator.getContactListService();

        this.init();
    }

    /**
     * Creates an instance of <tt>CreateGroupDialog</tt> that represents a dialog
     * that creates a new contact group.
     *
     * @param parentWindow the parent frame
     * @param isSaveSizeAndLocation indicates if this dialog should remember its
     * size and location
     */
    public CreateGroupDialog(Frame parentWindow, boolean isSaveSizeAndLocation)
    {
        super(parentWindow, isSaveSizeAndLocation);

        this.clist = GuiActivator.getContactListService();

        this.init();
    }

    /**
     * Initializes the dialog.
     */
    private void init()
    {
        this.setTitle(
            GuiActivator.getResources().getI18NString("service.gui.CREATE_GROUP"));

        this.getRootPane().setDefaultButton(addButton);
        this.addButton.setName("create");
        this.cancelButton.setName("cancel");

        this.addButton.addActionListener(this);
        this.cancelButton.addActionListener(this);

        this.addButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.CREATE"));
        this.cancelButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.CANCEL"));

        this.buttonsPanel.add(addButton);
        this.buttonsPanel.add(cancelButton);

        this.mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        this.mainPanel.add(groupPanel, BorderLayout.CENTER);
        this.mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

        this.getContentPane().add(mainPanel);
        this.addWindowFocusListener(this);
    }

    /**
     * Performs needed actions when one of the buttons is pressed.
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        JButton button = (JButton)e.getSource();
        String name = button.getName();

        if (name.equals("create"))
        {
            String groupName = groupPanel.getGroupName().trim();
            if (groupName.length() == 0)
            {
                     groupPanel.showErrorMessage(
                        GuiActivator.getResources().getI18NString(
                                "service.gui.ADD_GROUP_EMPTY_NAME",
                                new String[]{groupName}));
            }
            else
                new CreateGroup(clist, groupName).start();
        }
        else if(name.equals("cancel"))
        {
            dispose();
        }
    }

    /**
     * Indicates that the window has gained the focus. Requests the focus in
     * the text field.
     * @param e the <tt>WindowEvent</tt> that notified us
     */
    public void windowGainedFocus(WindowEvent e)
    {
        this.groupPanel.requestFocusInField();
    }

    public void windowLostFocus(WindowEvent e) {}

    /**
     * Creates a new meta contact group in a separate thread.
     */
    private class CreateGroup extends Thread
    {
        MetaContactListService mcl;
        String groupName;

        CreateGroup(MetaContactListService mcl, String groupName)
        {
            this.mcl = mcl;
            this.groupName = groupName;
        }

        @Override
        public void run()
        {
            try
            {
                newMetaGroup = mcl.createMetaContactGroup(
                    mcl.getRoot(), groupName);

                dispose();
            }
            catch (MetaContactListException ex)
            {
                logger.error(ex);
                int errorCode = ex.getErrorCode();

                if (errorCode
                        == MetaContactListException
                            .CODE_GROUP_ALREADY_EXISTS_ERROR)
                {
                    groupPanel.showErrorMessage(
                        GuiActivator.getResources().getI18NString(
                                "service.gui.ADD_GROUP_EXIST_ERROR",
                                new String[]{groupName}));
                }
                else if (errorCode
                    == MetaContactListException.CODE_LOCAL_IO_ERROR)
                {
                    groupPanel.showErrorMessage(
                        GuiActivator.getResources().getI18NString(
                                "service.gui.ADD_GROUP_LOCAL_ERROR",
                                new String[]{groupName}));
                }
                else if (errorCode
                        == MetaContactListException.CODE_NETWORK_ERROR)
                {
                    groupPanel.showErrorMessage(
                        GuiActivator.getResources().getI18NString(
                                "service.gui.ADD_GROUP_NET_ERROR",
                                new String[]{groupName}));
                }
                else
                {
                    groupPanel.showErrorMessage(
                        GuiActivator.getResources().getI18NString(
                                "service.gui.ADD_GROUP_ERROR",
                                new String[]{groupName}));
                }
            }
        }
    }

    /**
     * Returns the newly created <tt>MetaContactGroup</tt> if everything is
     * gone well, otherwise returns null.
     * @return the newly created <tt>MetaContactGroup</tt>
     */
    public MetaContactGroup getNewMetaGroup()
    {
        return newMetaGroup;
    }

    /**
     * Cancels the application if the window is closed.
     * @param isEscaped indicates if the window has been closed by pressing the
     * Esc button.
     */
    @Override
    protected void close(boolean isEscaped)
    {
        this.cancelButton.doClick();
    }
}
