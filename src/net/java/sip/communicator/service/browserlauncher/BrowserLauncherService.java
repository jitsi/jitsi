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
package net.java.sip.communicator.service.browserlauncher;

/**
 * Represents a service which is able to open an URL in a browser.
 *
 * @author Yana Stamcheva
 */
public interface BrowserLauncherService
{

    /**
     * Tries to open the specified URL in a browser. The attempt is asynchronously
     * executed and does not wait for possible errors related to the launching
     * of the associated browser and the opening of the specified URL in it i.e.
     * the method returns immediately and does not report the success or the
     * failure of the opening.
     *
     * @param url a <tt>String</tt> value which represents the URL to be opened
     * in a browser
     */
    public void openURL(String url);
}
