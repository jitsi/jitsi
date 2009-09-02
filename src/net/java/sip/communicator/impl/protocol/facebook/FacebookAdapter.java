/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.facebook;

import java.io.*;
import java.net.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.client.entity.*;
import org.apache.http.client.methods.*;
import org.apache.http.client.params.*;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.conn.*;
import org.apache.http.conn.scheme.*;
import org.apache.http.conn.ssl.*;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.tsccm.*;
import org.apache.http.message.*;
import org.apache.http.params.*;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.*;
import org.json.*;

/**
 * Adapter for the facebook protocol. With it we can login and send/receive
 * facebook chat messages.
 * 
 * @author Dai Zhiwei
 * @author Lubomir Marinov
 */
public class FacebookAdapter {
    private static Logger logger = Logger.getLogger(FacebookAdapter.class);

    /**
     * The url of the host
     */
    private static String hostUrl = "http://www.facebook.com";
    private static String hostUrlNew = "http://www.new.facebook.com";

    /**
     * The url of the login page
     */
    private static String loginPageUrl = "http://www.facebook.com/login.php";

    /**
     * The url of the home page
     */
    private static String homePageUrl = "http://www.facebook.com/home.php";

    private static final String RECONNECT_URL
        = "/ajax/presence/reconnect.php?reason=3&post_form_id=";

    /**
     * The http client we use to simulate a browser.
     */
    private HttpClient httpClient;

    /**
     * The default parameters.
     * Instantiated in {@link #setup setup}.
     */
    private static HttpParams defaultParameters = null;

    /**
     * The scheme registry.
     * Instantiated in {@link #setup setup}.
     */
    private static SchemeRegistry supportedSchemes;

    /**
     * Timeout until connection established
     * 0 means infinite waiting
     */
    //private int connectionTimeout = 20 * 1000;

    /**
     * Timeout until data received<br>
     * 0 means infinite waiting for data
     */
    //private int socketTimeout = 60 * 1000;

    /**
     * The UID of this account
     */
    private String uid = null;

    /**
     * The channel this account is using
     */
    private String channel = "15";

    /**
     * The post form id
     */
    private String post_form_id = null;

    /**
     * The current seq number
     */
    private int seq = -1;

    /**
     * IDs of the messages we receive,<br>
     * We can know if the incoming message has been handled before via looking up this collection.
     */
    private HashSet<String> msgIDCollection;

    /**
     * The buddy list of this account
     */
    private FacebookBuddyList buddyList;

    /**
     * Parent service provider
     */
    private final ProtocolProviderServiceFacebookImpl parentprovider;

    /**
     * true, we keep requesting new message and buddy list from server;
     * false, we exit the separate thread.
     */
    private boolean isClientRunning = true;

    /**
     * The thread which keeps requesting new messages.
     */
    private Thread msgRequester;

    /**
     * The thread which requests buddy list every 90 seconds.
     */
    private Thread buddyListRequester;

    /**
     * Adapter for each Facebook Chat account.
     * @param pprovider the parent service provider
     */
    public FacebookAdapter(ProtocolProviderServiceFacebookImpl pprovider){
        //initialize the http client
        setup();
        httpClient = createHttpClient();

        msgIDCollection = new HashSet<String>();
        msgIDCollection.clear();
        buddyList = new FacebookBuddyList(FacebookAdapter.this);
        parentprovider = pprovider;
        isClientRunning = true;
        logger.trace("FacebookAdapter() begin");
    }

