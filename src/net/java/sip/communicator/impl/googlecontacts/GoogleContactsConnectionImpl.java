/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.googlecontacts;

import java.io.*;
import java.util.concurrent.atomic.*;

import net.java.sip.communicator.service.googlecontacts.*;
import net.java.sip.communicator.util.*;

import com.google.api.client.auth.oauth2.*;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.*;
import com.google.api.client.json.jackson2.*;
import com.google.gdata.client.contacts.*;
import com.google.gdata.data.contacts.*;
import com.google.gdata.util.*;

/**
 * Google Contacts credentials to connect to the service.
 *
 * @author Sebastien Vincent
 * @author Danny van Heumen
 */
public class GoogleContactsConnectionImpl
    implements GoogleContactsConnection
{
    /**
     * Logger.
     */
    private static final Logger logger =
        Logger.getLogger(GoogleContactsConnectionImpl.class);

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

    // FIXME Actually use scopes!
    private static final String[] GOOGLE_API_OAUTH2_SCOPES = new String[]
    { "profile", "email", "https://www.google.com/m8/feeds" };

    // FIXME Actually use the redirect URL!
    private static final String GOOGLE_API_OAUTH2_REDIRECT_URI =
        "urn:ietf:wg:oauth:2.0:oob";

    // FIXME (Danny) Temporary stored refresh token during development...
    private static final String TEMP_REFRESH_TOKEN =
        "1/mjFeVE86qVmm-O2B6SSLweW6hBcj4qxuYSb0fGvJvH0";

    /**
     * The credential store to pass around.
     */
    private final AtomicReference<Credential> credential = new AtomicReference<Credential>(null);

    /**
     * Login.
     */
    private String login = null;

    /**
     * Password.
     */
    private String password = null;

    /**
     * If the connection is enabled.
     */
    private boolean enabled = false;

    /**
     * The google contact prefix.
     */
    private String prefix = null;

    /**
     * Google Contacts service.
     */
    private final ContactsService googleService =
        new ContactsService("GoogleContacts service for Jitsi");

    /**
     * Constructor.
     *
     * @param login the login to connect to the service
     * @param password the password to connect to the service
     */
    public GoogleContactsConnectionImpl(String login, String password)
    {
        this.login = login;
        this.password = password;
        googleService.useSsl();
    }

    /**
     * Returns the Google service.
     *
     * @return the Google service
     */
    public ContactsService getGoogleService()
    {
        return googleService;
    }

    /**
     * Get login.
     *
     * @return login to connect to the service
     */
    public String getLogin()
    {
        return login;
    }

    /**
     * get password.
     *
     * @return password to connect to the service
     */
    public String getPassword()
    {
        return password;
    }

    /**
     * Set login.
     *
     * @param login login to connect to the service
     */
    public void setLogin(String login)
    {
        this.login = login;
    }

    /**
     * Set password.
     *
     * @param password password to connect to the service
     */
    public void setPassword(String password)
    {
        this.password = password;
    }

    /**
     * Initialize connection.
     *
     * @return connection status
     */
    public synchronized ConnectionStatus connect()
    {
        createCredential(GOOGLE_OAUTH2_TOKEN_SERVER);
        googleService.setOAuth2Credentials(this.credential.get());
        return ConnectionStatus.SUCCESS;
    }

    /**
     * Query for contacts using provided ContactQuery.
     *
     * Executes query. In case of failure, refresh OAuth2 token and retry query.
     * If query fails again, throws FailedContactQueryException.
     *
     * @param query the contact query
     * @return Returns the contact feed with matching contacts.
     * @throws IOException
     * @throws ServiceException
     * @throws FailedContactQueryException Throws in case of failed query.
     * @throws FailedTokenRefreshException Throws in case refreshing OAuth2
     *             token fails.
     */
    public synchronized ContactFeed query(final ContactQuery query)
        throws IOException,
        ServiceException,
        FailedContactQueryException,
        FailedTokenRefreshException
    {
        try
        {
            return this.googleService.query(query, ContactFeed.class);
        }
        catch (Exception e)
        {
            // FIXME if possible narrow down the exceptions on which to
            // refresh token
            logger.info("Failed to execute query. Going to refresh token"
                + " and try again.", e);
            refreshToken();
        }
        try
        {
            return this.googleService.query(query, ContactFeed.class);
        }
        catch (Exception e)
        {
            throw new FailedContactQueryException(e);
        }
    }

    /**
     * Refresh OAuth2 authentication token.
     *
     * @throws IOException
     */
    private void refreshToken() throws IOException, FailedTokenRefreshException
    {
        final Credential credential = this.credential.get();
        if (!credential.refreshToken())
        {
            logger.warn("Refresh of OAuth2 authentication token failed.");
            throw new FailedTokenRefreshException();
        }
    }

    /**
     * Create credential instance suitable for use in Google Contacts API.
     * @param tokenServer the token server URL
     * @return Returns a Credential instance.
     */
    private void createCredential(final GenericUrl tokenServerURL)
    {
        final Credential.Builder builder =
            new Credential.Builder(
                BearerToken.authorizationHeaderAccessMethod());
        builder.setTokenServerUrl(tokenServerURL);
        builder.setTransport(new NetHttpTransport());
        builder.setJsonFactory(new JacksonFactory());
        builder.setClientAuthentication(new HttpExecuteInterceptor()
        {

            @Override
            public void intercept(HttpRequest request) throws IOException
            {
                final RefreshTokenRequest content =
                    (RefreshTokenRequest) ((UrlEncodedContent) request
                        .getContent()).getData();
                content.put("client_id", GOOGLE_API_CLIENT_ID);
                content.put("client_secret", GOOGLE_API_CLIENT_SECRET);
                logger.warn("Refresh token request: " + content.toString());
            }
        });
        builder.addRefreshListener(new CredentialRefreshListener()
        {
            final AtomicReference<Credential> store =
                GoogleContactsConnectionImpl.this.credential;

            @Override
            public void onTokenResponse(Credential credential,
                TokenResponse tokenResponse) throws IOException
            {
                logger.debug("Successful token refresh response: "
                    + tokenResponse.toPrettyString());
                store.set(credential);
            }

            @Override
            public void onTokenErrorResponse(Credential credential,
                TokenErrorResponse tokenErrorResponse) throws IOException
            {
                logger.debug("Failed token refresh response: "
                    + tokenErrorResponse.toPrettyString());
                logger.error("Failed to refresh OAuth2 token: "
                    + tokenErrorResponse.getError() + ": "
                    + tokenErrorResponse.getErrorDescription());
            }
        });
        final Credential credential = builder.build();
        credential.setAccessToken("ya29.iwG37LYEgB4FCwfPLq8vV6Q-CX1vQ5sJrb_2AGydhLAiUT4wmz4iW4FlVkZE57s1B6NgA3BJAspLIw");
        credential.setRefreshToken(TEMP_REFRESH_TOKEN);
        credential.setExpiresInSeconds(3600L);
        this.credential.set(credential);
    }

    /**
     * Returns if the connection is enabled.
     *
     * @return true if connection is enabled, false otherwise
     */
    public boolean isEnabled()
    {
        return enabled;
    }

    /**
     * Set the connection to be enabled or not.
     *
     * @param enabled value to set
     */
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    /**
     * Sets the google contacts prefix.
     *
     * @param prefix the phone number prefix to set
     */
    public void setPrefix(String prefix)
    {
        this.prefix = prefix;
    }

    /**
     * Returns the google contacts phone number prefix.
     *
     * @return the google contacts phone number prefix
     */
    public String getPrefix()
    {
        return prefix;
    }

    public static class FailedContactQueryException
        extends Exception
    {

        private FailedContactQueryException(Throwable cause)
        {
            super("Failed to query Google Contacts API.", cause);
        }
    }

    public static class FailedTokenRefreshException
        extends Exception
    {

        private FailedTokenRefreshException()
        {
            super("Failed to refresh OAuth2 token.");
        }
    }
}
