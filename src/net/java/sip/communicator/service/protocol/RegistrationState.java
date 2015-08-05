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
 * The class represents a set of states that a protocol provider may take while
 * registering, logging in, or signing on to a public service server, such as
 * a SIP registrar, the ICQ/AIM login and registration servers or a Jabber
 * server. States are generally supposed to be appearing in the following order:
 * <p>
 * Note that the order strongly depends on the particular protocol, which may
 * also influence the exact states that might actually be entered.
 * </p><p>
 * For more information on the particular states, please check the documentation
 * for each of them.
 * </p>
 *
 * @author Emil Ivov
 */
public class RegistrationState
{
    private static final String _INIT = "Initial";
    /**
     * The initial state of a protocol provider, assigned to it upon creation
     * and before any registration action has been undertaken by the user.
     */
    public static final RegistrationState INIT =
        new RegistrationState(_INIT);


    public static final String _REGISTERING = "Registering";
    /**
     * A transition state indicating that registration has been undertaken but
     * has not yet been confirmed by the registrar server/service. The state
     * generally occurs after the client has undertaken action to completing the
     * registration and the server is about to respond.
     */
    public static final RegistrationState REGISTERING =
        new RegistrationState(_REGISTERING);


    public static final String _CHALLENGED_FOR_AUTHENTICATION =
        "Challenged for authentication";
    /**
     * The registrar service requires authentication and we are about to send
     * one.
     */
    public static final RegistrationState CHALLENGED_FOR_AUTHENTICATION =
        new RegistrationState(_CHALLENGED_FOR_AUTHENTICATION);


    public static final String _AUTHENTICATING = "Authenticating";
    /**
     * In the process of authenticating. The state is entered when a protocol
     * provider sends authentication info and waits for a confirmation
     */
    public static final RegistrationState AUTHENTICATING =
        new RegistrationState(_AUTHENTICATING);


    public static final String _FINALIZING_REGISTRATION =
        "Finalizing Registration";
    /**
     * Representing any transition state after authentication is completed
     * and before it has been completed. This state wouldn't make sense for
     * many services, and would only be used with others such as ICQ/AIM for
     * example.
     */
    public static final RegistrationState FINALIZING_REGISTRATION =
        new RegistrationState(_FINALIZING_REGISTRATION);

    public static final String _REGISTERED = "Registered";
    /**
     * Registration has completed successfully and we are currently signed
     * on the registration service.
     */
    public static final RegistrationState REGISTERED =
        new RegistrationState(_REGISTERED);

    public static final String _CONNECTION_FAILED = "Connection Failed";
    /**
     * Registration has failed for a technical reason, such as connection
     * disruption for example.
     */
    public static final RegistrationState CONNECTION_FAILED =
        new RegistrationState(_CONNECTION_FAILED);

    public static final String _AUTHENTICATION_FAILED = "Authentication Failed";
    /**
     * Registration has failed because of a problem with the authentication.
     */
    public static final RegistrationState AUTHENTICATION_FAILED =
        new RegistrationState(_AUTHENTICATION_FAILED);

    public static final String _UPDATING_REGISTRATION = "Updating Registration";
    /**
     * Indicates that a protocol provider is currently updating its registration.
     */
    public static final RegistrationState UPDATING_REGISTRATION =
        new RegistrationState(_UPDATING_REGISTRATION);

    public static final String _EXPIRED = "Expired";
    /**
     * The registration has expired.
     */
    public static final RegistrationState EXPIRED =
        new RegistrationState(_EXPIRED);

    public static final String _UNREGISTERING = "Unregistering";

    /**
     * The Protocol Provider is being unregistered. Most probably due to a
     * user request.
     */
    public static final RegistrationState UNREGISTERING =
        new RegistrationState(_UNREGISTERING);

    public static final String _UNREGISTERED = "Unregistered";
    /**
     * The Protocol Provider is not registered. Most probably due to a
     * unregistration.
     */
    public static final RegistrationState UNREGISTERED =
        new RegistrationState(_UNREGISTERED);

    private final String statusString;

    private RegistrationState(String statusString)
    {
        this.statusString = statusString;
    }

    /**
     * Returns a String representation of the provider state.
     * @return a String representation of the state.
     */
    public String getStateName()
    {
        return statusString;
    }

    /**
     * Returns a String representation of the provider state.
     * @return a String representation of the state.
     */
    @Override
    public String toString()
    {
        return "RegistrationState=" + getStateName();
    }

    /**
     * Returns true if the specified object is equal to this provider state.
     * @param obj the object to compare this provider state with.
     * @return true if the specified object represents the same state as this
     * one.
     */
    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof RegistrationState
               && obj != null
               && statusString.equals(((RegistrationState)obj).statusString);
    }
}
