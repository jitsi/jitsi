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
package net.java.sip.communicator.service.protocol;

/**
 * This class is used to represent both incoming and outgoing
 * AuthorizationRequests-s
 * <p>
 * An outgoing Authorization Request is to be created by the user interface
 * when an authorization error/challenge has been received by the underlying
 * protocol. The user interface or any other bundle responsible of handling
 * such requests is to implement the AuthoizationHandler interface and register
 * itself as an authorization handler of a protocol provider. Whenever a request
 * needs to be sent the protocol provider would ask the the AuthorizationHandler
 * to create one through the createAuthorizationRequest() method.
 * <p>
 * Incoming Authorization requests are delivered to the ProtocolProviderService
 * implementation through the AuthorizationHandler.processAuthorizationRequest()
 * method.
 *
 * @author Emil Ivov
 */
public class AuthorizationRequest
{
    /**
     * The reason phrase that should be sent to the user we're demanding for
     * authorization.
     */
    private String reason = "";

    /**
     * Creates an empty authorization request with no reason or any other
     * properties.
     */
    public AuthorizationRequest()
    {
    }

    /**
     * Sets the reason phrase that should be sent to the user we're demanding
     * for authorization.
     * @param reason a human readable text to be set by the user.
     */
    public void setReason(String reason)
    {
        this.reason = reason;
    }

    /**
     * Returns the reason that should be sent to the remote user when asking
     * for authorization.
     *
     * @return a String containing a reason phrase that should be sent to the
     * remote user when asking them for authorization.
     */
    public String getReason()
    {
        return reason;
    }
}
