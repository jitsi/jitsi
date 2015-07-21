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

import java.io.*;
import java.net.URI;

import javax.sip.address.*;

import net.java.sip.communicator.impl.protocol.sip.*;
import net.java.sip.communicator.impl.protocol.sip.xcap.model.*;
import net.java.sip.communicator.impl.protocol.sip.xcap.model.commonpolicy.*;
import net.java.sip.communicator.impl.protocol.sip.xcap.model.prescontent.*;
import net.java.sip.communicator.impl.protocol.sip.xcap.model.resourcelists.*;
import net.java.sip.communicator.impl.protocol.sip.xcap.model.xcapcaps.*;
import net.java.sip.communicator.util.*;

import org.apache.http.*;
import org.jitsi.util.*;

/**
 * XCAP client implementation.
 * <p/>
 * Compliant with rfc4825, rfc4826, rfc5025 and Presence Content XDM
 * Specification v1.0
 *
 * @author Grigorii Balutsel
 */
public class XCapClientImpl extends BaseHttpXCapClient implements XCapClient
{
    /**
     * Current xcap-caps.
     */
    private XCapCapsType xCapCaps;

    /**
     * Indicates whether or not resource-lists is supported.
     */
    private boolean resourceListsSupported;

    /**
     * Indicates whether or not pres-rules is supported.
     */
    private boolean presRulesSupported;

    /**
     * Indicates whether or not pres-content is supported.
     */
    private boolean presContentSupported;

    /**
     * Connects user to XCap server. Loads xcap-caps server capabilities and
     * anaylyze if resource-lists, pres-rules, pres-content is supported.
     *
     * @param uri         the server location.
     * @param userAddress the URI of the user used for requests
     * @param username the user name.
     * @param password    the user password.
     * @throws XCapException if there is some error during operation.
     */
    @Override
    public void connect(URI uri, Address userAddress, String username, String password)
            throws XCapException
    {
        super.connect(uri, userAddress, username, password);
        try
        {
            xCapCaps = loadXCapCaps();
        }
        catch (XCapException e)
        {
            disconnect();
            throw e;
        }
        for (String namespace : xCapCaps.getNamespaces().getNamespace())
        {
            if (ResourceListsClient.NAMESPACE.equals(namespace))
            {
                resourceListsSupported = true;
            }
            if (PresRulesClient.NAMESPACE.equals(namespace))
            {
                presRulesSupported = true;
            }
            if (PresContentClient.NAMESPACE.equals(namespace))
            {
                presContentSupported = true;
            }
        }
    }

    /**
     * Disconnects user from the XCAP server.
     */
    @Override
    public void disconnect()
    {
        super.disconnect();
        xCapCaps = null;
        resourceListsSupported = false;
    }

    /**
     * Puts the resource-lists to the server.
     *
     * @param resourceLists the resource-lists to be saved on the server.
     * @throws IllegalStateException if the user has not been connected, or
     *                               resource-lists is not supported.
     * @throws XCapException         if there is some error during operation.
     */
    public void putResourceLists(ResourceListsType resourceLists)
            throws XCapException
    {
        assertConnected();
        assertResourceListsSupported();
        String resourceListsDocument = getResourceListsDocument();
        XCapResourceId resourceId = new XCapResourceId(resourceListsDocument);
        try
        {
            if (resourceLists.getList().size() == 0)
            {
                deleteResourceLists();
                return;
            }
            String xml = ResourceListsParser.toXml(resourceLists);
            XCapResource resource = new XCapResource(resourceId, xml,
                    ResourceListsClient.RESOURCE_LISTS_CONTENT_TYPE);
            // Put resource-lists to the server
            putResource(resource);
        }
        catch (ParsingException e)
        {
            throw new XCapException("ResourceLists cannot be parsed", e);
        }
    }

    /**
     * Gets the resource-lists from the server.
     *
     * @return the resource-lists.
     * @throws IllegalStateException if the user has not been connected, or
     *                               resource-lists is not supported.
     * @throws XCapException         if there is some error during operation.
     */
    public ResourceListsType getResourceLists()
            throws XCapException
    {
        assertConnected();
        assertResourceListsSupported();
        String resourceListsDocument = getResourceListsDocument();
        XCapResourceId resourceId = new XCapResourceId(resourceListsDocument);
        try
        {
            String xml = getResource(resourceId,
                    ResourceListsClient.RESOURCE_LISTS_CONTENT_TYPE);
            if (xml == null)
            {
                return new ResourceListsType();
            }
            return ResourceListsParser.fromXml(xml);
        }
        catch (ParsingException e)
        {
            throw new XCapException("ResourceLists cannot be parsed", e);
        }
    }

