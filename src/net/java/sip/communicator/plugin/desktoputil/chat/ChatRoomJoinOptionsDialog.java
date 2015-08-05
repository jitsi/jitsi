/**
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
package net.java.sip.communicator.plugin.desktoputil.chat;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Dialog with fields for nickname and subject.
 *
 * @author Hristo Terezov
 */
public class ChatRoomJoinOptionsDialog extends ChatOperationReasonDialog
{
    /**
     * Serial id.
     */
    private static final long serialVersionUID = -916498752420264164L;

    /** 
     * Text field for the subject.
     */
    private SIPCommTextField subject = new SIPCommTextField(DesktopUtilActivator
        .getResources().getI18NString("service.gui.SUBJECT"));
    
    /**
     * Label that hides and shows the subject fields panel on click.
     */
    private JLabel cmdExpandSubjectFields;
    
    /**
     * Panel that holds the subject fields.
     */
    private JPanel subjectFieldsPannel = new JPanel(new BorderLayout());
    
    /**
     * Adds the subject fields to dialog. Sets action listeners.
     * 
     * @param title the title of the dialog
     * @param message the message shown in this dialog
     * @param disableOKIfReasonIsEmpty if true the OK button will be 
     * disabled if the reason text is empty.
     * @param showReasonLabel specify if we want the "Reason:" label
     * @param dontDisplaySubjectFields if true the sibject fields will be 
     * hidden.
     */
    public ChatRoomJoinOptionsDialog(String title, String message,
        boolean showReasonLabel,
        boolean disableOKIfReasonIsEmpty,
        boolean dontDisplaySubjectFields)
    {
        super(title,
            message,
            showReasonLabel,
            disableOKIfReasonIsEmpty);
        
        if(dontDisplaySubjectFields)
            return;
        
        JPanel subjectPanel = new JPanel(new BorderLayout());
        subjectPanel.setOpaque(false);
        subjectPanel.setBorder(
            BorderFactory.createEmptyBorder(10, 0, 0, 0));
        
        subjectFieldsPannel.setBorder(
            BorderFactory.createEmptyBorder(10, 30, 0, 0));
        subjectFieldsPannel.setOpaque(false);
        subjectFieldsPannel.add(subject, BorderLayout.CENTER);
        subjectFieldsPannel.setVisible(false);
        subject.setFont(getFont().deriveFont(12f));
        
        cmdExpandSubjectFields = new JLabel();
        cmdExpandSubjectFields.setBorder(new EmptyBorder(0, 5, 0, 0));
        cmdExpandSubjectFields.setIcon(DesktopUtilActivator.getResources()
            .getImage("service.gui.icons.RIGHT_ARROW_ICON"));
        cmdExpandSubjectFields.setText(DesktopUtilActivator
            .getResources().getI18NString("service.gui.SET_SUBJECT"));
        cmdExpandSubjectFields.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                cmdExpandSubjectFields.setIcon(
                        UtilActivator.getResources().getImage(
                            subjectFieldsPannel.isVisible()
                                    ? "service.gui.icons.RIGHT_ARROW_ICON"
                                    : "service.gui.icons.DOWN_ARROW_ICON"));

                subjectFieldsPannel.setVisible(
                    !subjectFieldsPannel.isVisible());

                pack();
            }
        });
        subjectPanel.add(cmdExpandSubjectFields,BorderLayout.NORTH);
        subjectPanel.add(subjectFieldsPannel,BorderLayout.CENTER);
        addToReasonFieldPannel(subjectPanel);
        this.pack();
    }

   /**
    * Returns the text entered in the subject field.
    * 
    * @return the text from the subject field.
    */
   public String getSubject()
   {
       return subject.getText();
   }
   
   /**
    * Opens a dialog with a fields for the nickname and the subject of the room
    *  and returns them.
    *
    * @param pps the protocol provider associated with the chat room.
    * @param chatRoomId the id of the chat room.
    * @param defaultNickname the nickname to show if any
    * @return array with the nickname and subject values.
    */
   public static String[] getJoinOptions(ProtocolProviderService pps, 
       String chatRoomId, String defaultNickname)
   {
       return getJoinOptions(false, pps, chatRoomId, defaultNickname);
   }

   /**
    * Opens a dialog with a fields for the nickname and the subject of the room
    *  and returns them.
    *
    * @param dontDisplaySubjectFields if true the subject fields will be hidden
    * @param pps the protocol provider associated with the chat room.
    * @param chatRoomId the id of the chat room.
    * @param defaultNickname the nickname to show if any
    * @return array with the nickname and subject values.
    */
   public static String[] getJoinOptions(boolean dontDisplaySubjectFields,
       ProtocolProviderService pps, String chatRoomId, String defaultNickname)
   {
       String nickName = null;
       ChatRoomJoinOptionsDialog reasonDialog =
           new ChatRoomJoinOptionsDialog(DesktopUtilActivator.getResources()
               .getI18NString("service.gui.CHANGE_NICKNAME"), 
               DesktopUtilActivator.getResources().getI18NString(
                   "service.gui.CHANGE_NICKNAME_LABEL"), false, true, 
                   dontDisplaySubjectFields);
       reasonDialog.setIcon(new ImageIcon(DesktopUtilActivator.getImage(
           "service.gui.icons.CHANGE_NICKNAME_16x16")));

       if(defaultNickname != null)
            reasonDialog.setReasonFieldText(defaultNickname);
       
       int result = reasonDialog.showDialog();

       if (result == MessageDialog.OK_RETURN_CODE)
       {
           nickName = reasonDialog.getReason().trim();
           ConfigurationUtils.updateChatRoomProperty(
               pps,
               chatRoomId, "userNickName", nickName);
           
       }
       String[] joinOptions = {nickName, reasonDialog.getSubject()};
       return joinOptions;
   }
   
}
