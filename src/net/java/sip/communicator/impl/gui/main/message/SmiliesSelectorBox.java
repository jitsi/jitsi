/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.message;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JEditorPane;

import net.java.sip.communicator.impl.gui.main.customcontrols.BoxPopupMenu;
import net.java.sip.communicator.impl.gui.main.customcontrols.MsgToolbarButton;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;
import net.java.sip.communicator.impl.gui.utils.Smiley;

/**
 * SmiliesSelectorBox is the component where user could choose a smily to send.
 * 
 * @author Yana Stamcheva
 */
public class SmiliesSelectorBox extends BoxPopupMenu implements ActionListener {

    private ChatWindow messageWindow;

    private ArrayList imageList;

    /**
     * Creates an instance of this SmiliesSelectorBox and initializes the panel
     * with the smily icons given by the incoming imageList.
     * 
     * @param imageList The pack of smily icons.
     */
    public SmiliesSelectorBox(ArrayList imageList) {

        super(imageList.size());

        this.imageList = imageList;

        for (int i = 0; i < imageList.size(); i++) {

            Smiley smiley = (Smiley) this.imageList.get(i);

            MsgToolbarButton imageButton = new MsgToolbarButton(
                    ImageLoader.getImage(smiley.getImageID()));

            imageButton.setToolTipText(smiley.getSmileyStrings()[0]);

            imageButton.addActionListener(this);

            this.add(imageButton);
        }

    }

    public void actionPerformed(ActionEvent e) {

        JButton imageButton = (JButton) e.getSource();
        String buttonText = imageButton.getToolTipText();

        for (int i = 0; i < this.imageList.size(); i++) {

            Smiley smiley = (Smiley) this.imageList.get(i);

            if (buttonText.equals(smiley.getSmileyStrings()[0])) {

                ChatPanel chatPanel = this.messageWindow
                        .getCurrentChatPanel();

                chatPanel.addTextInWriteArea(
                        smiley.getSmileyStrings()[0] + " ");

                chatPanel.requestFocusInWriteArea();
            }
        }
    }

    public ChatWindow getMessageWindow() {
        return messageWindow;
    }

    public void setMessageWindow(ChatWindow messageWindow) {
        this.messageWindow = messageWindow;
    }
}
