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
package net.java.sip.communicator.util;

/**
 * This class describes an DNS'S SRV record.
 *
 * @author Sebastien Vincent
 */
public class SRVRecord
{
    /**
     * DNSJava SRVRecord.
     */
    private org.xbill.DNS.SRVRecord record;

    /**
     * Constructor.
     *
     * @param record DNSJava SRVRecord
     */
    public SRVRecord(org.xbill.DNS.SRVRecord record)
    {
        this.record = record;
    }

    /**
     * Get port.
     *
     * @return port
     */
    public int getPort()
    {
        return record.getPort();
    }

    /**
     * Get target.
     *
     * @return target
     */
    public String getTarget()
    {
        return record.getTarget().toString();
    }

    /**
     * Get priority.
     *
     * @return priority
     */
    public int getPriority()
    {
        return record.getPriority();
    }

    /**
     * Get weight.
     *
     * @return weight
     */
    public int getWeight()
    {
        return record.getWeight();
    }

    /**
     * Get DNS TTL.
     *
     * @return DNS TTL
     */
    public long getTTL()
    {
        return record.getTTL();
    }

    /**
     * Get domain name.
     *
     * @return domain name
     */
    public String getName()
    {
        return record.getName().toString();
    }

    /**
     * Returns the toString of the org.xbill.DNS.SRVRecord that was passed to
     * the constructor.
     *
     * @return the toString of the org.xbill.DNS.SRVRecord that was passed to
     *         the constructor.
     */
    @Override
    public String toString()
    {
        return record.toString();
    }
}
