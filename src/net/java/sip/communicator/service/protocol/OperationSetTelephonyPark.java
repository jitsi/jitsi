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
 * Provides operations necessary to park calls.
 *
 * @author Damian Minkov
 */
public interface OperationSetTelephonyPark
    extends OperationSet
{
    /**
     * The name of the property that enables/disables call paring.
     */
    public static final String IS_CALL_PARK_ENABLED
        = "IS_CALL_PARK_ENABLED";

    /**
     * The name of the property that can specify a call address prefix to be
     * added when parking a call.
     */
    public static final String CALL_PARK_PREFIX_PROPERTY
        = "CALL_PARK_PREFIX_PROPERTY";

    /**
     * Parks an already existing call to the specified parkSlot.
     *
     * @param parkSlot the parking slot where to park the call.
     * @param peer the <tt>CallPeer</tt> to be parked to the specified
     * parking slot.
     * @throws OperationFailedException if parking the specified call to the
     * specified park slot fails
     */
    public void parkCall(String parkSlot, CallPeer peer)
        throws OperationFailedException;
}