    /**
     * Deletes the resource-lists from the server.
     *
     * @throws IllegalStateException if the user has not been connected, or
     *                               resource-lists is not supported.
     * @throws XCapException         if there is some error during operation.
     */
    public void deleteResourceLists()
            throws XCapException
    {
        assertConnected();
        assertResourceListsSupported();
        String resourceListsDocument = getResourceListsDocument();
        XCapResourceId resourceId = new XCapResourceId(resourceListsDocument);
        deleteResource(resourceId);
    }

    /**
     * Gets the resource-lists from the server.
     *
     * @param anchor reference to the list.
     * @return the list.
     * @throws IllegalStateException if the user has not been connected, or
     *                               resource-lists is not supported.
     * @throws XCapException         if there is some error during operation.
     */
    public ListType getList(String anchor)
            throws XCapException
    {
        assertConnected();
        assertResourceListsSupported();
        return null;
        // TODO: uncomment after OpenXCAP fixes
//        XCapResourceId resourceId = XCapResourceId.create(anchor);
//        try
//        {
//            // Load list from the server
//            String xml = getResource(resourceId,
//                    ResourceListsClient.ELEMENT_CONTENT_TYPE);
//            if (xml == null)
//            {
//                throw new XCapException(resourceId.toString() + "wasn't find");
//            }
//            return (ListType) XmlUtils.createDocument(ListType.class, xml);
//        }
//        catch (JAXBException e)
//        {
//            throw new XCapException("ResourceLists cannot be parsed", e);
//        }
    }

    /**
     * Gets the xcap-caps from the server.
     *
     * @return the xcap-caps.
     * @throws IllegalStateException if the user has not been connected.
     * @throws XCapException         if there is some error during operation.
     */
    public XCapCapsType getXCapCaps()
            throws XCapException
    {
        assertConnected();
        return xCapCaps;
    }

    /**
     * Loads the xcap-caps from the server.
     *
     * @return the xcap-caps.
     * @throws IllegalStateException if the user has not been connected.
     * @throws XCapException         if there is some error during operation.
     */
    private XCapCapsType loadXCapCaps()
            throws XCapException
    {
        String xCapCapsDocument = getXCapCapsDocument();
        XCapResourceId resourceId = new XCapResourceId(xCapCapsDocument);
        try
        {
            // Load xcap-caps from the server
            String xml = getResource(resourceId, XCapCapsClient.CONTENT_TYPE);
            if (xml == null)
            {
                throw new XCapException("Server xcap-caps wasn't find");
            }
            return XCapCapsParser.fromXml(xml);
        }
        catch (ParsingException e)
        {
            throw new XCapException("XCapCapsType cannot be parsed", e);
        }
    }

    /**
     * Gets the pres-rules from the server.
     *
     * @return the pres-rules.
     * @throws IllegalStateException if the user has not been connected, or
     *                               pres-rules is not supported.
     * @throws XCapException         if there is some error during operation.
     */
    public RulesetType getPresRules()
            throws XCapException
    {
        assertConnected();
        assertPresRulesSupported();
        String presRulesDocument = getPresRulesDocument();
        XCapResourceId resourceId = new XCapResourceId(presRulesDocument);
        try
        {
            // Load pres-rules from the server
            String xml = getResource(resourceId, PresRulesClient.CONTENT_TYPE);
            if (xml == null)
            {
                return new RulesetType();
            }
            return CommonPolicyParser.fromXml(xml);
        }
        catch (Exception e)
        {
            throw new XCapException("PresRules cannot be parsed", e);
        }
    }

