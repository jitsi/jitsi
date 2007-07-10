package net.java.sip.communicator.impl.netaddr;

import net.java.stun4j.*;
import net.java.stun4j.message.*;
import net.java.stun4j.stack.*;
import java.io.*;

/**
 * A utility used to flatten the multithreaded architecture of the Stack
 * and execute the discovery process in a synchronized manner. Roughly what
 * happens here is:
 *
 * ApplicationThread:
 *     sendMessage()
 *    wait();
 *
 * StackThread:
 *     processMessage/Timeout()
 *     {
 *          saveMessage();
 *          notify();
 *     }
 *
 *
 * <p>Organisation: <p> Louis Pasteur University, Strasbourg, France</p>
 * <p>Network Research Team (http://www-r2.u-strasbg.fr)</p></p>
 * @author Emil Ivov
 * @version 0.1
 */
class BlockingRequestSender
    implements ResponseCollector
{
    private StunProvider             stunProvider  = null;
    private NetAccessPointDescriptor apDescriptor  = null;

    StunMessageEvent responseEvent = null;

    BlockingRequestSender(StunProvider             stunProvider,
                          NetAccessPointDescriptor apDescriptor)
    {
        this.stunProvider = stunProvider;
        this.apDescriptor = apDescriptor;
    }

    /**
     * Saves the message event and notifies the discoverer thread so that
     * it may resume.
     * @param evt the newly arrived message event.
     */
    public synchronized void processResponse(StunMessageEvent evt)
    {
        this.responseEvent = evt;
        notifyAll();
    }

    /**
     * Notifies the discoverer thread when a message has timeouted so that
     * it may resume and consider it as unanswered.
     */
    public synchronized void processTimeout()
    {
        notifyAll();
    }

    /**
     * Sends the specified request and blocks until a response has been
     * received or the request transaction has timed out.
     * @param request the reuqest to send
     * @param serverAddress the request destination address
     * @return the event encapsulating the response or null if no response
     * has been received.
     * @throws StunException NETWORK_ERROR or other if we fail to send
     * the message
     */
    public synchronized StunMessageEvent sendRequestAndWaitForResponse(
                                                    Request request,
                                                    StunAddress serverAddress)
            throws StunException, IOException, IllegalArgumentException
    {
        stunProvider.sendRequest(request, serverAddress, apDescriptor,
                                 BlockingRequestSender.this);

        try
        {
            wait();
        }
        catch (InterruptedException ex)
        { /** @todo log */
            ex.printStackTrace();
        }

        StunMessageEvent res = responseEvent;
        responseEvent = null; //prepare for next message

        return res;
    }
}
