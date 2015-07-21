/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
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
package net.java.sip.communicator.slick.protocol.sip;

import static net.java.sip.communicator.service.protocol.ProtocolProviderFactory.USER_ID;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.net.*;
import java.text.*;

import junit.framework.*;
import net.java.sip.communicator.impl.protocol.sip.*;
import net.java.sip.communicator.impl.protocol.sip.net.*;
import net.java.sip.communicator.service.dns.*;
import net.java.sip.communicator.util.*;

/**
 * Tests all variations of automatic proxy detection through (simulated) DNS.
 *
 * @author Ingo Bauersachs
 */
public class TestAutoProxyDetection
    extends TestCase
{
    private static class TestedAutoProxyDetection extends AutoProxyConnection
    {
        public TestedAutoProxyDetection(SipAccountIDImpl account,
            String defaultTransport)
        {
            super(account, defaultTransport);
        }

        @Override
        public void setNetworkUtils(LocalNetworkUtils nu)
        {
            super.setNetworkUtils(nu);
        }

        public static class NetworkUtils extends LocalNetworkUtils
        {
        }
    }

    private SipAccountIDImpl account;
    private TestedAutoProxyDetection.NetworkUtils nu;
    private SRVRecord srv1;
    private SRVRecord srv2;
    private SRVRecord srv3;
    private InetSocketAddress a1;
    private InetSocketAddress a2;
    private InetSocketAddress a3;
    private InetSocketAddress a4;
    private final static String DOMAIN = "example.com";
    private InetAddress ia1;
    private InetAddress ia2;
    private InetAddress ia3;
    private InetAddress ia4;
    private TestedAutoProxyDetection apd;

    @Override
    public void setUp()
    {
        account = createMock(SipAccountIDImpl.class);
        expect(account.getAccountPropertyString(USER_ID))
            .andReturn("unit@" + DOMAIN);
        replay(account);

        nu = createMock(TestedAutoProxyDetection.NetworkUtils.class);
        apd = new TestedAutoProxyDetection(account, "UDP");
        apd.setNetworkUtils(nu);

        srv1 = createMock(SRVRecord.class);
        expect(srv1.getTarget()).andReturn("proxy1."+DOMAIN);
        expect(srv1.getPort()).andReturn(5060);
        srv2 = createMock(SRVRecord.class);
        expect(srv2.getTarget()).andReturn("proxy2."+DOMAIN);
        expect(srv2.getPort()).andReturn(5061);
        srv3 = createMock(SRVRecord.class);
        expect(srv3.getTarget()).andReturn("proxy3."+DOMAIN);
        expect(srv3.getPort()).andReturn(5062);
        try
        {
            ia1 = InetAddress.getByAddress("proxy1." + DOMAIN,
                new byte[]{0x7f,0,0,1});
            ia2 = InetAddress.getByAddress("proxy2." + DOMAIN,
                new byte[]{0x7f,0,0,2});
            ia3 = InetAddress.getByAddress("proxy3." + DOMAIN,
                new byte[]{0x7f,0,0,3});
            ia4 = InetAddress.getByAddress("proxy4." + DOMAIN,
                new byte[]{0x7f,0,0,4});
        }
        catch (UnknownHostException e)
        {
            fail("unable to initialize: " + e.getMessage());
        }
        a1 = new InetSocketAddress(ia1, 5060);
        a2 = new InetSocketAddress(ia2, 5061);
        a3 = new InetSocketAddress(ia3, 5062);
        a4 = new InetSocketAddress(ia4, 5063);
    }

    private void prepareOneNaptrOneSrv() throws ParseException, DnssecException
    {
        expect(nu.getNAPTRRecords(DOMAIN)).andReturn(new String[][]{
            {"0", "udp", "_sip._udp." + DOMAIN}
        });
        expect(nu.getSRVRecords("_sip._udp."+DOMAIN))
            .andReturn(new SRVRecord[]{ srv1 });
    }

    private void prepareOneNaptrTwoSrv() throws ParseException, DnssecException
    {
        expect(nu.getNAPTRRecords(DOMAIN)).andReturn(new String[][]{
            {"0", "udp", "_sip._udp." + DOMAIN}
        });
        expect(nu.getSRVRecords("_sip._udp."+DOMAIN))
            .andReturn(new SRVRecord[]{ srv1, srv2 });
    }

    public void testOneNaptrNoSrv() throws ParseException, DnssecException
    {
        expect(nu.getNAPTRRecords(DOMAIN)).andReturn(new String[][]{
            {"0", "udp", "_sip._udp." + DOMAIN}
        });
        expect(nu.getSRVRecords("_sip._udp." + DOMAIN)).andReturn(null);
        replay(nu);

        assertFalse(apd.getNextAddress());
        verify(account, nu);
    }

    public void testOneNaptrOneSrvOneA() throws ParseException, DnssecException
    {
        prepareOneNaptrOneSrv();
        expect(nu.getAandAAAARecords("proxy1." + DOMAIN, 5060))
            .andReturn(new InetSocketAddress[]{a1});
        replay(nu, srv1);

        assertTrue(apd.getNextAddress());
        assertEquals(a1, apd.getAddress());
        assertEquals("UDP", apd.getTransport());

        assertFalse(apd.getNextAddress());
        verify(account, nu, srv1);
    }

    public void testOneNaptrOneSrvTwoA() throws ParseException, DnssecException
    {
        prepareOneNaptrOneSrv();
        expect(nu.getAandAAAARecords("proxy1." + DOMAIN, 5060))
            .andReturn(new InetSocketAddress[]{a1, a2});
        replay(nu, srv1);

        assertTrue(apd.getNextAddress());
        assertEquals(a1, apd.getAddress());
        assertEquals("UDP", apd.getTransport());
        assertTrue(apd.getNextAddress());
        assertEquals(a2, apd.getAddress());
        assertEquals("UDP", apd.getTransport());

        assertFalse(apd.getNextAddress());
        verify(account, nu, srv1);
    }

    //-----------------------

    public void testOneNaptrTwoSrvOneA() throws ParseException, DnssecException
    {
        prepareOneNaptrTwoSrv();
        expect(nu.getAandAAAARecords("proxy1." + DOMAIN, 5060))
            .andReturn(new InetSocketAddress[]{a1});
        expect(nu.getAandAAAARecords("proxy2." + DOMAIN, 5061))
            .andReturn(new InetSocketAddress[]{a2});
        replay(nu, srv1, srv2);

        assertTrue(apd.getNextAddress());
        assertEquals(a1, apd.getAddress());
        assertEquals("UDP", apd.getTransport());
        assertTrue(apd.getNextAddress());
        assertEquals(a2, apd.getAddress());
        assertEquals("UDP", apd.getTransport());

        assertFalse(apd.getNextAddress());
        verify(account, nu, srv1, srv2);
    }

    public void testOneNaptrTwoSrvTwoA() throws ParseException, DnssecException
    {
        prepareOneNaptrTwoSrv();
        expect(nu.getAandAAAARecords("proxy1." + DOMAIN, 5060))
            .andReturn(new InetSocketAddress[]{a1, a2});
        expect(nu.getAandAAAARecords("proxy2." + DOMAIN, 5061))
            .andReturn(new InetSocketAddress[]{a3, a4});
        replay(nu, srv1, srv2);

        assertTrue(apd.getNextAddress());
        assertEquals(a1, apd.getAddress());
        assertEquals("UDP", apd.getTransport());

        assertTrue(apd.getNextAddress());
        assertEquals(a2, apd.getAddress());
        assertEquals("UDP", apd.getTransport());

        assertTrue(apd.getNextAddress());
        assertEquals(a3, apd.getAddress());
        assertEquals("UDP", apd.getTransport());

        assertTrue(apd.getNextAddress());
        assertEquals(a4, apd.getAddress());
        assertEquals("UDP", apd.getTransport());

        assertFalse(apd.getNextAddress());
        verify(account, nu, srv1, srv2);
    }

    //-------------------

    public void testThreeNaptrOneSrvEachOneAEach()
        throws ParseException,
        DnssecException
    {
        expect(nu.getNAPTRRecords(DOMAIN)).andReturn(new String[][]{
            {"0", "udp", "_sip._udp." + DOMAIN},
            {"0", "tcp", "_sip._tcp." + DOMAIN},
            {"0", "tls", "_sips._tcp." + DOMAIN}
        });
        expect(nu.getSRVRecords("_sip._udp."+DOMAIN))
            .andReturn(new SRVRecord[]{ srv1 });
        expect(nu.getSRVRecords("_sip._tcp."+DOMAIN))
            .andReturn(new SRVRecord[]{ srv2 });
        expect(nu.getSRVRecords("_sips._tcp."+DOMAIN))
            .andReturn(new SRVRecord[]{ srv3 });
        expect(nu.getAandAAAARecords("proxy1." + DOMAIN, 5060))
            .andReturn(new InetSocketAddress[]{a1});
        expect(nu.getAandAAAARecords("proxy2." + DOMAIN, 5061))
            .andReturn(new InetSocketAddress[]{a1});
        expect(nu.getAandAAAARecords("proxy3." + DOMAIN, 5062))
            .andReturn(new InetSocketAddress[]{a1});

        replay(nu, srv1, srv2, srv3);

        assertTrue(apd.getNextAddress());
        assertEquals(a1, apd.getAddress());
        assertEquals("UDP", apd.getTransport());

        assertTrue(apd.getNextAddress());
        assertEquals(a1, apd.getAddress());
        assertEquals("TCP", apd.getTransport());

        assertTrue(apd.getNextAddress());
        assertEquals(a1, apd.getAddress());
        assertEquals("TLS", apd.getTransport());

        assertFalse(apd.getNextAddress());
        verify(account, nu, srv1, srv2, srv3);
    }

    //-----------------------

    public void testNoSrvOneA() throws ParseException, DnssecException
    {
        expect(nu.getNAPTRRecords(DOMAIN)).andReturn(new String[][]{});
        expect(nu.getSRVRecords("sips", "TCP", DOMAIN)).andReturn(null);
        expect(nu.getSRVRecords("sip", "TCP", DOMAIN)).andReturn(null);
        expect(nu.getSRVRecords("sip", "UDP", DOMAIN)).andReturn(null);
        expect(nu.getAandAAAARecords(DOMAIN, 5060))
            .andReturn(new InetSocketAddress[]{a1});

        replay(nu);

        assertTrue(apd.getNextAddress());
        assertEquals(a1, apd.getAddress());
        assertEquals("UDP", apd.getTransport());

        assertFalse(apd.getNextAddress());
        verify(account, nu);
    }

    public void testOneSrvNoA() throws ParseException, DnssecException
    {
        expect(nu.getNAPTRRecords(DOMAIN)).andReturn(new String[][]{});
        expect(nu.getSRVRecords("sips", "TCP", DOMAIN)).andReturn(null);
        expect(nu.getSRVRecords("sip", "TCP", DOMAIN)).andReturn(null);
        expect(nu.getSRVRecords("sip", "UDP", DOMAIN))
            .andReturn(new SRVRecord[]{srv1});
        expect(nu.getAandAAAARecords("proxy1." + DOMAIN, 5060))
            .andReturn(null);

        replay(nu, srv1);

        assertFalse(apd.getNextAddress());
        verify(account, nu, srv1);
    }

    public void testOneSrvOneA() throws ParseException, DnssecException
    {
        expect(nu.getNAPTRRecords(DOMAIN)).andReturn(new String[][]{});
        expect(nu.getSRVRecords("sips", "TCP", DOMAIN)).andReturn(null);
        expect(nu.getSRVRecords("sip", "TCP", DOMAIN)).andReturn(null);
        expect(nu.getSRVRecords("sip", "UDP", DOMAIN))
            .andReturn(new SRVRecord[]{srv1});
        expect(nu.getAandAAAARecords("proxy1." + DOMAIN, 5060))
            .andReturn(new InetSocketAddress[]{a1});

        replay(nu, srv1);

        assertTrue(apd.getNextAddress());
        assertEquals(a1, apd.getAddress());
        assertEquals("UDP", apd.getTransport());

        assertFalse(apd.getNextAddress());
        verify(account, nu, srv1);
    }

    public void testOneSrvTwoA() throws ParseException, DnssecException
    {
        expect(nu.getNAPTRRecords(DOMAIN)).andReturn(new String[][]{});
        expect(nu.getSRVRecords("sips", "TCP", DOMAIN)).andReturn(null);
        expect(nu.getSRVRecords("sip", "TCP", DOMAIN)).andReturn(null);
        expect(nu.getSRVRecords("sip", "UDP", DOMAIN))
            .andReturn(new SRVRecord[]{srv1});
        expect(nu.getAandAAAARecords("proxy1." + DOMAIN, 5060))
            .andReturn(new InetSocketAddress[]{a1, a2});

        replay(nu, srv1);

        assertTrue(apd.getNextAddress());
        assertEquals(a1, apd.getAddress());
        assertEquals("UDP", apd.getTransport());

        assertTrue(apd.getNextAddress());
        assertEquals(a2, apd.getAddress());
        assertEquals("UDP", apd.getTransport());

        assertFalse(apd.getNextAddress());
        verify(account, nu, srv1);
    }

    public void testTwoSrvOneA() throws ParseException, DnssecException
    {
        expect(nu.getNAPTRRecords(DOMAIN)).andReturn(new String[][]{});
        expect(nu.getSRVRecords("sips", "TCP", DOMAIN))
            .andReturn(new SRVRecord[]{srv2});
        expect(nu.getSRVRecords("sip", "TCP", DOMAIN)).andReturn(null);
        expect(nu.getSRVRecords("sip", "UDP", DOMAIN))
            .andReturn(new SRVRecord[]{srv1});
        expect(nu.getAandAAAARecords("proxy1." + DOMAIN, 5060))
            .andReturn(new InetSocketAddress[]{a1});
        expect(nu.getAandAAAARecords("proxy2." + DOMAIN, 5061))
            .andReturn(new InetSocketAddress[]{a2});

        replay(nu, srv1, srv2);

        assertTrue(apd.getNextAddress());
        assertEquals(a2, apd.getAddress());
        assertEquals("TLS", apd.getTransport());

        assertTrue(apd.getNextAddress());
        assertEquals(a1, apd.getAddress());
        assertEquals("UDP", apd.getTransport());

        assertFalse(apd.getNextAddress());
        verify(account, nu, srv1, srv2);
    }

    public void testTwoSameSrvOneA() throws ParseException, DnssecException
    {
        expect(nu.getNAPTRRecords(DOMAIN)).andReturn(new String[][]{});
        expect(nu.getSRVRecords("sips", "TCP", DOMAIN))
            .andReturn(new SRVRecord[]{srv1, srv2});
        expect(nu.getSRVRecords("sip", "TCP", DOMAIN)).andReturn(null);
        expect(nu.getSRVRecords("sip", "UDP", DOMAIN)).andReturn(null);
        expect(nu.getAandAAAARecords("proxy1." + DOMAIN, 5060))
            .andReturn(new InetSocketAddress[]{a1});
        expect(nu.getAandAAAARecords("proxy2." + DOMAIN, 5061))
            .andReturn(new InetSocketAddress[]{a2});

        replay(nu, srv1, srv2);

        assertTrue(apd.getNextAddress());
        assertEquals(a1, apd.getAddress());
        assertEquals("TLS", apd.getTransport());
        assertEquals(5060, apd.getAddress().getPort());

        assertTrue(apd.getNextAddress());
        assertEquals(a2, apd.getAddress());
        assertEquals("TLS", apd.getTransport());
        assertEquals(5061, apd.getAddress().getPort());

        assertFalse(apd.getNextAddress());
        verify(account, nu, srv1, srv2);
    }

    //----------------------

    public void testNoA() throws ParseException, DnssecException
    {
        expect(nu.getNAPTRRecords(DOMAIN)).andReturn(new String[][]{});
        expect(nu.getSRVRecords("sips", "TCP", DOMAIN)).andReturn(null);
        expect(nu.getSRVRecords("sip", "TCP", DOMAIN)).andReturn(null);
        expect(nu.getSRVRecords("sip", "UDP", DOMAIN)).andReturn(null);
        expect(nu.getAandAAAARecords(DOMAIN, 5060))
            .andReturn(new InetSocketAddress[]{});

        replay(nu);

        assertFalse(apd.getNextAddress());
        verify(account, nu);
    }

    public void testOneA() throws ParseException, DnssecException
    {
        expect(nu.getNAPTRRecords(DOMAIN)).andReturn(new String[][]{});
        expect(nu.getSRVRecords("sips", "TCP", DOMAIN)).andReturn(null);
        expect(nu.getSRVRecords("sip", "TCP", DOMAIN)).andReturn(null);
        expect(nu.getSRVRecords("sip", "UDP", DOMAIN)).andReturn(null);
        expect(nu.getAandAAAARecords(DOMAIN, 5060))
            .andReturn(new InetSocketAddress[]{a1});

        replay(nu);

        assertTrue(apd.getNextAddress());
        assertEquals(a1, apd.getAddress());
        assertEquals("UDP", apd.getTransport());

        assertFalse(apd.getNextAddress());
        verify(account, nu);
    }

    public void testTwoA() throws ParseException, DnssecException
    {
        expect(nu.getNAPTRRecords(DOMAIN)).andReturn(new String[][]{});
        expect(nu.getSRVRecords("sips", "TCP", DOMAIN)).andReturn(null);
        expect(nu.getSRVRecords("sip", "TCP", DOMAIN)).andReturn(null);
        expect(nu.getSRVRecords("sip", "UDP", DOMAIN)).andReturn(null);
        expect(nu.getAandAAAARecords(DOMAIN, 5060))
            .andReturn(new InetSocketAddress[]{a1, a2});

        replay(nu);

        assertTrue(apd.getNextAddress());
        assertEquals(a1, apd.getAddress());
        assertEquals("UDP", apd.getTransport());

        assertTrue(apd.getNextAddress());
        assertEquals(a2, apd.getAddress());
        assertEquals("UDP", apd.getTransport());

        assertFalse(apd.getNextAddress());
        verify(account, nu);
    }

    public void testNotReturningSameAddressTwice()
        throws ParseException,
        DnssecException
    {
        expect(srv1.getTarget()).andReturn("proxy1."+DOMAIN);
        expect(srv1.getPort()).andReturn(5060);
        expect(nu.getNAPTRRecords(DOMAIN)).andReturn(new String[][]{
            {"0", "udp", "_sip._udp." + DOMAIN},
            {"1", "udp", "_sip._udp." + DOMAIN}
        });
        expect(nu.getSRVRecords("_sip._udp."+DOMAIN)).andReturn(new SRVRecord[]{
            srv1
        });
        expect(nu.getSRVRecords("_sip._udp."+DOMAIN)).andReturn(new SRVRecord[]{
            srv1
        });
        expect(nu.getAandAAAARecords("proxy1." + DOMAIN, 5060))
            .andReturn(new InetSocketAddress[]{a1});
        expect(nu.getAandAAAARecords("proxy1." + DOMAIN, 5060))
            .andReturn(new InetSocketAddress[]{a1});

        replay(nu, srv1);

        assertTrue(apd.getNextAddress());
        assertEquals(a1, apd.getAddress());
        assertEquals("UDP", apd.getTransport());

        assertFalse(apd.getNextAddress());
        verify(account, nu, srv1);
    }
}