    /**
     * Puts the pres-rules to the server.
     *
     * @param presRules the pres-rules to be saved on the server.
     * @throws IllegalStateException if the user has not been connected, or
     *                               pres-rules is not supported.
     * @throws XCapException         if there is some error during operation.
     */
    public void putPresRules(RulesetType presRules)
            throws XCapException
    {
        assertConnected();
        assertPresRulesSupported();
        String resourceListsDocument = getPresRulesDocument();
        XCapResourceId resourceId = new XCapResourceId(resourceListsDocument);
        try
        {
            String xml = CommonPolicyParser.toXml(presRules);
            XCapResource resource = new XCapResource(resourceId, xml,
                    PresRulesClient.CONTENT_TYPE);
            // Put pres-rules to the server
            putResource(resource);
        }
        catch (ParsingException e)
        {
            throw new XCapException("PresRules cannot be parsed", e);
        }
    }

    /**
     * Deletes the pres-rules from the server.
     *
     * @throws IllegalStateException if the user has not been connected, or
     *                               pres-rules is not supported.
     * @throws XCapException         if there is some error during operation.
     */
    public void deletePresRules()
            throws XCapException
    {
        assertConnected();
        assertResourceListsSupported();
        String presRulesDocument = getPresRulesDocument();
        XCapResourceId resourceId = new XCapResourceId(presRulesDocument);
        deleteResource(resourceId);
    }

    /**
     * Puts the pres-content to the server.
     *
     * @param content   the pres-content to be saved on the server.
     * @param imageName the image name under which pres-content would be saved.
     * @throws IllegalStateException if the user has not been connected, or
     *                               pres-content is not supported.
     * @throws XCapException         if there is some error during operation.
     */
    public void putPresContent(ContentType content, String imageName)
            throws XCapException
    {
        assertConnected();
        assertPresContentSupported();
        String presContentDocument = getPresContentDocument(imageName);
        XCapResourceId resourceId = new XCapResourceId(presContentDocument);
        try
        {
            String xml = PresContentParser.toXml(content);
            XCapResource resource = new XCapResource(resourceId, xml,
                    PresContentClient.CONTENT_TYPE);
            // Put pres-content to the server
            putResource(resource);
        }
        catch (ParsingException e)
        {
            throw new XCapException("ContentType cannot be parsed", e);
        }
    }

    /**
     * Gets the pres-content from the server.
     *
     * @param imageName the image name under which pres-content is saved.
     * @return the pres-content or null if there is no pres-content on
     *         the server.
     * @throws IllegalStateException if the user has not been connected, or
     *                               pres-content is not supported.
     * @throws XCapException         if there is some error during operation.
     */
    public ContentType getPresContent(String imageName)
            throws XCapException
    {
        assertConnected();
        assertPresContentSupported();
        String presContentDocument = getPresContentDocument(imageName);
        XCapResourceId resourceId = new XCapResourceId(presContentDocument);
        try
        {
            // Load pres-content from the server
            XCapHttpResponse response = this.get(resourceId);
            int httpCode = response.getHttpCode();
            String contentType = response.getContentType();
            byte[] content = response.getContent();
            // Analyze the responce
            if (httpCode != HttpStatus.SC_OK)
            {
                if (httpCode == HttpStatus.SC_NOT_FOUND)
                {
                    return null;
                }
                String errorMessage = String.format(
                        "Error %1s while getting %1s PresContent from XCAP server",
                        httpCode,
                        resourceId.toString());
                throw new XCapException(errorMessage);
            }
            if (!contentType.startsWith(PresContentClient.CONTENT_TYPE))
            {
                String errorMessage = String.format(
                        "XCAP server returns invalid PresContent content type: %1s",
                        contentType);
                throw new XCapException(errorMessage);
            }
            if (content == null || content.length == 0)
            {
                throw new XCapException(
                        "XCAP server returns invalid PresContent content");
            }
            try
            {
                return PresContentParser.fromXml(new String(content, "UTF-8"));
            }
            catch (ParsingException e)
            {
                // TODO: remove it after the OpenXCAP fixes
                // The only server that supports it is OpenXCAP server.
                // They do not follow for 100% percent the RFC
                ContentType presContent = new ContentType();
                DataType data = new DataType();
                data.setValue(new String(Base64.encode(content)));
                presContent.setData(data);
                return presContent;
            }
        }
        catch (IOException e)
        {
            String errorMessage = String.format(
                    "%1s resource cannot be read",
                    resourceId.toString());
            throw new XCapException(errorMessage, e);
        }
    }

