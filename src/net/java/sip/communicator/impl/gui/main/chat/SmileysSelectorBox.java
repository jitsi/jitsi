/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.lookandfeel.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>SmileysSelectorBox</tt> is the component where user could choose a
 * smiley icon to send.
 * 
 * @author Yana Stamcheva
 */
public class SmileysSelectorBox
    extends SIPCommMenuBar
    implements  ActionListener,
                MouseListener
{
    private final ChatWritePanel chatWritePanel;

    private final Hashtable<JMenuItem, Smiley> smileysList
        = new Hashtable<JMenuItem, Smiley>();

    private int gridRowCount = 0;

    private int gridColCount = 0;

    private final SIPCommMenu selectorBox = new SIPCommMenu();

    private final GridBagConstraints gridBagConstraints
        = new GridBagConstraints();

    private final JLabel smileyTextLabel = new JLabel();
    private final JLabel smileyDescriptionLabel = new JLabel();

    /**
     * Creates an instance of this <tt>SmileysSelectorBox</tt> and initializes
     * the panel with the smiley icons given by the incoming imageList.
     * 
     * @param imageList The pack of smiley icons.
     */
    public SmileysSelectorBox(Collection<Smiley> imageList,
        ChatWritePanel writePanel)
    {
        this.chatWritePanel = writePanel;

        this.setOpaque(false);
        this.selectorBox.setOpaque(false);

        // Should explicitly remove any border in order to align correctly the
        // icon.
        this.selectorBox.setBorder(BorderFactory.createEmptyBorder());
        this.selectorBox.setIcon(new ImageIcon(ImageLoader
            .getImage(ImageLoader.SMILIES_ICON)));

        this.calculateGridDimensions(imageList.size());

        JPopupMenu popupMenu = this.selectorBox.getPopupMenu();

        popupMenu.setLayout(new GridBagLayout());
        popupMenu.setBackground(Color.WHITE);

        int count = 0;
        for (Smiley smiley : imageList)
        {
            this.addSmileyToGrid(smiley, count);

            count++;
        }

        smileyTextLabel.setPreferredSize(new Dimension(50, 25));
        smileyTextLabel.setBorder(
            BorderFactory.createEmptyBorder(0, 0, 0, 5));
        smileyDescriptionLabel.setBorder(
            BorderFactory.createEmptyBorder(0, 5, 0, 0));

        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridRowCount;
        gridBagConstraints.gridwidth = gridColCount;

        popupMenu.add(smileyDescriptionLabel, gridBagConstraints);

        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.gridx = gridColCount/2;
        gridBagConstraints.gridy = gridRowCount;

        smileyTextLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        popupMenu.add(smileyTextLabel, gridBagConstraints);

        this.add(selectorBox);
    }

    /**
     * In order to have a popup which is at the form closest to sqware.
     * 
     * @param itemsCount the count of items that will be laied out.
     */
    private void calculateGridDimensions(int itemsCount)
    {
        this.gridRowCount = (int) Math.round(Math.sqrt(itemsCount));

        /*
         * FIXME The original code was "(int)Math.ceil(itemsCount/gridRowCount)".
         * But it was unnecessary because both itemsCount and gridRowCount are
         * integers and, consequently, itemsCount/gridRowCount gives an integer.
         * Was the intention to have the division produce a real number?
         */
        this.gridColCount = itemsCount / gridRowCount;
    }

    /**
     * Adds the given smiley to the grid of the selector box popup menu.
     * 
     * @param smiley the smiley to add
     * @param smileyIndex the index of the smiley in the table
     */
    private void addSmileyToGrid(   Smiley smiley,
                                    int smileyIndex)
    {
        ImageIcon imageIcon =
            new ImageIcon(ImageLoader.getImage(smiley.getImageID()));

        SmileyMenuItem smileyItem = new SmileyMenuItem(imageIcon);

        smileyItem.setPreferredSize(new Dimension(36, 36));

        smileyItem.addActionListener(this);
        smileyItem.addMouseListener(this);

        gridBagConstraints.anchor = GridBagConstraints.EAST;
        gridBagConstraints.gridx = smileyIndex%gridColCount;
        gridBagConstraints.gridy = smileyIndex%gridRowCount;

        selectorBox.getPopupMenu().add(smileyItem, gridBagConstraints);

        smileysList.put(smileyItem, smiley);
    }

    /**
     * Opens the smileys selector box.
     */
    public void open()
    {
        this.selectorBox.doClick();
    }

    /**
     * Returns TRUE if the selector box is opened, otherwise returns FALSE.
     * 
     * @return TRUE if the selector box is opened, otherwise returns FALSE
     */
    public boolean isMenuSelected()
    {
        return selectorBox.isPopupMenuVisible();
    }

    /**
     * Writes the symbol corresponding to a chosen smiley icon to the write
     * message area at the end of the current text.
     */
    public void actionPerformed(ActionEvent e)
    {
        JMenuItem smileyItem = (JMenuItem) e.getSource();

        Smiley smiley = smileysList.get(smileyItem);

        chatWritePanel.appendText(smiley.getSmileyStrings()[0]);

        chatWritePanel.getEditorPane().requestFocus();

        clearMouseOverEffects(smileyItem, smiley);
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
        this.selectorBox.setForeground(new Color(GuiActivator.getResources()
            .getColor("service.gui.CHAT_MENU_FOREGROUND")));
    }

    public void paintComponent(Graphics g)
    {
    }

    /**
     * A custom menu item, which paints round border over selection.
     */
    private class SmileyMenuItem extends JMenuItem
    {
        public SmileyMenuItem(Icon imageIcon)
        {
            super(imageIcon);
            this.setUI(new SIPCommMenuItemUI());
        }
    }

    /**
     * Changes the static image of the underlying smiley with a dynamic one.
     * Also shows the description and smiley string in the description area.
     */
    public void mouseEntered(MouseEvent e)
    {
        JMenuItem smileyItem = (JMenuItem) e.getSource();

        Smiley smiley = smileysList.get(smileyItem);

        String smileyIconHtml = "<HTML><IMG SRC=\""
            + ImageLoader.getSmiley(smiley.getDefaultString()).getImagePath()
            + "\"></IMG></HTML>";
        smileyItem.setIcon(null);
        smileyItem.setText(smileyIconHtml);

        smileyDescriptionLabel.setText(smiley.getDescription());
        smileyTextLabel.setText(smiley.getSmileyStrings()[0]);
    }

    /**
     * Clears all mouse over effects when the mouse has exited the smiley area.
     */
    public void mouseExited(MouseEvent e)
    {
        JMenuItem smileyItem = (JMenuItem) e.getSource();
        Smiley smiley = smileysList.get(smileyItem);

        this.clearMouseOverEffects(smileyItem, smiley);
    }

    public void mouseClicked(MouseEvent e)
    {}

    public void mousePressed(MouseEvent e)
    {}

    public void mouseReleased(MouseEvent e)
    {}

    /**
     * Clears all mouse over effects for the given smiley item. This method
     * should be invoked when the mouse has exited the smiley area or when
     * a smiley has been selected and the popup menu is closed.
     * 
     * @param smileyItem the item for which we clear mouse over effects.
     */
    private void clearMouseOverEffects(JMenuItem smileyItem, Smiley smiley)
    {
        ImageIcon imageIcon =
            new ImageIcon(ImageLoader.getImage(smiley.getImageID()));

        smileyItem.setIcon(imageIcon);
        smileyItem.setText(null);
        smileyTextLabel.setText("");
        smileyDescriptionLabel.setText("");
    }
}
