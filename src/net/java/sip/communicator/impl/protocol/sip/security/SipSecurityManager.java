/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.security;

import javax.sip.*;
import javax.sip.message.*;
import javax.sip.address.*;
import javax.sip.header.*;

import java.util.*;

import java.text.*;
import javax.sip.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.impl.protocol.sip.*;

/**
 * The class handles authentication challenges, caches user credentials and
 * takes care (through the SecurityAuthority interface) about retrieving
 * passwords.
 *
 * @author Emil Ivov
 * @version 1.0
 */

public class SipSecurityManager
{
    private static final Logger logger
        = Logger.getLogger(SipSecurityManager.class);

    private SecurityAuthority securityAuthority = null;
    private HeaderFactory headerFactory = null;

    /**
     * Credentials cached so far.
     */
    CredentialsCache cachedCredentials = new CredentialsCache();

    public SipSecurityManager()
    {

    }

    /**
     * Set the header factory to be used when creating authorization headers
     *
     * @param headerFactory the header factory that we'll be using when creating
     * authorization headers.
     */
    public void setHeaderFactory(HeaderFactory headerFactory)
    {
        this.headerFactory = headerFactory;
    }

    /**
     * Uses securityAuthority to determinie a set of valid user credentials
     * for the specified Response (Challenge) and appends it to the challenged
     * request so that it could be retransmitted.
     *
     * Fredrik Wickstrom reported that dialog cseq counters are not incremented
     * when resending requests. He later uncovered additional problems and proposed
     * a way to fix them (his proposition was taken into account).
     *
     * @param challenge the 401/407 challenge response
     * @param challengedTransaction the transaction established by the challenged
     * request
     * @param transactionCreator the JAIN SipProvider that we should use to
     * create the new transaction.
     *
     * @return a transaction containing a reoriginated request with the
     *         necessary authorization header.
     * @throws SipException if we get an exception white creating the
     * new transaction
     * @throws InvalidArgumentException if we fail to create a new header
     * containing user credentials.
     * @throws ParseException if we fail to create a new header containing user
     * credentials.
     * @throws NullPointerException if an argument or a header is null.
     * @throws OperationFailedException if we fail to acquire a password from
     * our security authority.
     */
    public ClientTransaction handleChallenge(
                                    Response          challenge,
                                    ClientTransaction challengedTransaction,
                                    SipProvider       transactionCreator)
        throws SipException,
               InvalidArgumentException,
               ParseException,
               OperationFailedException,
               NullPointerException
    {
        String branchID = challengedTransaction.getBranchId();
        Request challengedRequest = challengedTransaction.getRequest();

        Request reoriginatedRequest = (Request) challengedRequest.clone();

        ListIterator authHeaders = null;

        if (challenge == null || reoriginatedRequest == null)
            throw new NullPointerException(
                "A null argument was passed to handle challenge.");

        if (challenge.getStatusCode() == Response.UNAUTHORIZED)
            authHeaders = challenge.getHeaders(WWWAuthenticateHeader.NAME);
        else if (challenge.getStatusCode() ==
                 Response.PROXY_AUTHENTICATION_REQUIRED)
            authHeaders = challenge.getHeaders(ProxyAuthenticateHeader.NAME);

        if (authHeaders == null)
            throw new NullPointerException(
                "Could not find WWWAuthenticate or ProxyAuthenticate headers");

        //Remove all authorization headers from the request (we'll re-add them
        //from cache)
        reoriginatedRequest.removeHeader(AuthorizationHeader.NAME);
        reoriginatedRequest.removeHeader(ProxyAuthorizationHeader.NAME);

        //rfc 3261 says that the cseq header should be augmented for the new
        //request. do it here so that the new dialog (created together with
        //the new client transaction) takes it into account.
        //Bug report - Fredrik Wickstrom
        CSeqHeader cSeq =
            (CSeqHeader) reoriginatedRequest.getHeader( (CSeqHeader.NAME));
        cSeq.setSeqNumber(cSeq.getSeqNumber() + 1l);

        ClientTransaction retryTran =
            transactionCreator.getNewClientTransaction(reoriginatedRequest);

        WWWAuthenticateHeader authHeader = null;
        while (authHeaders.hasNext())
        {
            authHeader = (WWWAuthenticateHeader) authHeaders.next();
            String realm = authHeader.getRealm();

            //Check whether we have cached credentials for authHeader's realm
            //make sure that if such credentials exist they get removed. The
            //challenge means that there's something wrong with them.
            CredentialsCacheEntry ccEntry =
                (CredentialsCacheEntry) cachedCredentials.remove(realm);

            //Try to guess user name and facilitate user
            UserCredentials defaultCredentials = new UserCredentials();
            FromHeader from =
                (FromHeader) reoriginatedRequest.getHeader(FromHeader.NAME);
            URI uri = from.getAddress().getURI();
            if (uri.isSipURI())
            {
                String user = ( (SipURI) uri).getUser();
                defaultCredentials.setUserName(
                    user == null
                    ? null //PropertiesDepot.getProperty("net.java.sip.communicator.sip.USER_NAME")
                    : user);
            }

            boolean ccEntryHasSeenTran = false;

            if (ccEntry != null)
                ccEntryHasSeenTran = ccEntry.processResponse(branchID);

            //get a new pass
            if (ccEntry == null // we don't have credentials for the specified
                                //realm
                || ( (ccEntryHasSeenTran // we have already tried with those
                      && !authHeader.isStale()))) // and this is (!stale) not
                                                  // just a request to reencode
            {

                logger.debug(
                    "We don't seem to have a good pass! Get one.");
                if (ccEntry == null)
                    ccEntry = new CredentialsCacheEntry();

                ccEntry.userCredentials =
                    getSecurityAuthority().obtainCredentials(
                        realm,
                        defaultCredentials);

                //put the returned user name in the properties file
                //so that it appears as a default one next time user is prompted for pass
//                        PropertiesDepot.setProperty("net.java.sip.communicator.sip.USER_NAME",
//                                                    ccEntry.userCredentials.getUserName()) ;
//                        PropertiesDepot.storeProperties();
            }
            //encode and send what we have
            else if (ccEntry != null
                     && (!ccEntryHasSeenTran || authHeader.isStale()))
            {
                logger.debug(
                    "We seem to have a pass in the cache. Let's try with it.");
            }

            //if user canceled or sth else went wrong
            if (ccEntry.userCredentials == null)
                throw new OperationFailedException(
                    "Unable to authenticate with realm " + realm
                    , OperationFailedException.GENERAL_ERROR);

            AuthorizationHeader authorization =
                this.getAuthorization(
                    reoriginatedRequest.getMethod(),
                    reoriginatedRequest.getRequestURI().toString(),
                    reoriginatedRequest.getContent() == null ? "" :
                    reoriginatedRequest.getContent().toString(),
                    authHeader,
                    ccEntry.userCredentials);

            ccEntry.processRequest(retryTran.getBranchId());
            cachedCredentials.cacheEntry(realm, ccEntry);

            logger.debug("Created authorization header: " +
                         authorization.toString());

            reoriginatedRequest.addHeader(authorization);
        }

        logger.debug("Returning authorization transaction.");
        return retryTran;
    }

