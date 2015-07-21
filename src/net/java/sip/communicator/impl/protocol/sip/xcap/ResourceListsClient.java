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
package net.java.sip.communicator.impl.protocol.sip.xcap;

import net.java.sip.communicator.impl.protocol.sip.xcap.model.resourcelists.*;

/**
 * XCAP resource-lists client interface.
 * <p/>
 * Compliant with rfc4825, rfc4826
 *
 * @author Grigorii Balutsel
 */
public interface ResourceListsClient
{
    /**
     * Resource-lists uri format.
     */
    public static String DOCUMENT_FORMAT = "resource-lists/users/%2s/index";

    /**
     * Resource-lists content type.
     */
    public static String RESOURCE_LISTS_CONTENT_TYPE =
            "application/resource-lists+xml";

    /**
     * Resource-lists content type.
     */
    public static String ELEMENT_CONTENT_TYPE = "application/xcap-el+xml";

    /**
     * Resource-lists namespace.
     */
    public static String NAMESPACE = "urn:ietf:params:xml:ns:xcap-caps";

    /**
     * Puts the resource-lists to the server.
     *
     * @param resourceLists the resource-lists to be saved on the server.
     * @throws IllegalStateException if the user has not been connected.
     * @throws XCapException         if there is some error during operation.
     */
    public void putResourceLists(ResourceListsType resourceLists)
            throws XCapException;

    /**
     * Gets the resource-lists from the server.
     *
     * @return the resource-lists.
     * @throws IllegalStateException if the user has not been connected.
     * @throws XCapException         if there is some error during operation.
     */
    public ResourceListsType getResourceLists()
            throws XCapException;

    /**
     * Deletes the resource-lists from the server.
     *
     * @throws IllegalStateException if the user has not been connected.
     * @throws XCapException         if there is some error during operation.
     */
    public void deleteResourceLists()
            throws XCapException;

    /**
     * Gets the resource-lists from the server.
     *
     * @param anchor reference to the list.
     * @return the list.
     * @throws IllegalStateException if the user has not been connected.
     * @throws XCapException         if there is some error during operation.
     */
    public ListType getList(String anchor)
            throws XCapException;

}
