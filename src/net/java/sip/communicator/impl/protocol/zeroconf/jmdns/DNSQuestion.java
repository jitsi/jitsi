//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL
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