    /**
     * Deletes the pres-content from the server.
     *
     * @param imageName the image name under which pres-content is saved.
     * @throws IllegalStateException if the user has not been connected, or
     *                               pres-content is not supported.
     * @throws XCapException         if there is some error during operation.
     */
    public void deletePresContent(String imageName)
            throws XCapException
    {
        assertConnected();
        assertPresContentSupported();
        String presContentDocument = getPresContentDocument(imageName);
        XCapResourceId resourceId = new XCapResourceId(presContentDocument);
        deleteResource(resourceId);
    }

    /**
     * Gets the pres-content image uri.
     *
     * @param imageName the image name under which pres-content is saved.
     * @return the pres-content image uri.
     * @throws IllegalStateException if the user has not been connected.
     */
    public URI getPresContentImageUri(String imageName)
    {
        assertConnected();
        String presContentDocument = getPresContentDocument(imageName);
        XCapResourceId resourceId = new XCapResourceId(presContentDocument);
        return getResourceURI(resourceId);
    }

    /**
     * Gets image from the specified uri.
     *
     * @param imageUri the image uri.
     * @return the image.
     * @throws XCapException if there is some error during operation.
     */
    public byte[] getImage(URI imageUri)
            throws XCapException
    {
        assertConnected();
        XCapHttpResponse response = this.get(imageUri);
        int httpCode = response.getHttpCode();
        byte[] content = response.getContent();
        // Analyze the responce
        if (httpCode != HttpStatus.SC_OK)
        {
            String errorMessage = String.format(
                    "Error %1s while getting %2s image from the server",
                    httpCode,
                    imageUri);
            throw new XCapException(errorMessage);
        }
        return content;
    }

    /**
     * Utility method throwing an exception if the resource-lists
     * is not supported.
     *
     * @throws IllegalStateException if the user is not connected.
     */
    protected void assertResourceListsSupported()
    {
        if (!resourceListsSupported)
        {
            throw new IllegalStateException(
                    "XCAP server doesn't support resource-lists");
        }
    }

    /**
     * Utility method throwing an exception if the pres-rules
     * is not supported.
     *
     * @throws IllegalStateException if the user is not connected.
     */
    protected void assertPresRulesSupported()
    {
        if (!presRulesSupported)
        {
            throw new IllegalStateException(
                    "XCAP server doesn't support pres-rules");
        }
    }

    /**
     * Utility method throwing an exception if the pres-content
     * is not supported.
     *
     * @throws IllegalStateException if the user is not connected.
     */
    protected void assertPresContentSupported()
    {
        if (!presContentSupported)
        {
            throw new IllegalStateException(
                    "XCAP server doesn't support pres-content");
        }
    }

    /**
     * Puts XCAP resources to the server. Analyzes HTTP code and tryes to get
     * xcap-error if possible.
     *
     * @param resource the resource.
     * @throws XCapException if there is some error during operation.
     */
    private void putResource(XCapResource resource)
            throws XCapException
    {
        XCapHttpResponse response = this.put(resource);
        int httpCode = response.getHttpCode();
        if (httpCode != HttpStatus.SC_OK && httpCode != HttpStatus.SC_CREATED)
        {
            String errorMessage;
            String xCapErrorMessage = getXCapErrorMessage(response);
            if (xCapErrorMessage != null)
            {
                errorMessage = String.format(
                        "Error %1s while putting %2s to XCAP server. %3s",
                        httpCode,
                        resource.getId().toString(),
                        xCapErrorMessage);
            }
            else
            {
                errorMessage = String.format(
                        "Error %1s while putting %2s to XCAP server",
                        httpCode,
                        resource.getId().toString());
            }
            throw new XCapException(errorMessage);
        }
    }

