/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.security;

import gov.nist.javax.sip.header.*;
import gov.nist.javax.sip.message.*;

import java.text.*;
import java.util.*;

import javax.sip.*;
import javax.sip.header.*;
import javax.sip.message.*;

import net.java.sip.communicator.impl.protocol.sip.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The class handles authentication challenges, caches user credentials and
 * takes care (through the SecurityAuthority interface) about retrieving
 * passwords.
 *
 * @author Emil Ivov
 * @author Jeroen van Bemmel
 * @version 1.0
 */
public class SipSecurityManager
{
    /**
     * Our class logger.
     */
    private static final Logger logger
        = Logger.getLogger(SipSecurityManager.class);

    /**
     * The SecurityAuthority instance that we could use to obtain new passwords
     * for the user.
     */
    private SecurityAuthority securityAuthority = null;

    /**
     * An instance of the header factory that we have to use to create our
     * authentication headers.
     */
    private HeaderFactory headerFactory = null;

    /**
     * Credentials cached so far.
     */
    private CredentialsCache cachedCredentials = new CredentialsCache();

    /**
     * The ID of the account that this security manager instance is serving.
     */
    private final AccountID accountID;

    /**
     * Default constructor for the security manager.
     *
     * @param accountID the id of the account that this security manager is
     * going to serve.
     */
    public SipSecurityManager(AccountID accountID)
    {
        this.accountID = accountID;
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
     * Uses securityAuthority to determine a set of valid user credentials
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
               OperationFailedException,
               NullPointerException
    {
        String branchID = challengedTransaction.getBranchId();
        Request challengedRequest = challengedTransaction.getRequest();

        Request reoriginatedRequest = (Request) challengedRequest.clone();

        //remove the branch id so that we could use the request in a new
        //transaction
        removeBranchID(reoriginatedRequest);

        ListIterator authHeaders = null;

        if (challenge.getStatusCode() == Response.UNAUTHORIZED)
        {
            authHeaders = challenge.getHeaders(WWWAuthenticateHeader.NAME);

            // Remove all authorization headers from the request (we'll re-add
            // them from cache)
            reoriginatedRequest.removeHeader(AuthorizationHeader.NAME);
        }
        else if (challenge.getStatusCode()
                 == Response.PROXY_AUTHENTICATION_REQUIRED)
        {
            authHeaders = challenge.getHeaders(ProxyAuthenticateHeader.NAME);

            // Remove all authorization headers from the request (we'll re-add
            // them from cache)
            reoriginatedRequest.removeHeader(ProxyAuthorizationHeader.NAME);
        }

        //rfc 3261 says that the cseq header should be augmented for the new
        //request. do it here so that the new dialog (created together with
        //the new client transaction) takes it into account.
        //Bug report - Fredrik Wickstrom
        CSeqHeader cSeq =
            (CSeqHeader) reoriginatedRequest.getHeader(CSeqHeader.NAME);
        cSeq.setSeqNumber(cSeq.getSeqNumber() + 1l);

        ClientTransaction retryTran =
            transactionCreator.getNewClientTransaction(reoriginatedRequest);

        WWWAuthenticateHeader authHeader = null;
        while (authHeaders.hasNext())
        {
            authHeader = (WWWAuthenticateHeader) authHeaders.next();
            String realm = authHeader.getRealm();

            //Check whether we have cached credentials for authHeader's realm.
            //We remove them with the intention to re-add them at the end of the
            //method. If we fail to get to the end then it's best for the cache
            //entry to remain outside since it might have caused the problem
            CredentialsCacheEntry ccEntry = cachedCredentials.remove(realm);

            boolean ccEntryHasSeenTran = false;

            if (ccEntry != null)
                ccEntryHasSeenTran = ccEntry.popBranchID(branchID);

            String storedPassword = SipActivator.getProtocolProviderFactory()
                .loadPassword(accountID);

            if(ccEntry == null)
            {
                //we haven't yet authentified this realm since we were started.
                if(storedPassword != null)
                {
                    //use the stored password to authenticate
                    ccEntry = createCcEntryWithStoredPassword(storedPassword);
                    logger.trace("seem to have a stored pass! Try with it.");
                }
                else
                {
                    //obtain new credentials
                    logger.trace("We don't seem to have a good pass! Get one.");

                    ccEntry = createCcEntryWithNewCredentials(
                        realm, SecurityAuthority.AUTHENTICATION_REQUIRED);

                    if(ccEntry == null)
                        throw new OperationFailedException(
                            "User has canceled the authentication process.",
                            OperationFailedException.AUTHENTICATION_CANCELED);
                }
            }
            else
            {
                //we have already authentified against this realm since we were
                //started. this authentication is either for a different request
                //or the previous authentication used a wrong pass.

                if (ccEntryHasSeenTran)
                {
                    //this is the transaction that created the cc entry. if we
                    //need to authenticate the same transaction then the
                    //credentials we supplied the first time were wrong.
                    //remove password and ask user again.
                    SipActivator.getProtocolProviderFactory().storePassword(
                        accountID, null);

                    ccEntry = createCcEntryWithNewCredentials(
                        realm, SecurityAuthority.WRONG_PASSWORD);

                    if(ccEntry == null)
                        throw new OperationFailedException(
                            "User has canceled the authentication process.",
                            OperationFailedException.AUTHENTICATION_CANCELED);
                }
                else
                {
                    //we have a cache entry and it has not seen this transaction
                    //lets use it again.
                    //(this "else" is here for readability only)
                    logger.trace( "We seem to have a pass in the cache. "
                                  +"Let's try with it.");
                }
            }

            //get a new pass
            if (ccEntry == null // we don't have credentials for the specified
                                //realm
                || ( (ccEntryHasSeenTran // we have already tried with those
                      && !authHeader.isStale()))) // and this is (!stale) not
                                                  // just a request to reencode
            {

            }

            //if user canceled or sth else went wrong
            if (ccEntry.userCredentials == null)
            {
                throw new OperationFailedException(
                    "Unable to authenticate with realm " + realm
                    + ". User did not provide credentials."
                    , OperationFailedException.AUTHENTICATION_FAILED);
            }

            AuthorizationHeader authorization =
                this.getAuthorization(
                    reoriginatedRequest.getMethod(),
                    reoriginatedRequest.getRequestURI().toString(),
                    ( reoriginatedRequest.getContent() == null )? "" :
                    reoriginatedRequest.getContent().toString(),
                    authHeader,
                    ccEntry.userCredentials);


            ccEntry.pushBranchID(retryTran.getBranchId());
            cachedCredentials.cacheEntry(realm, ccEntry);

            logger.debug("Created authorization header: " +
                         authorization.toString());

            // get the unique Call-ID
            CallIdHeader call = (CallIdHeader)reoriginatedRequest
                .getHeader(CallIdHeader.NAME);

            if(call != null)
            {
                String callid = call.getCallId();
                cachedCredentials
                    .cacheAuthorizationHeader (callid, authorization);
            }

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
     * Handles a 403 Forbidden response. Contrary to the
     * <tt>handleChallenge</tt> method this one would not attach an \
     * authentication header to the request since there was no challenge in
     * the response.As a result the use of this method would result in sending
     * one more request and receiving one more failure response. Not quite
     * efficient ... but what do you want ... life is tough.
     *
     * @param forbidden the 401/407 challenge response
     * @param endedTransaction the transaction established by the challenged
     * request
     * @param transactionCreator the JAIN SipProvider that we should use to
     * create the new transaction.
     *
     * @return the client transaction that can be used to try and reregister.
     *
     * @throws InvalidArgumentException if we fail to create a new header
     * containing user credentials.
     * @throws TransactionUnavailableException if we get an exception white
     * creating the new transaction
     */
    public ClientTransaction handleForbiddenResponse(
                                    Response          forbidden,
                                    ClientTransaction endedTransaction,
                                    SipProvider       transactionCreator)
        throws InvalidArgumentException,
               TransactionUnavailableException

    {
        //now empty the cache because the request we previously sent was
        //mal-authenticated.
        cachedCredentials.clear();

        //also remove the stored password:
        SipActivator.getProtocolProviderFactory().storePassword(
                        accountID, null);

        //now recreate a transaction so that we could start all over again.

        Request challengedRequest = endedTransaction.getRequest();
        Request reoriginatedRequest = (Request) challengedRequest.clone();

        //remove the branch id so that we could use the request in a new
        //transaction
        removeBranchID(reoriginatedRequest);

        //extract the realms that we tried to authenticate with the previous
        //request and remove the authorization headers.
        List<String> realms = removeAuthHeaders(reoriginatedRequest);

        //increment cseq (as per 3261)
        CSeqHeader cSeq =
            (CSeqHeader) reoriginatedRequest.getHeader( (CSeqHeader.NAME));
        cSeq.setSeqNumber(cSeq.getSeqNumber() + 1l);

        ClientTransaction retryTran =
            transactionCreator.getNewClientTransaction(reoriginatedRequest);

        //create a credentials entry with an empty password so that we can
        //store the transaction and when we get the next challenge notify the
        //user that their password was wrong.
        Iterator<String> realmsIter = realms.iterator();
        while(realmsIter.hasNext())
        {
            CredentialsCacheEntry ccEntry = createCcEntryWithStoredPassword("");
            ccEntry.pushBranchID(retryTran.getBranchId());
            cachedCredentials.cacheEntry(realmsIter.next(), ccEntry);
        }

        logger.debug("Returning authorization transaction.");
        return retryTran;
    }

    /**
     * Removes all authorization (and proxy authorization) headers from
     * <tt>request</tt> and returns the list of realms that they were about.
     *
     * @param request the request that we'd like to clear of all authorization
     * headers.
     * @return the <tt>List</tt> of realms that this request was supposed to
     * authenticate against.
     */
    private List<String> removeAuthHeaders(Request request)
    {
        Iterator<SIPHeader> headers = ((SIPRequest)request).getHeaders();
        List<String> realms = new LinkedList<String>();

        removeAuthHeaders(headers, realms);

        request.removeHeader(AuthorizationHeader.NAME);
        request.removeHeader(ProxyAuthorizationHeader.NAME);

        return realms;
    }

    /**
     * Adds realms from all authorization headers in the <tt>headers</tt> list
     * into the realms list (for use from <tt>removeAuthHeaders(Request)</tt>
     * only). The method also handles header lists and is recursive.
     *
     * @param headers the list of headers that we need to analyze.
     * @param realms the list that we should fill with the realms we encounter
     * in all kinds of authorization headers.
     */
    @SuppressWarnings("unchecked") //no way around it
    private void removeAuthHeaders(Iterator<SIPHeader> headers,
                                   List<String>        realms)
    {
        while(headers.hasNext())
        {
            SIPHeader header = headers.next();

            if(header instanceof AuthorizationHeader)
            {
                realms.add(((AuthorizationHeader)header).getRealm());
            }
            //expand header lists
            else if (header instanceof SIPHeaderList)
            {
                Iterator<SIPHeader> hdrListIter
                    = ((SIPHeaderList<SIPHeader>)header).iterator();
                removeAuthHeaders(hdrListIter, realms);
            }
        }
    }

    /**
     * Generates an authorization header in response to wwwAuthHeader.
     *
     * @param method method of the request being authenticated
     * @param uri digest-uri
     * @param requestBody the body of the request.
     * @param authHeader the challenge that we should respond to
     * @param userCredentials username and pass
     *
     * @return an authorization header in response to authHeader.
     *
     * @throws OperationFailedException if auth header was malformed.
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

        // JvB: authHeader.getQop() is a quoted _list_ of qop values
        // (e.g. "auth,auth-int") Client is supposed to pick one
        String qopList = authHeader.getQop();
        String qop = (qopList != null) ? "auth" : null;
        String nc_value = "00000001";
        String cnonce = "xyz";

        try
        {
            response = MessageDigestAlgorithm.calculateResponse(
                authHeader.getAlgorithm(),
                userCredentials.getUserName(),
                authHeader.getRealm(),
                new String(userCredentials.getPassword()),
                authHeader.getNonce(),
                nc_value, // JvB added
                cnonce,   // JvB added
                method,
                uri,
                requestBody,
                qop);//jvb changed
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
            {
                authorization.setAlgorithm(authHeader.getAlgorithm());
            }

            if (authHeader.getOpaque() != null)
            {
                authorization.setOpaque(authHeader.getOpaque());
            }

            // jvb added
            if (qop!=null)
            {
                authorization.setQop(qop);
                authorization.setCNonce(cnonce);
                authorization.setNonceCount( Integer.parseInt(nc_value) );
            }

            authorization.setResponse(response);

        }
        catch (ParseException ex)
        {
            throw new SecurityException(
                "Failed to create an authorization header!");
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

    /**
     * Removes all via headers from <tt>request</tt> and replaces them with a
     * new one, equal to the one that was top most.
     *
     * @param request the Request whose branchID we'd like to remove.
     */
    private void removeBranchID(Request request)
    {
        ViaHeader viaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);

        request.removeHeader(ViaHeader.NAME);

        ViaHeader newViaHeader;
        try
        {
            newViaHeader = headerFactory.createViaHeader(
                                            viaHeader.getHost()
                                            , viaHeader.getPort()
                                            , viaHeader.getTransport()
                                            , null);
            request.setHeader(newViaHeader);
        }
        catch (Exception exc)
        {
            // we are using the host port and transport of an existing Via
            // header so it would be quite weird to get this exception.
            logger.debug("failed to reset a Via header");
        }
    }

    /**
     * Obtains user credentials from the security authority for the specified
     * <tt>realm</tt> and creates a new CredentialsCacheEntry with them.
     *
     * @param realm the realm that we'd like to obtain a
     * <tt>CredentialsCacheEntry</tt> for.
     * @param reasonCode one of the fields defined in this class that indicate
     * the reason for asking the user to enter a password.
     *
     * @return a newly created <tt>CredentialsCacheEntry</tt> corresponding to
     * the specified <tt>realm</tt>.
     */
    private CredentialsCacheEntry createCcEntryWithNewCredentials(
                    String realm, int reasonCode)
    {
        CredentialsCacheEntry ccEntry = new CredentialsCacheEntry();

        UserCredentials defaultCredentials = new UserCredentials();

        String authName = accountID.getAccountPropertyString(
                                    ProtocolProviderFactory.AUTHORIZATION_NAME);
        if(authName != null && authName.length() > 0)
            defaultCredentials.setUserName(authName);
        else
            defaultCredentials.setUserName(accountID.getUserID());

        UserCredentials newCredentials = getSecurityAuthority()
            .obtainCredentials( realm, defaultCredentials, reasonCode);

        // in case user has canceled the login window
        if(newCredentials == null)
            return null;

        if(newCredentials.getPassword() == null)
            return null;

        ccEntry.userCredentials = newCredentials;

        //store the password if the user wants us to
        if( ccEntry.userCredentials != null
            && ccEntry.userCredentials.isPasswordPersistent())
                SipActivator.getProtocolProviderFactory().storePassword(
                    accountID
                    , ccEntry.userCredentials.getPasswordAsString());

        return ccEntry;
    }

    /**
     * Creates a new credentials cache entry using <tt>password</tt>.
     *
     * @param password the password that we'd like to use in our the credentials
     * associated with the new <tt>CredentialsCacheEntry</tt>.
     *
     * @return a newly created <tt>CredentialsCacheEntry</tt> using
     * <tt>password</tt>.
     */
    private CredentialsCacheEntry createCcEntryWithStoredPassword(
                                                                String password)
    {
        CredentialsCacheEntry ccEntry = new CredentialsCacheEntry();

        ccEntry.userCredentials = new UserCredentials();

        String authName = accountID.getAccountPropertyString(
                                    ProtocolProviderFactory.AUTHORIZATION_NAME);
        if(authName != null && authName.length() > 0)
            ccEntry.userCredentials.setUserName(authName);
        else
            ccEntry.userCredentials.setUserName(accountID.getUserID());

        ccEntry.userCredentials.setPassword(password.toCharArray());

        return ccEntry;
    }

    /**
     * Returns an authorization header cached against the specified
     * <tt>callID</tt> or <tt>null</tt> if no auth. header has been previously
     * cached for this callID.
     *
     * @param callID the ID of the call that we'd like to reString
     * @return the <tt>AuthorizationHeader</tt> cached against the specified
     * call ID or null if no such header has been cached.
     */
    public AuthorizationHeader getCachedAuthorizationHeader(String callID)
    {
        return this.cachedCredentials.getCachedAuthorizationHeader(callID);
    }
}
