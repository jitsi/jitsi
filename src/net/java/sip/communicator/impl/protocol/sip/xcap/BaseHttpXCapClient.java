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
import java.net.*;
import java.net.URI;

import javax.sip.address.*;

import net.java.sip.communicator.impl.protocol.sip.*;
import net.java.sip.communicator.impl.protocol.sip.xcap.model.*;
import net.java.sip.communicator.impl.protocol.sip.xcap.model.xcaperror.*;
import net.java.sip.communicator.impl.protocol.sip.xcap.utils.*;
import net.java.sip.communicator.service.certificate.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.httputil.*;
import net.java.sip.communicator.util.*;

import org.apache.http.*;
import org.apache.http.auth.*;
import org.apache.http.client.*;
import org.apache.http.client.methods.*;
import org.apache.http.entity.*;
import org.apache.http.impl.client.*;
import org.osgi.framework.*;

/**
 * Base HTTP XCAP client implementation.
 * <p/>
 * Compliant with rfc4825
 *
 * @author Grigorii Balutsel
 */
public abstract class BaseHttpXCapClient implements HttpXCapClient
{
    /**
     * Class logger.
     */
    private static final Logger logger =
            Logger.getLogger(BaseHttpXCapClient.class);

    /**
     * HTTP Content-Type header.
     */
    public static final String HEADER_CONTENT_TYPE = "Content-Type";

    /**
     * HTTP ETag header.
     */
    public static final String HEADER_ETAG = "ETag";

    /**
     * HTTP If-None-Match header.
     */
    public static final String HEADER_IF_NONE_MATCH = "If-None-Match";

    /**
     * XCap-error content type.
     */
    public static final String XCAP_ERROR_CONTENT_TYPE
            = "application/xcap-error+xml";

    /**
     * Current server uri.
     */
    protected URI uri;

    /**
     * Current user.
     */
    protected Address userAddress;

    /**
     * Current user loginname.
     */
    private String username;

    /**
     * Current user password.
     */
    private String password;

    /**
     * Indicates whether or not client is connected.
     */
    private boolean connected;

    /**
     * The service we use to interact with user regarding certificates.
     */
    private CertificateService certificateVerification;

    /**
     * Creates an instance of this XCAP client.
     */
    public BaseHttpXCapClient()
    {
        ServiceReference guiVerifyReference
            = SipActivator.getBundleContext().getServiceReference(
                CertificateService.class.getName());

        if(guiVerifyReference != null)
            certificateVerification
                = (CertificateService)SipActivator.getBundleContext()
                    .getService(guiVerifyReference);
    }

    /**
     * Connects user to XCap server.
     *
     * @param uri         the server location.
     * @param userAddress the URI of the user used for requests
     * @param username the user name.
     * @param password    the user password.
     * @throws XCapException if there is some error during operation.
     */
    public void connect(URI uri, Address userAddress, String username, String password)
            throws XCapException
    {
        if (!userAddress.getURI().isSipURI())
        {
            throw new IllegalArgumentException("Address must contains SipUri");
        }
        this.uri = uri;
        this.userAddress = (Address) userAddress.clone();
        this.username = username;
        this.password = password == null ? "" : password;
        connected = true;
    }

    /**
     * Checks if user is connected to the XCAP server.
     *
     * @return true if user is connected.
     */
    public boolean isConnected()
    {
        return connected;
    }

    /**
     * Disconnects user from the XCAP server.
     */
    public void disconnect()
    {
        this.uri = null;
        this.userAddress = null;
        this.password = null;
        connected = false;
    }

    /**
     * Gets the resource from the server.
     *
     * @param resourceId resource identifier.
     * @return the server response.
     * @throws IllegalStateException if the user has not been connected.
     * @throws XCapException         if there is some error during operation.
     */
    public XCapHttpResponse get(XCapResourceId resourceId)
            throws XCapException
    {
        return get(getResourceURI(resourceId));
    }

