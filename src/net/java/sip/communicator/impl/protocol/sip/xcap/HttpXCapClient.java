/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap;

import javax.sip.address.*;
import java.net.URI;

/**
 * HTTP XCAP client interface.
 * <p/>
 * Compliant with rfc4825
 *
 * @author Grigorii Balutsel
 */
public interface HttpXCapClient
{
    /**
     * Connects user to XCAP server.
     *
     * @param uri         the server location.
     * @param userAddress the user name.
     * @param password    the user password.
     * @throws XCapException if there is some error during operation.
     */
    public void connect(URI uri, Address userAddress, String password)
            throws XCapException;

    /**
     * Dicsonnects user from the XCAP server.
     */
    public void dicsonnect();

    /**
     * Checks if user is connected to the XCAP server.
     *
     * @return true if user is connected.
     */
    public boolean isConnected();

    /**
     * Gets the resource from the server.
     *
     * @param resourceId resource identifier.
     * @return the server response.
     * @throws IllegalStateException if the user has not been connected.
     * @throws XCapException         if there is some error during operation.
     */
    public XCapHttpResponse get(XCapResourceId resourceId)
            throws XCapException;

    /**
     * Puts the resource to the server.
     *
     * @param resource the resource  to be saved on the server.
     * @return the server response.
     * @throws IllegalStateException if the user has not been connected.
     * @throws XCapException         if there is some error during operation.
     */
    public XCapHttpResponse put(XCapResource resource)
            throws XCapException;

    /**
     * Deletes the resource from the server.
     *
     * @param resourceId resource identifier.
     * @return the server response.
     * @throws IllegalStateException if the user has not been connected.
     * @throws XCapException         if there is some error during operation.
     */
    public XCapHttpResponse delete(XCapResourceId resourceId)
            throws XCapException;

    /**
     * Gets connected user name.
     *
     * @return user name.
     */
    public String getUserName();

    /**
     * Gets the XCAP server location.
     *
     * @return server location.
     */
    public URI getUri();

    /**
     * Gets operation timeout.
     *
     * @return operation timeout.
     */
    public int getTimeout();

    /**
     * Sets operation timeout.
     *
     * @param timeout operation timeout.
     */
    public void setTimeout(int timeout);

}
