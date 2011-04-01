/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.httputil;

import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;
import org.apache.http.*;
import org.apache.http.auth.*;
import org.apache.http.client.*;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.*;
import org.apache.http.conn.scheme.*;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.*;
import org.apache.http.entity.mime.*;
import org.apache.http.entity.mime.content.*;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.*;
import org.apache.http.message.*;
import org.apache.http.params.*;
import org.apache.http.protocol.*;
import org.apache.http.util.*;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Common http utils querying http locations, handling redirects, self-signed
 * certificates, host verify on certificates, password protection and storing
 * and reusing credentials for password protected sites.
 *
 * @author Damian Minkov
 */
public class HttpUtils
{
    /**
     * The <tt>Logger</tt> used by the <tt>HttpUtils</tt> class for logging
     * output.
     */
    private static final Logger logger = Logger.getLogger(HttpUtils.class);

    /**
     * The prefix used when storing credentials for sites when no property
     * is provided.
     */
    private static final String HTTP_CREDENTIALS_PREFIX =
        "net.java.sip.communicator.util.http.credential.";

    /**
     * Opens a connection to the <tt>address</tt>.
     * @param address the address to contact.
     * @return the result if any or null if connection was not possible
     * or canceled by user.
     */
    public static HTTPResponseResult openURLConnection(String address)
    {
        return openURLConnection(address, null, null);
    }

    /**
     * Opens a connection to the <tt>address</tt>.
     * @param address the address to contact.
     * @param usernamePropertyName the property to use to retrieve/store
     * username value if protected site is hit, for username
     * ConfigurationService service is used.
     * @param passwordPropertyName the property to use to retrieve/store
     * password value if protected site is hit, for password
     * CredentialsStorageService service is used.
     * @return the result if any or null if connection was not possible
     * or canceled by user.
     */
    public static HTTPResponseResult openURLConnection(String address,
                                                String usernamePropertyName,
                                                String passwordPropertyName)
    {
        try
        {
            DefaultHttpClient httpClient = getHttpClient(
                usernamePropertyName, passwordPropertyName);

            HttpEntity result = executeMethod(httpClient, new HttpGet(address));

            if(result == null)
                return null;

            return new HTTPResponseResult(result, httpClient);
        }
        catch(Throwable t)
        {
            logger.error("Cannot open connection to:" + address, t);
        }

        return null;
    }

