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
package net.java.sip.communicator.impl.dns;

/**
 * Defines how DNSSEC validation errors should be handled.
 *
 * @author Ingo Bauersachs
 */
public enum SecureResolveMode
{
    /**
     * Any DNSSEC data is completely ignored.
     */
    IgnoreDnssec,

    /**
     * The result of a query is only returned if it validated successfully.
     */
    SecureOnly,

    /**
     * The result of a query is returned if it validated successfully or when
     * the zone is unsigned.
     */
    SecureOrUnsigned,

    /**
     * If the result of a query is bogus (manipulated, incorrect), the user is
     * to be asked how to proceed.
     */
    WarnIfBogus,

    /**
     * If the result of a query is bogus (manipulated, incorrect) or if the zone
     * is unsigned, the user is to be asked how to proceed.
     */
    WarnIfBogusOrUnsigned
}
