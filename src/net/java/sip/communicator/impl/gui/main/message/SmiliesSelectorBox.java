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
import net.java.sip.communicator.impl.gui.main.customcontrols.ChatToolbarButton;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;
import net.java.sip.communicator.impl.gui.utils.Smiley;

/**
 * The <tt>SmiliesSelectorBox</tt> is the component where user could choose
 * a smiley icon to send.
 * 
 * @author Yana Stamcheva
 */
public class SmiliesSelectorBox extends BoxPopupMenu
    implements ActionListener {

    private ChatWindow chatWindow;

    private ArrayList imageList;

    /**
     * Creates an instance of this <tt>SmiliesSelectorBox</tt> and initializes
     * the panel with the smiley icons given by the incoming imageList.
     * 
     * @param imageList The pack of smiley icons.
     */
    public SmiliesSelectorBox(ArrayList imageList, ChatWindow chatWindow) {

        super(imageList.size());
        
        this.imageList = imageList;

        this.chatWindow = chatWindow;
        
        for (int i = 0; i < imageList.size(); i++) {

            Smiley smiley = (Smiley) this.imageList.get(i);

            ChatToolbarButton imageButton = new ChatToolbarButton(
                    ImageLoader.getImage(smiley.getImageID()));

            imageButton.setToolTipText(smiley.getSmileyStrings()[0]);

            imageButton.addActionListener(this);

            this.add(imageButton);
        }

    }

    /**
     * Writes the symbol corresponding to a choosen smiley icon to the write
     * message area at the end of the current text.
     */
    public void actionPerformed(ActionEvent e) {

        JButton imageButton = (JButton) e.getSource();
        String buttonText = imageButton.getToolTipText();

        for (int i = 0; i < this.imageList.size(); i++) {

            Smiley smiley = (Smiley) this.imageList.get(i);

            if (buttonText.equals(smiley.getSmileyStrings()[0])) {

                ChatPanel chatPanel = this.chatWindow
                        .getCurrentChatPanel();

                chatPanel.addTextInWriteArea(
                        smiley.getSmileyStrings()[0] + " ");

                chatPanel.requestFocusInWriteArea();
            }
        }
    }
}