    /**
     * Executes the metod and return the result. Handle ask for password
     * when hitting password protected site.
     * Keep asking for password till user clicks cancel or enters correct
     * password. When 'remember password' is checked password is saved, if this
     * password and username are not correct clear them, if there are correct
     * they stay saved.
     * @param httpClient the configured http client to use.
     * @param req the request for now it is get or post.
     * @return the result http entity.
     */
    private static HttpEntity executeMethod(DefaultHttpClient httpClient,
                                            HttpRequestBase req)
        throws Throwable
    {
        // do it when response (first execution) or till we are unauthorized
        HttpResponse response = null;
        while(response == null
              || response.getStatusLine().getStatusCode()
                    == HttpStatus.SC_UNAUTHORIZED)
        {
            // if we were unauthorized, lets clear the method and recreate it
            // for new connection with new credentials.
            if(response != null)
            {
                if(logger.isDebugEnabled())
                    logger.debug("Will retry http connect and " +
                        "credentials input as latest are not correct!");

                URI uri = req.getURI();
                req.abort();
                req = req.getClass().newInstance();
                req.setURI(uri);

                httpClient.getCredentialsProvider().clear();
                response = httpClient.execute(req);
            }
            else
                response = httpClient.execute(req);

            // if user click cancel no need to retry, stop trying
            if(!((HTTPCredentialsProvider)httpClient
                    .getCredentialsProvider()).retry())
            {
                if(logger.isDebugEnabled())
                    logger.debug("User canceled credentials input.");
                break;
            }
        }

        // if we finally managed to login return the result.
        if(response != null
            && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
        {
            return response.getEntity();
        }

        // is user has canceled no result needed.
        return null;
    }

    /**
     * Posts a <tt>file</tt> to the <tt>address</tt>.
     * @param address the address to post the form to.
     * @param fileParamName the name of the param for the file.
     * @param file the file we will send.
     * @return the result or null if send was not possible or
     * credentials ask if any was canceled.
     */
    public static HTTPResponseResult postFile(String address,
                                   String fileParamName,
                                   File file)
    {
        return postFile(address, fileParamName, file, null, null);
    }

    /**
     * Posts a <tt>file</tt> to the <tt>address</tt>.
     * @param address the address to post the form to.
     * @param fileParamName the name of the param for the file.
     * @param file the file we will send.
     * @param usernamePropertyName the property to use to retrieve/store
     * username value if protected site is hit, for username
     * ConfigurationService service is used.
     * @param passwordPropertyName the property to use to retrieve/store
     * password value if protected site is hit, for password
     * CredentialsStorageService service is used.
     * @return the result or null if send was not possible or
     * credentials ask if any was canceled.
     */
    public static HTTPResponseResult postFile(String address,
                                   String fileParamName,
                                   File file,
                                   String usernamePropertyName,
                                   String passwordPropertyName)
    {
        DefaultHttpClient httpClient = null;
        try
        {
            httpClient = getHttpClient(
                usernamePropertyName, passwordPropertyName);

            HttpPost postMethod = new HttpPost(address);

            String mimeType = URLConnection.guessContentTypeFromName(
                file.getPath());
            if(mimeType == null)
                mimeType = "application/octet-stream";

            FileBody bin = new FileBody(file, mimeType);

            MultipartEntity reqEntity = new MultipartEntity();
            reqEntity.addPart(fileParamName, bin);

            postMethod.setEntity(reqEntity);

            HttpEntity resEntity = executeMethod(httpClient, postMethod);

            if(resEntity == null)
                return null;

            if(logger.isDebugEnabled())
                logger.debug("Post file response: "
                    + EntityUtils.toString(resEntity));

            return new HTTPResponseResult(reqEntity, httpClient);
        }
        catch(Throwable e)
        {
            logger.error("Cannot post file to:" + address, e);
        }

        return null;
    }

    /**
     * Posting form to <tt>address</tt>. For submission we use POST method
     * which is "application/x-www-form-urlencoded" encoded.
     * @param usernamePropertyName the property to use to retrieve/store
     * username value if protected site is hit, for username
     * ConfigurationService service is used.
     * @param passwordPropertyName the property to use to retrieve/store
     * password value if protected site is hit, for password
     * CredentialsStorageService service is used.
     * @param formParamNames the parameter names to include in post.
     * @param formParamValues the corresponding parameter values to use.
     * @param usernameParamIx the index of the username parameter in the
     * <tt>formParamNames</tt> and <tt>formParamValues</tt>
     * if any, otherwise -1.
     * @param passwordParamIx the index of the password parameter in the
     * <tt>formParamNames</tt> and <tt>formParamValues</tt>
     * if any, otherwise -1.
     * @return the result or null if send was not possible or
     * credentials ask if any was canceled.
     */
    public static HTTPResponseResult postForm(String address,
                                   String usernamePropertyName,
                                   String passwordPropertyName,
                                   String[] formParamNames,
                                   String[] formParamValues,
                                   int usernameParamIx,
                                   int passwordParamIx)
    {
        DefaultHttpClient httpClient = null;
        try
        {
            httpClient = getHttpClient(
                usernamePropertyName, passwordPropertyName);

            HttpPost postMethod = new HttpPost(address);

            // if we have username and password in the parameters, lets
            // retrieve their values
            Credentials creds = null;
            if(usernameParamIx != -1
                && usernameParamIx < formParamNames.length
                && passwordParamIx != -1
                && passwordParamIx < formParamNames.length)
            {
                URL url = new URL(address);
                creds = new HTTPCredentialsProvider(
                    usernamePropertyName, passwordPropertyName)
                        .getCredentials(new AuthScope(
                                                url.getHost(), url.getPort()));
            }

            // construct the name value pairs we will be sending
            List<NameValuePair> parameters = new ArrayList<NameValuePair>();
            for(int i = 0; i < formParamNames.length; i++)
            {
                // we are on the username index, insert retrieved username value
                if(i == usernameParamIx && creds != null)
                {
                    parameters.add(new BasicNameValuePair(
                        formParamNames[i], creds.getUserPrincipal().getName()));
                }// we are on the password index, insert retrieved password val
                else if(i == passwordParamIx && creds != null)
                {
                    parameters.add(new BasicNameValuePair(
                        formParamNames[i], creds.getPassword()));
                }
                else // common name value pair, all info is present
                {
                    parameters.add(new BasicNameValuePair(
                        formParamNames[i], formParamValues[i]));
                }
            }

            String s = URLEncodedUtils.format(parameters, HTTP.UTF_8);
            StringEntity entity = new StringEntity(s, HTTP.UTF_8);
            // set content type to "application/x-www-form-urlencoded"
            entity.setContentType(URLEncodedUtils.CONTENT_TYPE);

            // insert post values encoded.
            postMethod.setEntity(entity);

            // execute post
            HttpEntity resEntity = executeMethod(httpClient, postMethod);

            // canceled or no result
            if(resEntity == null)
                return null;

            if(logger.isDebugEnabled())
                logger.debug("Post form response: "
                    + EntityUtils.toString(resEntity));

            return new HTTPResponseResult(resEntity, httpClient);
        }
        catch(Throwable e)
        {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Returns the preconfigured http client,
     * using CertificateVerificationService, timeouts, user-agent,
     * hostname verifier, proxy settings are used from global java settings,
     * if protected site is hit asks for credentials
     * using util.swing.AuthenticationWindow.
     * @param usernamePropertyName the property to use to retrieve/store
     * username value if protected site is hit, for username
     * ConfigurationService service is used.
     * @param passwordPropertyName the property to use to retrieve/store
     * password value if protected site is hit, for password
     * CredentialsStorageService service is used.
     */
    private static DefaultHttpClient getHttpClient(
        String usernamePropertyName,
        String passwordPropertyName)
        throws IOException
    {
        HttpParams params = new BasicHttpParams();
        params.setParameter(CoreConnectionPNames.SO_TIMEOUT, 10000);
        params.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 10000);

        DefaultHttpClient httpClient = new DefaultHttpClient(params);

        HttpProtocolParams.setUserAgent(httpClient.getParams(),
            System.getProperty("sip-communicator.application.name")
                + "/"
                + System.getProperty("sip-communicator.version"));

        SSLContext sslCtx = HttpUtilActivator
                .getCertificateVerificationService().getSSLContext(
                    HttpUtilActivator.getResources().
                        getI18NString(
                            "service.gui.CERT_DIALOG_CLIENT_DESCRIPTION_TXT",
                            new String[]
                            {
                                HttpUtilActivator.getResources().getSettingsString(
                                    "service.gui.APPLICATION_NAME")
                            }));

        Scheme sch = new Scheme("https", 443,
            new SSLSocketFactory(
                    sslCtx, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER));
        httpClient.getConnectionManager().getSchemeRegistry().register(sch);

        // set proxy from default jre settings
        ProxySelectorRoutePlanner routePlanner = new ProxySelectorRoutePlanner(
            httpClient.getConnectionManager().getSchemeRegistry(),
        ProxySelector.getDefault());
        httpClient.setRoutePlanner(routePlanner);

        HTTPCredentialsProvider credentialsProvider =
            new HTTPCredentialsProvider(
                    usernamePropertyName, passwordPropertyName);
        httpClient.setCredentialsProvider(credentialsProvider);

        return httpClient;
    }

    /**
     * The provider asking for password that is inserted into httpclient.
     */
    private static class HTTPCredentialsProvider
        implements CredentialsProvider
    {
        /**
         * Should we continue retrying, this is set when user hits cancel.
         */
        private boolean retry = true;

        /**
         * The last scope we have used, no problem overriding cause
         * we use new HTTPCredentialsProvider instance for every
         * httpclient/request.
         */
        private AuthScope usedScope = null;

        /**
         * The property to use to retrieve/store
         * username value if protected site is hit, for username
         * ConfigurationService service is used.
         */
        private String usernamePropertyName = null;

        /**
         * The property to use to retrieve/store
         * password value if protected site is hit, for password
         * CredentialsStorageService service is used.
         */
        private String passwordPropertyName = null;

        /**
         * Creates HTTPCredentialsProvider.
         * @param usernamePropertyName the property to use to retrieve/store
         * username value if protected site is hit, for username
         * ConfigurationService service is used.
         * @param passwordPropertyName the property to use to retrieve/store
         * password value if protected site is hit, for password
         * CredentialsStorageService service is used.
         */
        HTTPCredentialsProvider(String usernamePropertyName,
                                String passwordPropertyName)
        {
            this.usernamePropertyName = usernamePropertyName;
            this.passwordPropertyName = passwordPropertyName;
        }

        /**
         * Not used.
         */
        public void setCredentials(AuthScope authscope, Credentials credentials)
        {}

        /**
         * Get the {@link org.apache.http.auth.Credentials credentials} for the
         * given authentication scope.
         *
         * @param authscope the {@link org.apache.http.auth.AuthScope
         *                  authentication scope}
         * @return the credentials
         * @see #setCredentials(org.apache.http.auth.AuthScope,
         *      org.apache.http.auth.Credentials)
         */
        public Credentials getCredentials(AuthScope authscope)
        {
            this.usedScope = authscope;

            // if we have specified password and username property will use them
            // if not create one from the scope/site we are connecting to.
            if(passwordPropertyName == null)
                passwordPropertyName = getCredentialProperty(authscope);
            if(usernamePropertyName == null)
                usernamePropertyName = getCredentialProperty(authscope);

            // load the password
            String pass =
                HttpUtilActivator.getCredentialsService().loadPassword(
                    passwordPropertyName);

            // if password is not saved ask user for credentials
            if(pass == null)
            {
                AuthenticationWindow authWindow =
                    new AuthenticationWindow(authscope.getHost(), true, null);
                authWindow.setVisible(true);

                if(!authWindow.isCanceled())
                {
                    Credentials cred = new UsernamePasswordCredentials(
                        authWindow.getUserName(),
                        new String(authWindow.getPassword())
                    );

                    // if password remember is checked lets save passwords,
                    // if they seem not correct later will be removed.
                    if(authWindow.isRememberPassword())
                    {
                        HttpUtilActivator.getConfigurationService().setProperty(
                            usernamePropertyName,
                            authWindow.getUserName());
                        HttpUtilActivator.getCredentialsService().storePassword(
                            passwordPropertyName,
                            new String(authWindow.getPassword())
                        );
                    }

                    return cred;
                }

                // well user canceled credentials input stop retry asking him
                // if credentials are not correct
                retry = false;
            }
            else
            {
                // we have saved values lets return them
                return new UsernamePasswordCredentials(
                        HttpUtilActivator.getConfigurationService().getString(
                            usernamePropertyName),
                        pass);
            }

            return null;
        }

        /**
         * Clear saved password. Used when we are in situation that
         * saved username and password are no longer valid.
         */
        public void clear()
        {
            if(usedScope != null)
            {
                if(passwordPropertyName == null)
                    passwordPropertyName = getCredentialProperty(usedScope);
                if(usernamePropertyName == null)
                    usernamePropertyName = getCredentialProperty(usedScope);

                HttpUtilActivator.getConfigurationService().removeProperty(
                    usernamePropertyName);
                HttpUtilActivator.getCredentialsService().removePassword(
                    passwordPropertyName);
            }
        }

        /**
         * Constructs property name for save if one is not specified.
         * Its in the form
         * HTTP_CREDENTIALS_PREFIX.host.realm.port
         * @param authscope the scope, holds host,realm, port info about
         * the host we are reaching.
         * @return return the constructed property.
         */
        private static String getCredentialProperty(AuthScope authscope)
        {
            StringBuilder pref = new StringBuilder();

            pref.append(HTTP_CREDENTIALS_PREFIX).append(authscope.getHost())
                .append(".").append(authscope.getRealm())
                .append(".").append(authscope.getPort());

            return  pref.toString();
        }

        /**
         * Whether we need to continue retrying.
         * @return whether we need to continue retrying.
         */
        boolean retry()
        {
            return retry;
        }
    }

    /**
     * Input stream wrapper which handles closing the httpclient when
     * everything is retrieved.
     */
    private static class HttpClientInputStream
        extends InputStream
    {
        /**
         * The original input stream.
         */
        InputStream in;

        /**
         * The http client to close.
         */
        HttpClient httpClient;

        /**
         * Creates HttpClientInputStream.
         * @param in the original input stream.
         * @param httpClient the http client to close.
         */
        HttpClientInputStream(InputStream in, HttpClient httpClient)
        {
            this.in = in;
            this.httpClient = httpClient;
        }

        /**
         * Uses parent InputStream read method.
         *
         * @return the next byte of data, or <code>-1</code> if the end of the
         *         stream is reached.
         * @throws java.io.IOException if an I/O error occurs.
         */
        @Override
        public int read()
            throws IOException
        {
            return in.read();
        }

        /**
         * Closes this input stream and releases any system resources associated
         * with the stream. Releases httpclient connections.
         *
         * <p> The <code>close</code> method of <code>InputStream</code> does
         * nothing.
         *
         * @exception  IOException  if an I/O error occurs.
         */
        public void close()
            throws IOException
        {
            super.close();

            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate de-allocation of all system resources
            httpClient.getConnectionManager().shutdown();
        }
    }

    /**
     * Utility class wraps the http requests result and some utility methods
     * for retrieving info and content for the result.
     */
    public static class HTTPResponseResult
    {
        /**
         * The httpclient entity.
         */
        HttpEntity entity;

        /**
         * The httpclient.
         */
        HttpClient httpClient;

        /**
         * Creates HTTPResponseResult.
         * @param entity the httpclient entity.
         * @param httpClient the httpclient.
         */
        HTTPResponseResult(HttpEntity entity, HttpClient httpClient)
        {
            this.entity = entity;
            this.httpClient = httpClient;
        }

        /**
         * Tells the length of the content, if known.
         *
         * @return  the number of bytes of the content, or
         *          a negative number if unknown. If the content length is known
         *          but exceeds {@link java.lang.Long#MAX_VALUE Long.MAX_VALUE},
         *          a negative number is returned.
         */
        public long getContentLength()
        {
            return entity.getContentLength();
        }

         /**
         * Returns a content stream of the entity.
         *
         * @return content stream of the entity.
         *
         * @throws IOException if the stream could not be created
         * @throws IllegalStateException
         *  if content stream cannot be created.
         */
        public InputStream getContent()
            throws IOException, IllegalStateException
        {
            return new HttpClientInputStream(entity.getContent(), httpClient);
        }

        /**
         * Returns a content string of the entity.
         *
         * @return content string of the entity.
         *
         * @throws IOException if the stream could not be created
         */
        public String getContentString()
            throws IOException
        {
            try
            {
                return EntityUtils.toString(entity);
            }
            finally
            {
                if(httpClient != null)
                    httpClient.getConnectionManager().shutdown();
            }
        }
    }
}
