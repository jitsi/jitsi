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
package net.java.sip.communicator.impl.protocol.sip.xcap.model.presrules;

/**
 * The Presence Rules sub-handling element. Specifies the subscription
 * authorization decision that the server should make.
 * <p/>
 * Compliant with rfc5025.
 *
 * @author Grigorii Balutsel
 */
public enum SubHandlingType
{
    /**
     * This action tells the server to reject the subscription, placing it in
     * the "terminated" state.
     */
    Block("block"),
    /**
     * This action tells the server to place the subscription in the "pending"
     * state, and await input from the presentity to determine how to proceed.
     */
    Confirm("confirm"),
    /**
     * This action tells the server to place the subscription into the "active"
     * state, and to produce a presence document that indicates that the
     * presentity is unavailable.
     */
    PoliteBlock("polite-block"),
    /**
     * This action tells the server to place the subscription into the "active"
     * state.
     */
    Allow("allow");

    /**
     * Current enum value.
     */
    private final String value;

    /**
     * Creates enum whith the specified value.
     *
     * @param value the value to set.
     */
    SubHandlingType(String value)
    {
        this.value = value;
    }

    /**
     * Gets the value.
     *
     * @return the value
     */
    public String value()
    {
        return value;
    }

    /**
     * Creates enum from its value.
     *
     * @param value the enum's value.
     * @return created enum.
     */
    public static SubHandlingType fromString(String value)
    {
        if (value != null)
        {
            for (SubHandlingType subHandling : SubHandlingType.values())
            {
                if (value.equalsIgnoreCase(subHandling.value()))
                {
                    return subHandling;
                }
            }
            return null;
        }
        return null;
    }
}
