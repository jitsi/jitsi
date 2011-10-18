/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.googlecontacts;

import java.util.regex.*;

/**
 * Describes a Google query.
 *
 * @author Sebastien Vincent
 */
public class GoogleQuery
{
    /**
     * If the query is cancelled.
     */
    private boolean cancelled = false;

    /**
     * The query pattern.
     */
    private Pattern query = null;

    /**
     * Constructor.
     *
     * @param query query string
     */
    public GoogleQuery(Pattern query)
    {
        this.query = query;
    }

    /**
     * Get the query pattern.
     *
     * @return query pattern
     */
    public Pattern getQueryPattern()
    {
        return query;
    }

    /**
     * Cancel the query.
     */
    public void cancel()
    {
        cancelled = true;
    }

    /**
     * If the query has been cancelled.
     *
     * @return true If the query has been cancelled, false otherwise
     */
    public boolean isCancelled()
    {
        return cancelled;
    }
}
