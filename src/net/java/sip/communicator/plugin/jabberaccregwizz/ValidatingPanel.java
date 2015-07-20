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
package net.java.sip.communicator.plugin.jabberaccregwizz;

/**
 * <tt>ValidatingPanel</tt> validate values in panel and give access of
 * the validation to the registration form.
 *
 * @author Damian Minkov
 */
public interface ValidatingPanel
{
    /**
     * Whether current inserted values into the panel are valid and enough
     * to continue with account creation/modification.
     * @return whether the input values are ok to continue with account
     * creation/modification.
     */
    public boolean isValidated();
}
