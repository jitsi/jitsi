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
import javax.swing.event.*;

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
 * @author Lubomir Marinov
 */
public class SmileysSelectorBox
    extends SIPCommMenuBar
    implements ActionListener,
               MouseListener,
               PopupMenuListener
{
    private final ChatWritePanel chatWritePanel;

    private final SIPCommMenu selectorBox = new SIPCommMenu();

    private final JLabel smileyTextLabel = new JLabel();
    private final JLabel smileyDescriptionLabel = new JLabel();

    /**
     * Initializes a new <tt>SmileysSelectorBox</tt> instance.
     * 
     * @param writePanel the <tt>ChatWritePanel</tt> the new instance is to
     * write the selected <tt>Smiley</tt> into when it is clicked
     */
    public SmileysSelectorBox(ChatWritePanel writePanel)
    {
        this.chatWritePanel = writePanel;

        this.setOpaque(false);
        this.selectorBox.setOpaque(false);

        // Should explicitly remove any border in order to align correctly the
        // icon.
        this.selectorBox.setBorder(BorderFactory.createEmptyBorder());
        this.selectorBox.setIcon(new ImageIcon(ImageLoader
            .getImage(ImageLoader.SMILIES_ICON)));

        JPopupMenu popupMenu = this.selectorBox.getPopupMenu();

        popupMenu.setLayout(new GridBagLayout());
        popupMenu.setBackground(Color.WHITE);

        /*
         * Load the smileys and the UI which represents them on demand because
         * they are not always necessary.
         */
        popupMenu.addPopupMenuListener(this);

        this.add(selectorBox);
    }

    /**
     * In order to have a popup which is at the form closest to square.
     * 
     * @param itemsCount the count of items that will be laid out.
     * @return the dimensions of the grid
     */
    private Dimension calculateGridDimensions(int itemsCount)
    {
        int gridRowCount = (int) Math.round(Math.sqrt(itemsCount));

        /*
         * FIXME The original code was "(int)Math.ceil(itemsCount/gridRowCount)".
         * But it was unnecessary because both itemsCount and gridRowCount are
         * integers and, consequently, itemsCount/gridRowCount gives an integer.
         * Was the intention to have the division produce a real number?
         */
        int gridColCount = itemsCount / gridRowCount;

        return new Dimension(gridColCount, gridRowCount);
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
        SmileyMenuItem smileyItem = (SmileyMenuItem) e.getSource();
        Smiley smiley = smileyItem.smiley;

        chatWritePanel.appendText(smiley.getDefaultString());

        chatWritePanel.getEditorPane().requestFocus();

        clearMouseOverEffects(smileyItem);
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
    private static class SmileyMenuItem
        extends JMenuItem
    {

        /**
         * The <tt>Smiley</tt> depicted by this instance.
         */
        public final Smiley smiley;

        /**
         * Initializes a new <tt>SmileyMenuItem</tt> instance which is to depict
         * a specific <tt>Smiley</tt>.
         * 
         * @param smiley the <tt>Smiley</tt> to be depicted by the new instance
         */
        public SmileyMenuItem(Smiley smiley)
        {
            super(new ImageIcon(ImageLoader.getImage(smiley.getImageID())));
            this.setUI(new SIPCommMenuItemUI());

            this.smiley = smiley;
        }
    }

    /**
     * Changes the static image of the underlying smiley with a dynamic one.
     * Also shows the description and smiley string in the description area.
     */
    public void mouseEntered(MouseEvent e)
    {
        SmileyMenuItem smileyItem = (SmileyMenuItem) e.getSource();
        Smiley smiley = smileyItem.smiley;

        ImageIcon imageIcon
            = GuiActivator.getResources().getImage(smiley.getImageID().getId());
        smileyItem.setIcon(imageIcon);

        smileyDescriptionLabel.setText(smiley.getDescription());
        smileyTextLabel.setText(smiley.getDefaultString());
    }

    /**
     * Clears all mouse over effects when the mouse has exited the smiley area.
     */
    public void mouseExited(MouseEvent e)
    {
        SmileyMenuItem smileyItem = (SmileyMenuItem) e.getSource();

        this.clearMouseOverEffects(smileyItem);
    }

    public void mouseClicked(MouseEvent e)
    {
    }

    public void mousePressed(MouseEvent e)
    {
    }

    public void mouseReleased(MouseEvent e)
    {
    }

    /**
     * Clears all mouse over effects for the given smiley item. This method
     * should be invoked when the mouse has exited the smiley area or when
     * a smiley has been selected and the popup menu is closed.
     * 
     * @param smileyItem the item for which we clear mouse over effects.
     */
    private void clearMouseOverEffects(SmileyMenuItem smileyItem)
    {
        ImageIcon imageIcon =
            new ImageIcon(ImageLoader.getImage(smileyItem.smiley.getImageID()));

        smileyItem.setIcon(imageIcon);
        smileyTextLabel.setText("");
        smileyDescriptionLabel.setText("");
    }

    /*
     * Implements PopupMenuListener#popupMenuCanceled(PopupMenuEvent). Does
     * nothing.
     */
    public void popupMenuCanceled(PopupMenuEvent e)
    {
    }

    /*
     * Implements
     * PopupMenuListener#popupMenuWillBecomeInvisible(PopupMenuEvent). Does
     * nothing.
     */
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
    {
    }

    /*
     * Implements PopupMenuListener#popupMenuWillBecomeVisible(PopupMenuEvent).
     * Loads the smileys and creates the UI to represent them when they are
     * first necessary.
     */
    public void popupMenuWillBecomeVisible(PopupMenuEvent e)
    {
        JPopupMenu popupMenu = (JPopupMenu) e.getSource();

        // Don't populate it again if it's already populated.
        if (popupMenu.getComponentIndex(smileyTextLabel) != -1)
            return;

        Collection<Smiley> imageList = ImageLoader.getDefaultSmileyPack();

        Dimension gridDimensions
            = this.calculateGridDimensions(imageList.size());
        int gridColCount = gridDimensions.width;
        int gridRowCount = gridDimensions.height;
        GridBagConstraints gridBagConstraints = new GridBagConstraints();

        int smileyIndex = 0;
        for (Smiley smiley : imageList)
        {
            SmileyMenuItem smileyItem = new SmileyMenuItem(smiley);

            smileyItem.setPreferredSize(new Dimension(36, 36));

            smileyItem.addActionListener(this);
            smileyItem.addMouseListener(this);

            gridBagConstraints.anchor = GridBagConstraints.EAST;
            gridBagConstraints.gridx = smileyIndex % gridColCount;
            gridBagConstraints.gridy = smileyIndex % gridRowCount;

            popupMenu.add(smileyItem, gridBagConstraints);

            smileyIndex++;
        }

        smileyDescriptionLabel.setBorder(
            BorderFactory.createEmptyBorder(0, 5, 0, 0));

        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridRowCount;
        gridBagConstraints.gridwidth = gridColCount;

        popupMenu.add(smileyDescriptionLabel, gridBagConstraints);

        smileyTextLabel.setBorder(
            BorderFactory.createEmptyBorder(0, 0, 0, 5));
        smileyTextLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        smileyTextLabel.setPreferredSize(new Dimension(50, 25));

        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.gridx = gridColCount/2;
        gridBagConstraints.gridy = gridRowCount;

        popupMenu.add(smileyTextLabel, gridBagConstraints);
    }
}
