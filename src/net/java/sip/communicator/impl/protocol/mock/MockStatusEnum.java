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
package net.java.sip.communicator.impl.protocol.mock;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * An implementation of <tt>PresenceStatus</tt> that allows third parties
 * (external to the protocol provider) to create and eventually set custom
 * presence status intances.
 *
 * @author Emil Ivov
 */
public class MockStatusEnum
    extends PresenceStatus
{
    /**
     * Indicates a status with 0 connectivity.
     */
    public static final MockStatusEnum MOCK_STATUS_00
        = new MockStatusEnum(0, "MockStatus00");

    /**
     * Indicates a status with a connectivity index of 10.
     */
    public static final MockStatusEnum MOCK_STATUS_10
        = new MockStatusEnum(10, "MockStatus10");

    /**
     * Indicates a status with a connectivity index of 20.
     */
    public static final MockStatusEnum MOCK_STATUS_20
        = new MockStatusEnum(20, "MockStatus20");

    /**
     * Indicates a status with a connectivity index of 30.
     */
    public static final MockStatusEnum MOCK_STATUS_30
        = new MockStatusEnum(30, "MockStatus30");

    /**
     * Indicates a status with a connectivity index of 40.
     */
    public static final MockStatusEnum MOCK_STATUS_40
        = new MockStatusEnum(40, "MockStatus40");

    /**
     * Indicates a status with a connectivity index of 50.
     */
    public static final MockStatusEnum MOCK_STATUS_50
        = new MockStatusEnum(50, "MockStatus50");

    /**
     * Indicates a status with a connectivity index of 60.
     */
    public static final MockStatusEnum MOCK_STATUS_60
        = new MockStatusEnum(60, "MockStatus60");

    /**
     * Indicates a status with a connectivity index of 70.
     */
    public static final MockStatusEnum MOCK_STATUS_70
        = new MockStatusEnum(70, "MockStatus70");

    /**
     * Indicates a status with a connectivity index of 80.
     */
    public static final MockStatusEnum MOCK_STATUS_80
        = new MockStatusEnum(80, "MockStatus80");

    /**
     * Indicates a status with a connectivity index of 90.
     */
    public static final MockStatusEnum MOCK_STATUS_90
        = new MockStatusEnum(90, "MockStatus90");

    /**
     * Indicates a status with a connectivity index of 100.
     */
    public static final MockStatusEnum MOCK_STATUS_100
        = new MockStatusEnum(100, "MockStatus100");


    private static List<PresenceStatus> supportedStatusSet = new LinkedList<PresenceStatus>();
    static{
        supportedStatusSet.add(MOCK_STATUS_00);
        supportedStatusSet.add(MOCK_STATUS_10);
        supportedStatusSet.add(MOCK_STATUS_20);
        supportedStatusSet.add(MOCK_STATUS_30);
        supportedStatusSet.add(MOCK_STATUS_40);
        supportedStatusSet.add(MOCK_STATUS_50);
        supportedStatusSet.add(MOCK_STATUS_60);
        supportedStatusSet.add(MOCK_STATUS_70);
        supportedStatusSet.add(MOCK_STATUS_80);
        supportedStatusSet.add(MOCK_STATUS_90);
        supportedStatusSet.add(MOCK_STATUS_100);
    }

    /**
     * Creates an instance of <tt>MockPresneceStatus</tt> with the specified
     * parameters.
     * @param status the connectivity level of the new presence status instance
     * @param statusName the name of the presence status.
     */
    private MockStatusEnum(int status, String statusName)
    {
        super(status, statusName);
    }

    /**
     * Returns an iterator over all status instances supproted by the mock
     * provider.
     * @return an <tt>Iterator</tt> over all status instances supported by the
     * mock provider.
     */
    static Iterator<PresenceStatus> supportedStatusSet()
    {
        return supportedStatusSet.iterator();
    }
}
