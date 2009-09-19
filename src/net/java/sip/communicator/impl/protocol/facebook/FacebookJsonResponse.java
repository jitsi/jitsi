/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.facebook;

import org.json.*;

/**
 * A simple parser which provides convenient cleanup of the json response from
 * Facebook.
 * 
 * @author Edgar Poce
 */
public class FacebookJsonResponse
{
    private final JSONObject json;

    /**
     * Parses the response and creates the json object, if the response has
     * errors a {@link FacebookErrorException} is thrown
     * 
     * @param session
     * @param body
     * @throws BrokenFacebookProtocolException
     *             if the text can not be converted to a json object
     * @throws FacebookErrorException
     *             if the response has a facebook error code
     */
    public FacebookJsonResponse(FacebookSession session, String body)
        throws BrokenFacebookProtocolException,
               FacebookErrorException
    {
        String prefix = "for (;;);";
        if (body.startsWith(prefix))
        {
            body = body.substring(prefix.length());
        }
        try
        {
            this.json = new JSONObject(body);
            if (this.json.has("error"))
            {
                int errorCode = getJson().getInt("error");
                if (errorCode != 0)
                {
                    throw new FacebookErrorException(session, errorCode);
                }
            }
        }
        catch (JSONException e)
        {
            throw new BrokenFacebookProtocolException(
                    "Unable to read error from response. " + this.getJson());
        }
    }

    public JSONObject getJson()
    {
        return json;
    }
}
