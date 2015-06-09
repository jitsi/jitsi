/*
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
package net.java.sip.communicator.impl.googlecontacts;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.util.*;

import com.google.api.client.auth.oauth2.*;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.*;
import com.google.api.client.json.jackson2.*;

/**
 * OAuth 2 token store.
 *
 * @author Danny van Heumen
 */
public class OAuth2TokenStore
{

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(OAuth2TokenStore.class);

    /**
     * Google OAuth 2 token server.
     */
    private static final GenericUrl GOOGLE_OAUTH2_TOKEN_SERVER =
        new GenericUrl("https://accounts.google.com/o/oauth2/token");

    /**
     * Client ID for OAuth 2 based authentication.
     */
    private static final String GOOGLE_API_CLIENT_ID = null;

    /**
     * Client secret for OAuth 2 based authentication.
     */
    private static final String GOOGLE_API_CLIENT_SECRET = null;

    /**
     * Required OAuth 2 authentication scopes.
     */
    private static final String GOOGLE_API_OAUTH2_SCOPES =
        "profile%20email%20https://www.google.com/m8/feeds";

    /**
     * OAuth 2 redirect URL.
     */
    private static final String GOOGLE_API_OAUTH2_REDIRECT_URI =
        "urn:ietf:wg:oauth:2.0:oob";

    /**
     * Approval URL.
     */
    private static final String APPROVAL_URL = String.format(
        "https://accounts.google.com/o/oauth2/auth?scope=%s&redirect_uri=%s&response_type=code&client_id=%s",
        GOOGLE_API_OAUTH2_SCOPES,
        GOOGLE_API_OAUTH2_REDIRECT_URI,
        GOOGLE_API_CLIENT_ID);

    /**
     * The credential store.
     *
     * Note: The AtomicReference container is used as a shared container that is
     * also passed on to some of the registered listeners for updating the
     * credential data.
     */
    private final AtomicReference<Credential> store =
        new AtomicReference<Credential>(null);

    /**
     * Get the credential from the store. In case a credential does not (yet)
     * exist, acquire one preferrably from the password store. Optionally,
     * involve the user if a credential is not yet stored.
     * 
     * @return Returns the credential.
     * @throws FailedAcquireCredentialException 
     * @throws MalformedURLException In case requesting authn token failed.
     */
    public Credential get() throws FailedAcquireCredentialException
    {
        if (this.store.get() == null)
        {
            try
            {
                acquireCredential(this.store);
            }
            catch (Exception e)
            {
                throw new FailedAcquireCredentialException(e);
            }
        }
        // should make sure that only succeeded requests reach up to here
        return this.store.get();
    }

    /**
     * Acquire a new credential instance.
     *
     * @param store credential store to update upon refreshing and other
     *            operations
     * @return Acquires and returns the credential instance.
     * @throws MalformedURLException In case of bad redirect URL.
     * @throws URISyntaxException In case of bad redirect URI.
     */
    private static void acquireCredential(
        final AtomicReference<Credential> store)
        throws MalformedURLException,
        URISyntaxException
    {
        LOGGER.info("No credentials available yet. Requesting user to "
            + "approve access to Contacts API using URL: " + APPROVAL_URL);
        final OAuthApprovalDialog dialog = new OAuthApprovalDialog();
        dialog.setVisible(true);
        final String approvalCode = dialog.getApprovalCode();
        LOGGER.debug("Approval code from user: " + approvalCode);
        final TokenData data = requestAuthenticationToken(approvalCode);
        store.set(createCredential(store, data));
    }

    /**
     * Refresh OAuth2 authentication token.
     *
     * @throws IOException
     * @throws FailedTokenRefreshException In case of failed token refresh
     *             operation.
     */
    public void refresh() throws IOException, FailedTokenRefreshException
    {
        final Credential credential = this.store.get();
        if (credential == null)
        {
            throw new IllegalStateException("A credential instance should "
                + "exist, but it does not. This is likely due to a bug.");
        }
        if (!credential.refreshToken())
        {
            LOGGER.warn("Refresh of OAuth2 authentication token failed.");
            throw new FailedTokenRefreshException();
        }
    }

