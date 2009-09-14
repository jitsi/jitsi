/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.conference;

import java.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * @author Valentin Martinet
 */
public class AdHocChatRoomProviderWrapper
{
    private static final Logger logger
        = Logger.getLogger(AdHocChatRoomProviderWrapper.class);

    private final ProtocolProviderService protocolProvider;

    private final List<AdHocChatRoomWrapper> chatRoomsOrderedCopy
        = new LinkedList<AdHocChatRoomWrapper>();

    /**
     * Creates an instance of <tt>AdHocChatRoomProviderWrapper</tt> by 
     * specifying the protocol provider, corresponding to the ad-hoc multi user 
     * chat account.
     *
     * @param protocolProvider protocol provider, corresponding to the ad-hoc 
     * multi user chat account.
     */
    public AdHocChatRoomProviderWrapper(
        ProtocolProviderService protocolProvider)
    {
        this.protocolProvider = protocolProvider;
    }

    /**
     * Returns the name of this ad-hoc chat room provider.
     * @return the name of this ad-hoc chat room provider.
     */
    public String getName()
    {
        return protocolProvider.getProtocolDisplayName();
    }

    public byte[] getIcon()
    {
        return protocolProvider.getProtocolIcon()
            .getIcon(ProtocolIcon.ICON_SIZE_64x64);
    }

    public byte[] getImage()
    {
        byte[] logoImage = null;
        ProtocolIcon protocolIcon = protocolProvider.getProtocolIcon();

        if(protocolIcon.isSizeSupported(ProtocolIcon.ICON_SIZE_64x64))
            logoImage = protocolIcon.getIcon(ProtocolIcon.ICON_SIZE_64x64);
        else if(protocolIcon.isSizeSupported(ProtocolIcon.ICON_SIZE_48x48))
            logoImage = protocolIcon.getIcon(ProtocolIcon.ICON_SIZE_48x48);

        return logoImage;
    }

    /**
     * Returns the protocol provider service corresponding to this server
     * wrapper.
     * 
     * @return the protocol provider service corresponding to this server
     * wrapper.
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return protocolProvider;
    }

    /**
     * Adds the given ad-hoc chat room to this chat room provider.
     * 
     * @param adHocChatRoom the ad-hoc chat room to add.
     */
    public void addAdHocChatRoom(AdHocChatRoomWrapper adHocChatRoom)
    {
        this.chatRoomsOrderedCopy.add(adHocChatRoom);
    }

    /**
     * Removes the given ad-hoc chat room from this provider.
     * 
     * @param adHocChatRoom the ad-hoc chat room to remove.
     */
    public void removeChatRoom(AdHocChatRoomWrapper adHocChatRoom)
    {
        this.chatRoomsOrderedCopy.remove(adHocChatRoom);
    }

    /**
     * Returns <code>true</code> if the given ad-hoc chat room is contained in 
     * this provider, otherwise - returns <code>false</code>.
     * 
     * @param adHocChatRoom the ad-hoc chat room to search for.
     * @return <code>true</code> if the given ad-hoc chat room is contained in 
     * this provider, otherwise - returns <code>false</code>.
     */
    public boolean containsAdHocChatRoom(AdHocChatRoomWrapper adHocChatRoom)
    {
        synchronized (chatRoomsOrderedCopy)
        {
            return chatRoomsOrderedCopy.contains(adHocChatRoom);
        }
    }

    /**
     * Returns the ad-hoc chat room wrapper contained in this provider that 
     * corresponds to the given ad-hoc chat room.
     * 
     * @param adHocChatRoom the ad-hoc chat room we're looking for.
     * @return the ad-hoc chat room wrapper contained in this provider that 
     * corresponds to the given ad-hoc chat room.
     */
    public AdHocChatRoomWrapper findChatRoomWrapperForAdHocChatRoom(
            AdHocChatRoom adHocChatRoom)
    {
        // compare ids, cause saved ad-hoc chatrooms don't have AdHocChatRoom 
        // object but Id's are the same
        for (AdHocChatRoomWrapper chatRoomWrapper : chatRoomsOrderedCopy)
        {
            if (chatRoomWrapper.getAdHocChatRoomID().equals(
                    adHocChatRoom.getIdentifier()))
            {
                return chatRoomWrapper;
            }
        }

        return null;
    }

    /**
     * Returns the number of ad-hoc chat rooms contained in this provider.
     * 
     * @return the number of ad-hoc chat rooms contained in this provider.
     */
    public int countAdHocChatRooms()
    {
        return chatRoomsOrderedCopy.size();
    }

    public AdHocChatRoomWrapper getAdHocChatRoom(int index)
    {
        return chatRoomsOrderedCopy.get(index);
    }

    /**
     * Returns the index of the given chat room in this provider.
     * 
     * @param chatRoomWrapper the chat room to search for.
     * 
     * @return the index of the given chat room in this provider.
     */
    public int indexOf(AdHocChatRoomWrapper chatRoomWrapper)
    {
        return chatRoomsOrderedCopy.indexOf(chatRoomWrapper);
    }

    /**
     * Goes through the locally stored chat rooms list and for each
     * {@link ChatRoomWrapper} tries to find the corresponding server stored
     * {@link ChatRoom} in the specified operation set. Joins automatically all
     * found chat rooms.
     *
     * @param protocolProvider the protocol provider for the account to
     * synchronize
     * @param opSet the ad-hoc multi-user chat operation set, which give us 
     * access to chat room server 
     */
    public void synchronizeProvider()
    {
        final OperationSetAdHocMultiUserChat groupChatOpSet
            = (OperationSetAdHocMultiUserChat) protocolProvider
                .getOperationSet(OperationSetAdHocMultiUserChat.class);

        for(final AdHocChatRoomWrapper chatRoomWrapper : chatRoomsOrderedCopy)
        {
            new Thread()
            {
                public void run()
                {
                    AdHocChatRoom chatRoom = null;

                    try
                    {
                        chatRoom = groupChatOpSet.findRoom(
                                    chatRoomWrapper.getAdHocChatRoomName());
                    }
                    catch (OperationFailedException e1)
                    {
                        logger.error("Failed to find chat room with name:"
                            + chatRoomWrapper.getAdHocChatRoomName(), e1);
                    }
                    catch (OperationNotSupportedException e1)
                    {                        
                        logger.error("Failed to find chat room with name:"
                            + chatRoomWrapper.getAdHocChatRoomName(), e1);
                    }

                    if(chatRoom != null)
                    {
                        chatRoomWrapper.setAdHocChatRoom(chatRoom);

                        String lastChatRoomStatus
                            = ConfigurationManager.getChatRoomStatus(
                                protocolProvider,
                                chatRoomWrapper.getAdHocChatRoomID());

                        if(lastChatRoomStatus == null
                            || lastChatRoomStatus.equals(
                                Constants.ONLINE_STATUS))
                        {
                            GuiActivator.getUIService()
                                .getConferenceChatManager()
                                    .joinChatRoom(chatRoomWrapper);
                        }
                    }
                }
            }.start();
        }
    }
}
