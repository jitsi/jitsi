/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.facebook;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.util.*;

import org.apache.http.*;
import org.apache.http.client.entity.*;
import org.apache.http.client.methods.*;
import org.apache.http.message.*;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.*;
import org.json.*;

/**
 * Facebook buddy list that store the online buddies information we got from the
 * server since we logged in. Some information of ourselves also included. <br>
 * This class is responsible of establishing the http connections and updating
 * its state when requested.
 * 
 * @author Dai Zhiwei
 * @author Edgar Poce
 */
public class FacebookBuddyList
{
    private static Logger logger = Logger.getLogger(FacebookBuddyList.class);

    /**
     * The url of the update
     */
    private static final String BUDDYLIST_URL
        = "http://www.facebook.com/ajax/chat/buddy_list.php";

    /**
     * The Facebook Session
     */
    private final FacebookSession session;

    /**
     * Our (online) buddies' information cache
     */
    private transient Map<String, FacebookUser> cache
        = new LinkedHashMap<String, FacebookUser>();

    /**
     * Some information of ourselves/myself.
     */
    private transient FacebookUser me;

    /**
     * Listener of this buddy list
     */
    private List<FacebookSessionListener> listeners
        = new ArrayList<FacebookSessionListener>();

    /**
     * Init the cache and the parent adapter.
     * 
     * @param session the Facebook session
     */
    public FacebookBuddyList(FacebookSession session)
    {
        this.session = session;
    }

    public void update()
        throws BrokenFacebookProtocolException,
               IOException,
               FacebookErrorException
    {
        // reconnecting
        this.session.reconnect();
        logger.info("Updating buddy list...");
        // perform POST request
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("buddy_list", "1"));
        nvps.add(new BasicNameValuePair("notifications", "1"));
        nvps.add(new BasicNameValuePair("force_render", "true"));
        nvps.add(new BasicNameValuePair("post_form_id", session.getFormId()));
        nvps.add(new BasicNameValuePair("user", session.getUid()));
        HttpPost post = new HttpPost(BUDDYLIST_URL);
        post.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
        HttpResponse response = this.session.getHttpClient().execute(post);
        String body = EntityUtils.toString(response.getEntity());
        if (response.getStatusLine().getStatusCode() != 200)
        {
            throw new BrokenFacebookProtocolException("Code "
                    + response.getStatusLine().getStatusCode()
                    + ". Unable to update Facebook buddy list.");
        }
        this.update(body);
    }

    /**
     * Get meta info of this account
     * 
     * @return meta info of this account
     */
    public FacebookUser getMyMetaInfo()
    {
        return me;
    }

    /**
     * Get our buddy who has the given id from the cache.
     * 
     * @param uid the id we want to look up
     * @return the buddy who has the given id
     * @throws FacebookErrorException
     * @throws IOException
     * @throws BrokenFacebookProtocolException
     */
    public FacebookUser getBuddyByUID(String uid)
            throws BrokenFacebookProtocolException,
                   IOException,
                   FacebookErrorException
    {
        FacebookUser buddy = this.cache.get(uid);
        if (buddy == null && this.me != null && this.me.uid.equals(uid))
        {
            buddy = me;
        }
        return buddy;
    }

    /**
     * @return the users in the cache
     */
    public Collection<FacebookUser> getBuddies()
    {
        return Collections.unmodifiableCollection(this.cache.values());
    }

    /**
     * Release the resource
     */
    public void clear()
    {
        for (FacebookUser u : this.cache.values())
        {
            u.isOnline = false;
        }
    }

    /**
     * Updates the buddy list<br>
     * If the {@link FacebookSession} is logged out then this method returns
     * without modifying the state of the buddy list.
     * 
     * @param body
     * @throws BrokenFacebookProtocolException
     * @throws FacebookErrorException
     */
    @SuppressWarnings("unchecked")
    public void update(String body)
        throws BrokenFacebookProtocolException,
               FacebookErrorException
    {
        // the session might be getting closed by another thread
        // do nothing if it's logged out
        synchronized (session)
        {
            if (!session.isLoggedIn())
            {
                return;
            }
        }

        JSONObject jsonBuddyList = parseBody(body);
        try {
            /*
             * If listChanged, then we can get the buddies available via looking
             * at the nowAvailableList else. we can only get the buddies' info,
             * and the nowAvailableList is empty.
             */
            JSONObject userInfos = (JSONObject) jsonBuddyList.get("userInfos");
            if (userInfos != null)
            {
                // Then add the new buddies and set them as online(constructor)
                Iterator<String> it = userInfos.keys();
                while (it.hasNext())
                {
                    String key = it.next();
                    JSONObject jsonUser = (JSONObject) userInfos.get(key);
                    if (jsonUser == null)
                    {
                        throw new BrokenFacebookProtocolException(
                                "unable to get user info. " + userInfos);
                    }
                    FacebookUser buddy = new FacebookUser(key, jsonUser);
                    if (buddy.uid.equals(this.session.getUid()))
                    {
                        this.me = buddy;
                    }
                    else
                    {
                        this.cache.put(key, buddy);
                    }
                }
            }

            JSONObject nowAvailableList = jsonBuddyList
                    .getJSONObject("nowAvailableList");

            if (nowAvailableList == null)
            {
                throw new BrokenFacebookProtocolException(
                        "Unable to read Facebook now available list");
            }

            for (FacebookUser user : this.cache.values())
            {
                if (nowAvailableList.has(user.uid))
                {
                    user.isOnline = true;
                    user.lastSeen = Calendar.getInstance().getTimeInMillis();
                    user.isIdle = nowAvailableList.getJSONObject(user.uid)
                            .getBoolean("i");
                }
                else
                {
                    user.isOnline = false;
                }
            }
            // notify listeners
            for (FacebookSessionListener l : this.listeners)
                l.onBuddyListUpdated();
        }
        catch (JSONException e)
        {
            throw new BrokenFacebookProtocolException(e);
        }
    }

    private JSONObject parseBody(String body)
        throws BrokenFacebookProtocolException,
               FacebookErrorException
    {
        FacebookJsonResponse jsonResponse = new FacebookJsonResponse(session,
                body);
        try
        {
            JSONObject json = jsonResponse.getJson();
            JSONObject payload = (JSONObject) json.get("payload");
            if (payload == null)
            {
                throw new BrokenFacebookProtocolException(
                        "unable to parse buddy list. there's no payload field. "
                                + jsonResponse);
            }
            JSONObject jsonBuddyList = (JSONObject) payload.get("buddy_list");
            if (jsonBuddyList == null)
            {
                throw new BrokenFacebookProtocolException(
                        "unable to parse buddy list. there's no buddy list field. "
                                + jsonResponse);
            }
            return jsonBuddyList;
        }
        catch (JSONException e)
        {
            throw new BrokenFacebookProtocolException(
                    "unable to parse json response");
        }
    }

    public int getSize()
    {
        return this.cache.size();
    }

    /**
     * Adds a listener which is notified when the buddy list state changes
     * 
     * @param listener
     */
    public void addListener(FacebookSessionListener listener)
    {
        this.listeners.add(listener);
    }
}
