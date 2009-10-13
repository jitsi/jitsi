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
import org.apache.http.client.methods.*;
import org.apache.http.util.*;
import org.json.*;

/**
 * This poller checks for new messages in Facebook servers and notifies to the
 * registered {@link FacebookSessionListener listeners} if a new
 * {@link FacebookMessage} is found.<br>
 * Registered {@link FacebookSessionListener listeners} are notified upon
 * arrival of both messages from other buddies and own messages.<br>
 * It's the responsibility of the {@link FacebookSessionListener listeners} to
 * discard undesired notifications, e.g. own messages.
 * 
 * @author Edgar Poce
 */
public class FacebookIncomingMessagePoller
{
    private static Logger logger = Logger.getLogger(FacebookIncomingMessagePoller.class);

    /**
     * History url
     */
    private static final String HISTORY_URL = "http://www.facebook.com/ajax/chat/history.php?id=";

    /**
     * The Facebook session
     */
    private final FacebookSession session;

    /**
     * Listeners which are notified when a new message arrives
     */
    private List<FacebookSessionListener> listeners = new ArrayList<FacebookSessionListener>();

    /**
     * A queue of received messages used to maintain a record of the last
     * messages received
     */
    private Queue<String> received = new LinkedList<String>();

    /**
     * After refreshing messages must be retrieved by calling the history, which
     * doesn't inform the SEQ number, that's why we keep the last receive time
     * in order to inform to registered listeners messages which have a time
     * after the time of the last message received.
     */
    private long lastReceiveTime = System.currentTimeMillis();

    /**
     * The sequence number of the last message received
     */
    private int seq = -1;

    /**
     * This random value generator is used when creating a URL to retrieve
     * messages, the random part of the URL is intended to avoid undesired
     * caching
     */
    private Random random = new Random();

    public FacebookIncomingMessagePoller(FacebookSession session)
    {
        this.session = session;
    }

    /**
     * Polls new messages from Facebook servers
     * 
     * @throws BrokenFacebookProtocolException
     * @throws IOException
     */
    public void poll()
        throws BrokenFacebookProtocolException,
               IOException,
               FacebookErrorException
    {
        try
        {
            if (this.seq == -1)
            {
                this.seq = getNewestSeq();
            }
            logger.debug("polling seq " + this.seq);
            // poll messages
            HttpGet get = new HttpGet(this.getMessagePollUrl());
            HttpResponse response = this.session.getHttpClient().execute(get);

            // validate response code
            if (response.getStatusLine().getStatusCode() != 200)
            {
                response.getEntity().consumeContent();
                throw new BrokenFacebookProtocolException(
                        "unable to poll messages. http code "
                                + response.getStatusLine().getStatusCode());
            }
            String body = EntityUtils.toString(response.getEntity());
            FacebookJsonResponse jsonResponse = new FacebookJsonResponse(
                    this.session, body);
            JSONObject json = jsonResponse.getJson();

            // parse json response
            if (json.has("t"))
            {
                String t = json.getString("t");
                if (t.equals("msg"))
                {
                    JSONArray ms = (JSONArray) json.get("ms");
                    for (int i = 0; i < ms.length(); i++)
                    {
                        JSONObject msg = ms.getJSONObject(i);
                        if (msg.get("type").equals("typ"))
                        {
                            for (FacebookSessionListener l : this.listeners)
                            {
                                l.onIncomingTypingNotification(msg
                                        .getString("from"), msg.getInt("st"));
                            }
                        }
                        else if (msg.get("type").equals("msg"))
                        {
                            // the message itself
                            FacebookMessage fm = new FacebookMessage(msg);
                            this.lastReceiveTime = fm.getTime();
                            this.notifyListeners(fm);
                        }
                        else
                        {
                            logger.debug("neither notification nor message "
                                    + msg);
                        }
                    }
                    this.seq++;
                }
                else if (t.equals("refresh"))
                {
                    logger.debug("refreshing...");
                    this.pollHistory();                    
                    if (json.has("seq"))
                    {
                        this.seq = json.getInt("seq");
                    }
                    else
                    {
                        Integer newSeq = this.getNewestSeq();
                        if (newSeq != null)
                        {
                            this.seq = newSeq;
                        }
                    }
                    this.session.reconnect();
                }
            }
        }
        catch (JSONException e)
        {
            throw new BrokenFacebookProtocolException(e);
        }

    }

