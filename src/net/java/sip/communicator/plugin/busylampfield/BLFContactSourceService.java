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
package net.java.sip.communicator.plugin.busylampfield;

import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.Logger;
import org.jitsi.util.*;

import java.util.*;

/**
 * The contact source service to manage query and lines.
 *
 * @author Damian Minkov
 */
public class BLFContactSourceService
    implements ContactSourceService
{
    /**
     * The <tt>Logger</tt> used by the
     * <tt>ConfCallsContactSource</tt> class for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(BLFContactSourceService.class);

    /**
     * Type of a recent messages source.
     */
    public static final int BLF_TYPE = CONTACT_LIST_TYPE;


    /**
     * Constants to display name <tt>ContactSource</tt>.
     */
    private final String displayName;

    /**
     * The latest query.
     */
    private BLFContactQuery ourContactQuery = null;

    /**
     * The list of lines this service will handle.
     */
    private Set<OperationSetTelephonyBLF.Line> monitoredLines
        = new HashSet<OperationSetTelephonyBLF.Line>();

    /**
     * The current group index.
     */
    private final int index;

    /**
     * Constructs.
     * @param displayName
     */
    public BLFContactSourceService(String displayName, int index)
    {
        this.displayName = displayName;
        this.index = index;
    }

    /**
     * Returns the identifier of this contact source. Some of the common
     * identifiers are defined here (For example the CALL_HISTORY identifier
     * should be returned by all call history implementations of this interface)
     * @return the identifier of this contact source
     */
    @Override
    public int getType()
    {
        return BLF_TYPE;
    }

    /**
     * Returns a user-friendly string that identifies this contact source.
     * @return the display name of this contact source
     */
    @Override
    public String getDisplayName()
    {
        return displayName;
    }

    /**
     * Queries this search source for the given <tt>queryString</tt>.
     * @param queryString the string to search for
     * @return the created query
     */
    @Override
    public ContactQuery createContactQuery(String queryString)
    {
        return createContactQuery(queryString, 20);
    }

    /**
     * Queries this search source for the given <tt>queryString</tt>.
     *
     * @param queryString the string to search for
     * @param contactCount the maximum count of result contacts
     * @return the created query
     */
    @Override
    public ContactQuery createContactQuery(String queryString, int contactCount)
    {
        if(!StringUtils.isNullOrEmpty(queryString))
            return null;

        if(ourContactQuery == null)
            ourContactQuery = new BLFContactQuery(
                this, monitoredLines, queryString, contactCount);

        return ourContactQuery;
    }

    /**
     * Returns the index of the contact source in the result list.
     *
     * @return the index of the contact source in the result list
     */
    @Override
    public int getIndex()
    {
        // shift it with 2 position (recent messages-0 and chat rooms-1),
        // counting from 0
        return 1 + index;
    }

    /**
     * Adds new line to display.
     * @param line to display status.
     */
    void addLine(OperationSetTelephonyBLF.Line line)
    {
        monitoredLines.add(line);

        if(ourContactQuery != null)
            ourContactQuery.addLine(line, true);
    }

    /**
     * Updates the source contact status.
     * @param line
     * @param status
     */
    void updateLineStatus(OperationSetTelephonyBLF.Line line, int status)
    {
        if(ourContactQuery == null)
            return;

        ourContactQuery.updateLineStatus(line, status);
    }
}
