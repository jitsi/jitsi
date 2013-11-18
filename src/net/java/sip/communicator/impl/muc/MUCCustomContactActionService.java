/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.muc;

import java.util.*;

import org.jitsi.service.resources.*;

import net.java.sip.communicator.plugin.desktoputil.chat.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.customcontactactions.*;
import net.java.sip.communicator.service.muc.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Implements <tt>CustomContactActionsService</tt> for MUC contact source.
 * 
 * @author Hristo Terezov
 */
public class MUCCustomContactActionService
    implements CustomContactActionsService<SourceContact>
{
    /**
     * List of custom menu items.
     */
    private final List<ContactActionMenuItem<SourceContact>> MUCActionMenuItems
        = new LinkedList<ContactActionMenuItem<SourceContact>>();
    
    /**
     * Array of labels for the custom menu items.
     */
    private String[] actionsLabels = {
      "service.gui.OPEN", "service.gui.JOIN", 
      "service.gui.JOIN_AS", "service.gui.LEAVE",
      "service.gui.REMOVE", "service.gui.CHANGE_NICK",
      "service.gui.JOIN_AUTOMATICALLY"
    };
    
    /**
     * Array of icons for the custom menu items.
     */
    private String[] actionsIcons = {
        "service.gui.icons.CHAT_ROOM_16x16_ICON", "service.gui.icons.JOIN_ICON",
        "service.gui.icons.JOIN_AS_ICON", "service.gui.icons.LEAVE_ICON", 
        "service.gui.icons.REMOVE_CHAT_ICON", 
        "service.gui.icons.RENAME_16x16_ICON",
        null
      };
    
    /**
     * Array of <tt>MUCCustomActionRunnable</tt> objects for the custom menu 
     * items. They will be executed when the item is pressed.
     */
    private MUCCustomActionRunnable[] actionsRunnable = {
        new MUCCustomActionRunnable()
        {
            @Override
            public void run()
            {
                String[] joinOptions;
                String subject = null;
                if(chatRoomWrapper.getChatRoom() == null)
                {
                    // this is not a server persistent room we must create it
                    // and join
                    chatRoomWrapper =
                        MUCActivator.getMUCService().createChatRoom(
                                chatRoomWrapper.getChatRoomName(),
                                chatRoomWrapper.getParentProvider()
                                    .getProtocolProvider(),
                                new ArrayList<String>(),
                                "",
                                false,
                                false,
                                true);
                }
                
                if(!chatRoomWrapper.getChatRoom().isJoined())
                {
                    String nickName = null;

                    nickName =
                        ConfigurationUtils.getChatRoomProperty(
                            chatRoomWrapper.getParentProvider()
                                .getProtocolProvider(), chatRoomWrapper
                                .getChatRoomID(), "userNickName");
                    if(nickName == null)
                    {
                        joinOptions = ChatRoomJoinOptionsDialog.getJoinOptions(
                            chatRoomWrapper.getParentProvider()
                                .getProtocolProvider(), 
                            chatRoomWrapper.getChatRoomID());
                        nickName = joinOptions[0];
                        subject = joinOptions[1];
                    }

                    if (nickName != null)
                        MUCActivator.getMUCService().joinChatRoom(chatRoomWrapper,
                            nickName, null, subject);
                    else
                        return;
                }

                MUCActivator.getUIService().openChatRoomWindow(chatRoomWrapper);
            }
        },
        new MUCCustomActionRunnable()
        {
            
            @Override
            public void run()
            {
                String[] joinOptions;
                String subject = null;
                String nickName = null;

                nickName =
                    ConfigurationUtils.getChatRoomProperty(
                        chatRoomWrapper.getParentProvider()
                            .getProtocolProvider(), chatRoomWrapper
                            .getChatRoomID(), "userNickName");
                if(nickName == null)
                {
                    joinOptions = ChatRoomJoinOptionsDialog.getJoinOptions(
                        chatRoomWrapper.getParentProvider()
                            .getProtocolProvider(), 
                        chatRoomWrapper.getChatRoomID());
                    nickName = joinOptions[0];
                    subject = joinOptions[1];
                }

                if (nickName != null)
                    MUCActivator.getMUCService().joinChatRoom(chatRoomWrapper, 
                        nickName, null, subject);
            }
        },
        new MUCCustomActionRunnable()
        {
            
            @Override
            public void run()
            {
                String[] joinOptions;
                joinOptions = ChatRoomJoinOptionsDialog.getJoinOptions(
                    chatRoomWrapper.getParentProvider().getProtocolProvider(), 
                    chatRoomWrapper.getChatRoomID());
                if(joinOptions[0] == null)
                    return;
                MUCActivator.getMUCService()
                    .joinChatRoom(chatRoomWrapper, joinOptions[0], null,
                        joinOptions[1]);
            }
        },
        new MUCCustomActionRunnable()
        {
            
            @Override
            public void run()
            {
                ChatRoomWrapper leavedRoomWrapped 
                    = MUCActivator.getMUCService().leaveChatRoom(
                        chatRoomWrapper);
                if(leavedRoomWrapped != null)
                    MUCActivator.getUIService().closeChatRoomWindow(
                        leavedRoomWrapped);
            }
        },
        new MUCCustomActionRunnable()
        {
            
            @Override
            public void run()
            {
                ChatRoom chatRoom = chatRoomWrapper.getChatRoom();

                if (chatRoom != null)
                {
                    ChatRoomWrapper leavedRoomWrapped 
                        = MUCActivator.getMUCService().leaveChatRoom(
                            chatRoomWrapper);
                    if(leavedRoomWrapped != null)
                        MUCActivator.getUIService().closeChatRoomWindow(
                            leavedRoomWrapped);
                }

                MUCActivator.getUIService().closeChatRoomWindow(chatRoomWrapper);

                MUCActivator.getMUCService().removeChatRoom(chatRoomWrapper);
            }
        },
        new MUCCustomActionRunnable()
        {
            
            @Override
            public void run()
            {
                ChatRoomJoinOptionsDialog.getJoinOptions(true,
                    chatRoomWrapper.getParentProvider().getProtocolProvider(), 
                    chatRoomWrapper.getChatRoomID());
            }
        },
        new MUCCustomActionRunnable()
        {
            
            @Override
            public void run()
            {
                    chatRoomWrapper.setAutoJoin(!chatRoomWrapper.isAutojoin());
            }
        }
    };
    
    /**
     * Array of <tt>EnableChecker</tt> objects for the custom menu items. They 
     * are used to check if the item is enabled or disabled.
     */
    private EnableChecker[] actionsEnabledCheckers = {
      null,
      new JoinEnableChecker(),
      new JoinEnableChecker(),
      new LeaveEnableChecker(),
      null,
      null,
      null
    };
    
    /**
     * Array for the custom menu items that describes the type of the menu item.
     * If <tt>true</tt> - the item is check box.
     */
    private boolean[] actionsIsCheckBox = {
        false,
        false,
        false,
        false,
        false,
        false,
        true
      };
    
    /**
     * The resource management service instance.
     */
    ResourceManagementService resources = MUCActivator.getResources();
    
    /**
     * Constructs the custom actions.
     */
    public MUCCustomContactActionService()
    {
        for(int i = 0; i < actionsLabels.length; i++)
        {
            MUCActionMenuItems item = new MUCActionMenuItems(actionsLabels[i], 
                actionsIcons[i], actionsRunnable[i], actionsIsCheckBox[i]);
            MUCActionMenuItems.add(item);
            if(actionsEnabledCheckers[i] != null)
                item.setEnabled(actionsEnabledCheckers[i]);
        }
        
    }
    
    /**
     * Returns the template class that this service has been initialized with
     *
     * @return the template class
     */
    public Class<SourceContact> getContactSourceClass()
    {
        return SourceContact.class;
    }

    @Override
    public Iterator<ContactActionMenuItem<SourceContact>> 
        getCustomContactActionsMenuItems()
    {
        return MUCActionMenuItems.iterator();
    }
    

    @Override
    public Iterator<ContactAction<SourceContact>> getCustomContactActions()
    {
        return null;
    }

    /**
     * Implements the MUC custom menu items.
     */
    private class MUCActionMenuItems
        implements ContactActionMenuItem<SourceContact>
    {
        /**
         * The key used to retrieve the label for the menu item.
         */
        private String textKey;
        
        /**
         * The key used to retrieve the icon for the menu item.
         */
        private String imageKey;
        
        /**
         * The action executed when the menu item is pressed.
         */
        private MUCCustomActionRunnable actionPerformed;
        
        /**
         * Object that is used to check if the item is enabled or disabled.
         */
        private EnableChecker enabled;
        
        /**
         * if <tt>true</tt> the item should be check box and if <tt>false</tt> 
         * it is standard menu item.
         */
        private boolean isCheckBox;

        /**
         * Constructs <tt>MUCActionMenuItems</tt> instance.
         * 
         * @param textKey the key used to retrieve the label for the menu item.
         * @param imageKey the key used to retrieve the icon for the menu item.
         * @param actionPerformed the action executed when the menu item is 
         * pressed.
         * @param isCheckBox if <tt>true</tt> the item should be check box.
         */
        public MUCActionMenuItems(String textKey, String imageKey,
            MUCCustomActionRunnable actionPerformed, boolean isCheckBox)
        {
            this.textKey = textKey;
            this.imageKey = imageKey;
            this.actionPerformed = actionPerformed;
            this.enabled = new EnableChecker();
            this.isCheckBox = isCheckBox;
        }

        @Override
        public void actionPerformed(SourceContact actionSource)
            throws OperationFailedException
        {
            if(!(actionSource instanceof ChatRoomSourceContact))
                return;
            actionPerformed.setContact(actionSource);
            new Thread(actionPerformed).start();
        }

        @Override
        public byte[] getIcon()
        {
            return (imageKey == null)? null : 
                resources.getImageInBytes(imageKey);
        }


        @Override
        public String getText()
        {
            return resources.getI18NString(textKey);
        }

        @Override
        public boolean isVisible(SourceContact actionSource)
        {
            return (actionSource instanceof ChatRoomSourceContact);
        }

        @Override
        public char getMnemonics()
        {
            return resources.getI18nMnemonic(textKey);
        }

        @Override
        public boolean isEnabled(SourceContact actionSource)
        {
            return enabled.check(actionSource);
        }

        /**
         * Sets <tt>EnabledChecker</tt> instance that will be used to check if 
         * the item should be enabled or disabled.
         * 
         * @param enabled the <tt>EnabledChecker</tt> instance.
         */
        public void setEnabled(EnableChecker enabled)
        {
            this.enabled = enabled;
        }

        @Override
        public boolean isCheckBox()
        {
            return isCheckBox;
        }

        @Override
        public boolean isSelected(SourceContact contact)
        {
            ChatRoomWrapper chatRoomWrapper = MUCActivator.getMUCService()
                .findChatRoomWrapperFromSourceContact(contact);
            return chatRoomWrapper.isAutojoin();
        }

    }

    /**
     * Checks if the menu item should be enabled or disabled. This is default 
     * implementation. Always returns that the item should be enabled.
     */
    private class EnableChecker
    {
        /**
         * Checks if the menu item should be enabled or disabled.
         * 
         * @param contact the contact associated with the menu item.
         * @return always <tt>true</tt>
         */
        public boolean check(SourceContact contact)
        {
            return true;
        }
    }
    
    /**
     * Implements <tt>EnableChecker</tt> for the join menu items.
     */
    private class JoinEnableChecker extends EnableChecker
    {
        /**
         * Checks if the menu item should be enabled or disabled.
         * 
         * @param contact the contact associated with the menu item.
         * @return <tt>true</tt> if the item should be enabled and 
         * <tt>false</tt> if not.
         */
        public boolean check(SourceContact contact)
        {
            ChatRoomWrapper chatRoomWrapper = MUCActivator.getMUCService()
                .findChatRoomWrapperFromSourceContact(contact);
            ChatRoom chatRoom = null;
            if(chatRoomWrapper != null)
            {
                chatRoom = chatRoomWrapper.getChatRoom();
            }
            
            if((chatRoom != null) && chatRoom.isJoined())
                return false;
            return true;
        }
    }
    
    /**
     * Implements <tt>EnableChecker</tt> for the leave menu item.
     */
    private class LeaveEnableChecker extends JoinEnableChecker
    {
        /**
         * Checks if the menu item should be enabled or disabled.
         * 
         * @param contact the contact associated with the menu item.
         * @return <tt>true</tt> if the item should be enabled and 
         * <tt>false</tt> if not.
         */
        public boolean check(SourceContact contact)
        {
            return !super.check(contact);
        }
    }
    
    /**
     * Implements base properties for the MUC menu items.These properties are 
     * used when the menu item is pressed.
     */
    private abstract class MUCCustomActionRunnable implements Runnable
    {
        /**
         * The contact associated with the menu item.
         */
        protected SourceContact contact;
        
        /**
         * The contact associated with the menu item.
         */
        protected ChatRoomWrapper chatRoomWrapper;

        /**
         * Sets the source contact.
         * @param contact the contact to set
         */
        public void setContact(SourceContact contact)
        {
            this.contact = contact;
            chatRoomWrapper = MUCActivator.getMUCService()
                .findChatRoomWrapperFromSourceContact(contact);
        }
        
    }
}