    /**
     * Gets XCAP resources from the server. Analyzes HTTP code and tryes to get
     * xcap-error if possible.
     *
     * @param resourceId  the resource identifier.
     * @param contentType the resource content-type.
     * @return XCAP resource.
     * @throws XCapException if there is some error during operation.
     */
    private String getResource(XCapResourceId resourceId, String contentType)
            throws XCapException
    {
        try
        {
            // Load resource from the server
            XCapHttpResponse response = this.get(resourceId);
            int httpCode = response.getHttpCode();
            byte[] content = response.getContent();
            // Analyze the response
            if (httpCode != HttpStatus.SC_OK)
            {
                if (httpCode == HttpStatus.SC_NOT_FOUND)
                {
                    return null;
                }
                String errorMessage;
                String xCapErrorMessage = getXCapErrorMessage(response);
                if (xCapErrorMessage != null)
                {
                    errorMessage = String.format(
                            "Error %1s while getting %2s from XCAP server. %3s",
                            httpCode,
                            resourceId.toString(),
                            xCapErrorMessage);
                }
                else
                {
                    errorMessage = String.format(
                            "Error %1s while getting %2s from XCAP server",
                            httpCode,
                            resourceId.toString());
                }
                if (httpCode == HttpStatus.SC_UNAUTHORIZED
                    || httpCode == HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED)
                {
                    String displayName = userAddress.getDisplayName();
                    if(StringUtils.isNullOrEmpty(displayName))
                        displayName = userAddress.toString();

                    showError(null, null,
                        SipActivator.getResources().getI18NString(
                            "impl.protocol.sip.XCAP_ERROR_UNAUTHORIZED",
                            new String[]{displayName}));
                }
                throw new XCapException(errorMessage);
            }

            // an empty list
            if (StringUtils.isNullOrEmpty(response.getContentType())
                    || (content == null || content.length == 0))
            {
                return null;
            }

            if (!response.getContentType().startsWith(contentType))
            {
                String errorMessage = String.format(
                        "XCAP server returns invalid content type: %1s",
                        response.getContentType());
                throw new XCapException(errorMessage);
            }

            return new String(content, "UTF-8");
        }
        catch (IOException e)
        {
            String errorMessage = String.format(
                    "%1s resource cannot be read",
                    resourceId.toString());
            throw new XCapException(errorMessage, e);
        }
    }

    /**
     * Deletes XCAP resources from the server. Analyzes HTTP code and tryes to
     * get xcap-error if possible.
     *
     * @param resourceId the resource identifier.
     * @throws XCapException if there is some error during operation.
     */
    private void deleteResource(XCapResourceId resourceId)
            throws XCapException
    {
        XCapHttpResponse response = this.delete(resourceId);
        int httpCode = response.getHttpCode();
        if (httpCode != HttpStatus.SC_OK && httpCode != HttpStatus.SC_NOT_FOUND)
        {
            String errorMessage;
            String xCapErrorMessage = getXCapErrorMessage(response);
            if (xCapErrorMessage != null)
            {
                errorMessage = String.format(
                        "Error %1s while deleting %2s resource from XCAP server. %3s",
                        httpCode,
                        resourceId.toString(),
                        xCapErrorMessage);
            }
            else
            {
                errorMessage = String.format(
                        "Error %1s while deleting %2s resource from XCAP server",
                        httpCode,
                        resourceId.toString());
            }
            throw new XCapException(errorMessage);
        }
    }

    /**
     * Returns resource lists uri according to rfc4825.
     *
     * @return resource lists uri.
     */
    private String getResourceListsDocument()
    {
        return String.format(ResourceListsClient.DOCUMENT_FORMAT,
                userAddress.getURI().toString());
    }

    /**
     * Returns xcap-caps uri according to rfc4825.
     *
     * @return xcap-caps uri.
     */
    private String getXCapCapsDocument()
    {
        return XCapCapsClient.DOCUMENT_FORMAT;
    }

    /**
     * Gets resource-lists uri according to rfc5025.
     *
     * @return resource-lists uri.
     */
    private String getPresRulesDocument()
    {
        return String.format(PresRulesClient.DOCUMENT_FORMAT,
                userAddress.getURI().toString());
    }

    /**
     * Gets pres-content uri according to rfc.
     *
     * @param imageName the pres-content image name.
     * @return pres-content uri.
     */
    private String getPresContentDocument(String imageName)
    {
        return String.format(PresContentClient.DOCUMENT_FORMAT,
                userAddress.getURI().toString(), imageName);
    }

    /**
     * Indicates whether or not pres-rules is supported.
     */
    public boolean isResourceListsSupported()
    {
        assertConnected();
        return resourceListsSupported;
    }

    /**
     * Indicates whether or not pres-rules is supported.
     */
    public boolean isPresRulesSupported()
    {
        assertConnected();
        return presRulesSupported;
    }

    /**
     * Indicates whether or not pres-rules is supported.
     */
    public boolean isPresContentSupported()
    {
        return presContentSupported;
    }
}
