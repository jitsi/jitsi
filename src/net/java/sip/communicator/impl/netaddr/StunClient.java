package net.java.sip.communicator.impl.netaddr;

import net.java.stun4j.*;
import net.java.stun4j.attribute.*;
import net.java.stun4j.message.*;
import net.java.stun4j.stack.*;

/**
 * <p>
 * This class implements the STUN Discovery Process as described by section 10.1
 * of rfc 3489.
 * </p><p>
 * The flow makes use of three tests.  In test I, the client sends a
 * STUN Binding Request to a server, without any flags set in the
 * CHANGE-REQUEST attribute, and without the RESPONSE-ADDRESS attribute.
 * This causes the server to send the response back to the address and
 * port that the request came from.  In test II, the client sends a
 * Binding Request with both the "change IP" and "change port" flags
 * from the CHANGE-REQUEST attribute set.  In test III, the client sends
 * a Binding Request with only the "change port" flag set.
 * </p><p>
 * The client begins by initiating test I.  If this test yields no
 * response, the client knows right away that it is not capable of UDP
 * connectivity.  If the test produces a response, the client examines
 * the MAPPED-ADDRESS attribute.  If this address and port are the same
 * as the local IP address and port of the socket used to send the
 * request, the client knows that it is not natted.  It executes test
 * II.
 * </p><p>
 * If a response is received, the client knows that it has open access
 * to the Internet (or, at least, its behind a firewall that behaves
 * like a full-cone NAT, but without the translation).  If no response
 * is received, the client knows its behind a symmetric UDP firewall.
 * </p><p>
 * In the event that the IP address and port of the socket did not match
 * the MAPPED-ADDRESS attribute in the response to test I, the client
 * knows that it is behind a NAT.  It performs test II.  If a response
 * is received, the client knows that it is behind a full-cone NAT.  If
 * no response is received, it performs test I again, but this time,
 * does so to the address and port from the CHANGED-ADDRESS attribute
 * from the response to test I.  If the IP address and port returned in
 * the MAPPED-ADDRESS attribute are not the same as the ones from the
 * first test I, the client knows its behind a symmetric NAT.  If the
 * address and port are the same, the client is either behind a
 * restricted or port restricted NAT.  To make a determination about
 * which one it is behind, the client initiates test III.  If a response
 * is received, its behind a restricted NAT, and if no response is
 * received, its behind a port restricted NAT.
 * </p><p>
 * This procedure yields substantial information about the operating
 * condition of the client application.  In the event of multiple NATs
 * between the client and the Internet, the type that is discovered will
 * be the type of the most restrictive NAT between the client and the
 * Internet.  The types of NAT, in order of restrictiveness, from most
 * to least, are symmetric, port restricted cone, restricted cone, and
 * full cone.
 * </p><p>
 * Typically, a client will re-do this discovery process periodically to
 * detect changes, or look for inconsistent results.  It is important to
 * note that when the discovery process is redone, it should not
 * generally be done from the same local address and port used in the
 * previous discovery process.  If the same local address and port are
 * reused, bindings from the previous test may still be in existence,
 * and these will invalidate the results of the test.  Using a different
 * local address and port for subsequent tests resolves this problem.
 * An alternative is to wait sufficiently long to be confident that the
 * old bindings have expired (half an hour should more than suffice).
 * </p><p>
 * <p>Organisation: <p> Louis Pasteur University, Strasbourg, France</p>
 * <p>Network Research Team (http://www-r2.u-strasbg.fr)</p></p>
 * @author Emil Ivov
 * @version 0.1
 */
public class StunClient
{

    /**
     * Indicates whether the underlying stack has been initialized and started
     * and that the discoverer is operational.
     */
    private boolean started = false;

    /**
     * The stack to use for STUN communication.
     */
    private StunStack stunStack = null;

    /**
     * The provider to send our messages through
     */
    private StunProvider stunProvider = null;

    /**
     * The point where we'll be listening.
     */
    private NetAccessPointDescriptor apDescriptor = null;

    /**
     * The address of the stun server
     */
    private StunAddress serverAddress = null;

    /**
     * A utility used to flatten the multithreaded architecture of the Stack
     * and execute the discovery process in a synchronized manner
     */
    private BlockingRequestSender requestSender = null;

    /**
     * Creates a StunAddressDiscoverer. In order to use it one must start the
     * discoverer.
     * @param localAddress the address where the stach should bind.
     */
    public StunClient(StunAddress localAddress)
    {
        apDescriptor = new NetAccessPointDescriptor(localAddress);
    }

