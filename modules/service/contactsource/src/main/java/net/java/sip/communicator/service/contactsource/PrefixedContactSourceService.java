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
package net.java.sip.communicator.service.contactsource;

/**
 * Extension for {@link ContactSourceService}s to provide a prefix for phone
 * number to be dialed.
 */
public interface PrefixedContactSourceService
{
    /**
     * Returns the global phone number prefix to be used when calling contacts
     * from this contact source.
     *
     * @return the global phone number prefix
     */
    public String getPhoneNumberPrefix();
}
