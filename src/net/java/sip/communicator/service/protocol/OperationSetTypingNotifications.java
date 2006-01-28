package net.java.sip.communicator.service.protocol;

/**
 *
 * @author Emil Ivov
 */
public interface OperationSetTypingNotifications
    extends OperationSet
{
    public void addTypingNotificationsListener();

    public void sendTypingNotification(Contact notifiedContact);
}
