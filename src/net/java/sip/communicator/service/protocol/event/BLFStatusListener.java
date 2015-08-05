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
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * Listens for changes in the BLFStatus of the monitored lines we have
 * subscribed for.
 * @author Damian Minkov
 */
public interface BLFStatusListener
    extends EventListener
{
    /**
     * Called whenever a change occurs in the BLFStatus of one of the
     * monitored lines that we have subscribed for.
     * @param event the BLFStatusEvent describing the status change.
     */
    public void blfStatusChanged(BLFStatusEvent event);
}
