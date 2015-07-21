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
import java.util.*;
import java.util.concurrent.atomic.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.util.*;

import org.apache.http.HttpResponse;
import org.apache.http.client.*;
import org.apache.http.client.entity.*;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;
import org.apache.http.message.*;

import com.google.api.client.auth.oauth2.*;
import com.google.api.client.http.*;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.javanet.*;
import com.google.api.client.json.*;
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
    private static final Logger LOGGER = Logger
        .getLogger(OAuth2TokenStore.class);

    /**
     * Symbol for refresh token in token server response.
     */
    private static final String REFRESH_TOKEN_SYMBOL = "refresh_token";

    /**
     * Symbol for access token in token server response.
     */
    private static final String ACCESS_TOKEN_SYMBOL = "access_token";

    /**
     * Symbol for expiration time in token server response.
     */
    private static final String EXPIRES_IN_SYMBOL = "expires_in";

    /**
     * Interesting token server response fields.
     */
    private static final Set<String> TOKEN_RESPONSE_FIELDS;

    static
    {
        final HashSet<String> set = new HashSet<String>();
        set.add(REFRESH_TOKEN_SYMBOL);
        set.add(ACCESS_TOKEN_SYMBOL);
        set.add(EXPIRES_IN_SYMBOL);
        TOKEN_RESPONSE_FIELDS = Collections.unmodifiableSet(set);
    }

    /**
     * Google OAuth 2 token server.
     */
    private static final GenericUrl GOOGLE_OAUTH2_TOKEN_SERVER =
        new GenericUrl("https://accounts.google.com/o/oauth2/token");

    /**
     * Client ID for OAuth 2 based authentication.
     */
    private static final String GOOGLE_API_CLIENT_ID
        = GoogleAPIClientToken.GOOGLE_API_CLIENT_ID;

    /**
     * Client secret for OAuth 2 based authentication.
     */
    private static final String GOOGLE_API_CLIENT_SECRET
        = GoogleAPIClientToken.GOOGLE_API_CLIENT_SECRET;

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
     * Grant type for communication with token server.
     */
    private static final String GOOGLE_API_GRANT_TYPE = "authorization_code";

    /**
     * Approval URL.
     */
    private static final String APPROVAL_URL = String.format(
        "https://accounts.google.com/o/oauth2/auth?scope=%s&redirect_uri=%s"
            + "&response_type=code&client_id=%s", GOOGLE_API_OAUTH2_SCOPES,
        GOOGLE_API_OAUTH2_REDIRECT_URI, GOOGLE_API_CLIENT_ID);

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
     * @param identity The identity of the API token.
     * @return Returns the credential.
     * @throws FailedAcquireCredentialException 
     * @throws MalformedURLException In case requesting authn token failed.
     */
    public synchronized Credential get(final String identity)
        throws FailedAcquireCredentialException
    {
        if (GOOGLE_API_CLIENT_ID == null || GOOGLE_API_CLIENT_SECRET == null)
        {
            throw new IllegalStateException("Missing client ID or client "
                + "secret. It is not possible to use Google Contacts API "
                + "without it.");
        }
        if (this.store.get() == null)
        {
            try
            {
                acquireCredential(this.store, identity);
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
     * @param identity the identity to which the refresh token belongs
     * @return Acquires and returns the credential instance.
     * @throws URISyntaxException In case of bad redirect URI.
     * @throws IOException 
     * @throws ClientProtocolException 
     */
    private static void acquireCredential(
        final AtomicReference<Credential> store, final String identity)
        throws URISyntaxException, ClientProtocolException, IOException
    {
        final TokenData token;
        String refreshToken = restoreRefreshToken(identity);
        if (refreshToken == null)
        {
            LOGGER.info("No credentials available yet. Requesting user to "
                + "approve access to Contacts API for identity " + identity
                + " using URL: " + APPROVAL_URL);
            final OAuthApprovalDialog dialog =
                new OAuthApprovalDialog(identity);
            dialog.setVisible(true);
            switch (dialog.getResponse())
            {
            case CONFIRMED:
                // dialog is confirmed, so process entered approval code
                final String approvalCode = dialog.getApprovalCode();
                LOGGER.debug("Approval code from user: " + approvalCode);
                token = requestAuthenticationToken(approvalCode);
                saveRefreshToken(token, identity);
                break;
            case CANCELLED:
            default:
                // user one time cancellation
                // let token remain null, as we do not have new information yet
                token = null;
                break;
            }
        }
        else
        {
            token = new TokenData(null, refreshToken, 0);
        }
        store.set(createCredential(store, token));
    }

    /**
     * Restore refresh token from encrypted credentials store.
     *
     * @param identity The identity corresponding to the refresh token.
     * @return Returns the refresh token.
     */
    private static String restoreRefreshToken(final String identity)
    {
        final CredentialsStorageService credentials =
            GoogleContactsActivator.getCredentialsService();
        return credentials
            .loadPassword(GoogleContactsServiceImpl.CONFIGURATION_PATH + "."
                + identity);
    }

    /**
     * Save refresh token for provided identity.
     *
     * @param token The refresh token.
     * @param identity The identity.
     * @throws IOException An IOException in case of errors.
     */
    private static void saveRefreshToken(final TokenData token,
        final String identity) throws IOException
    {
        final CredentialsStorageService credentials =
            GoogleContactsActivator.getCredentialsService();
        credentials.storePassword(GoogleContactsServiceImpl.CONFIGURATION_PATH
            + "." + identity, token.refreshToken);
    }

    /**
     * Refresh OAuth2 authentication token.
     *
     * @throws IOException
     * @throws FailedTokenRefreshException In case of failed token refresh
     *             operation.
     */
    public synchronized void refresh() throws IOException, FailedTokenRefreshException
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
        final AtomicReference<Credential> store, final TokenData data)
        throws URISyntaxException
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
                    // Inject client authentication credentials in requests.
                    final RefreshTokenRequest content =
                        (RefreshTokenRequest) data;
                    content.put("client_id", GOOGLE_API_CLIENT_ID);
                    content.put("client_secret", GOOGLE_API_CLIENT_SECRET);
                    LOGGER.info("Inserting client authentication data into "
                        + "refresh token request.");
                    if (LOGGER.isDebugEnabled())
                    {
                        LOGGER.debug("Request: " + content.toString());
                    }
                }
                else
                {
                    LOGGER.debug("Unexpected type of request found.");
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

    /**
     * Request an authentication token using the approval code received from the
     * user.
     * 
     * @param approvalCode the approval code
     * @return Returns the acquired token data from OAuth 2 token server.
     * @throws IOException 
     * @throws ClientProtocolException 
     */
    private static TokenData requestAuthenticationToken(
        final String approvalCode) throws ClientProtocolException, IOException
    {
        final HttpClient client = new DefaultHttpClient();
        final HttpPost post = new HttpPost(GOOGLE_OAUTH2_TOKEN_SERVER.toURI());
        final UrlEncodedFormEntity entity =
            new UrlEncodedFormEntity(Arrays.asList(new BasicNameValuePair(
                "code", approvalCode), new BasicNameValuePair("client_id",
                GOOGLE_API_CLIENT_ID), new BasicNameValuePair("client_secret",
                GOOGLE_API_CLIENT_SECRET), new BasicNameValuePair(
                "redirect_uri", GOOGLE_API_OAUTH2_REDIRECT_URI),
                new BasicNameValuePair("grant_type", GOOGLE_API_GRANT_TYPE)));
        post.setEntity(entity);
        final HttpResponse httpResponse = client.execute(post);
        final JsonParser parser =
            JacksonFactory.getDefaultInstance().createJsonParser(
                httpResponse.getEntity().getContent());
        try
        {
            // Token response components initialized with defaults in case
            // fields are missing in the token server response.
            String accessToken = "";
            String refreshToken = "";
            long expiresIn = 3600;
            // Parse token server response.
            String found;
            while (parser.nextToken() != JsonToken.END_OBJECT)
            {
                found = parser.skipToKey(TOKEN_RESPONSE_FIELDS);
                if (REFRESH_TOKEN_SYMBOL.equals(found))
                {
                    refreshToken = parser.getText();
                }
                else if (ACCESS_TOKEN_SYMBOL.equals(found))
                {
                    accessToken = parser.getText();
                }
                else if (EXPIRES_IN_SYMBOL.equals(found))
                {
                    expiresIn = parser.getLongValue();
                }
            }
            return new TokenData(accessToken, refreshToken, expiresIn);
        }
        finally
        {
            parser.close();
        }
    }

    /**
     * OAuth 2 approval dialog for instructing user for instructing the user to
     * open a web browser and approve Jitsi Google Contacts plugin access and
     * for receiving the resulting approval code.
     *
     * @author Danny van Heumen
     */
    private static class OAuthApprovalDialog extends SIPCommDialog {
        private static final long serialVersionUID = 6792589736608633346L;

        private final SIPCommLinkButton label;

        private final SIPCommTextField code = new SIPCommTextField("");

        private UserResponseType response = UserResponseType.CANCELLED;

        public OAuthApprovalDialog(final String identity)
        {
            this.setModal(true);
            this.label =
                new SIPCommLinkButton("Approve Jitsi's access to " + identity);
            this.label.addActionListener(new ActionListener()
            {

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    LOGGER.info("Request user for approval via web page: "
                        + APPROVAL_URL);
                    GoogleContactsActivator.getBrowserLauncherService()
                        .openURL(APPROVAL_URL);
                }
            });
            this.setLayout(new BorderLayout());
            this.add(this.label, BorderLayout.NORTH);
            this.add(new JLabel("Code"), BorderLayout.WEST);
            this.add(this.code, BorderLayout.CENTER);
            // buttons panel
            final JPanel buttonPanel = new JPanel(new BorderLayout());
            final JButton doneButton = new JButton("Done");
            doneButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    OAuthApprovalDialog.this.response =
                        UserResponseType.CONFIRMED;
                    OAuthApprovalDialog.this.dispose();
                }
            });
            buttonPanel.add(doneButton, BorderLayout.EAST);
            this.add(buttonPanel, BorderLayout.SOUTH);
            this.pack();
        }

        /**
         * Get approval code entered by the user in the dialog input field.
         *
         * @return Returns the approval code.
         */
        public String getApprovalCode()
        {
            return this.code.getText();
        }

        /**
         * Is approval dialog confirmed with "Done" button.
         *
         * @return Returns whether or not the dialog is confirmed.
         */
        public UserResponseType getResponse()
        {
            return this.response;
        }
    }

    /**
     * Container for token data for internal use.
     *
     * @author Danny van Heumen
     */
    private static class TokenData
    {
        /**
         * OAuth 2 access token.
         */
        private final String accessToken;

        /**
         * OAuth 2 refresh token.
         */
        private final String refreshToken;

        /**
         * Available time before expiration of the current access token.
         */
        private final long expiration;

        /**
         * Constructor for TokenData container.
         *
         * @param accessToken the access token
         * @param refreshToken the refresh token (cannot be null)
         * @param expirationTime the expiration time (must be >= 0)
         */
        private TokenData(final String accessToken, final String refreshToken,
            final long expirationTime)
        {
            this.accessToken = accessToken;
            if (refreshToken == null)
            {
                throw new NullPointerException("refresh token cannot be null");
            }
            this.refreshToken = refreshToken;
            if (expirationTime < 0)
            {
                throw new IllegalArgumentException(
                    "Expiration time cannot be null");
            }
            this.expiration = expirationTime;
        }
    }

    /**
     * Exception for error case where we failed to acquire initial credential
     * for OAuth 2 authentication and authorization.
     * 
     * @author Danny van Heumen
     */
    public static class FailedAcquireCredentialException
        extends Exception
    {
        private static final long serialVersionUID = 5810534617383420431L;

        private FailedAcquireCredentialException(final Throwable cause)
        {
            super(cause);
        }
    }

    /**
     * Exception for error case where we failed to refresh the OAuth 2 authn
     * token.
     * 
     * @author Danny van Heumen
     */
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
