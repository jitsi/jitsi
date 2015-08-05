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
package net.java.sip.communicator.service.contactsource;

import java.util.regex.*;

/**
 * The <tt>ExtendedContactSourceService</tt> as its name states it, extends the
 * <tt>ContactSourceService</tt> in order to provide the possibility to query
 * a contact source by specifying a given <tt>Pattern</tt>. This interface is
 * meant to be implemented by contact sources, where it is possible to match
 * source results directly through a given <tt>Pattern</tt>, i.e. by using:<br>
 * <code>Matcher matcher = pattern.matcher(myString);
 *       if(matcher.find())
 *          ....
 * </code>
 * The advantages of passing a <tt>Pattern</tt> over a <tt>String</tt> are that
 * a <tt>Pattern</tt> can use a predefined regular expression and
 * can define certain properties like Pattern.CASE_INSENSITIVE,
 * Pattern.UNICODE_CASE, etc., which could be important for the search.
 *
 * @author Yana Stamcheva
 */
public interface ExtendedContactSourceService
    extends ContactSourceService
{
    /**
     * Creates query for the given <tt>searchPattern</tt>.
     * @param queryPattern the pattern to search for
     * @return the created query
     */
    public ContactQuery createContactQuery(Pattern queryPattern);
}
