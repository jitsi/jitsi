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
package net.java.sip.communicator.impl.protocol.sip.xcap;

import java.net.URI;

import javax.sip.address.*;

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
     * @param userAddress the URI of the user used for requests
     * @param username the user name.
     * @param password    the user password.
     * @throws XCapException if there is some error during operation.
     */
    public void connect(URI uri, Address userAddress, String username, String password)
            throws XCapException;

    /**
     * Disconnects user from the XCAP server.
     */
    public void disconnect();

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
}