    /**
     * Returns the SEQ number from the server via requesting a message with SEQ
     * =-1. The server will return the current SEQ number because -1 is a
     * invalid SEQ number.
     * 
     * @throws BrokenFacebookProtocolException
     * @throws IOException
     * @throws FacebookErrorException
     * @throws JSONException
     */
    private Integer getNewestSeq()
        throws BrokenFacebookProtocolException,
               IOException,
               FacebookErrorException
    {
        logger.debug("GET newest seq");
        Integer newest = null;
        try
        {
            String url = getMessagePollUrl();
            HttpPost post = new HttpPost(url);
            HttpResponse response = this.session.getHttpClient().execute(post);
            if (response.getStatusLine().getStatusCode() == 200)
            {
                String body = EntityUtils.toString(response.getEntity());
                FacebookJsonResponse jsonResponse = new FacebookJsonResponse(
                        this.session, body);
                JSONObject json = jsonResponse.getJson();
                if (json.has("seq"))
                {
                    newest = json.getInt("seq");
                    logger.debug("seq updated:" + newest);
                }
            }
            else
            {
                response.getEntity().consumeContent();
                throw new BrokenFacebookProtocolException("http code "
                        + response.getStatusLine().getStatusCode());
            }
        }
        catch (JSONException e)
        {
            throw new BrokenFacebookProtocolException(e);
        }
        return newest;
    }

    public void addListener(FacebookSessionListener listener)
    {
        this.listeners.add(listener);
    }

    public int getSeq()
    {
        return seq;
    }

    private String getMessagePollUrl()
    {
        return "http://0.channel" + this.session.getChannel()
                + ".facebook.com/x/" + random.nextInt() + "/false/p_"
                + this.session.getUid() + "=" + this.seq;
    }

    /**
     * Notifies the listeners that a new message is available. This method
     * performs a validation to avoid publishing the same message more than
     * once.
     * 
     * @param msg
     *            the message to public
     */
    private synchronized void notifyListeners(FacebookMessage msg)
    {
        logger.debug("message received " + msg.getMsgID());
        if (this.received.contains(msg.getMsgID()))
        {
            logger.debug("discarding duplicated message " + msg.getMsgID());
        }
        else
        {
            for (FacebookSessionListener l : this.listeners)
                l.onIncomingChatMessage(msg);
            this.received.add(msg.getMsgID());
            if (this.received.size() > 20)
            {
                this.received.remove();
            }
        }
    }

    /**
     * Retrieves the history from Facebook servers for every contact in the
     * buddy list
     * 
     * @throws IOException
     * @throws BrokenFacebookProtocolException
     * @throws FacebookErrorException
     */
    public void pollHistory()
        throws IOException,
               BrokenFacebookProtocolException,
               FacebookErrorException
    {
        for (FacebookUser user : this.session.getBuddyList().getBuddies())
        {
            HttpGet get = new HttpGet(HISTORY_URL + user.uid);
            HttpResponse response = this.session.getHttpClient().execute(get);
            String body = EntityUtils.toString(response.getEntity());
            if (response.getStatusLine().getStatusCode() != 200)
            {
                throw new BrokenFacebookProtocolException("http code "
                        + response.getStatusLine().getStatusCode() + ". "
                        + body);
            }
            try
            {
                FacebookJsonResponse jsonResponse = new FacebookJsonResponse(
                        this.session, body);
                JSONArray history = jsonResponse.getJson().getJSONObject(
                        "payload").getJSONArray("history");
                for (int i = 0; i < history.length(); i++)
                {
                    JSONObject msg = history.getJSONObject(i);
                    if (msg.getString("type").equals("msg"))
                    {
                        FacebookMessage fm = null;
                        String fromUid = msg.getString("from");
                        if (fromUid.equals(user.uid))
                        {
                            fm = new FacebookMessage(user, msg);
                            if (fm.getTime() > this.lastReceiveTime)
                            {
                                this.lastReceiveTime = fm.getTime();
                                this.notifyListeners(fm);
                            }
                        }
                    }
                }
            }
            catch (JSONException e)
            {
                throw new BrokenFacebookProtocolException("response " + body, e);
            }
        }
    }
}
