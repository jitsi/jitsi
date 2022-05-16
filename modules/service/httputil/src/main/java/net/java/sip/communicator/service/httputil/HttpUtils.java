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
import java.nio.charset.*;
import java.security.*;
import java.util.*;

import java.util.concurrent.atomic.*;
import javax.net.ssl.*;

import net.java.sip.communicator.service.gui.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.auth.*;
import org.apache.http.client.*;
import org.apache.http.client.config.*;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.*;
import org.apache.http.conn.ssl.*;
import org.apache.http.entity.*;
import org.apache.http.entity.mime.*;
import org.apache.http.impl.client.*;
import org.apache.http.message.*;
import org.apache.http.util.*;

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
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(HttpUtils.class);

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
        try
        {
            HttpGet httpGet = new HttpGet(address);
            AtomicReference<CredentialsProvider> credentialsProvider
                = new AtomicReference<>(null);
            CloseableHttpClient httpClient = getHttpClient(
                null, null,
                httpGet.getURI().getHost(), credentialsProvider);

            HttpEntity result = executeMethod(httpClient,
                (HTTPCredentialsProvider) credentialsProvider.get(), httpGet);

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
     *
     * @return the result http entity.
     */
    private static HttpEntity executeMethod(CloseableHttpClient httpClient,
                                            HTTPCredentialsProvider credentialsProvider,
                                            HttpRequestBase req)
        throws Throwable
    {
        // do it when response (first execution) or till we are unauthorized
        HttpResponse response = null;
        while(response == null
              || response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED
              || response.getStatusLine().getStatusCode() == HttpStatus.SC_FORBIDDEN)
        {
            // if we were unauthorized, lets clear the method and recreate it
            // for new connection with new credentials.
            if(response != null
               && (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED
                    || response.getStatusLine().getStatusCode() == HttpStatus.SC_FORBIDDEN))
            {
                logger.debug("Will retry http connect and credentials input as latest are not correct!");
                throw new AuthenticationException("Authorization needed");
            }
            else
                response = httpClient.execute(req);

            // if user click cancel no need to retry, stop trying
            if (!credentialsProvider.retry())
            {
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
        HttpPost postMethod = new HttpPost(address);
        AtomicReference<CredentialsProvider> credentialsProvider
            = new AtomicReference<>(null);
        try (CloseableHttpClient httpClient = getHttpClient(
            null, null,
            postMethod.getURI().getHost(), credentialsProvider))
        {
            String mimeType = URLConnection.guessContentTypeFromName(
                file.getPath());
            if(mimeType == null)
                mimeType = "application/octet-stream";

            HttpEntity reqEntity = MultipartEntityBuilder.create()
                .addBinaryBody(fileParamName, file, ContentType.create(mimeType), fileParamName)
                .build();

            postMethod.setEntity(reqEntity);

            HttpEntity resEntity =
                executeMethod(httpClient,
                    (HTTPCredentialsProvider) credentialsProvider.get(), postMethod);

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
     * @param formParams the parameters to include in post.
     * @param usernameParamIx the index of the username parameter in the
     * <tt>formParams</tt> if any, otherwise null.
     * @param passwordParamIx the index of the password parameter in the
     * <tt>formParams</tt>
     * if any, otherwise null.
     * @return the result or null if send was not possible or
     * credentials ask if any was canceled.
     */
    public static HTTPResponseResult postForm(String address,
                                   String usernamePropertyName,
                                   String passwordPropertyName,
                                   Map<String, String> formParams,
                                   String usernameParamIx,
                                   String passwordParamIx)
        throws Throwable
    {
        CloseableHttpClient httpClient;
        HttpPost postMethod;
        HttpEntity resEntity = null;

        // if any authentication exception rise while executing
        // will retry
        AuthenticationException authEx;
        AtomicReference<CredentialsProvider> credentialsProviderHolder
            = new AtomicReference<>(null);
        do
        {
            postMethod = new HttpPost(address);
            httpClient = getHttpClient(
                usernamePropertyName, passwordPropertyName,
                postMethod.getURI().getHost(), credentialsProviderHolder);
            HTTPCredentialsProvider credentialsProvider
                = (HTTPCredentialsProvider) credentialsProviderHolder.get();
            try
            {
                // execute post
                resEntity = postForm(
                        httpClient,
                        (HTTPCredentialsProvider) credentialsProviderHolder.get(),
                        postMethod,
                        address,
                        formParams,
                        usernameParamIx,
                        passwordParamIx);

                authEx = null;
            }
            catch(AuthenticationException ex)
            {
                authEx = ex;

                // lets reuse credentialsProvider
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
     * @param formParams the parameter names to include in post.
     * @param usernameParamIx the name of the username parameter in the
     * <tt>formParams</tt> if any, otherwise null.
     * @param passwordParamIx the index of the password parameter in the
     * <tt>formParams</tt>
     * if any, otherwise null.
     * @return the result or null if send was not possible or
     * credentials ask if any was canceled.
     */
    private static HttpEntity postForm(
                                   CloseableHttpClient httpClient,
                                   HTTPCredentialsProvider credentialsProvider,
                                   HttpPost postMethod,
                                   String address,
                                   Map<String, String> formParams,
                                   String usernameParamIx,
                                   String passwordParamIx)
        throws Throwable
    {
        // if we have username and password in the parameters, lets
        // retrieve their values
        // if there are already filled skip asking the user
        Credentials creds = null;
        if(usernameParamIx != null
            && formParams.containsKey(usernameParamIx)
            && passwordParamIx != null
            && formParams.containsKey(passwordParamIx)
            && StringUtils.isEmpty(formParams.get(usernameParamIx))
            && StringUtils.isEmpty(formParams.get(passwordParamIx)))
        {
            URL url = new URL(address);

            // don't allow empty username
            while(creds == null
                  || creds.getUserPrincipal() == null
                  || StringUtils.isEmpty(creds.getUserPrincipal().getName()))
            {
                creds = credentialsProvider.getCredentials(
                        new AuthScope(url.getHost(), url.getPort()));

                // it was user canceled lets stop processing
                if(creds == null && !credentialsProvider.retry())
                {
                    return null;
                }
            }
        }

        // construct the name value pairs we will be sending
        List<NameValuePair> parameters = new ArrayList<>();
        // there can be no params
        if(formParams != null)
        {
            for (Map.Entry<String, String> param : formParams.entrySet())
            {
                // we are on the username index, insert retrieved username value
                if(param.getKey().equals(usernameParamIx) && creds != null)
                {
                    parameters.add(new BasicNameValuePair(
                        param.getKey(), creds.getUserPrincipal().getName()));
                }// we are on the password index, insert retrieved password val
                else if(param.getKey().equals(passwordParamIx) && creds != null)
                {
                    parameters.add(new BasicNameValuePair(
                        param.getKey(), creds.getPassword()));
                }
                else // common name value pair, all info is present
                {
                    parameters.add(new BasicNameValuePair(
                        param.getKey(), param.getValue()));
                }
            }
        }

        String s = URLEncodedUtils.format(parameters, StandardCharsets.UTF_8);
        StringEntity entity = new StringEntity(s, StandardCharsets.UTF_8);
        // set content type to "application/x-www-form-urlencoded"
        entity.setContentType(URLEncodedUtils.CONTENT_TYPE);

        // insert post values encoded.
        postMethod.setEntity(entity);

        // execute post
        return executeMethod(httpClient, credentialsProvider, postMethod);
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
    public static CloseableHttpClient getHttpClient(
        String usernamePropertyName,
        String passwordPropertyName,
        final String address,
        AtomicReference<CredentialsProvider> credentialsProvider)
        throws IOException
    {
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
            throw new IOException(e);
        }

        HTTPCredentialsProvider newCredentialsProvider =
                new HTTPCredentialsProvider(
                    usernamePropertyName, passwordPropertyName);
        if (credentialsProvider == null)
        {
            credentialsProvider = new AtomicReference<>(newCredentialsProvider);
        }
        else
        {
            credentialsProvider.compareAndSet(null, newCredentialsProvider);
        }

        String userAgent = System.getProperty("sip-communicator.application.name")
            + "/"
            + System.getProperty("sip-communicator.version");

        return HttpClientBuilder.create()
            .setUserAgent(userAgent)
            .setSSLContext(sslCtx)
            // The custom SSLContext already takes care of validating the hostname
            .setSSLHostnameVerifier(new NoopHostnameVerifier())
            .setDefaultRequestConfig(RequestConfig.copy(RequestConfig.DEFAULT)
                .setSocketTimeout(10_000)
                .setConnectTimeout(10_000)
                .setMaxRedirects(MAX_REDIRECTS)
                .build())
            .setDefaultCredentialsProvider(credentialsProvider.get())

            // enable retry connecting with default retry handler
            // when connecting has prompted for authentication
            // connection can be disconnected nefore user answers and
            // we need to retry connection, using the credentials provided
            .setRetryHandler(new DefaultHttpRequestRetryHandler(3, true))
            .build();
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
        private String usernamePropertyName;

        /**
         * The property to use to retrieve/store
         * password value if protected site is hit, for password
         * CredentialsStorageService service is used.
         */
        private String passwordPropertyName;

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
            return HTTP_CREDENTIALS_PREFIX + authscope.getHost()
                + "." + authscope.getRealm()
                + "." + authscope.getPort();
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
            return entity.getContent();
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
            return EntityUtils.toString(entity);
        }
    }
}
