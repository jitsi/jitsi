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

import java.util.logging.*;

/**
 * A DNS question.
 *
 * @version %I%, %G%
 * @author  Arthur van Hoff
 */
public final class DNSQuestion
    extends DNSEntry
{
    private static Logger logger =
        Logger.getLogger(DNSQuestion.class.toString());

    /**
     * Create a question.
     * @param name
     * @param type
     * @param clazz
     */
    public DNSQuestion(String name, int type, int clazz)
    {
        super(name, type, clazz);

        String SLevel = System.getProperty("jmdns.debug");
        if (SLevel == null) SLevel = "INFO";
        logger.setLevel(Level.parse(SLevel));
    }

    /**
     * Check if this question is answered by a given DNS record.
     */
    boolean answeredBy(DNSRecord rec)
    {
        return (clazz == rec.clazz) &&
            ((type == rec.type) ||
            (type == DNSConstants.TYPE_ANY)) &&
            name.equals(rec.name);
    }

    /**
     * For debugging only.
     */
    @Override
    public String toString()
    {
        return toString("question", null);
    }
}
