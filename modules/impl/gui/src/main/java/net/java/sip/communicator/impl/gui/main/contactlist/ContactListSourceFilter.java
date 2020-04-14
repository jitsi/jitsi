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
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.util.*;

import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.gui.*;

/**
 * The <tt>ContactListSourceFilter</tt> is a <tt>ContactListFilter</tt> that
 * allows to apply the filter to only one of its contact sources at a time.
 *
 * @author Yana Stamcheva
 */
public interface ContactListSourceFilter
    extends ContactListFilter
{
    /**
     * Applies this filter to the given <tt>contactSource</tt>.
     *
     * @param contactSource the <tt>ExternalContactSource</tt> to apply the
     * filter to
     * @return the <tt>ContactQuery</tt> that tracks this filter
     */
    public ContactQuery applyFilter(ExternalContactSource contactSource);

    /**
     * Returns the list of current <tt>ExternalContactSource</tt>s this filter
     * works with.
     * @return the list of current <tt>ExternalContactSource</tt>s this filter
     * works with
     */
    public Collection<ExternalContactSource> getContactSources();

    /**
     * Indicates if this filter contains a default source.
     * @return <tt>true</tt> if this filter contains a default source,
     * <tt>false</tt> otherwise
     */
    public boolean hasDefaultSource();
}