    /**
     * Creates a StunAddressDiscoverer. In order to use it one must start the
     * discoverer.
     * @param apDescriptor the address where the stach should bind.
     * @param serverAddress the address of the server to interrogate.
     */
    public StunClient(NetAccessPointDescriptor apDescriptor,
                      StunAddress serverAddress)
    {
        this.apDescriptor = apDescriptor;
        this.serverAddress = serverAddress;

    }

    /**
     * Shuts down the underlying stack and prepares the object for garbage
     * collection.
     */
    public void shutDown()
    {
//        try
//        {
//            stunStack.removeNetAccessPoint(apDescriptor);
//        }
//        catch (StunException ex)
//        {
//            logger.warn("Failed to remove NetAP: "+apDescriptor.toString(), ex);
//        }
//        stunStack = null;
//        stunProvider = null;
//        apDescriptor = null;
//        requestSender = null;
//
//        this.started = false;

    }

    /**
     * Puts the discoverer into an operational state.
     * @throws StunException if we fail to bind or some other error occurs.
     */
    public void start() throws StunException
    {
//        stunStack = StunStack.getInstance();
//        //stunStack.start();
//
//        stunStack.installNetAccessPoint(apDescriptor);
//
//        stunProvider = stunStack.getProvider();
//
//        requestSender = new BlockingRequestSender(stunProvider, apDescriptor);
//
//        started = true;
    }

    /**
     * Sends a binding request to the specified server address. Both change IP
     * and change port flags are set to false.
     * @param serverAddress the address where to send the bindingRequest.
     * @return The returned message encapsulating event or null if no message
     * was received.
     * @throws StunException if an exception occurs while sending the messge
     */
    public StunMessageEvent doStunTestI(StunAddress serverAddress) throws
        StunException
    {
        Request request = MessageFactory.createBindingRequest();

        ChangeRequestAttribute changeRequest =
            (ChangeRequestAttribute) request.getAttribute(Attribute.
            CHANGE_REQUEST);
        changeRequest.setChangeIpFlag(false);
        changeRequest.setChangePortFlag(false);
        StunMessageEvent evt = null;
//            requestSender.sendRequestAndWaitForResponse(request, serverAddress);

        return evt;
    }

    /**
     * Sends a binding request to the specified server address with both change
     * IP and change port flags are set to true.
     * @param serverAddress the address where to send the bindingRequest.
     * @return The returned message encapsulating event or null if no message
     * was received.
     * @throws StunException if an exception occurs while sending the messge
     */
    public StunMessageEvent doStunTestII(StunAddress serverAddress) throws
        StunException
    {
        Request request = MessageFactory.createBindingRequest();

        ChangeRequestAttribute changeRequest =
            (ChangeRequestAttribute) request.getAttribute(Attribute.
            CHANGE_REQUEST);
        changeRequest.setChangeIpFlag(true);
        changeRequest.setChangePortFlag(true);

        StunMessageEvent evt = null;
//            requestSender.sendRequestAndWaitForResponse(request, serverAddress);

        return evt;
    }

    /**
     * Sends a binding request to the specified server address with only change
     * port flag set to true and change IP flag - to false.
     * @param serverAddress the address where to send the bindingRequest.
     * @return The returned message encapsulating event or null if no message
     * was received.
     * @throws StunException if an exception occurs while sending the messge
     */
    public StunMessageEvent doStunTestIII(StunAddress serverAddress) throws
        StunException
    {
        Request request = MessageFactory.createBindingRequest();

        ChangeRequestAttribute changeRequest = (ChangeRequestAttribute) request.
            getAttribute(Attribute.CHANGE_REQUEST);
        changeRequest.setChangeIpFlag(false);
        changeRequest.setChangePortFlag(true);

        StunMessageEvent evt = null;
//            requestSender.sendRequestAndWaitForResponse(request, serverAddress);

        return evt;
    }

    /**
     * Makes shure the discoverer is operational and throws an
     * StunException.ILLEGAL_STATE if that is not the case.
     * @throws StunException ILLEGAL_STATE if the discoverer is not operational.
     */
    private void checkStarted() throws StunException
    {
        if (!started)
            throw new StunException(StunException.ILLEGAL_STATE,
                                    "The Discoverer must be started before "
                                    + "launching the discovery process!");
    }
}
/**
 * Sample run results.
 *
 * TEST I res=/69.0.209.22:3478 - stun01bak.sipphone.com
 * mapped address is=193.108.24.226./193.108.24.226:5678,  name=193.108.24.226.
 * backup server address is=69.0.208.27./69.0.208.27:3478, name=69.0.208.27.
 * NO RESPONSE received to Test II.
 * TEST I res=/69.0.208.27:3478 - stun01.sipphone.com
 * NO RESPONSE received to Test III.
 * The detected network configuration is: Port Restricted Cone NAT
 * Your mapped public address is: 193.108.24.226./193.
 */
