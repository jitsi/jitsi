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
package net.java.sip.communicator.service.gui;

/**
 * The <tt>ContactListFilter</tt> is an interface meant to be implemented by
 * modules interested in filtering the contact list. An implementation of this
 * interface should be able to answer if an <tt>UIContact</tt> or an
 * <tt>UIGroup</tt> is matching the corresponding filter.
 *
 * @author Yana Stamcheva
 */
public interface ContactListFilter
{
    /**
     * Indicates if the given <tt>uiGroup</tt> is matching the current filter.
     * @param uiContact the <tt>UIContact</tt> to check
     * @return <tt>true</tt> to indicate that the given <tt>uiContact</tt>
     * matches this filter, <tt>false</tt> - otherwise
     */
    public boolean isMatching(UIContact uiContact);

    /**
     * Indicates if the given <tt>uiGroup</tt> is matching the current filter.
     * @param uiGroup the <tt>UIGroup</tt> to check
     * @return <tt>true</tt> to indicate that the given <tt>uiGroup</tt>
     * matches this filter, <tt>false</tt> - otherwise
     */
    public boolean isMatching(UIGroup uiGroup);

    /**
     * Applies this filter to any interested sources
     * @param filterQuery the <tt>FilterQuery</tt> that tracks the results of
     * this filtering
     */
    public void applyFilter(FilterQuery filterQuery);
}
