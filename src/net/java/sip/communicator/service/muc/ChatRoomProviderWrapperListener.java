package net.java.sip.communicator.service.muc;

/**
 * Listener which registers for provider add/remove changes.
 */
public interface ChatRoomProviderWrapperListener
{
    /**
     * When a provider wrapper is added this method is called to inform
     * listeners.
     * @param provider which was added.
     */
    public void chatRoomProviderWrapperAdded(
        ChatRoomProviderWrapper provider);

    /**
     * When a provider wrapper is removed this method is called to inform
     * listeners.
     * @param provider which was removed.
     */
    public void chatRoomProviderWrapperRemoved(
        ChatRoomProviderWrapper provider);
}