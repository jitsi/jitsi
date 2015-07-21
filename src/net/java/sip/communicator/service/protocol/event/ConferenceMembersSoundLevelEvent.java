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

import net.java.sip.communicator.service.protocol.*;

/**
 * Notifies interested parties in <tt>ConferenceMember</tt>s sound level changes.
 * When a <tt>CallPeer</tt> is participating in the conference also as a
 * <tt>ConferenceMember</tt> its sound level would be included in the map of
 * received levels.
 *
 * @author Yana Stamcheva
 */
public class ConferenceMembersSoundLevelEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The maximum level that can be reported for a participant in a conference.
     * Level values should be distributed among MAX_LEVEL and MIN_LEVEL in a
     * way that would appear uniform to users.
     */
    public static final int MAX_LEVEL = 255;

    /**
     * The maximum (zero) level that can be reported for a participant in a
     * conference. Level values should be distributed among MAX_LEVEL and
     * MIN_LEVEL in a way that would appear uniform to users.
     */
    public static final int MIN_LEVEL = 0;

    /**
     * The mapping of <tt>ConferenceMember</tt>s to sound levels. It is
     * presumed that all <tt>ConferenceMember</tt>s not contained in the map has
     * a 0 sound level.
     */
    private final Map<ConferenceMember, Integer> levels;

    /**
     * Creates an instance of <tt>ConferenceMembersSoundLevelEvent</tt> for the
     * given <tt>callPeer</tt> by indicating the mapping of
     * <tt>ConferenceMember</tt>s and sound levels.
     *
     * @param callPeer the <tt>CallPeer</tt> for which this event occurred
     * @param levels the mapping of <tt>ConferenceMember</tt>s to sound levels
     */
    public ConferenceMembersSoundLevelEvent(
        CallPeer callPeer,
        Map<ConferenceMember, Integer> levels)
    {
        super(callPeer);

        this.levels = levels;
    }

    /**
     * Returns the source <tt>CallPeer</tt> for which the event occurred.
     * @return the source <tt>CallPeer</tt> for which the event occurred
     */
    public CallPeer getSourcePeer()
    {
        return (CallPeer) getSource();
    }

    /**
     * Returns the mapping of <tt>ConferenceMember</tt>s to sound levels. It is
     * presumed that all <tt>ConferenceMember</tt>s not contained in the map has
     * a 0 sound level.
     * @return the mapping of <tt>ConferenceMember</tt>s to sound levels
     */
    public Map<ConferenceMember, Integer> getLevels()
    {
        return levels;
    }
}
