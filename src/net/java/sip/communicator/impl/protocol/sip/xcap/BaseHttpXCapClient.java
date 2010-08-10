/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap;

import net.java.sip.communicator.impl.protocol.sip.xcap.utils.*;
import net.java.sip.communicator.impl.protocol.sip.xcap.model.*;
import net.java.sip.communicator.impl.protocol.sip.xcap.model.xcaperror.*;
import net.java.sip.communicator.util.*;
import org.apache.http.*;
import org.apache.http.auth.*;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.*;

import javax.sip.address.*;
import java.io.*;
import java.net.URI;
import java.net.*;

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
     * The default timeout (10 seconds)
     */
    private static int DEFAULT_TIMEOUT = 10 * 1000;

    /**
     * Current server uri.
     */
    protected URI uri;

    /**
     * Current user.
     */
    protected Address userAddress;

    /**
     * Current user password.
     */
    private String password;

    /**
     * Indicates whether or not client is connected.
     */
    private boolean connected;

    /**
     * How many seconds should the client wait for HTTP response.
     */
    private int timeout;

    /**
     * Creates an instance of this XCAP client.
     */
    public BaseHttpXCapClient()
    {
        timeout = DEFAULT_TIMEOUT;
    }

    /**
     * Connects user to XCap server.
     *
     * @param uri         the server location.
     * @param userAddress the user name.
     * @param password    the user password.
     * @throws XCapException if there is some error during operation.
     */
    public void connect(URI uri, Address userAddress, String password)
            throws XCapException
    {
        if (!userAddress.getURI().isSipURI())
        {
            throw new IllegalArgumentException("Address must contains SipUri");
        }
        this.uri = uri;
        this.userAddress = (Address) userAddress.clone();
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
     * Dicsonnects user from the XCAP server.
     */
    public void dicsonnect()
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
        DefaultHttpClient httpClient = createHttpClient();
        try
        {
            HttpGet getMethod = new HttpGet(uri);
            getMethod.setHeader("Connection", "close");
            Credentials credentials =
                    new UsernamePasswordCredentials(getUserName(), password);
            httpClient.getCredentialsProvider().
                    setCredentials(AuthScope.ANY, credentials);
            if (logger.isDebugEnabled())
            {
                String logMessage = String.format(
                        "Getting resource %1s from the server",
                        uri.toString()
                );
                logger.debug(logMessage);
            }
            HttpResponse response = httpClient.execute(getMethod);
            return createResponse(response);
        }
        catch (IOException e)
        {
            String errorMessage = String.format(
                    "%1s resource cannot be read",
                    uri.toString());
            throw new XCapException(errorMessage, e);
        }
        finally
        {
            httpClient.getConnectionManager().shutdown();
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
        DefaultHttpClient httpClient = createHttpClient();
        try
        {
            URI resourceUri = getResourceURI(resource.getId());
            HttpPut putMethod = new HttpPut(resourceUri);
            putMethod.setHeader("Connection", "close");
            StringEntity stringEntity = new StringEntity(resource.getContent());
            stringEntity.setContentType(resource.getContentType());
            stringEntity.setContentEncoding("UTF-8");
            putMethod.setEntity(stringEntity);
            Credentials credentials =
                    new UsernamePasswordCredentials(getUserName(), password);
            httpClient.getCredentialsProvider().
                    setCredentials(AuthScope.ANY, credentials);
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
        DefaultHttpClient httpClient = createHttpClient();
        try
        {
            URI resourceUri = getResourceURI(resourceId);
            HttpDelete deleteMethod = new HttpDelete(resourceUri);
            deleteMethod.setHeader("Connection", "close");
            Credentials credentials =
                    new UsernamePasswordCredentials(getUserName(), password);
            httpClient.getCredentialsProvider().
                    setCredentials(AuthScope.ANY, credentials);
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
        return userAddress != null ?
                ((SipURI) userAddress.getURI()).getUser() : null;
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
     * Gets operation timeout.The deffault value is 10 seconds.
     *
     * @return operation timeout.
     */
    public int getTimeout()
    {
        return timeout;
    }

    /**
     * Sets operation timeout. The deffault value is 10 seconds.
     *
     * @param timeout operation timeout.
     */
    public void setTimeout(int timeout)
    {
        this.timeout = timeout;
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
    {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpParams httpParams = httpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, timeout);
        HttpConnectionParams.setSoTimeout(httpParams, timeout);
        return httpClient;
    }

    /**
     * Creates XCAP response from HTTP response.
     * If HTTP code is 200, 201 or 409 the HTTP content would be readed.
     *
     * @param response the HTTP response.
     * @return the XCAP response.
     * @throws IOException if there is error during reading the HTTP content.
     */
    private XCapHttpResponse createResponse(HttpResponse response)
            throws IOException
    {
        XCapHttpResponse XCapHttpResponse = new XCapHttpResponse();
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
            XCapHttpResponse.setContentType(contentType);
            XCapHttpResponse.setContent(content);
            XCapHttpResponse.setETag(eTag);
        }
        XCapHttpResponse.setHttpCode(statusCode);
        return XCapHttpResponse;
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
}