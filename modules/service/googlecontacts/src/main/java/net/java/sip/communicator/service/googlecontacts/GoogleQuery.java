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