    /**
     * Performs general setup.
     * This should be called only once.
     */
    private final static void setup() {
        supportedSchemes = new SchemeRegistry();

        // Register the "http" and "https" protocol schemes, they are
        // required by the default operator to look up socket factories.
        SocketFactory sf = PlainSocketFactory.getSocketFactory();
        supportedSchemes.register(new Scheme("http", sf, 80));
        sf = SSLSocketFactory.getSocketFactory();
        supportedSchemes.register(new Scheme("https", sf, 443));

        // prepare parameters
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, "UTF-8");
        HttpProtocolParams.setUseExpectContinue(params, true);
        HttpProtocolParams.setHttpElementCharset(params, "UTF-8");
        HttpProtocolParams.setUserAgent(params, "Mozilla/5.0 (Windows; U; Windows NT 5.1; zh-CN; rv:1.9) Gecko/2008052906 Firefox/3.0");
        defaultParameters = params;
    } // setup

    /**
     * Get default http client parameters
     * @return default http client parameters
     */
    private final static HttpParams getParams() {
        return defaultParameters;
    }

    /**
     * Creat a http client with default settings
     * @return a http client with default settings
     */
    private final HttpClient createHttpClient() {
        ClientConnectionManager ccm =
            new ThreadSafeClientConnManager(getParams(), supportedSchemes);

        DefaultHttpClient dhc =
            new DefaultHttpClient(ccm, getParams());
        
        dhc.getParams().setParameter(
            ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
        
        //DefaultRedirectHandler
        dhc.setRedirectHandler(new DefaultRedirectHandler());
        // ------------------- Proxy Setting Block BEGINE -------------------
        // If we needn't a proxy, just comment this block
        //setUpProxy(dhc);
        // --------------------- Proxy Setting Block END --------------------
        
        return dhc;
    }

    /**
     * Update the buddy list from the given data(JSON Object)
     * @param buddyListJO the JSON Object that contains the buddy list
     */
    public void updateBuddyList(JSONObject buddyListJO){
        try {
            this.buddyList.updateBuddyList(buddyListJO);
        } catch (JSONException e) {
            logger.warn(e.getMessage());
        }
    }

    /**
     * Get the facebook id of this account
     * @return the facebook id of this account
     */
    public String getUID(){
        return uid;
    }

    /**
     * Get the parent service provider
     * @return parent service provider
     */
    public ProtocolProviderServiceFacebookImpl getParentProvider(){
        return parentprovider;
    }

    /**
     * Whether this message id already exists in our collection.
     * If do, we already handle it, so we just omit it.
     * @param msgID the id of current message
     * @return if this id already exists in our collection
     */
    public boolean isMessageHandledBefore(String msgID){
        if(msgIDCollection.contains(msgID)){
            logger.debug("Omitting a already handled message: msgIDCollection.contains(msgID)");
            return true;
        }
        return false;
    }

    /**
     * Add the given message id to our collection,
     * that means this message has been handled.
     * @param msgID the id of current message
     */
    public void addMessageToCollection(String msgID){
        msgIDCollection.add(msgID);
    }

    /**
     * Initialize the connection,<br>
     * Initialize the variables, e.g. uid, channel, etc.
     *
     * @param email our facebook "username"
     * @param pass the password
     * @return the error code
     */
    public int initialize(final String email, final String pass){
        logger.trace("initialize() [begin]");
        isClientRunning = true;

        boolean[] doParseHomePageIsSuccessful = new boolean[1];
        int loginErrorCode
            = connectAndLogin(email, pass, doParseHomePageIsSuccessful);
        if(loginErrorCode == FacebookErrorCode.Error_Global_NoError){
            //login successfully, let's do some parsing
            int hpParsingErrorCode
                = doParseHomePageIsSuccessful[0]
                    ? FacebookErrorCode.Error_Global_NoError
                    : doParseHomePage();
            if(hpParsingErrorCode == FacebookErrorCode.Error_Global_NoError){
                //now we log in successfully,
                //so start two threads to request new messages and buddy list.

                //keep requesting message from the server
                msgRequester = new Thread(new Runnable(){
                    public void run() {
                        logger.info("Keep requesting...");
                        while(isClientRunning){
                            try{
                                keepRequesting();
                            } catch (Exception e){
                                logger.warn(e.getMessage());
                            }
                        }
                    }
                });
                msgRequester.start();

                //requests buddy list every 60 seconds
                buddyListRequester = new Thread(new Runnable(){
                    public void run() {
                        logger.info("Keep requesting buddylist...");
                        while(isClientRunning){
                            try{
                                int errorCode = getBuddyList();
                                if(errorCode == FacebookErrorCode.kError_Async_NotLoggedIn){
                                    //not logged in. try to log in again.
                                    //TODO will this cause infinate loop?
                                    initialize(email, pass);
                                }
                            } catch (Exception e){
                                logger.warn(e.getMessage());
                            }
                            // it's said that the buddy list is updated every 3 minutes at the server end.
                            // we refresh the buddy list every 1 minute
                            try {
                                Thread.sleep(60 * 1000);
                            } catch (InterruptedException e) {
                                logger.warn(e.getMessage());
                            }
                        }
                    }
                });
                buddyListRequester.start();

                logger.trace("initialize() [END]");
                return FacebookErrorCode.Error_Global_NoError;
            } else {
                //log in successfully but can't get home page
                logger.trace("initialize() [Home Page Parsing Error]");
                return hpParsingErrorCode;
            }
        } else if(loginErrorCode == FacebookErrorCode.kError_Login_GenericError){
            //handle the error derived from this login
            logger.error("Not logged in, please check your input or the internet connection!");
        } else {
            //handle the error derived from this login
            logger.error("Not logged in, please check your internet connection!");
        }
        logger.trace("initialize() [Login Error]");
        return loginErrorCode;
    }

    /**
     * Connect and login with the email and password
     * @param email the account email
     * @param pass the password
     * @return the error code
     * @throws URISyntaxException
     * @throws UnsupportedEncodingException
     */
    private int connectAndLogin(
        String email,
        String pass,
        boolean[] doParseHomePageIsSuccessful)
    {
        doParseHomePageIsSuccessful[0] = false;

        logger.trace("=========connectAndLogin begin===========");

        String httpResponseBody = facebookGetMethod(loginPageUrl);
        if(httpResponseBody == null){
            //Why don't we try again?
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                logger.trace(e.getMessage());
            }
            httpResponseBody = facebookGetMethod(loginPageUrl);
        }
        logger.trace("========= get login page ResponseBody begin===========");
        logger.trace(httpResponseBody);
        logger.trace("+++++++++ get login page ResponseBody end+++++++++");

        logger.trace("Initial cookies: ");
        List<Cookie> cookies = ((DefaultHttpClient) httpClient).getCookieStore().getCookies();
        if (cookies.isEmpty()) {
            logger.trace("None");
        } else {
            for (int i = 0; i < cookies.size(); i++) {
                logger.trace("- " + cookies.get(i).toString());
            }
        }
        if(httpResponseBody == null){
            logger.warn("Warning: Failed to get facebook login page.");
        }

        try
        {
            HttpPost httpost = new HttpPost(loginPageUrl);

            List <NameValuePair> nvps = new ArrayList <NameValuePair>();
            nvps.add(new BasicNameValuePair("email", email));
            nvps.add(new BasicNameValuePair("pass", pass));
            //don't know if is this necessary
            nvps.add(new BasicNameValuePair("login", ""));

            httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            logger.info("@executing post method to:" + loginPageUrl);
            
            HttpResponse loginPostResponse = httpClient.execute(httpost);
            HttpEntity entity = loginPostResponse.getEntity();

            logger.trace("Login form post: " + loginPostResponse.getStatusLine());
            if (entity != null) {
                httpResponseBody = EntityUtils.toString(entity);
                logger.trace(httpResponseBody);
                entity.consumeContent();
            } else {
                logger.error("Error: login post's response entity is null");
                return FacebookErrorCode.kError_Login_GenericError;
            }

            logger.trace("Post logon cookies:");
            cookies = ((DefaultHttpClient) httpClient).getCookieStore().getCookies();
            if (cookies.isEmpty()) {
                logger.trace("None");
            } else {
                for (int i = 0; i < cookies.size(); i++) {
                    logger.trace("- " + cookies.get(i).toString());
                }
            }
            
            int statusCode = loginPostResponse.getStatusLine().getStatusCode();
            
            logger.info("Post Method done(" + statusCode + ")");
            
            switch(statusCode){
            case 100: break;//we should try again;
            case 200:
                if (FacebookErrorCode.Error_Global_NoError
                        == doParseHomePage(httpResponseBody))
                    doParseHomePageIsSuccessful[0] = true;
                break;
            case 301:
            case 302:
            case 303:
            case 307:
            {
                //redirect
                Header[] headers = loginPostResponse.getAllHeaders();
                for (int i=0; i<headers.length; i++) {
                    logger.trace(headers[i]);
                }
                Header locationHeader = loginPostResponse.getFirstHeader("location");
                if(locationHeader != null){
                    homePageUrl = locationHeader.getValue();
                    logger.info("Redirect Location: " + homePageUrl);
                    if(homePageUrl == null 
                        || !homePageUrl.contains("facebook.com/home.php")){
                        logger.error("Login error! Redirect Location Url not contains \"facebook.com/home.php\"");
                        return FacebookErrorCode.kError_Login_GenericError;
                    }
                } else {
                    logger.warn("Warning: Got no redirect location.");
                }
            }
            break;
            default:;
            }
        }
        catch (IOException ioe)
        {
            logger.error("IOException\n" + ioe.getMessage());
            return FacebookErrorCode.kError_Global_ValidationError;
        }
        
        logger.trace("=========connectAndLogin end==========");
        return FacebookErrorCode.Error_Global_NoError;
    }

    /**
     * Get the home page, and get the information we need,
     * e.g.:<br>
     * <ol>
     * <li>our uid</li>
     * <li>the channel we're using</li>
     * <li>the post form id</li>
     * </ol>
     *
     * @return
     */
    private int doParseHomePage()
    {
        String getMethodResponseBody = facebookGetMethod(homePageUrl);
        if(getMethodResponseBody == null){
            //Why don't we try again?
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                logger.trace(e.getMessage());
            }
            getMethodResponseBody = facebookGetMethod(homePageUrl);
        }

        return doParseHomePage(getMethodResponseBody);
    }

    private int doParseHomePage(String getMethodResponseBody)
    {
        logger.trace("=========HomePage: getMethodResponseBody begin=========");
        logger.trace(getMethodResponseBody);
        logger.trace("+++++++++HomePage: getMethodResponseBody end+++++++++");

        //deal with the cookies
        logger.trace("The final cookies:");
        List<Cookie> finalCookies = ((DefaultHttpClient) httpClient).getCookieStore().getCookies();
        if (finalCookies.isEmpty()) {
            logger.trace("None");
        } else {
            for (int i = 0; i < finalCookies.size(); i++) {
                logger.trace("- " + finalCookies.get(i).toString());
                //looking for our uid
                if(finalCookies.get(i).getName().equals("c_user"))
                    uid = finalCookies.get(i).getValue();
            }
        }
        
        if(getMethodResponseBody == null){
            logger.fatal("Can't get the home page! Exit.");
            return FacebookErrorCode.Error_Async_UnexpectedNullResponse;
        }

        if(uid == null){
            logger.fatal("Can't get the user's id! Exit.");
            return FacebookErrorCode.Error_System_UIDNotFound;
        }
        //<a href="http://www.facebook.com/profile.php?id=xxxxxxxxx" class="profile_nav_link">
        /*String uidPrefix = "<a href=\"http://www.facebook.com/profile.php?id=";
        String uidPostfix = "\" class=\"profile_nav_link\">";
        //getMethodResponseBody.lastIndexOf(str, fromIndex)
        int uidPostFixPos = getMethodResponseBody.indexOf(uidPostfix);
        if(uidPostFixPos >= 0){
            int uidBeginPos = getMethodResponseBody.lastIndexOf(uidPrefix, uidPostFixPos) + uidPrefix.length();
            if(uidBeginPos < uidPrefix.length()){
                logger.error("Can't get the user's id! Exit.");
                return FacebookErrorCode.Error_System_UIDNotFound;
            }
            uid = getMethodResponseBody.substring(uidBeginPos, uidPostFixPos);
            logger.info("UID: " + uid);
        }else{
            logger.error("Can't get the user's id! Exit.");
            return FacebookErrorCode.Error_System_UIDNotFound;
        }*/

        //find the channel
        String channelPrefix = " \"channel";
        int channelBeginPos = getMethodResponseBody.indexOf(channelPrefix)
                + channelPrefix.length();
        if (channelBeginPos < channelPrefix.length()){
            logger.fatal("Error: Can't find channel!");
            return FacebookErrorCode.Error_System_ChannelNotFound;
        }
        else {
            channel
                = substring(
                    getMethodResponseBody,
                    channelBeginPos,
                    channelBeginPos + 2);
            logger.info("Channel: " + channel);
        }

        //find the post form id
        // <input type="hidden" id="post_form_id" name="post_form_id"
        // value="3414c0f2db19233221ad8c2374398ed6" />
        String postFormIDPrefix = "<input type=\"hidden\" id=\"post_form_id\" name=\"post_form_id\" value=\"";
        int formIdBeginPos = getMethodResponseBody.indexOf(postFormIDPrefix)
                + postFormIDPrefix.length();
        if (formIdBeginPos < postFormIDPrefix.length()){
            logger.fatal("Error: Can't find post form ID!");
            return FacebookErrorCode.Error_System_PostFormIDNotFound;
        }
        else {
            post_form_id
                = substring(
                    getMethodResponseBody, formIdBeginPos, formIdBeginPos + 32);
            logger.info("post_form_id: " + post_form_id);
        }

        return FacebookErrorCode.Error_Global_NoError;
    }

    /**
     * Post a buddy list request to the facebook server, get and parse the response.
     * @return the error code
     */
    private int getBuddyList(){
        logger.trace("====== getBuddyList begin======");

        List <NameValuePair> nvps = new ArrayList <NameValuePair>();
        nvps.add(new BasicNameValuePair("buddy_list", "1"));
        nvps.add(new BasicNameValuePair("notifications", "1"));
        nvps.add(new BasicNameValuePair("force_render", "true"));
        //nvps.add(new BasicNameValuePair("popped_out", "false"));
        nvps.add(new BasicNameValuePair("post_form_id", post_form_id));
        nvps.add(new BasicNameValuePair("user", uid));
        
        try{
            String responseStr = facebookPostMethod(hostUrl, "/ajax/presence/update.php", nvps);

            //for (;;);{"error":0,"errorSummary":"","errorDescription":"No error.","payload":{"buddy_list":{"listChanged":true,"availableCount":1,"nowAvailableList":{"UID1":{"i":false}},"wasAvailableIDs":[],"userInfos":{"UID1":{"name":"Buddy 1","firstName":"Buddy","thumbSrc":"http:\/\/static.ak.fbcdn.net\/pics\/q_default.gif","status":null,"statusTime":0,"statusTimeRel":""},"UID2":{"name":"Buddi 2","firstName":"Buddi","thumbSrc":"http:\/\/static.ak.fbcdn.net\/pics\/q_default.gif","status":null,"statusTime":0,"statusTimeRel":""}},"forcedRender":true},"time":1209560380000}}
            //for (;;);{"error":0,"errorSummary":"","errorDescription":"No error.","payload":{"time":1214626375000,"buddy_list":{"listChanged":true,"availableCount":1,"nowAvailableList":{},"wasAvailableIDs":[],"userInfos":{"1386786477":{"name":"\u5341\u4e00","firstName":"\u4e00","thumbSrc":"http:\/\/static.ak.fbcdn.net\/pics\/q_silhouette.gif","status":null,"statusTime":0,"statusTimeRel":""}},"forcedRender":null,"flMode":false,"flData":{}},"notifications":{"countNew":0,"count":1,"app_names":{"2356318349":"\u670b\u53cb"},"latest_notif":1214502420,"latest_read_notif":1214502420,"markup":"<div id=\"presence_no_notifications\" style=\"display:none\" class=\"no_notifications\">\u65e0\u65b0\u901a\u77e5\u3002<\/div><div class=\"notification clearfix notif_2356318349\" onmouseover=\"CSS.addClass(this, 'hover');\" onmouseout=\"CSS.removeClass(this, 'hover');\"><div class=\"icon\"><img src=\"http:\/\/static.ak.fbcdn.net\/images\/icons\/friend.gif?0:41046\" alt=\"\" \/><\/div><div class=\"notif_del\" onclick=\"return presenceNotifications.showHideDialog(this, 2356318349)\"><\/div><div class=\"body\"><a href=\"http:\/\/www.facebook.com\/profile.php?id=1190346972\"   >David Willer<\/a>\u63a5\u53d7\u4e86\u60a8\u7684\u670b\u53cb\u8bf7\u6c42\u3002 <span class=\"time\">\u661f\u671f\u56db<\/span><\/div><\/div>","inboxCount":"0"}},"bootload":[{"name":"js\/common.js.pkg.php","type":"js","src":"http:\/\/static.ak.fbcdn.net\/rsrc.php\/pkg\/60\/106715\/js\/common.js.pkg.php"}]}
            logger.trace("+++++++++ getBuddyList end +++++++++");
            // testHttpClient("http://www.facebook.com/home.php?");
            int errorCode = FacebookResponseParser.buddylistParser(FacebookAdapter.this, responseStr);

            return errorCode;
        } catch (JSONException e) {
            logger.warn(e.getMessage());
        }
        return FacebookErrorCode.Error_Global_JSONError;
    }

    /**
     * Post a Facebook Chat message to our contact "to", get and parse the response
     * @param msg the message to be sent
     * @param to the buddy to send our message to
     * @return MessageDeliveryFailedEvent(null if no error)
     * @throws JSONException json parsing JSONException
     */
    public MessageDeliveryFailedEvent postFacebookChatMessage(Message msg, Contact to) throws JSONException{
        if(to.getAddress().equals(this.uid))
            return null;

        logger.trace("====== Post Facebook Chat Message begin======");

        logger.trace("PostMessage(): to:"+to.getAddress());
        logger.trace("PostMessage(): msg:"+msg.getContent());

        List <NameValuePair> nvps = new ArrayList <NameValuePair>();
        nvps.add(new BasicNameValuePair("msg_text", (msg.getContent() == null)? "":msg.getContent()));
        nvps.add(new BasicNameValuePair("msg_id", msg.getMessageUID()));
        nvps.add(new BasicNameValuePair("client_time", new Date().getTime() + ""));
        nvps.add(new BasicNameValuePair("to", to.getAddress()));
        nvps.add(new BasicNameValuePair("post_form_id", post_form_id));

        logger.info("@executeMethod PostMessage() ing... : posting facebook chat message to " + to.getAddress());
        // execute postMethod
        String responseStr = facebookPostMethod(hostUrl, "/ajax/chat/send.php", nvps);
        //TODO process the respons string
        //if statusCode == 200: no error;(responsStr contains "errorDescription":"No error.")
        //else retry?

        //for (;;);{"t":"continue"}
        //for (;;);{"t":"refresh"}
        //for (;;);{"t":"refresh", "seq":0}
        //for (;;);{"error":0,"errorSummary":"","errorDescription":"No error.","payload":[],"bootload":[{"name":"js\/common.js.pkg.php","type":"js","src":"http:\/\/static.ak.fbcdn.net\/rsrc.php\/pkg\/60\/106715\/js\/common.js.pkg.php"}]}
        //for (;;);{"error":1356003,"errorSummary":"Send destination not online","errorDescription":"This person is no longer online.","payload":null,"bootload":[{"name":"js\/common.js.pkg.php","type":"js","src":"http:\/\/static.ak.fbcdn.net\/rsrc.php\/pkg\/60\/106715\/js\/common.js.pkg.php"}]}
        logger.trace("+++++++++ Post Facebook Chat Message end +++++++++");

        return FacebookResponseParser.messagePostingResultParser(msg, to, responseStr);
    }

    /**
     * Post a message(NOT facebook chat message) to our contact "to",
     *  get and parse the response.
     * He/She will find this message in his/her inbox.
     * @param msg the message to be sent
     * @param to the buddy to send our message to
     * @return MessageDeliveryFailedEvent(null if no error)
     * @throws JSONException json parsing JSONException
     */
    public MessageDeliveryFailedEvent postMessage(Message msg, Contact to) throws JSONException{
        if(to.getAddress().equals(this.uid))
            return null;
        
        logger.trace("====== PostMessage begin======");

        logger.trace("PostMessage(): to:"+to.getAddress());
        logger.trace("PostMessage(): msg:"+msg.getContent());

        //post_form_id=e699815281b6793a4e228417ee8e68d1&rand_id=12517489&message=ddddddddddddddddddddddddddd&subject=sssssssssssssssssssssssss&ids[0]=1355527894&ids[1]=1190346972&action=compose
        List <NameValuePair> nvps = new ArrayList <NameValuePair>();
        nvps.add(new BasicNameValuePair("post_form_id", post_form_id));
        nvps.add(new BasicNameValuePair("rand_id", msg.getMessageUID()));
        nvps.add(new BasicNameValuePair("message", (msg.getContent() == null)? "":msg.getContent()));
        nvps.add(new BasicNameValuePair("subject", (msg.getSubject() == null)? "":msg.getSubject()));
        nvps.add(new BasicNameValuePair("ids[0]", to.getAddress()));
        nvps.add(new BasicNameValuePair("action", "compose"));

        logger.info("@executeMethod PostMessage() ing... : posting message to " + to.getAddress());
        // execute postMethod
        String responseStr = facebookPostMethod(hostUrl, "/inbox/ajax/ajax.php", nvps);

        //for (;;);{"error":0,"errorSummary":"","errorDescription":"No error.","payload":{"title":"Message Sent","content":"<div class=\"status\">Your message has been sent.<\/div>"},"bootload":[{"name":"js\/common.js.pkg.php","type":"js
        logger.trace("+++++++++ PostMessage end +++++++++");

        return FacebookResponseParser.messagePostingResultParser(msg, to, responseStr);
    }
    
    /**
     * Keep requesting new messages from the server.
     * If we've got one, parse it, do something to promote the message, and request the next message.
     * If there's no new message yet, this "thread" just wait for it.
     * If time out, we try again.
     * @throws Exception
     */
    private void keepRequesting() throws Exception
    {
        int latestSeq = getSeq();
        logger.trace("My seq:" + seq + " | Latest/current seq:" + latestSeq);
        if (seq < 0)
            seq = latestSeq;
        else if (seq > latestSeq)
            seq = latestSeq;

        while (isClientRunning)
        {
            // get the old message between oldseq and seq
            String msgResponseBody =
                facebookGetMethod(getMessageRequestingUrl(seq));

            logger.trace("=========msgResponseBody begin=========");
            logger.trace(msgResponseBody);
            logger.trace("+++++++++msgResponseBody end+++++++++");

            latestSeq
                = FacebookResponseParser
                    .messageRequestResultParser(
                        FacebookAdapter.this,
                        msgResponseBody,
                        seq);
            if (latestSeq > seq)
                seq++;
            else if (latestSeq < seq)
                seq = latestSeq;
        }
    }

    /**
     * Get the current seq number from the server via requesting a message with seq=-1<br>
     * Because -1 is a invalid seq number, the server will return the current seq number.<br>
     * @return the current(newest) seq number
     */
    private int getSeq()
    {
        int tempSeq = -1;
        while (tempSeq == -1)
        {
            // for (;;);{"t":"refresh", "seq":0}
            String seqResponseBody = null;
            try
            {
                seqResponseBody =
                    facebookGetMethod(getMessageRequestingUrl(-1));
                tempSeq
                    = FacebookResponseParser
                        .messageRequestResultParser(
                            this,
                            seqResponseBody,
                            -1);
                logger.trace("getSeq(): SEQ: " + tempSeq);

                if (tempSeq >= 0)
                    return tempSeq;
            }
            catch (JSONException e)
            {
                logger.warn(e.getMessage() + ": response= " + seqResponseBody);
            }
            try
            {
                logger
                    .trace("retrying to fetch the seq code after 1 second...");
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                logger.warn(e.getMessage());
            }
        }
        return tempSeq;
    }

    /**
     * A util to make a message requesting URL.
     * @param seq the seq number
     * @return the message requesting URL
     */
    private String getMessageRequestingUrl(long seq)
    {
        // http://0.channel06.facebook.com/x/0/false/p_MYID=-1
        String url =
            "http://0.channel" + channel + ".facebook.com/x/0/false/p_" + uid
                + "=" + seq;
        logger.trace("request url:" + url);
        return url;
    }

    /**
     * We got a message that should be put into the GUI,
     *  so pass this message to the opration set.
     * @param fm facebook message we got
     */
    public void promoteMessage(FacebookMessage fm)
    {
        // promote the incoming message
        logger.trace("in promoteMessage(): Got a message: " + fm.text);

        // subscribe to the contact list
        Map<String, OperationSet> supportedOperationSets
            = getParentProvider().getSupportedOperationSets();

        if (supportedOperationSets == null || supportedOperationSets.size() < 1)
            throw new NullPointerException(
                "No OperationSet implementations are supported by "
                    + "this implementation. ");

        // get the operation set presence here.
        OperationSetBasicInstantMessagingFacebookImpl operationSetIM =
            (OperationSetBasicInstantMessagingFacebookImpl) supportedOperationSets
                .get(OperationSetBasicInstantMessaging.class.getName());
        operationSetIM.receivedInstantMessage(fm);
    }

    /**
     * Get the buddy who has the given ID from our "buddy cache".
     * @param contactID the facebook user ID
     * @return the buddy, if we can't find him/her, return null.
     */
    public FacebookUser getBuddyFromCacheByID(String contactID)
    {
        return buddyList.getBuddyFromCacheByID(contactID);
    }

    /**
     * Get meta info of this account
     * 
     * @return meta info of this account
     */
    public FacebookUser getMyMetaInfo()
    {
        return buddyList.getMyMetaInfo();
    }

    /**
     * Set the visibility.
     *
     * @param isVisible true(visible) or false(invisible)
     */
    public void setVisibility(boolean isVisible)
    {
        //post("apps.facebook.com", "/ajax/chat/settings.php", "visibility=false");
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("visibility", isVisible + ""));
        nvps.add(new BasicNameValuePair("post_form_id", post_form_id));
        logger.info("@executeMethod setVisibility() ing ...");
        // we don't care the response string now
        facebookPostMethod(hostUrl, "/ajax/chat/settings.php", nvps);
    }

    /**
     * Set status message
     *
     * @param statusMsg status message
     */
    public void setStatusMessage(String statusMsg)
    {
        //post("www.facebook.com", "/updatestatus.php", "status=%s&post_form_id=%s");
        //post("www.facebook.com", "/updatestatus.php", "clear=1&post_form_id=%s");
        //new format:
        //profile_id=1190346972
        //&status=is%20hacking%20again
        //&home_tab_id=1
        //&test_name=INLINE_STATUS_EDITOR
        //&action=HOME_UPDATE
        //&post_form_id=3f1ee64144470cd29f28fb8b0354ef65
        //&_ecdc=false
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        if(statusMsg.length() < 1)
            nvps.add(new BasicNameValuePair("clear", "1"));
        else
            nvps.add(new BasicNameValuePair("status", statusMsg));
        
        nvps.add(new BasicNameValuePair("profile_id", uid));
        nvps.add(new BasicNameValuePair("home_tab_id", "1"));
        nvps.add(new BasicNameValuePair("test_name", "INLINE_STATUS_EDITOR"));
        nvps.add(new BasicNameValuePair("action", "HOME_UPDATE"));
        nvps.add(new BasicNameValuePair("post_form_id", post_form_id));
        logger.info("@executeMethod setStatusMessage() ing ...");
        // we don't care the response string now
        facebookPostMethod(hostUrl, "/updatestatus.php", nvps);
    }

    /**
     * Pause the client.<br>
     * Ensure that initialize() can resume httpclient
     *
     * FIXME logout first
     */
    public void pause()
    {
        // TODO pause the client
        // maybe we should log out first
        isClientRunning = false;
        Logout();
    }

    /**
     * Log out
     */
    private void Logout()
    {
        Map<String, OperationSet> supportedOperationSets
            = getParentProvider().getSupportedOperationSets();

        if (supportedOperationSets == null || supportedOperationSets.size() < 1)
            throw new NullPointerException(
                "No OperationSet implementations are supported by "
                    + "this implementation. ");

        // get the operation set presence here.
        OperationSetPersistentPresenceFacebookImpl operationSetPresence =
            (OperationSetPersistentPresenceFacebookImpl) supportedOperationSets
                .get(OperationSetPresence.class.getName());
        // tag all the buddies as offline
        operationSetPresence
            .setPresenceStatusForAllContacts(FacebookStatusEnum.OFFLINE);
        
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("confirm", "1"));
        logger.info("@executeMethod Logout() ing ...");
        // we don't care the response string now
        facebookPostMethod(hostUrl, "/logout.php", nvps);
    }

    /**
     * Shut down the client.
     */
    public void shutdown()
    {
        // If every http client has its own ConnectionManager, then shut it
        // down.
        this.httpClient.getConnectionManager().shutdown();
        this.httpClient = null;
        this.msgIDCollection.clear();
        this.buddyList.clear();
    }

    /**
     * Post typing notification to the given contact.
     * @param notifiedContact the contact we want to notify
     * @param typingState our current typing state(SC)
     * @throws HttpException the http exception
     * @throws IOException IO exception
     * @throws JSONException JSON parsing exception
     * @throws Exception the general exception
     */
    public void postTypingNotification(Contact notifiedContact, int typingState)
        throws HttpException,
        IOException,
        JSONException,
        Exception
    {
        if (notifiedContact.getAddress().equals(this.uid))
            return;

        int facebookTypingState = 0;

        switch (typingState)
        {
        case OperationSetTypingNotifications.STATE_TYPING:
            facebookTypingState = 1;
            break;
        /*
         * case OperationSetTypingNotifications.STATE_STOPPED:
         * facebookTypingState = 0; break;
         */
        default:
            facebookTypingState = 0;
            break;
        }
        logger.trace("====== PostTypingNotification begin======");

        logger.trace("PostTypingNotification(): to:"
            + notifiedContact.getAddress());
        logger.trace("PostTypingNotification(): typing state:"
            + facebookTypingState);

        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("typ", facebookTypingState + ""));
        nvps.add(new BasicNameValuePair("to", notifiedContact.getAddress()));
        nvps.add(new BasicNameValuePair("post_form_id", post_form_id));

        logger
            .info("@executeMethod PostMessage() ing... : posting TypingNotification to "
                + notifiedContact.getAddress());
        // we don't care the response string now
        facebookPostMethod(hostUrl, "/ajax/chat/typ.php", nvps);
        // TODO process the respons string
        // if statusCode == 200: no error;(responsStr contains "errorDescription":"No error.")
        // else retry?

        // for (;;);{"t":"continue"}
        // for (;;);{"t":"refresh"}
        // for (;;);{"t":"refresh", "seq":0}
        // for (;;);{"error":0,"errorSummary":"","errorDescription":"No
        // error.","payload":[],"bootload":[{"name":"js\/common.js.pkg.php","type":"js","src":"http:\/\/static.ak.fbcdn.net\/rsrc.php\/pkg\/60\/106715\/js\/common.js.pkg.php"}]}
        // for (;;);{"error":1356003,"errorSummary":"Send destination not
        // online","errorDescription":"This person is no longer
        // online.","payload":null,"bootload":[{"name":"js\/common.js.pkg.php","type":"js","src":"http:\/\/static.ak.fbcdn.net\/rsrc.php\/pkg\/60\/106715\/js\/common.js.pkg.php"}]}
        logger.trace("+++++++++ PostTypingNotification end +++++++++");
        // testHttpClient("http://www.facebook.com/home.php?");
    }

    /**
     * We got a typing notification from the facebook server,
     * we pass it to the GUI.
     * @param fromID where the typing notification from
     * @param facebookTypingState facebook typing state: 1: typing; 0: stop typing.
     */
    public void promoteTypingNotification(String fromID, int facebookTypingState)
    {
        // promote the incoming message
        logger
            .info("in promoteTypingNotification(): Got a TypingNotification: "
                + facebookTypingState);

        Map<String, OperationSet> supportedOperationSets
            = getParentProvider().getSupportedOperationSets();

        if (supportedOperationSets == null || supportedOperationSets.size() < 1)
            throw new NullPointerException(
                "No OperationSet implementations are supported by "
                    + "this implementation. ");

        // get the operation set presence here.
        OperationSetPersistentPresenceFacebookImpl operationSetPP =
            (OperationSetPersistentPresenceFacebookImpl) supportedOperationSets
                .get(OperationSetPersistentPresence.class.getName());

        Contact fromContact = operationSetPP.findContactByID(fromID);
        if (fromContact == null)
        {
            fromContact = operationSetPP.createVolatileContact(fromID);
        }

        int typingState = OperationSetTypingNotifications.STATE_UNKNOWN;
        switch (facebookTypingState)
        {
        case 1:
            typingState = OperationSetTypingNotifications.STATE_TYPING;
            break;
        case 0:
            typingState = OperationSetTypingNotifications.STATE_STOPPED;
            break;
        default:
            typingState = OperationSetTypingNotifications.STATE_UNKNOWN;
        }
        // get the operation set presence here.
        OperationSetTypingNotificationsFacebookImpl operationSetTN =
            (OperationSetTypingNotificationsFacebookImpl) supportedOperationSets
                .get(OperationSetTypingNotifications.class.getName());
        operationSetTN.receivedTypingNotification(fromContact, typingState);
    }

    /**
     * Get the profile page for parsing. It's invoked when user opens contact info box.
     * @param contactAddress the contact address
     * @return profile page string
     */
    public String getProfilePage(String contactAddress)
    {
        //TODO if homePageUrl.contains("new.facebook.com") return;
        // because if we try to get the "new" page, the account's layout would be set to new style.
        //Someone may not like this.

        //http://www.new.facebook.com/profile.php?id=1190346972&v=info&viewas=1190346972
        // http://www.new.facebook.com/profile.php?id=1386786477&v=info
        return facebookGetMethod(hostUrlNew + "/profile.php?id="
            + contactAddress + "&v=info");
    }

    /**
     * The general facebook post method.
     * @param host the host
     * @param urlPostfix the post fix of the URL
     * @param data the parameter
     * @return the response string
     */
    private String facebookPostMethod(String host, String urlPostfix,
        List<NameValuePair> nvps)
    {
        logger.info("@executing facebookPostMethod():" + host + urlPostfix);
        String responseStr = null;
        try
        {
            HttpPost httpost = new HttpPost(host + urlPostfix);
            httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

            // execute postMethod
            HttpResponse postResponse = httpClient.execute(httpost);
            HttpEntity entity = postResponse.getEntity();

            logger.trace("facebookPostMethod: " + postResponse.getStatusLine());
            if (entity != null)
            {
                responseStr = EntityUtils.toString(entity);
                logger.trace(responseStr);
                entity.consumeContent();
            }
            logger.info("Post Method done("
                + postResponse.getStatusLine().getStatusCode()
                + "), response string length: "
                + (responseStr == null ? 0 : responseStr.length()));
        }
        catch (IOException e)
        {
            logger.warn(e.getMessage());
        }
        //TODO process the respons string
        //if statusCode == 200: no error;(responsStr contains "errorDescription":"No error.")
        //else retry?
        return responseStr;
    }

    /**
     * The general facebook get method.
     * @param url the URL of the page we wanna get
     * @return the response string
     */
    private String facebookGetMethod(String url)
    {
        logger.info("@executing facebookGetMethod():" + url);
        String responseStr = null;

        try
        {
            HttpGet loginGet = new HttpGet(url);
            HttpResponse response = httpClient.execute(loginGet);
            HttpEntity entity = response.getEntity();

            logger.trace("facebookGetMethod: " + response.getStatusLine());
            if (entity != null)
            {
                responseStr = EntityUtils.toString(entity);
                entity.consumeContent();
            }

            int statusCode = response.getStatusLine().getStatusCode();

            /**
             * @fixme I am not sure of if 200 is the only code that means
             *        "success"
             */
            if (statusCode != 200)
            {
                // error occured
                logger.warn("Error Occured! Status Code = " + statusCode);
                responseStr = null;
            }
            logger.info("Get Method done(" + statusCode
                + "), response string length: "
                + (responseStr == null ? 0 : responseStr.length()));
        }
        catch (IOException e)
        {
            logger.warn(e.getMessage());
        }

        return responseStr;
    }

    int reconnect()
        throws JSONException
    {
        String responseStr
            = facebookGetMethod(hostUrl + RECONNECT_URL + post_form_id);
        String prefix = "for (;;);";
        if (responseStr.startsWith(prefix))
            responseStr = responseStr.substring(prefix.length());

        JSONObject response = new JSONObject(responseStr);
        JSONObject payload = response.getJSONObject("payload");

        String host = payload.getString("host");
        channel = substring(host, "channel".length(), host.length());

        return payload.getInt("seq");
    }

    /**
     * Returns a new string that is a substring of a specific string
     * <code>s</code> but does not share its internal char buffer and thus does
     * not retain it. This prevents unnecessary and undesired retaining of
     * memory when all references to <code>s</code> are broken. The substring
     * begins at the specified <code>beginIndex</code> and extends to the
     * character at index <code>endIndex-1</code>. Thus the length of the
     * substring is <code>endIndex-beginIndex</code>.
     * 
     * @param s
     *            the string to extract the specified substring from
     * @param beginIndex
     *            the beginning index, inclusive
     * @param endIndex
     *            the ending index, exclusive
     * @return the specified substring which, unlike
     *         <code>String#substring(int, int)</code>, does not share the same
     *         internal char buffer and thus does not retain the char buffer of
     *         <code>s</code>
     */
    private static String substring(String s, int beginIndex, int endIndex)
    {

        /*
         * XXX The use of the String constructor bellow is explicit because it
         * does not share the internal char buffer between the two String
         * instances.
         */
        return new String(s.substring(beginIndex, endIndex));
    }
}
