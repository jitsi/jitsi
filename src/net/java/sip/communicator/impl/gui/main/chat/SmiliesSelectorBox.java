/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.chat;

import java.util.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.lookandfeel.*;
import net.java.sip.communicator.impl.gui.utils.*;

/**
 * The <tt>SmiliesSelectorBox</tt> is the component where user could choose
 * a smiley icon to send.
 * 
 * @author Yana Stamcheva
 */
public class SmiliesSelectorBox extends JMenuBar
    implements ActionListener {

    private ChatWindow chatWindow;

    private ArrayList imageList;

    private int gridRowCount = 0;

    private int gridColCount = 0;
    
    private SIPCommMenu selectorBox = new SIPCommMenu();
    
    /**
     * Creates an instance of this <tt>SmiliesSelectorBox</tt> and initializes
     * the panel with the smiley icons given by the incoming imageList.
     * 
     * @param imageList The pack of smiley icons.
     */
    public SmiliesSelectorBox(ArrayList imageList, ChatWindow chatWindow) {

        this.imageList = imageList;

        this.chatWindow = chatWindow;
        
        this.selectorBox.setUI(new SIPCommChatSelectorMenuUI());
        
        this.selectorBox.setPreferredSize(new Dimension(24, 24));
        
        //Should explicetly remove any border in order to align correctly the
        //icon.
        this.selectorBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        
        this.selectorBox.setIcon(new ImageIcon(ImageLoader
            .getImage(ImageLoader.SMILIES_ICON)));
        
        this.calculateGridDimensions(imageList.size());

        this.selectorBox.getPopupMenu().setLayout(new GridLayout(
                this.gridRowCount, this.gridColCount, 5, 5));
                
        for (int i = 0; i < imageList.size(); i++) {

            Smiley smiley = (Smiley) this.imageList.get(i);

            JMenuItem smileyItem = new JMenuItem (
                    new ImageIcon(ImageLoader.getImage(smiley.getImageID())));
            
            smileyItem.setPreferredSize(new Dimension(28, 28));
            smileyItem.setMargin(new Insets(2, -5, 2, 0));
            
            smileyItem.setToolTipText(smiley.getSmileyStrings()[0]);

            smileyItem.addActionListener(this);

            this.selectorBox.add(smileyItem);
        }

        this.add(selectorBox);
    }
    
    /**
     * In order to have a popup which is at the form closest to sqware.
     * @param itemsCount the count of items that will be laied out.
     */
    private void calculateGridDimensions(int itemsCount) {

        this.gridRowCount = (int) Math.round(Math.sqrt(itemsCount));

        this.gridColCount = (int) Math.round(itemsCount / gridRowCount);
    }

    /**
     * Opens the smilies selector box.
     */
    public void open()
    {
        this.selectorBox.doClick();
    }

    /**
     * Returns TRUE if the selector box is opened, otherwise returns FALSE.
     * @return TRUE if the selector box is opened, otherwise returns FALSE
     */
    public boolean isMenuSelected()
    {
        if(selectorBox.isPopupMenuVisible())
            return true;
        
        return false;
    }

    /**
     * Writes the symbol corresponding to a choosen smiley icon to the write
     * message area at the end of the current text.
     */
    public void actionPerformed(ActionEvent e)
    {
        JMenuItem smileyItem = (JMenuItem) e.getSource();
        String buttonText = smileyItem.getToolTipText();

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
