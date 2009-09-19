/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.facebook;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import net.java.sip.communicator.util.*;

import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.client.entity.*;
import org.apache.http.client.methods.*;
import org.apache.http.client.params.*;
import org.apache.http.conn.*;
import org.apache.http.conn.scheme.*;
import org.apache.http.conn.ssl.*;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.tsccm.*;
import org.apache.http.message.*;
import org.apache.http.params.*;
import org.apache.http.protocol.*;
import org.apache.http.util.*;

/**
 * A Facebook Session.<br>
 * This class is responsible of performing the login, logout and eventual
 * reconnection to the server.<br>
 * Remember to {@link FacebookSession#logout() logout} the session after usage,
 * until logout this {@link FacebookSession} will maintain HTTP connections open
 * to retrieve messages and maintain the buddy list updated.<br>
 * In some cases, e.g. after repeated IO errors, the {@link FacebookSession}
 * will be closed automatically and the registered
 * {@link FacebookSessionListener listeners} will be notified.<br>
 * The {@link FacebookSession} will be automatically closed if a
 * {@link FacebookErrorException} is thrown with the code
 * {@link FacebookErrorException#kError_Async_NotLoggedIn}.
 * 
 * @author Edgar Poce
 */
public class FacebookSession
{
    private static Logger logger = Logger.getLogger(FacebookSession.class);

    /**
     * Pattern to find the UID
     */
    private final static Pattern UID_PATTERN = Pattern
            .compile("profile.php\\?id=[\\d]+\\&amp;ref=profile");

    /**
     * Pattern to find the Channel
     */
    private final static Pattern CHANNEL_PATTERN = Pattern
            .compile("\"channel[\\d]+\"");

    /**
     * Pattern to find the Channel
     */
    private final static Pattern FORMID_PATTERN = Pattern
            .compile("<input type=\\\"hidden\\\" id=\\\"post_form_id\\\" name=\\\"post_form_id\\\" value=\\\"[^\"]+\\\" />");

    /**
     * The url of the login page
     */
    private static final String LOGIN_PAGE_URL = "http://www.facebook.com/login.php";

    /**
     * The url of the home page
     */
    private static final String HOME_PAGE_URL = "http://www.facebook.com/home.php";

    /**
     * The url of the home page
     */
    private static final String POPOUT_PAGE_URL = "http://www.facebook.com/presence/popout.php";

    /**
     * The url to logout
     */
    private static final String LOGOUT_URL = "http://www.facebook.com/logout.php";

    /**
     * The url to the settings page
     */
    private static final String SETTINGS_URL = "http://www.facebook.com/ajax/chat/settings.php";

    /**
     * The url to update the status
     */
    private static final String UPDATE_STATUS_URL = "http://www.facebook.com/updatestatus.php";

    /**
     * the url to reconnect
     */
    private static final String RECONNECT_URL = "http://www.facebook.com/ajax/presence/reconnect.php";

    /**
     * Listener registry
     */
    private List<FacebookSessionListener> listeners = new ArrayList<FacebookSessionListener>();

    /**
     * The {@link HttpClient} used to login
     */
    private final HttpClient httpClient;

    /**
     * Flag that indicates whether the current session represents a logged in
     * user
     */
    private boolean loggedIn = false;

    /**
     * The user id
     */
    private String uid;

    /**
     * The channel assigned by the server
     */
    private String channel;

    /**
     * The form id which must be used in every request
     */
    private String formId;

    /**
     * The buddy list of this account
     */
    private final FacebookBuddyList buddyList;

    /**
     * Runnable responsible of updating the buddy list
     */
    private BuddyListRefresher buddyListRefresher;

    /**
     * Message poller
     */
    private final FacebookIncomingMessagePoller poller;

    /**
     * Runnable which keeps requesting new messages.
     */
    private PollerRefresher pollerRefresher;

    public FacebookSession()
    {
        this.httpClient = createHttpClient();
        this.buddyList = new FacebookBuddyList(this);
        this.poller = new FacebookIncomingMessagePoller(this);
    }

    /**
     * Creat a http client with default settings
     * 
     * @return a http client with default settings
     */
    private final HttpClient createHttpClient()
    {
        HttpParams params = new BasicHttpParams();
        // prevent deadlocks caused by network failures
        params.setParameter("http.socket.timeout", 300000);
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, "UTF-8");
        HttpProtocolParams.setUseExpectContinue(params, true);
        HttpProtocolParams.setHttpElementCharset(params, "UTF-8");
        HttpProtocolParams
                .setUserAgent(
                        params,
                        "Mozilla/5.0 (Windows; U; Windows NT 5.1; zh-CN; rv:1.9) Gecko/2008052906 Firefox/3.0");