    /**
     * Gets resource from the server.
     *
     * @param uri the resource uri.
     * @return the server response.
     * @throws XCapException if there is error during reading the resource's
     *                       content.
     */
    protected XCapHttpResponse get(URI uri)
            throws XCapException
    {
        DefaultHttpClient httpClient = null;
        try
        {
            httpClient = createHttpClient();

            HttpGet getMethod = new HttpGet(uri);
            getMethod.setHeader("Connection", "close");

            HttpResponse response = httpClient.execute(getMethod);
            XCapHttpResponse result = createResponse(response);
            if (logger.isDebugEnabled())
            {
                byte[] contentBytes = result.getContent();
                String contenString;
                // for debug purposes print only xmls
                // skip the icon queries
                if(contentBytes != null && result.getContentType() != null
                        && !result.getContentType()
                                .startsWith(PresContentClient.CONTENT_TYPE))
                    contenString = new String(contentBytes);
                else
                    contenString = "";

                String logMessage = String.format(
                        "Getting resource %1s from the server content:%2s",
                        uri.toString(),
                        contenString
                );
                logger.debug(logMessage);
            }
            return result;
        }
        catch(UnknownHostException uhe)
        {
            showError(uhe, null, null);
            disconnect();
            throw new XCapException(uhe.getMessage(), uhe);
        }
        catch (IOException e)
        {
            String errorMessage =
                SipActivator.getResources().getI18NString(
                    "impl.protocol.sip.XCAP_ERROR_RESOURCE_ERR",
                    new String[]{uri.toString(),
                                userAddress.getDisplayName()});
            showError(e, null, errorMessage);
            throw new XCapException(errorMessage, e);
        }
        finally
        {
            if(httpClient != null)
                httpClient.getConnectionManager().shutdown();
        }
    }

    /**
     * Shows an error and a short description.
     * @param ex the exception
     */
    static void showError(Exception ex, String title, String message)
    {
        try
        {
            if(title == null)
                title = SipActivator.getResources().getI18NString(
                    "impl.protocol.sip.XCAP_ERROR_TITLE");

            if(message == null)
                message = title + "\n" +
                    ex.getClass().getName() + ": " +
                    ex.getLocalizedMessage();


            if(SipActivator.getUIService() != null)
                SipActivator.getUIService().getPopupDialog()
                    .showMessagePopupDialog(
                        message,
                        title,
                        PopupDialog.ERROR_MESSAGE);
        }
        catch(Throwable t)
        {
            logger.error("Error for error dialog", t);
        }
    }

    /**
     * Puts the resource to the server.
     *
     * @param resource the resource  to be saved on the server.
     * @return the server response.
     * @throws IllegalStateException if the user has not been connected.
     * @throws XCapException         if there is some error during operation.
     */
    public XCapHttpResponse put(XCapResource resource)
            throws XCapException
    {
        DefaultHttpClient httpClient = null;
        try
        {
            httpClient = createHttpClient();

            URI resourceUri = getResourceURI(resource.getId());
            HttpPut putMethod = new HttpPut(resourceUri);
            putMethod.setHeader("Connection", "close");
            StringEntity stringEntity = new StringEntity(resource.getContent());
            stringEntity.setContentType(resource.getContentType());
            stringEntity.setContentEncoding("UTF-8");
            putMethod.setEntity(stringEntity);

            if (logger.isDebugEnabled())
            {
                String logMessage = String.format(
                        "Puting resource %1s to the server %2s",
                        resource.getId().toString(),
                        resource.getContent()
                );
                logger.debug(logMessage);
            }
            HttpResponse response = httpClient.execute(putMethod);
            return createResponse(response);
        }
        catch (IOException e)
        {
            String errorMessage = String.format(
                    "%1s resource cannot be put",
                    resource.getId().toString());
            throw new XCapException(errorMessage, e);
        }
        finally
        {
            if(httpClient != null)
                httpClient.getConnectionManager().shutdown();
        }
    }

    /**
     * Deletes the resource from the server.
     *
     * @param resourceId resource identifier.
     * @return the server response.
     * @throws IllegalStateException if the user has not been connected.
     * @throws XCapException         if there is some error during operation.
     */
    public XCapHttpResponse delete(XCapResourceId resourceId)
            throws XCapException
    {
        assertConnected();
        DefaultHttpClient httpClient = null;
        try
        {
            httpClient = createHttpClient();

            URI resourceUri = getResourceURI(resourceId);
            HttpDelete deleteMethod = new HttpDelete(resourceUri);
            deleteMethod.setHeader("Connection", "close");

            if (logger.isDebugEnabled())
            {
                String logMessage = String.format(
                        "Deleting resource %1s from the server",
                        resourceId.toString()
                );
                logger.debug(logMessage);
            }
            HttpResponse response = httpClient.execute(deleteMethod);
            return createResponse(response);
        }
        catch (IOException e)
        {
            String errorMessage = String.format(
                    "%1s resource cannot be deleted",
                    resourceId.toString());
            throw new XCapException(errorMessage, e);
        }
        finally
        {
            if(httpClient != null)
                httpClient.getConnectionManager().shutdown();
        }
    }

    /**
     * Gets user name.
     *
     * @return the user name.
     */
    public String getUserName()
    {
        return username;
    }

    /**
     * Gets server uri.
     *
     * @return the server uri.
     */
    public URI getUri()
    {
        return uri;
    }

