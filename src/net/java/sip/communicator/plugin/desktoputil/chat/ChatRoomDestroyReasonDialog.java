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
package net.java.sip.communicator.plugin.desktoputil.chat;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;

/**
 * Dialog with fields for reason and alternate address.
 *
 * @author Hristo Terezov
 */
public class ChatRoomDestroyReasonDialog extends MessageDialog
{
    /**
     * The <tt>Logger</tt> used by the <tt>ChatRoomDestroyReasonDialog</tt>
     * class and its instances for logging output.
     */
    private static Logger logger
        = Logger.getLogger(ChatRoomDestroyReasonDialog.class);

    /**
     * Serial id.
     */
    private static final long serialVersionUID = -916498752420264164L;

    /**
     * Text field for the alternate address.
     */
    private SIPCommTextField alternateAddress
        = new SIPCommTextField("chatroom@example.com");

    /**
     * Text field for reason text.
     */
    private JTextField reasonField = new JTextField();

    /**
     * Constructs new chat room destroy dialog.
     *
     * @param title the title of the dialog
     * @param message the message shown in this dialog
     */
    public ChatRoomDestroyReasonDialog(String title, String message)
    {
        super(null, title, message,
            DesktopUtilActivator.getResources().getI18NString("service.gui.OK"),
            false);
        this.setIcon((ImageIcon)null);

        alternateAddress.setFont(alternateAddress.getFont().deriveFont(12f));

        JLabel altAddressLabel
            = new JLabel(DesktopUtilActivator.getResources()
                .getI18NString("service.gui.ALTERNATE_ADDRESS") + ":");

        JLabel reasonLabel
            = new JLabel(DesktopUtilActivator.getResources()
                .getI18NString("service.gui.REASON") + ":");

        JPanel labelsPanel = new JPanel(new GridLayout(2, 1));
        labelsPanel.add(reasonLabel);
        labelsPanel.add(altAddressLabel);

        JPanel valuesPanel = new JPanel(new GridLayout(2, 1));
        valuesPanel.add(reasonField);
        valuesPanel.add(alternateAddress);

        JPanel fieldsPanel = new JPanel(new BorderLayout());
        fieldsPanel .add(labelsPanel, BorderLayout.WEST);

        fieldsPanel.add(valuesPanel, BorderLayout.CENTER);
        fieldsPanel.add(new JLabel("          "), BorderLayout.EAST);
        fieldsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        fieldsPanel.setOpaque(false);



        replaceCheckBoxPanel(fieldsPanel);
        this.pack();
    }

   /**
    * Returns the text entered in the alternate address field.
    *
    * @return the text from the alternate address field.
    */
   public String getAlternateAddress()
   {
       return alternateAddress.getText();
   }

   /**
    * Returns the text entered in the reason field.
    *
    * @return the text from the reason field.
    */
   public String getReason()
   {
       return reasonField.getText();
   }

   /**
    * Opens a dialog with a fields for the reason and alternate address and
    * returns them.
    *
    * @return array with the reason and alternate address values.
    */
   public static String[] getDestroyOptions()
   {
       final ChatRoomDestroyReasonDialog[] res
           = new ChatRoomDestroyReasonDialog[1];

       try
       {
           SwingUtilities.invokeAndWait(new Runnable()
           {
               @Override
               public void run()
               {
                   ResourceManagementService R
                       = DesktopUtilActivator.getResources();

                   res[0] = new ChatRoomDestroyReasonDialog(
                       R.getI18NString("service.gui.DESTROY_CHATROOM"),
                       R.getI18NString("service.gui.DESTROY_MESSAGE"));
               }
           });
       }
       catch(Throwable t)
       {
           logger.error("Error creating dialog", t);
           return null;
       }

       ChatRoomDestroyReasonDialog reasonDialog = res[0];

       int result = reasonDialog.showDialog();

       String destroyOptions[] = new String[2];

       if (result == MessageDialog.OK_RETURN_CODE)
       {
           destroyOptions[0] = proccessFieldValues(reasonDialog.getReason());
           destroyOptions[1]
               = proccessFieldValues(reasonDialog.getAlternateAddress());
       }
       else
       {
           destroyOptions = null;
       }


       return destroyOptions;
   }

   private static String proccessFieldValues(String value)
   {
       if(value != null)
       {
           value = value.trim();
           if(value.equals(""))
               value = null;
       }
       return value;
   }

}
