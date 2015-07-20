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
package net.java.sip.communicator.impl.gui.main.chat.conference;

import java.awt.*;
import java.lang.annotation.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.skin.*;

/**
 * Implements a list with announced conferences in chat room.
 * 
 * @author Hristo Terezov
 */
public class ChatRoomConferenceCallsListPanel
    extends JPanel
    implements Skinnable
{
    private static final long serialVersionUID = -8250816784228586068L;

    /**
     * The list of conferences.
     */
    private final JList conferenceCallList;

    /**
     * The model of the conferences list.
     */
    private final ChatConferenceCallsListModels conferenceCallsListModel;

    /**
     * Current chat panel.
     */
    private final ChatPanel chatPanel;
    
    /**
     * Custom renderer for the conference items.
     */
    private class ChatConferenceCallsListRenderer
        extends JPanel
        implements ListCellRenderer, Skinnable
    {
        /**
         * The label that will display the name of the conference.
         */
        private JLabel conferenceLabel = new JLabel();
        
        /**
         * Foreground color for the item.
         */
        private Color contactForegroundColor;
        
        /**
         * Indicates whether the item is selected or not.
         */
        private boolean isSelected;
        
        /**
         * The icon for the conference item.
         */
        private final ImageIcon conferenceIcon = new ImageIcon(
            ImageLoader.getImage(ImageLoader.CONFERENCE_ICON));
        
        /**
         * Creates new <tt>ChatConferenceCallsListRenderer</tt> instance.
         */
        public ChatConferenceCallsListRenderer()
        {
            super(new BorderLayout());
            this.setOpaque(false);
            this.conferenceLabel.setOpaque(false);
            this.conferenceLabel.setPreferredSize(new Dimension(10, 20));
            setFont(this.getFont().deriveFont(Font.PLAIN));
            this.setBorder(BorderFactory.createEmptyBorder(2, 5, 1, 1));
            this.conferenceLabel.setOpaque(false);
            this.conferenceLabel.setIcon(conferenceIcon);
            this.add(conferenceLabel, BorderLayout.CENTER);
        }
        
        /**
         * {@link Inherited}
         */
        @Override
        public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus)
        {
            this.isSelected = isSelected;
            if(contactForegroundColor != null)
                setForeground(contactForegroundColor);
            
            setFont(this.getFont().deriveFont(Font.PLAIN));
            conferenceLabel.setText(
                ((ConferenceDescription)value).getDisplayName());
            return this;
        }
        
        /**
         * Paints a customized background.
         *
         * @param g the <tt>Graphics</tt> object through which we paint
         */
        @Override
        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);
    
            g = g.create();
            try
            {
                internalPaintComponent(g);
            }
            finally
            {
                g.dispose();
            }
        }
    
        /**
         * Paint a background round blue border and background
         * when a cell is selected.
         *
         * @param g the <tt>Graphics</tt> object through which we paint
         */
        private void internalPaintComponent(Graphics g)
        {
            if(this.isSelected)
            {
                AntialiasingManager.activateAntialiasing(g);
    
                Graphics2D g2 = (Graphics2D) g;
    
                g2.setColor(Constants.SELECTED_COLOR);
                g2.fillRoundRect(   1, 1,
                                    this.getWidth() - 2, this.getHeight() - 1,
                                    10, 10);
            }
        }
    
        
        /**
         * Reloads skin information for this render class.
         */
        public void loadSkin()
        {
            int contactForegroundProperty = GuiActivator.getResources()
                    .getColor("service.gui.CHATROOM_CONFERENCE_LIST_FOREGROUND");
    
            if (contactForegroundProperty > -1)
                contactForegroundColor = new Color(contactForegroundProperty);
        }
        
    }
    
    /**
     * Initializes a new <tt>ChatRoomConferenceCallsListPanel</tt> instance 
     * which is to depict the conferences of a chat specified by its 
     * <tt>ChatPanel</tt>.
     *
     * @param chatPanel the <tt>ChatPanel</tt> which specifies the chat.
     */
    public ChatRoomConferenceCallsListPanel(final ChatPanel chatPanel)
    {
        super(new BorderLayout());

        this.chatPanel = chatPanel;
        this.conferenceCallsListModel
            = new ChatConferenceCallsListModels(chatPanel.getChatSession());
        this.conferenceCallList 
            = new JList(conferenceCallsListModel);
        this.conferenceCallList.addKeyListener(
            new CListKeySearchListener(conferenceCallList));
        this.conferenceCallList.setCellRenderer(
            new ChatConferenceCallsListRenderer());

        JScrollPane conferenceCallsScrollPane = new SIPCommScrollPane();
        conferenceCallsScrollPane.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        conferenceCallsScrollPane.setOpaque(false);
        conferenceCallsScrollPane.setBorder(null);

        JViewport viewport = conferenceCallsScrollPane.getViewport();
        viewport.setOpaque(false);
        viewport.add(conferenceCallList);

        this.add(conferenceCallsScrollPane);
        
    }

    /**
     * Adds a <tt>ConferenceDescription</tt> to the list of conferences 
     * contained in the chat.
     *
     * @param chatConference the <tt>ConferenceDescription</tt> to add
     */
    public void addConference(ConferenceDescription chatConference)
    {
        conferenceCallsListModel.addElement(chatConference);
    }
    
    /**
     * Returns the size of the list.
     * 
     * @return the number of elements in the list.
     */
    public int getListSize()
    {
        return conferenceCallsListModel.getSize();
    }
    
    /**
     * Returns the <tt>ConferenceDescription</tt> of the selected conference in 
     * the list.
     * 
     * @return the <tt>ConferenceDescription</tt> of the selected conference
     */
    public ConferenceDescription getSelectedValue()
    {
        return (ConferenceDescription) conferenceCallList.getSelectedValue();
    }
    
    /**
     * Initializes the list of the conferences that are already announced.
     */
    public void initConferences()
    {
        conferenceCallsListModel.initConferences();
        setSelectedIndex(0);
    }
    
    /**
     * Selects an item from the list.
     * @param index the index of the item to be selected.
     */
    public void setSelectedIndex(int index)
    {
        conferenceCallList.setSelectedIndex(index);
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public void loadSkin()
    {
        ((ChatConferenceCallsListRenderer)conferenceCallList.getCellRenderer())
            .loadSkin();
        
    }
    
    /**
     * Removes the given <tt>ConferenceDescription</tt> from the list of chat 
     * conferences.
     *
     * @param chatConference the <tt>ConferenceDescription</tt> to remove
     */
    public void removeConference(ConferenceDescription chatConference)
    {
        conferenceCallsListModel.removeElement(chatConference);
    }
}
