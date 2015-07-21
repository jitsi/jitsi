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
package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.lookandfeel.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.replacement.smilies.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The <tt>SmileysSelectorBox</tt> is the component where user could choose a
 * smiley icon to send.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 * @author Adam Netocny
 */
public class SmileysSelectorBox
    extends SIPCommMenu
    implements ActionListener,
               MouseListener,
               PopupMenuListener,
               Skinnable
{
    /**
     * The chat write panel.
     */
    private ChatPanel chatPanel;

    /**
     * The smiley text label.
     */
    private final JLabel smileyTextLabel = new JLabel();

    /**
     * The smiley description label.
     */
    private final JLabel smileyDescriptionLabel = new JLabel();

    /**
     * The smilies service.
     */
    private final SmiliesReplacementService smiliesService;

    /**
     * PopupMenu
     */
    private JPopupMenu popupMenu;

    /**
     * Initializes a new <tt>SmileysSelectorBox</tt> instance.
     */
    public SmileysSelectorBox()
    {
        this.setOpaque(false);
        // Should explicitly remove any border in order to align correctly the
        // icon.
        this.setBorder(BorderFactory.createEmptyBorder());

        popupMenu = this.getPopupMenu();

        popupMenu.setLayout(new GridBagLayout());
        popupMenu.setBackground(Color.WHITE);

        /*
         * Load the smileys and the UI which represents them on demand because
         * they are not always necessary.
         */
        popupMenu.addPopupMenuListener(this);

        this.smiliesService = GuiActivator.getSmiliesReplacementSource();

        loadSkin();
    }

    /**
     * Sets the chat panel, for which smilieys would be created.
     *
     * @param chatPanel the chat panel, for which smilieys would be created
     */
    public void setChat(ChatPanel chatPanel)
    {
        this.chatPanel = chatPanel;
    }

    /**
     * In order to have a popup which is at the form closest to square.
     *
     * @param itemsCount the count of items that will be laid out.
     * @return the dimensions of the grid
     */
    private Dimension calculateGridDimensions(int itemsCount)
    {
        int gridColCount = (int) Math.ceil(Math.sqrt(itemsCount));

        /*
         * FIXME The original code was "(int)Math.ceil(itemsCount/gridRowCount)".
         * But it was unnecessary because both itemsCount and gridRowCount are
         * integers and, consequently, itemsCount/gridRowCount gives an integer.
         * Was the intention to have the division produce a real number?
         */
        int gridRowCount = itemsCount / gridColCount;
        if (itemsCount > gridRowCount*gridColCount)
            gridColCount++;

        return new Dimension(gridColCount, gridRowCount);
    }

    /**
     * Opens the smileys selector box.
     */
    public void open()
    {
        this.doClick();
    }

    /**
     * Returns TRUE if the selector box is opened, otherwise returns FALSE.
     *
     * @return TRUE if the selector box is opened, otherwise returns FALSE
     */
    public boolean isMenuSelected()
    {
        return isPopupMenuVisible();
    }

    /**
     * Writes the symbol corresponding to a chosen smiley icon to the write
     * message area at the end of the current text.
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        SmileyMenuItem smileyItem = (SmileyMenuItem) e.getSource();
        Smiley smiley = smileyItem.smiley;

        chatPanel.getChatWritePanel().appendText(smiley.getDefaultString());

        chatPanel.getChatWritePanel().getEditorPane().requestFocus();

        clearMouseOverEffects(smileyItem);
    }

    /**
     * A custom menu item, which paints round border over selection.
     */
    private static class SmileyMenuItem
        extends JMenuItem
    {
        /**
         * Class id key used in UIDefaults.
         */
        private static final String uiClassID =
            SmileyMenuItem.class.getName() +  "TreeUI";

        /**
         * Adds the ui class to UIDefaults.
         */
        static
        {
            UIManager.getDefaults().put(uiClassID,
                SIPCommMenuItemUI.class.getName());
        }

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
            super(GuiActivator.getResources().getImage(smiley.getImageID()));

            this.smiley = smiley;
        }

        /**
         * Returns the name of the L&F class that renders this component.
         *
         * @return the string "TreeUI"
         * @see JComponent#getUIClassID
         * @see UIDefaults#getUI
         */
        @Override
        public String getUIClassID()
        {
            return uiClassID;
        }
    }

    /**
     * Changes the static image of the underlying smiley with a dynamic one.
     * Also shows the description and smiley string in the description area.
     * @param e the <tt>MouseEvent</tt> that notified us
     */
    public void mouseEntered(MouseEvent e)
    {
        SmileyMenuItem smileyItem = (SmileyMenuItem) e.getSource();
        Smiley smiley = smileyItem.smiley;

        ImageIcon imageIcon
            = GuiActivator.getResources().getImage(smiley.getImageID());
        smileyItem.setIcon(imageIcon);

        smileyDescriptionLabel.setText(smiley.getDescription());
        smileyTextLabel.setText(
            "<html>" + smiley.getDefaultString() + "</html>");
    }

    /**
     * Clears all mouse over effects when the mouse has exited the smiley area.
     * @param e the <tt>MouseEvent</tt> that notified us
     */
    public void mouseExited(MouseEvent e)
    {
        SmileyMenuItem smileyItem = (SmileyMenuItem) e.getSource();

        this.clearMouseOverEffects(smileyItem);
    }

    public void mouseClicked(MouseEvent e) {}

    public void mousePressed(MouseEvent e) {}

    public void mouseReleased(MouseEvent e) {}

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
            GuiActivator.getResources().getImage(smileyItem.smiley.getImageID());

        smileyItem.setIcon(imageIcon);
        smileyTextLabel.setText("");
        smileyDescriptionLabel.setText("");
    }

    /**
     * Implements PopupMenuListener#popupMenuCanceled(PopupMenuEvent). Does
     * nothing.
     * @param e the <tt>PopupMenuEvent</tt>
     */
    public void popupMenuCanceled(PopupMenuEvent e) {}

    /**
     * Implements
     * PopupMenuListener#popupMenuWillBecomeInvisible(PopupMenuEvent). Does
     * nothing.
     * @param e the <tt>PopupMenuEvent</tt>
     */
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
    {
        // fixes a leak where gif images leak "Image Animator" threads
        JPopupMenu popupMenu = (JPopupMenu) e.getSource();

        for(Component c : popupMenu.getComponents())
        {
            if(c instanceof SmileyMenuItem)
            {
                SmileyMenuItem si = (SmileyMenuItem)c;
                if(si.getIcon() instanceof ImageIcon)
                {
                    ImageIcon ii = (ImageIcon)si.getIcon();
                    if(ii != null && ii.getImage() != null)
                        ii.getImage().flush();
                }
            }
        }
    }

    /**
     * Implements PopupMenuListener#popupMenuWillBecomeVisible(PopupMenuEvent).
     * Loads the smileys and creates the UI to represent them when they are
     * first necessary.
     * @param e the <tt>PopupMenuEvent</tt> that notified us
     */
    public void popupMenuWillBecomeVisible(PopupMenuEvent e)
    {
        JPopupMenu popupMenu = (JPopupMenu) e.getSource();

        // Don't populate it again if it's already populated.
        if (popupMenu.getComponentIndex(smileyTextLabel) != -1)
            return;

        Collection<Smiley> imageList = smiliesService.getSmiliesPack();

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
            gridBagConstraints.gridy = (int)(Math.floor(smileyIndex / gridColCount)) % gridRowCount;

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

    /**
     * Reloads icons in this menu.
     */
    public void loadSkin()
    {
        this.setIcon(new ImageIcon(ImageLoader
            .getImage(ImageLoader.SMILIES_ICON)));

        if (popupMenu != null)
        {
            popupMenu = this.getPopupMenu();

            if (smiliesService != null)
                smiliesService.reloadSmiliesPack();

            popupMenu.removeAll();
        }
    }
}
