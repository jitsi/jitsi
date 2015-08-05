/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright 2003-2005 Arthur van Hoff Rick Blair
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
package net.java.sip.communicator.impl.protocol.zeroconf.jmdns;

import java.util.*;
import java.util.logging.*;

/**
 * DNSState defines the possible states for services registered with JmDNS.
 *
 * @author Werner Randelshofer, Rick Blair
 * @version 1.0  May 23, 2004  Created.
 */
public class DNSState
    implements Comparable<DNSState>
{
    private static Logger logger =
        Logger.getLogger(DNSState.class.toString());

    private final String name;

    /**
     * Ordinal of next state to be created.
     */
    private static int nextOrdinal = 0;
    /**
     * Assign an ordinal to this state.
     */
    private final int ordinal = nextOrdinal++;
    /**
     * Logical sequence of states.
     * The sequence is consistent with the ordinal of a state.
     * This is used for advancing through states.
     */
    private final static ArrayList<DNSState> sequence
        = new ArrayList<DNSState>();

    private DNSState(String name)
    {
        this.name = name;
        sequence.add(this);

        String SLevel = System.getProperty("jmdns.debug");
        if (SLevel == null) SLevel = "INFO";
        logger.setLevel(Level.parse(SLevel));
    }

    @Override
    public final String toString()
    {
        return name;
    }

    public static final DNSState PROBING_1 = new DNSState("probing 1");
    public static final DNSState PROBING_2 = new DNSState("probing 2");
    public static final DNSState PROBING_3 = new DNSState("probing 3");
    public static final DNSState ANNOUNCING_1 = new DNSState("announcing 1");
    public static final DNSState ANNOUNCING_2 = new DNSState("announcing 2");
    public static final DNSState ANNOUNCED = new DNSState("announced");
    public static final DNSState CANCELED = new DNSState("canceled");

    /**
     * Returns the next advanced state.
     * In general, this advances one step in the following sequence: PROBING_1,
     * PROBING_2, PROBING_3, ANNOUNCING_1, ANNOUNCING_2, ANNOUNCED.
     * Does not advance for ANNOUNCED and CANCELED state.
     * @return Returns the next advanced state.
     */
    public final DNSState advance()
    {
        return (isProbing() || isAnnouncing()) ?
            sequence.get(ordinal + 1) :
            this;
    }

    /**
     * Returns to the next reverted state.
     * All states except CANCELED revert to PROBING_1.
     * Status CANCELED does not revert.
     * @return Returns to the next reverted state.
     */
    public final DNSState revert()
    {
        return (this == CANCELED) ? this : PROBING_1;
    }

    /**
     * Returns true, if this is a probing state.
     * @return Returns true, if this is a probing state.
     */
    public boolean isProbing()
    {
        return compareTo(PROBING_1) >= 0 && compareTo(PROBING_3) <= 0;
    }

    /**
     * Returns true, if this is an announcing state.
     * @return Returns true, if this is an announcing state.
     */
    public boolean isAnnouncing()
    {
        return compareTo(ANNOUNCING_1) >= 0 && compareTo(ANNOUNCING_2) <= 0;
    }

    /**
     * Returns true, if this is an announced state.
     * @return Returns true, if this is an announced state.
     */
    public boolean isAnnounced()
    {
        return compareTo(ANNOUNCED) == 0;
    }

    /**
     * Compares two states.
     * The states compare as follows:
     * PROBING_1 &lt; PROBING_2 &lt; PROBING_3 &lt; ANNOUNCING_1 &lt;
     * ANNOUNCING_2 &lt; RESPONDING &lt; ANNOUNCED &lt; CANCELED.
     */
    public int compareTo(DNSState state)
    {
        return ordinal - state.ordinal;
    }
}