    /**
     * Create credential instance suitable for use in Google Contacts API.
     * 
     * @param store reference to the credential store for updating credential
     *            data upon refreshing and other cases
     * @param approvalCode the approval code received from Google by the user
     *            accepting the authorization request
     * @return Returns a Credential instance.
     * @throws URISyntaxException In case of bad OAuth 2 redirect URI.
     */
    private static Credential createCredential(
        final AtomicReference<Credential> store, final TokenData data) throws URISyntaxException
    {
        final Credential.Builder builder =
            new Credential.Builder(
                BearerToken.authorizationHeaderAccessMethod());
        builder.setTokenServerUrl(GOOGLE_OAUTH2_TOKEN_SERVER);
        builder.setTransport(new NetHttpTransport());
        builder.setJsonFactory(new JacksonFactory());
        builder.setClientAuthentication(new HttpExecuteInterceptor()
        {

            @Override
            public void intercept(HttpRequest request) throws IOException
            {
                final Object data =
                    ((UrlEncodedContent) request.getContent()).getData();
                if (data instanceof RefreshTokenRequest)
                {
                    // Insert client authentication credentials in requests.
                    final RefreshTokenRequest content =
                        (RefreshTokenRequest) data;
                    content.put("client_id", GOOGLE_API_CLIENT_ID);
                    content.put("client_secret", GOOGLE_API_CLIENT_SECRET);
                    LOGGER.info("Inserting client authentication data into "
                        + " refresh token request.");
                    if (LOGGER.isDebugEnabled())
                    {
                        LOGGER.debug("Request: " + content.toString());
                    }
                }
                else
                {
                    LOGGER.info("Unexpected type of request found.");
                }
            }
        });
        builder.addRefreshListener(new CredentialRefreshListener()
        {

            @Override
            public void onTokenResponse(Credential credential,
                TokenResponse tokenResponse) throws IOException
            {
                LOGGER.debug("Successful token refresh response: "
                    + tokenResponse.toPrettyString());
                store.set(credential);
            }

            @Override
            public void onTokenErrorResponse(Credential credential,
                TokenErrorResponse tokenErrorResponse) throws IOException
            {
                if (LOGGER.isDebugEnabled())
                {
                    LOGGER.debug("Failed token refresh response: "
                        + tokenErrorResponse.toPrettyString());
                }
                LOGGER.error("Failed to refresh OAuth2 token: "
                    + tokenErrorResponse.getError() + ": "
                    + tokenErrorResponse.getErrorDescription());
            }
        });
        final Credential credential = builder.build();
        credential.setAccessToken(data.accessToken);
        credential.setRefreshToken(data.refreshToken);
        credential.setExpiresInSeconds(data.expiration);
        return credential;
    }

    private static TokenData requestAuthenticationToken(final String approvalCode)
    {
        // FIXME actually acquire credential
        return new TokenData("", "", 3600L);
    }

    private static class OAuthApprovalDialog extends SIPCommDialog {
        private static final long serialVersionUID = 6792589736608633346L;

        private final SIPCommLinkButton label;

        private final SIPCommTextField code = new SIPCommTextField("");

        public OAuthApprovalDialog() throws MalformedURLException
        {
            this.setModal(true);
            this.label = new SIPCommLinkButton("Click here to approve.");
            this.label.addActionListener(new ActionListener()
            {

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    LOGGER.info("Request user for approval via web page: " + APPROVAL_URL);
                    // FIXME open browser
                }
            });
            this.setLayout(new BorderLayout());
            this.add(this.label, BorderLayout.NORTH);
            this.add(new JLabel("Code"), BorderLayout.WEST);
            this.add(this.code, BorderLayout.CENTER);
            final JButton button = new JButton("Done");
            button.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    OAuthApprovalDialog.this.dispose();
                }
            });
            this.add(button, BorderLayout.SOUTH);
            this.pack();
        }

        public String getApprovalCode() {
            return this.code.getText();
        }
    }

    private static class TokenData
    {
        private final String accessToken;

        private final String refreshToken;

        private final long expiration;

        private TokenData(final String accessToken, final String refreshToken, final long expirationTime)
        {
            if (accessToken == null)
            {
                throw new NullPointerException("access token cannot be null");
            }
            this.accessToken = accessToken;
            if (refreshToken == null)
            {
                throw new NullPointerException("refresh token cannot be null");
            }
            this.refreshToken = refreshToken;
            this.expiration = expirationTime;
        }
    }

    public static class FailedAcquireCredentialException
        extends Exception
    {
        private static final long serialVersionUID = 5810534617383420431L;

        private FailedAcquireCredentialException(final Throwable cause)
        {
            super(cause);
        }
    }

    public static class FailedTokenRefreshException
        extends Exception
    {
        private static final long serialVersionUID = 3166027054735734199L;

        private FailedTokenRefreshException()
        {
            super("Failed to refresh OAuth2 token.");
        }
    }
}
