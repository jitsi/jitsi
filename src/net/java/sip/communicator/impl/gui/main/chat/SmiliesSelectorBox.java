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

import net.java.sip.communicator.impl.gui.*;
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
    implements ActionListener
{
    private ChatWritePanel chatWritePanel;

    private ArrayList imageList;

    private int gridRowCount = 0;

    private int gridColCount = 0;

    private SIPCommMenu selectorBox = new SIPCommMenu();

    private static int BUTTON_HEIGHT
        = GuiActivator.getResources().getSettingsInt("mainToolbarButtonHeight");

    private static int BUTTON_WIDTH
        = GuiActivator.getResources().getSettingsInt("mainToolbarButtonWidth");

    private SelectorBoxRolloverListener rolloverListener
        = new SelectorBoxRolloverListener();

    /**
     * Creates an instance of this <tt>SmiliesSelectorBox</tt> and initializes
     * the panel with the smiley icons given by the incoming imageList.
     * 
     * @param imageList The pack of smiley icons.
     */
    public SmiliesSelectorBox(ArrayList imageList, ChatWritePanel writePanel)
    {
        this.imageList = imageList;

        this.chatWritePanel = writePanel;

        this.selectorBox.setUI(new SIPCommChatSelectorMenuUI());

        this.setOpaque(false);
        this.selectorBox.setOpaque(false);
        this.setPreferredSize(
            new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));

        //Should explicitly remove any border in order to align correctly the
        //icon.
        this.selectorBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        this.selectorBox.setIcon(new ImageIcon(ImageLoader
            .getImage(ImageLoader.SMILIES_ICON)));

        this.calculateGridDimensions(imageList.size());

        this.selectorBox.getPopupMenu().setLayout(new GridLayout(
                this.gridRowCount, this.gridColCount, 5, 5));

        for (int i = 0; i < imageList.size(); i++) {

            Smiley smiley = (Smiley) this.imageList.get(i);

            ImageIcon imageIcon
                = new ImageIcon(ImageLoader.getImage(smiley.getImageID()));

            JMenuItem smileyItem = new JMenuItem (imageIcon);

//            smileyItem.setPreferredSize(
//                new Dimension(  imageIcon.getIconWidth(),
//                                imageIcon.getIconHeight()));

//            smileyItem.setMargin(new Insets(2, 2, 2, 2));

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

        for (int i = 0; i < this.imageList.size(); i++)
        {
            Smiley smiley = (Smiley) this.imageList.get(i);

            String smileyString = smiley.getSmileyStrings()[0];

            if (buttonText.equals(smileyString))
            {
                chatWritePanel.appendText(smileyString);

                chatWritePanel.getEditorPane().requestFocus();
            }
        }
    }

    /**
     * Sets the given text to this smiley selector box. The given text will be
     * position by default on the bottom of the icon.
     * 
     * @param text the text to be added to this selector box.
     */
    public void setText(String text)
    {
        this.selectorBox.setText(text);

        this.selectorBox.setFont(getFont().deriveFont(Font.BOLD, 10f));
        this.selectorBox.setVerticalTextPosition(SwingConstants.BOTTOM);
        this.selectorBox.setHorizontalTextPosition(SwingConstants.CENTER);
        this.selectorBox.setForeground(
            new Color(GuiActivator.getResources().
                getColor("chatMenuForeground")));
    }

    /**
     * Enables or disabled the roll-over effect, when user moves the mouse over
     * this smilies selector box.
     * 
     * @param isRollover <code>true</code> to enable the roll-over,
     * <code>false</code> - otherwise.
     */
    public void setRollover(boolean isRollover)
    {
        if(isRollover)
            selectorBox.addMouseListener(rolloverListener);
        else
            selectorBox.removeMouseListener(rolloverListener);
    }

    /**
     * Handles <tt>MouseEvent</tt>s and changes the state of the contained
     * selector box in order to make a roll-over effect.
     */
    private class SelectorBoxRolloverListener extends MouseAdapter
    {
        public void mouseEntered(MouseEvent e)
        {
            selectorBox.setMouseOver(true);
        }

        public void mouseExited(MouseEvent e)
        {
            selectorBox.setMouseOver(false);
        }
    }
}