    /**
     * Utility method throwing an exception if the user is not connected.
     *
     * @throws IllegalStateException if the user is not connected.
     */
    protected void assertConnected()
    {
        if (!connected)
        {
            throw new IllegalStateException(
                    "User is not connected to the server");
        }
    }

    /**
     * Gets resource uri from XCAP resource identifier.
     *
     * @param resourceId the resource identifier.
     * @return the resource uri.
     */
    protected URI getResourceURI(XCapResourceId resourceId)
    {
        try
        {
            return new URI(uri.toString() + "/" + resourceId);
        }
        catch (URISyntaxException e)
        {
            throw new IllegalArgumentException(
                    "Invalid XCAP resource identifier", e);
        }
    }

    /**
     * Creates HTTP client with special parameters.
     *
     * @return the HTTP client.
     */
    private DefaultHttpClient createHttpClient()
        throws IOException
    {
        XCapCredentialsProvider credentialsProvider
            = new XCapCredentialsProvider();
        credentialsProvider.setCredentials(
            AuthScope.ANY,
            new UsernamePasswordCredentials(getUserName(), password));

        return HttpUtils.getHttpClient(
            null , null, uri.getHost(), credentialsProvider);
    }

    /**
     * Creates XCAP response from HTTP response.
     * If HTTP code is 200, 201 or 409 the HTTP content would be read.
     *
     * @param response the HTTP response.
     * @return the XCAP response.
     * @throws IOException if there is error during reading the HTTP content.
     */
    private XCapHttpResponse createResponse(HttpResponse response)
            throws IOException
    {
        XCapHttpResponse xcapHttpResponse = new XCapHttpResponse();
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == HttpStatus.SC_OK ||
                statusCode == HttpStatus.SC_CREATED ||
                statusCode == HttpStatus.SC_CONFLICT)
        {
            String contentType = getSingleHeaderValue(response,
                    HEADER_CONTENT_TYPE);
            byte[] content = StreamUtils.read(
                    response.getEntity().getContent());
            String eTag = getSingleHeaderValue(response, HEADER_ETAG);
            xcapHttpResponse.setContentType(contentType);
            xcapHttpResponse.setContent(content);
            xcapHttpResponse.setETag(eTag);
        }
        xcapHttpResponse.setHttpCode(statusCode);
        return xcapHttpResponse;
    }

    /**
     * Reads response from http.
     * @param response the response
     * @return the result String.
     * @throws IOException
     */
    private static String readResponse(HttpResponse response)
            throws IOException
    {
        HttpEntity responseEntity = response.getEntity();
        if (responseEntity.getContentLength() == 0)
        {
            return "";
        }
        byte[] content = StreamUtils.read(responseEntity.getContent());
        return new String(content, "UTF-8");
    }

    /**
     * Gets HTTP header value.
     *
     * @param response   the HTTP response.
     * @param headerName the header name.
     * @return the header value.
     */
    protected static String getSingleHeaderValue(
            HttpResponse response,
            String headerName)
    {
        Header[] headers = response.getHeaders(headerName);
        if (headers != null && headers.length > 0)
        {
            return headers[0].getValue();
        }
        return null;
    }

    /**
     * Analyzes the response and returns xcap error or null
     * if response doesn't have it.
     *
     * @param response the server response.
     * @return xcap error or null.
     */
    protected String getXCapErrorMessage(XCapHttpResponse response)
    {
        int httpCode = response.getHttpCode();
        String contentType = response.getContentType();
        try
        {
            if (httpCode != HttpStatus.SC_CONFLICT || contentType == null ||
                    !contentType.startsWith(XCAP_ERROR_CONTENT_TYPE))
            {
                return null;
            }
            String content = new String(response.getContent());
            XCapErrorType xCapError = XCapErrorParser.fromXml(content);
            XCapError error = xCapError.getError();
            if (error == null)
            {
                return null;
            }
            return error.getPhrase();
        }
        catch (ParsingException e)
        {
            logger.error("XCapError cannot be parsed.");
            return null;
        }
    }

    /**
     * Our credentials provider simple impl.
     */
    private class XCapCredentialsProvider
        implements CredentialsProvider
    {
        /**
         * The credentials to use.
         */
        private Credentials credentials;

        /**
         * Sets credentials no matter of the scope.
         * @param authscope the scope is not used.
         * @param credentials the credentials to use
         */
        public void setCredentials(AuthScope authscope,
                                   Credentials credentials)
        {
            this.credentials = credentials;
        }

        /**
         * Returns the credentials no matter of the scope.
         * @param authscope not important
         * @return the credentials.
         */
        public Credentials getCredentials(AuthScope authscope)
        {
            return credentials;
        }

        /**
         * Clears credentials.
         */
        public void clear()
        {
            credentials = null;
        }
    }
}
