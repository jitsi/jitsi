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
package net.java.sip.communicator.service.httputil;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;

import javax.net.ssl.*;

import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.service.gui.*;

import org.apache.http.*;
import org.apache.http.Header;
import org.apache.http.ProtocolException;
import org.apache.http.auth.*;
import org.apache.http.client.*;
import org.apache.http.client.methods.*;
import org.apache.http.client.params.*;
import org.apache.http.client.utils.*;
import org.apache.http.conn.scheme.*;
import org.apache.http.entity.*;
import org.apache.http.entity.mime.*;
import org.apache.http.entity.mime.content.*;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.*;
import org.apache.http.message.*;
import org.apache.http.params.*;
import org.apache.http.protocol.*;
import org.apache.http.util.*;
import org.jitsi.util.*;

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
     * Maximum number of http redirects (301, 302, 303).
     */
    private static final int MAX_REDIRECTS = 10;

    /**
     * Opens a connection to the <tt>address</tt>.
     * @param address the address to contact.
     * @return the result if any or null if connection was not possible
     * or canceled by user.
     */
    public static HTTPResponseResult openURLConnection(String address)
    {
        return openURLConnection(address, null, null, null, null);
    }

    /**
     * Opens a connection to the <tt>address</tt>.
     * @param address the address to contact.
     * @param headerParamNames additional header name to include
     * @param headerParamValues corresponding header value to include
     * @return the result if any or null if connection was not possible
     * or canceled by user.
     */
    public static HTTPResponseResult openURLConnection(String address,
            String[] headerParamNames,
            String[] headerParamValues)
    {
        return openURLConnection(address, null, null, headerParamNames,
                headerParamValues);
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
     * @param headerParamNames additional header name to include
     * @param headerParamValues corresponding header value to include
     * @return the result if any or null if connection was not possible
     * or canceled by user.
     */
    public static HTTPResponseResult openURLConnection(String address,
                                                String usernamePropertyName,
                                                String passwordPropertyName,
                                                String[] headerParamNames,
                                                String[] headerParamValues)
    {
        try
        {
            HttpGet httpGet = new HttpGet(address);
            DefaultHttpClient httpClient = getHttpClient(
                usernamePropertyName, passwordPropertyName,
                httpGet.getURI().getHost(), null);

            /* add additional HTTP header */
            if(headerParamNames != null && headerParamValues != null)
            {
                for(int i = 0 ; i < headerParamNames.length ; i++)
                {
                    httpGet.addHeader(new BasicHeader(headerParamNames[i],
                            headerParamValues[i]));
                }
            }

            HttpEntity result = executeMethod(httpClient, httpGet, null, null);

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
     * Executes the method and return the result. Handle ask for password
     * when hitting password protected site.
     * Keep asking for password till user clicks cancel or enters correct
     * password. When 'remember password' is checked password is saved, if this
     * password and username are not correct clear them, if there are correct
     * they stay saved.
     * @param httpClient the configured http client to use.
     * @param req the request for now it is get or post.
     * @param redirectHandler handles redirection, should we redirect and
     * the actual redirect.
     * @param parameters if we are redirecting we can use already filled
     * username and password in order to avoid asking the user twice.
     *
     * @return the result http entity.
     */
    private static HttpEntity executeMethod(DefaultHttpClient httpClient,
                                            HttpRequestBase req,
                                            RedirectHandler redirectHandler,
                                            List<NameValuePair> parameters)
        throws Throwable
    {
        // do it when response (first execution) or till we are unauthorized
        HttpResponse response = null;
        int redirects = 0;
        while(response == null
              || response.getStatusLine().getStatusCode()
                    == HttpStatus.SC_UNAUTHORIZED
              || response.getStatusLine().getStatusCode()
                            == HttpStatus.SC_FORBIDDEN)
        {
            // if we were unauthorized, lets clear the method and recreate it
            // for new connection with new credentials.
            if(response != null
               && (response.getStatusLine().getStatusCode()
                    == HttpStatus.SC_UNAUTHORIZED
                    || response.getStatusLine().getStatusCode()
                                        == HttpStatus.SC_FORBIDDEN))
            {
                if(logger.isDebugEnabled())
                    logger.debug("Will retry http connect and " +
                        "credentials input as latest are not correct!");

                throw new AuthenticationException("Authorization needed");
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

            // check for post redirect as post redirects are not handled
            // automatically
            // RFC2616 (10.3 Redirection 3xx).
            // The second request (forwarded method) can only be a GET or HEAD.
            Header locationHeader = response.getFirstHeader("location");

            if(locationHeader != null
                && req instanceof HttpPost
                &&  (response.getStatusLine().getStatusCode()
                        == HttpStatus.SC_MOVED_PERMANENTLY
                     || response.getStatusLine().getStatusCode()
                        == HttpStatus.SC_MOVED_TEMPORARILY
                     || response.getStatusLine().getStatusCode()
                        == HttpStatus.SC_SEE_OTHER)
                && redirects < MAX_REDIRECTS)
            {
                HttpRequestBase oldreq = req;
                oldreq.abort();

                String newLocation = locationHeader.getValue();

                // lets ask redirection handler if any
                if(redirectHandler != null
                    && redirectHandler.handleRedirect(newLocation, parameters))
                {
                    return null;
                }

                req = new HttpGet(newLocation);
                req.setParams(oldreq.getParams());
                req.setHeaders(oldreq.getAllHeaders());

                redirects++;
                response = httpClient.execute(req);
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
            HttpPost postMethod = new HttpPost(address);

            httpClient = getHttpClient(
                usernamePropertyName, passwordPropertyName,
                postMethod.getURI().getHost(), null);

            String mimeType = URLConnection.guessContentTypeFromName(
                file.getPath());
            if(mimeType == null)
                mimeType = "application/octet-stream";

            FileBody bin = new FileBody(file, mimeType);

            MultipartEntity reqEntity = new MultipartEntity();
            reqEntity.addPart(fileParamName, bin);

            postMethod.setEntity(reqEntity);

            HttpEntity resEntity =
                executeMethod(httpClient, postMethod, null, null);

            if(resEntity == null)
                return null;

            return new HTTPResponseResult(resEntity, httpClient);
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
     * @param address HTTP address.
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
                                   ArrayList<String> formParamNames,
                                   ArrayList<String> formParamValues,
                                   int usernameParamIx,
                                   int passwordParamIx)
        throws Throwable
    {
        return postForm(
            address,
            usernamePropertyName, passwordPropertyName,
            formParamNames, formParamValues,
            usernameParamIx, passwordParamIx,
            null);
    }

    /**
     * Posting form to <tt>address</tt>. For submission we use POST method
     * which is "application/x-www-form-urlencoded" encoded.
     * @param address HTTP address.
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
     * @param redirectHandler handles redirection, should we redirect and
     * the actual redirect.
     * @return the result or null if send was not possible or
     * credentials ask if any was canceled.
     */
    public static HTTPResponseResult postForm(String address,
                                              String usernamePropertyName,
                                              String passwordPropertyName,
                                              ArrayList<String> formParamNames,
                                              ArrayList<String> formParamValues,
                                              int usernameParamIx,
                                              int passwordParamIx,
                                              RedirectHandler redirectHandler)
        throws Throwable
    {
        return postForm(
            address,
            usernamePropertyName, passwordPropertyName,
            formParamNames, formParamValues,
            usernameParamIx, passwordParamIx,
            redirectHandler,
            null, null);
    }

    /**
     * Posting form to <tt>address</tt>. For submission we use POST method
     * which is "application/x-www-form-urlencoded" encoded.
     * @param address HTTP address.
     * @param headerParamNames additional header name to include
     * @param headerParamValues corresponding header value to include
     * @return the result or null if send was not possible or
     * credentials ask if any was canceled.
     */
    public static HTTPResponseResult postForm(String address,
                                              List<String> headerParamNames,
                                              List<String> headerParamValues)
        throws Throwable
    {
        return postForm(address, null, null, null, null, -1, -1, null,
            headerParamNames, headerParamValues);
    }

    /**
     * Posting form to <tt>address</tt>. For submission we use POST method
     * which is "application/x-www-form-urlencoded" encoded.
     * @param address HTTP address.
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
     * @param redirectHandler handles redirection, should we redirect and
     * the actual redirect.
     * @param headerParamNames additional header name to include
     * @param headerParamValues corresponding header value to include
     * @return the result or null if send was not possible or
     * credentials ask if any was canceled.
     */
    public static HTTPResponseResult postForm(String address,
                                   String usernamePropertyName,
                                   String passwordPropertyName,
                                   ArrayList<String> formParamNames,
                                   ArrayList<String> formParamValues,
                                   int usernameParamIx,
                                   int passwordParamIx,
                                   RedirectHandler redirectHandler,
                                   List<String> headerParamNames,
                                   List<String> headerParamValues)
        throws Throwable
    {
        DefaultHttpClient httpClient;
        HttpPost postMethod;
        HttpEntity resEntity = null;

        // if any authentication exception rise while executing
        // will retry
        AuthenticationException authEx;
        HTTPCredentialsProvider credentialsProvider = null;
        do
        {
            postMethod = new HttpPost(address);
            httpClient = getHttpClient(
                usernamePropertyName, passwordPropertyName,
                postMethod.getURI().getHost(), credentialsProvider);

            try
            {
                // execute post
                resEntity = postForm(
                        httpClient,
                        postMethod,
                        address,
                        formParamNames,
                        formParamValues,
                        usernameParamIx,
                        passwordParamIx,
                        redirectHandler,
                        headerParamNames,
                        headerParamValues);

                authEx = null;
            }
            catch(AuthenticationException ex)
            {
                authEx = ex;

                // lets reuse credentialsProvider
                credentialsProvider = (HTTPCredentialsProvider)
                    httpClient.getCredentialsProvider();
                String userName = credentialsProvider.authUsername;

                // clear
                credentialsProvider.clear();

                // lets show the same username
                credentialsProvider.authUsername = userName;
                credentialsProvider.errorMessage =
                    HttpUtilActivator.getResources().getI18NString(
                    "service.gui.AUTHENTICATION_FAILED",
                    new String[]
                            {credentialsProvider.usedScope.getHost()});
            }
        }
        while(authEx != null);

        // canceled or no result
        if(resEntity == null)
            return null;

        return new HTTPResponseResult(resEntity, httpClient);
    }

    /**
     * Posting form to <tt>address</tt>. For submission we use POST method
     * which is "application/x-www-form-urlencoded" encoded.
     * @param httpClient the http client
     * @param postMethod the post method
     * @param address HTTP address.
     * @param formParamNames the parameter names to include in post.
     * @param formParamValues the corresponding parameter values to use.
     * @param usernameParamIx the index of the username parameter in the
     * <tt>formParamNames</tt> and <tt>formParamValues</tt>
     * if any, otherwise -1.
     * @param passwordParamIx the index of the password parameter in the
     * <tt>formParamNames</tt> and <tt>formParamValues</tt>
     * if any, otherwise -1.
     * @param headerParamNames additional header name to include
     * @param headerParamValues corresponding header value to include
     * @return the result or null if send was not possible or
     * credentials ask if any was canceled.
     */
    private static HttpEntity postForm(
                                   DefaultHttpClient httpClient,
                                   HttpPost postMethod,
                                   String address,
                                   ArrayList<String> formParamNames,
                                   ArrayList<String> formParamValues,
                                   int usernameParamIx,
                                   int passwordParamIx,
                                   RedirectHandler redirectHandler,
                                   List<String> headerParamNames,
                                   List<String> headerParamValues)
        throws Throwable
    {
        // if we have username and password in the parameters, lets
        // retrieve their values
        // if there are already filled skip asking the user
        Credentials creds = null;
        if(usernameParamIx != -1
            && usernameParamIx < formParamNames.size()
            && passwordParamIx != -1
            && passwordParamIx < formParamNames.size()
            && (formParamValues.get(usernameParamIx) == null
                || formParamValues.get(usernameParamIx).length() == 0)
            && (formParamValues.get(passwordParamIx) == null
                || formParamValues.get(passwordParamIx).length() == 0))
        {
            URL url = new URL(address);
            HTTPCredentialsProvider prov = (HTTPCredentialsProvider)
                    httpClient.getCredentialsProvider();

            // don't allow empty username
            while(creds == null
                  || creds.getUserPrincipal() == null
                  || StringUtils.isNullOrEmpty(
                        creds.getUserPrincipal().getName()))
            {
                creds =  prov.getCredentials(
                        new AuthScope(url.getHost(), url.getPort()));

                // it was user canceled lets stop processing
                if(creds == null && !prov.retry())
                {
                    return null;
                }
            }
        }

        // construct the name value pairs we will be sending
        List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        // there can be no params
        if(formParamNames != null)
        {
            for(int i = 0; i < formParamNames.size(); i++)
            {
                // we are on the username index, insert retrieved username value
                if(i == usernameParamIx && creds != null)
                {
                    parameters.add(new BasicNameValuePair(
                        formParamNames.get(i), creds.getUserPrincipal().getName()));
                }// we are on the password index, insert retrieved password val
                else if(i == passwordParamIx && creds != null)
                {
                    parameters.add(new BasicNameValuePair(
                        formParamNames.get(i), creds.getPassword()));
                }
                else // common name value pair, all info is present
                {
                    parameters.add(new BasicNameValuePair(
                        formParamNames.get(i), formParamValues.get(i)));
                }
            }
        }

        // our custom strategy, will check redirect handler should we redirect
        // if missing will use the default handler
        httpClient.setRedirectStrategy(
            new CustomRedirectStrategy(redirectHandler, parameters));

        // Uses String UTF-8 to keep compatible with android version and
        // older versions of the http client libs, as the one used
        // in debian (4.1.x)
        String s = URLEncodedUtils.format(parameters, "UTF-8");
        StringEntity entity = new StringEntity(s, "UTF-8");
        // set content type to "application/x-www-form-urlencoded"
        entity.setContentType(URLEncodedUtils.CONTENT_TYPE);

        // insert post values encoded.
        postMethod.setEntity(entity);

        if(headerParamNames != null)
        {
            for(int i = 0; i < headerParamNames.size(); i++)
            {
                postMethod.addHeader(
                    headerParamNames.get(i),
                    headerParamValues.get(i));
            }
        }

        // execute post
        return executeMethod(
            httpClient, postMethod, redirectHandler, parameters);
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
     * @param credentialsProvider if not null provider will bre reused
     * in the new client
     * @param address the address we will be connecting to
     */
    public static DefaultHttpClient getHttpClient(
        String usernamePropertyName,
        String passwordPropertyName,
        final String address,
        CredentialsProvider credentialsProvider)
        throws IOException
    {
        HttpParams params = new BasicHttpParams();
        params.setParameter(CoreConnectionPNames.SO_TIMEOUT, 10000);
        params.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 10000);
        params.setParameter(ClientPNames.MAX_REDIRECTS, MAX_REDIRECTS);

        DefaultHttpClient httpClient = new DefaultHttpClient(params);

        HttpProtocolParams.setUserAgent(httpClient.getParams(),
            System.getProperty("sip-communicator.application.name")
                + "/"
                + System.getProperty("sip-communicator.version"));

        SSLContext sslCtx;
        try
        {
            sslCtx = HttpUtilActivator.getCertificateVerificationService()
                .getSSLContext(
                    HttpUtilActivator.getCertificateVerificationService()
                        .getTrustManager(address));
        }
        catch (GeneralSecurityException e)
        {
            throw new IOException(e.getMessage());
        }

        // note to any reviewer concerned about ALLOW_ALL_HOSTNAME_VERIFIER:
        // the SSL context obtained from the certificate service takes care of
        // certificate validation
        try
        {
            Scheme sch =
                new Scheme("https", 443, new SSLSocketFactoryEx(sslCtx));
            httpClient.getConnectionManager().getSchemeRegistry().register(sch);
        }
        catch(Throwable t)
        {
            logger.error("Error creating ssl socket factory", t);
        }

        // set proxy from default jre settings
        ProxySelectorRoutePlanner routePlanner = new ProxySelectorRoutePlanner(
            httpClient.getConnectionManager().getSchemeRegistry(),
        ProxySelector.getDefault());
        httpClient.setRoutePlanner(routePlanner);

        if(credentialsProvider == null)
            credentialsProvider =
                new HTTPCredentialsProvider(
                        usernamePropertyName, passwordPropertyName);
        httpClient.setCredentialsProvider(credentialsProvider);

        // enable retry connecting with default retry handler
        // when connecting has prompted for authentication
        // connection can be disconnected nefore user answers and
        // we need to retry connection, using the credentials provided
        httpClient.setHttpRequestRetryHandler(
            new DefaultHttpRequestRetryHandler(3, true));

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
         * Authentication username if any.
         */
        private String authUsername = null;

        /**
         * Authentication password if any.
         */
        private String authPassword = null;

        /**
         * Error message.
         */
        private String errorMessage = null;

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
                AuthenticationWindowService authenticationWindowService =
                    HttpUtilActivator.getAuthenticationWindowService();

                if(authenticationWindowService == null)
                {
                    logger.error(
                        "No AuthenticationWindowService implementation");
                    return null;
                }

                AuthenticationWindowService.AuthenticationWindow authWindow =
                    authenticationWindowService.create(
                        authUsername, null,
                        authscope.getHost(),
                        true,
                        false,
                        null, null, null, null, null,
                        errorMessage,
                        HttpUtilActivator.getResources().getSettingsString(
                            "plugin.provisioning.SIGN_UP_LINK"));
                authWindow.setVisible(true);

                if(!authWindow.isCanceled())
                {
                    Credentials cred = new UsernamePasswordCredentials(
                        authWindow.getUserName(),
                        new String(authWindow.getPassword())
                    );

                    authUsername = authWindow.getUserName();
                    authPassword = new String(authWindow.getPassword());

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
                authUsername =
                HttpUtilActivator.getConfigurationService().getString(
                        usernamePropertyName);
                authPassword = pass;

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
            authUsername = null;
            authPassword = null;
            errorMessage = null;
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

        /**
         * Returns authentication username if any
         * @return authentication username or null
         */
        public String getAuthenticationUsername()
        {
            return authUsername;
        }

        /**
         * Returns authentication password if any
         * @return authentication password or null
         */
        public String getAuthenticationPassword()
        {
            return authPassword;
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
        @Override
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
        DefaultHttpClient httpClient;

        /**
         * Creates HTTPResponseResult.
         * @param entity the httpclient entity.
         * @param httpClient the httpclient.
         */
        HTTPResponseResult(HttpEntity entity, DefaultHttpClient httpClient)
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

        /**
         * Get the credentials used by the request.
         *
         * @return the credentials (login at index 0 and password at index 1)
         */
        public String[] getCredentials()
        {
            String cred[] = new String[2];

            if(httpClient != null)
            {
                HTTPCredentialsProvider prov = (HTTPCredentialsProvider)
                        httpClient.getCredentialsProvider();
                cred[0] = prov.getAuthenticationUsername();
                cred[1] = prov.getAuthenticationPassword();
            }

            return cred;
        }
    }

    /**
     * Custom redirect handler that extends DefaultRedirectStrategy
     * We will check redirect handler should we redirect
     * If redirect handler is missing will continue with default strategy
     */
    private static class CustomRedirectStrategy
        extends DefaultRedirectStrategy
    {
        /**
         * The redirect handler to check.
         */
        private final RedirectHandler handler;

        /**
         * The already filled parameters to be used when redirecting.
         */
        private final List<NameValuePair> parameters;

        /**
         * Created custom redirect strategy.
         * @param handler the redirect handler.
         * @param parameters already filled parameters.
         */
        CustomRedirectStrategy(RedirectHandler handler,
                               List<NameValuePair> parameters)
        {
            this.handler = handler;
            this.parameters = parameters;
        }

        /**
         * Check whether we need to redirect.
         * @param request the initial request
         * @param response the response containing the location param for
         * redirect.
         * @param context the http context.
         * @return should we redirect.
         * @throws ProtocolException
         */
        public boolean isRedirected(
                final HttpRequest request,
                final HttpResponse response,
                final HttpContext context)
            throws ProtocolException
        {
            Header locationHeader = response.getFirstHeader("location");

            if(handler != null
                && locationHeader != null
                && handler.hasParams(locationHeader.getValue()))
            {
                //we will cancel this redirect and will schedule new redirect
                handler.handleRedirect(locationHeader.getValue(), parameters);
                return false;
            }

            return super.isRedirected(request, response, context);
        }
    }

    /**
     * The redirect handler will cancel/proceed the redirection. Can
     * schedule new request with the redirect location, reusing the already
     * filled parameters.
     */
    public static interface RedirectHandler
    {
        /**
         * Schedule new request with the redirect location, reusing the already
         * filled parameters.
         *
         * @param location the new location.
         * @param parameters the parameters that were already filled.
         * @return should we continue with normal redirect.
         */
        public boolean handleRedirect(String location,
                                      List<NameValuePair> parameters);

        /**
         * Do the new location has params that need to be filled, return
         * <tt>true</tt> will cause to handle redirect.
         * @param location the new location.
         * @return <tt>true</tt> if we need to redirect in the handler or
         * <tt>false</tt> if we will continue with default redirect handling.
         */
        boolean hasParams(String location);
    }
}