    /**
     * Sets the SecurityAuthority instance that should be queried for user
     * credentials.
     *
     * @param authority the SecurityAuthority instance that should be queried
     * for user credentials.
     */
    public void setSecurityAuthority(SecurityAuthority authority)
    {
        this.securityAuthority = authority;
    }

    /**
     * Returns the SecurityAuthority instance that SipSecurityManager uses to
     * obtain user credentials.
     *
     * @return the SecurityAuthority instance that SipSecurityManager uses to
     * obtain user credentials.
     */
    public SecurityAuthority getSecurityAuthority()
    {
        return this.securityAuthority;
    }

    /**
     * Generates an authorisation header in response to wwwAuthHeader.
     *
     * @param method method of the request being authenticated
     * @param uri digest-uri
     * @param requestBody the body of the request.
     * @param authHeader the challenge that we should respond to
     * @param userCredentials username and pass
     *
     * @return an authorisation header in response to authHeader.
     *
     * @throws OperationFailedException if auth header was malformated.
     */
    private AuthorizationHeader getAuthorization(
                String                method,
                String                uri,
                String                requestBody,
                WWWAuthenticateHeader authHeader,
                UserCredentials       userCredentials)
        throws OperationFailedException
    {
        String response = null;
        try
        {
            response = MessageDigestAlgorithm.calculateResponse(
                authHeader.getAlgorithm(),
                userCredentials.getUserName(),
                authHeader.getRealm(),
                new String(userCredentials.getPassword()),
                authHeader.getNonce(),
                //TODO we should one day implement those two null-s
                null, //nc-value
                null, //cnonce
                method,
                uri,
                requestBody,
                authHeader.getQop());
        }
        catch (NullPointerException exc)
        {
            throw new OperationFailedException(
                "The authenticate header was malformatted"
                , OperationFailedException.GENERAL_ERROR
                , exc);
        }

        AuthorizationHeader authorization = null;
        try
        {
            if (authHeader instanceof ProxyAuthenticateHeader)
            {
                authorization = headerFactory.createProxyAuthorizationHeader(
                    authHeader.getScheme());
            }
            else
            {
                authorization = headerFactory.createAuthorizationHeader(
                    authHeader.getScheme());
            }

            authorization.setUsername(userCredentials.getUserName());
            authorization.setRealm(authHeader.getRealm());
            authorization.setNonce(authHeader.getNonce());
            authorization.setParameter("uri", uri);
            authorization.setResponse(response);
            if (authHeader.getAlgorithm() != null)
                authorization.setAlgorithm(authHeader.getAlgorithm());
            if (authHeader.getOpaque() != null)
                authorization.setOpaque(authHeader.getOpaque());

            authorization.setResponse(response);
        }
        catch (ParseException ex)
        {
            throw new
                SecurityException("Failed to create an authorization header!");
        }

        return authorization;
    }

    /**
     * Caches <tt>realm</tt> and <tt>credentials</tt> for later usage.
     *
     * @param realm the
     * @param credentials UserCredentials
     */
    public void cacheCredentials(String realm, UserCredentials credentials)
    {
        CredentialsCacheEntry ccEntry = new CredentialsCacheEntry();
        ccEntry.userCredentials = credentials;

        this.cachedCredentials.cacheEntry(realm, ccEntry);
    }
}
