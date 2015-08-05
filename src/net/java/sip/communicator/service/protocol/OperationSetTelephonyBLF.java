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
package net.java.sip.communicator.service.protocol;

import java.util.*;

import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Provides operations necessary to monitor line activity and pickup calls
 * if needed. BLF stands for Busy Lamp Field.
 * @author Damian Minkov
 */
public interface OperationSetTelephonyBLF
    extends OperationSet
{
    /**
     * Adds BLFStatus listener
     * @param listener the listener to add.
     */
    public void addStatusListener(BLFStatusListener listener);

    /**
     * Removes BLFStatus listener.
     * @param listener the listener to remove.
     */
    public void removeStatusListener(BLFStatusListener listener);

    /**
     * To pickup the call for the monitored line if possible.
     *
     * @param line to try to pick up.
     *
     * @throws OperationFailedException if <tt>line</tt> address is not valid.
     */
    public void pickup(Line line)
        throws OperationFailedException;

    /**
     * List of currently monitored lines.
     * @return list of currently monitored lines.
     */
    public List<Line> getCurrentlyMonitoredLines();

    /**
     * The monitored line.
     */
    public static class Line
        extends DataObject
    {
        /**
         * The address of the line.
         */
        private String address;

        /**
         * The display name of the line.
         */
        private String name;

        /**
         * The group under witch to display the line.
         */
        private String group;

        /**
         * The parent provider.
         */
        private ProtocolProviderService provider;

        /**
         * Constructs Line.
         *
         * @param address the address of the line.
         * @param name the display name if any
         * @param group the group name if any
         * @param provider the parent provider.
         */
        public Line(String address, String name, String group,
                    ProtocolProviderService provider)
        {
            this.address = address;
            this.name = name;
            this.group = group;
            this.provider = provider;
        }

        /**
         * The address of the line.
         * @return address of the line.
         */
        public String getAddress()
        {
            return address;
        }

        /**
         * The name of the line.
         * @return the name of the line.
         */
        public String getName()
        {
            return name;
        }

        /**
         * The group name.
         * @return the group name.
         */
        public String getGroup()
        {
            return group;
        }

        /**
         * The provider.
         * @return the provider.
         */
        public ProtocolProviderService getProvider()
        {
            return provider;
        }

        @Override
        public boolean equals(Object o)
        {
            if(this == o) return true;
            if(o == null || getClass() != o.getClass()) return false;

            Line line = (Line) o;

            return address.equals(line.address);

        }

        @Override
        public int hashCode()
        {
            return address.hashCode();
        }
    }
}
