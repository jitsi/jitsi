package net.java.sip.communicator.service.protocol.event;

/**
 * Listener that is informed when a change has been made to some server
 * stored detail.
 *
 * @author Damian Minkov
 */
public interface ServerStoredDetailsChangeListener
{
    /**
     * The method is called by a ProtocolProvider implementation whenever
     * a change in the server stored detail occurred.
     * @param evt ServerStoredDetailsChangeEvent the event describing the detail
     * change.
     */
    public void serverStoredDetailsChanged(ServerStoredDetailsChangeEvent evt);
}
