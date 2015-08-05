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
package net.java.sip.communicator.util.launchutils;

/**
 * The <tt>UriDelegationPeer</tt> is used as a mechanism to pass arguments from
 * the UriArgManager which resides in "launcher space" to our argument
 * delegation service implementation that lives as an osgi bundle. An instance
 * of this peer is created from within the argument delegation service impl
 * and is registered with the UriArgManager.
 *
 * @author Emil Ivov
 */
public interface ArgDelegationPeer
{
    /**
     * Handles <tt>uriArg</tt> in whatever way it finds fit.
     *
     * @param uriArg the uri argument that this delegate has to handle.
     */
    public void handleUri(String uriArg);

    /**
     * Called when the user has tried to launch a second instance of
     * SIP Communicator while a first one was already running. A typical
     * implementation of this method would simply bring the application on
     * focus but it may also show an error/information message to the user
     * notifying them that a second instance is not to be launched.
     */
    public void handleConcurrentInvocationRequest();
}