        // Register the "http" and "https" protocol schemes, they are
        // required by the default operator to look up socket factories.
        SchemeRegistry supportedSchemes = new SchemeRegistry();
        SocketFactory sf = PlainSocketFactory.getSocketFactory();
        supportedSchemes.register(new Scheme("http", sf, 80));
        sf = SSLSocketFactory.getSocketFactory();
        supportedSchemes.register(new Scheme("https", sf, 443));

        ClientConnectionManager connManager = new ThreadSafeClientConnManager(
                params, supportedSchemes);
        DefaultHttpClient httpClient = new DefaultHttpClient(connManager,
                params);
        httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY,
                CookiePolicy.BROWSER_COMPATIBILITY);
        httpClient.setRedirectHandler(new DefaultRedirectHandler());
        httpClient.setCookieStore(new BasicCookieStore());
        return httpClient;
    }

    /**
     * Tries to login to Facebook with the given credentials
     * 
     * @param email
     *            the user email
     * @param password
     *            the user password
     * @return true if the session was initiated successfully with the given
     *         credentials
     * @throws BrokenFacebookProtocolException
     *             if the protocol implementation is broken
     * @throws IOException
     *             if there's an IO error
     */
    public synchronized boolean login(String email, String password)
        throws BrokenFacebookProtocolException,
               IOException
    {
        if (this.isLoggedIn())
            throw new IllegalStateException("already logged in");

        HttpGet loginGet = new HttpGet(LOGIN_PAGE_URL);
        HttpResponse getLoginResponse = httpClient.execute(loginGet);
        int getLoginStatusCode = getLoginResponse.getStatusLine()
                .getStatusCode();
        getLoginResponse.getEntity().consumeContent();
        if (getLoginStatusCode != 200)
            throw
                new BrokenFacebookProtocolException(
                        "Unable to GET Facebook login page");

        // POST credentials
        HttpPost httpost = new HttpPost(LOGIN_PAGE_URL);
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("email", email));
        nvps.add(new BasicNameValuePair("pass", password));
        nvps.add(new BasicNameValuePair("login", ""));
        httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
        HttpResponse postLoginResponse = httpClient.execute(httpost);
        int postLoginStatusCode = postLoginResponse.getStatusLine()
                .getStatusCode();
        String postBody = EntityUtils.toString(postLoginResponse.getEntity());
        if (postLoginStatusCode >= 400)
            throw new BrokenFacebookProtocolException("Code "
                    + postLoginStatusCode
                    + ". Unable to POST Facebook login credentials.");

        String uid = parseUid(postBody);
        String channel = parseChannel(postBody);
        String formId = parseFormId(postBody);

        // Parse popout.php if needed
        if (uid == null || channel == null || formId == null)
        {
            HttpGet channelGet = new HttpGet(POPOUT_PAGE_URL);
            HttpResponse channelResponse = httpClient.execute(channelGet);
            String channelBody = EntityUtils.toString(channelResponse
                    .getEntity());
            if (channel == null)
                channel = parseChannel(channelBody);
            if (formId == null)
                formId = parseFormId(channelBody);
        }

        // Parse the HOME page if needed
        if (uid == null || channel == null || formId == null)
        {
            HttpGet homeGet = new HttpGet(HOME_PAGE_URL);
            HttpResponse homeResponse = httpClient.execute(homeGet);
            String homeBody = EntityUtils.toString(homeResponse.getEntity());
            if (uid == null)
                uid = parseUid(homeBody);
            if (channel == null)
                channel = parseChannel(homeBody);
            if (formId == null)
                formId = parseFormId(homeBody);
        }

        if (uid != null && channel != null && formId != null)
        {
            this.uid = uid;
            this.channel = channel;
            this.formId = formId;

            // update state
            this.loggedIn = true;

            // start refreshing threads
            this.buddyListRefresher = new BuddyListRefresher(this);
            Thread buddyListThread = new Thread(this.buddyListRefresher,
                    "facebook buddy list refresher " + this.hashCode());
            buddyListThread.start();
            this.pollerRefresher = new PollerRefresher(this);
            Thread pollerRefresher = new Thread(this.pollerRefresher,
                    "facebook messages refresher " + this.hashCode());
            pollerRefresher.start();

            // return result
            return this.loggedIn;
        }
        else if (uid != null | channel != null | formId != null)
        {
            throw new BrokenFacebookProtocolException(
                    "One of the session elements could not be read from the home page. UID: "
                            + uid + " - CHANNEL: " + channel + " - FORMID: "
                            + formId);
        }
        else
        {
            this.loggedIn = false;
            return false;
        }
    }

    public void reconnect()
        throws BrokenFacebookProtocolException,
               IOException,
               FacebookErrorException
    {
        HttpGet get = new HttpGet(RECONNECT_URL + "?reason=3&post_form_id="
                + this.formId);
        HttpResponse response = httpClient.execute(get);
        String body = EntityUtils.toString(response.getEntity());
        new FacebookJsonResponse(this, body);
    }

    /**
     * Logout the {@link FacebookSession} by stopping the threads and by
     * requesting Facebook server to logout<br>
     * It clears the {@link FacebookBuddyList} and notifies to registered
     * listeners that every contact is OFFLINE.
     */
    public synchronized void logout()
    {
        if (this.buddyListRefresher != null)
            this.buddyListRefresher.setKeepRunning(false);
        if (this.pollerRefresher != null)
            this.pollerRefresher.setKeepRunning(false);

        this.buddyList.clear();
        for (FacebookSessionListener l : this.listeners)
            l.onBuddyListUpdated();

        if (this.loggedIn)
        {
            this.loggedIn = false;
            for (FacebookSessionListener l : this.listeners)
                l.onFacebookConnectionLost();
            try
            {
                List<NameValuePair> nvps = new ArrayList<NameValuePair>();
                nvps.add(new BasicNameValuePair("confirm", "1"));
                HttpPost post = new HttpPost(LOGOUT_URL);
                post.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
                HttpResponse response = httpClient.execute(post);
                response.getEntity().consumeContent();
                if (response.getStatusLine().getStatusCode() != 200) {
                    logger.warn("unable to send logout command");
                }
            }
            catch (IOException e)
            {
                logger.warn("IOException during logout", e);
            }
        }
    }

    /**
     * Parses the UID from the home page
     * 
     * @param body
     *            the HTML body
     * @return the UID
     */
    private static String parseUid(String body)
    {
        return parse(body, UID_PATTERN, 15, 16);
    }

    /**
     * Parses the channel from the home page
     * 
     * @param body
     *            the HTML body
     * @return the Channel
     */
    private static String parseChannel(String body)
    {
        return parse(body, CHANNEL_PATTERN, 8, 1);
    }

    /**
     * Parses the form id from the home page
     * 
     * @param body
     *            the HTML body
     * @return the form id
     */
    private static String parseFormId(String body)
    {
        return parse(body, FORMID_PATTERN, 66, 4);
    }

    private static String parse(
        String body,
        Pattern pattern,
        int beginIndex,
        int excludedEndLength)
    {
        Matcher matcher = pattern.matcher(body);
        boolean found = matcher.find();
        if (found)
        {
            String str = body.substring(matcher.start(), matcher.end());
            str = str.substring(beginIndex, str.length() - excludedEndLength);

            /*
             * String#substring(int, int) has created a reference to the
             * internal char array of body. But body is huge and is not
             * necessary once the data of interest has been extracted from it.
             * So make sure that the internal char array of body is not
             * referenced.
             */
            return new String(str);
        } else
            return null;
    }

    public String getUid()
    {
        return uid;
    }

    public String getChannel()
    {
        return channel;
    }

    public String getFormId()
    {
        return formId;
    }

    /**
     * Set the visibility.
     * 
     * @param isVisible
     *            true(visible) or false(invisible)
     * @throws IOException
     */
    public void setVisibility(boolean isVisible)
        throws IOException,
               BrokenFacebookProtocolException
    {
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("visibility", isVisible + ""));
        nvps.add(new BasicNameValuePair("post_form_id", this.formId));
        HttpPost post = new HttpPost(SETTINGS_URL);
        post.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
        HttpResponse response = this.httpClient.execute(post);
        response.getEntity().consumeContent();
        if (response.getStatusLine().getStatusCode() != 200)
            throw new BrokenFacebookProtocolException(
                    "unable to set visibility");
    }

    /**
     * Updates the status messsage
     * 
     * @param statusMsg
     * @throws IOException
     */
    public void setStatusMessage(String statusMsg) throws IOException
    {
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        if (statusMsg.length() < 1)
            nvps.add(new BasicNameValuePair("clear", "1"));
        else
            nvps.add(new BasicNameValuePair("status", statusMsg));
        nvps.add(new BasicNameValuePair("profile_id", this.formId));
        nvps.add(new BasicNameValuePair("home_tab_id", "1"));
        nvps.add(new BasicNameValuePair("test_name", "INLINE_STATUS_EDITOR"));
        nvps.add(new BasicNameValuePair("action", "HOME_UPDATE"));
        nvps.add(new BasicNameValuePair("post_form_id", this.formId));
        HttpPost post = new HttpPost(UPDATE_STATUS_URL);
        post.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
        HttpResponse response = this.httpClient.execute(post);
        response.getEntity().consumeContent();
    }

    HttpClient getHttpClient()
    {
        return httpClient;
    }

    public boolean isLoggedIn()
    {
        return loggedIn;
    }

    public FacebookBuddyList getBuddyList()
    {
        return buddyList;
    }

    public FacebookUser getMetaInfo()
    {
        return this.buddyList.getMyMetaInfo();
    }

    public FacebookIncomingMessagePoller getPoller()
    {
        return poller;
    }

    public void addListener(FacebookSessionListener listener)
    {
        this.listeners.add(listener);
        this.poller.addListener(listener);
        this.buddyList.addListener(listener);
    }

    /**
     * Get the profile page for parsing. It's invoked when user opens contact
     * info box.
     * 
     * @param contactAddress
     *            the contact address
     * @return profile page string
     */
    public String getProfilePage(String contactAddress)
    {
        // TODO if homePageUrl.contains("new.facebook.com") return;
        // because if we try to get the "new" page, the account's layout would
        // be set to new style.
        // Someone may not like this.

        // http://www.new.facebook.com/profile.php?id=1190346972&v=info&viewas=1190346972
        // http://www.new.facebook.com/profile.php?id=1386786477&v=info

        // facebookGetMethod(hostUrlNew + "/profile.php?id=" + contactAddress +
        // "&v=info") ;
        return "";
    }

    /**
     * Runnable to poll messages<br>
     * It keeps polling for new messages until the corresponding
     * {@link FacebookSession} is logged out
     */
    private static class PollerRefresher
        implements Runnable
    {
        private final FacebookSession session;
        /**
         * Flag which indicates if the current runnable should keep running
         */
        private boolean keepRunning = true;
        private int retries = 0;

        public PollerRefresher(FacebookSession session)
        {
            this.session = session;
        }

        public void run()
        {
            Thread t = Thread.currentThread();
            while (keepRunning & this.session.isLoggedIn() & t.isAlive()
                    & !t.isInterrupted())
            {
                try
                {
                    this.session.getPoller().poll();
                    this.retries = 0;
                }
                catch (BrokenFacebookProtocolException e)
                {
                    try
                    {
                        this.session.logout();
                    }
                    catch (Exception e1)
                    {
                    }
                }
                catch (IOException e)
                {
                    try
                    {
                        if (this.retries > 3)
                        {
                            this.session.logout();
                        }
                        else
                        {
                            this.retries++;
                            Thread.sleep(3000);
                        }
                    }
                    catch (Exception e1)
                    {
                        throw new RuntimeException(e1);
                    }
                }
                catch (FacebookErrorException e)
                {
                    // FIXME handle facebook error exception
                }
            }
        }

        public void setKeepRunning(boolean keepRunning)
        {
            this.keepRunning = keepRunning;
        }
    }

    /**
     * Runnable to refresh the buddy list.<br>
     * It refreshes the buddy list each minute until the corresponding
     * {@link FacebookSession} is logged out
     */
    private static class BuddyListRefresher
        implements Runnable
    {
        private final FacebookSession session;
        /**
         * Flag which indicates if the current runnable should keep running
         */
        private boolean keepRunning = true;
        private int retries = 0;

        public BuddyListRefresher(FacebookSession session)
        {
            this.session = session;
        }

        public void run()
        {
            Thread t = Thread.currentThread();

            while (keepRunning & this.session.isLoggedIn() & t.isAlive()
                    & !t.isInterrupted())
            {
                try
                {
                    this.session.getBuddyList().update();
                    this.retries = 0;
                }
                catch (BrokenFacebookProtocolException e)
                {
                    try
                    {
                        this.session.logout();
                    }
                    catch (Exception e1)
                    {
                    }
                    throw new IllegalStateException(e);
                }
                catch (IOException e)
                {
                    if (this.retries > 3)
                    {
                        try
                        {
                            this.session.logout();
                        }
                        catch (Exception e1)
                        {
                            throw new RuntimeException(e1);
                        }
                    }
                    else
                    {
                        this.retries++;
                    }
                }
                catch (FacebookErrorException e)
                {
                    // FIXME handle exception
                }
                try
                {
                    Thread.sleep(59 * 1000);
                }
                catch (InterruptedException e)
                {
                }
            }
        }

        public void setKeepRunning(boolean keepRunning)
        {
            this.keepRunning = keepRunning;
        }
    }
}
