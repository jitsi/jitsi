/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import java.security.*;
import java.security.cert.*;
import java.security.interfaces.*;
import java.util.*;

import javax.naming.*;
import javax.naming.ldap.*;
import javax.security.auth.x500.*;
import javax.swing.*;
import javax.swing.tree.*;

import org.jitsi.service.resources.*;

/**
 * Panel that shows the content of an X509Certificate.
 */
public class X509CertificatePanel
    extends TransparentPanel
{
    private static final long serialVersionUID = -8368302061995971947L;

    /**
     * Constructs a X509 certificate panel from a single certificate.
     * If a complete chain is available instead use the second constructor.
     * This constructor is kept for backwards compatibility.
     *
     * @param certificate <tt>X509Certificate</tt> object
     */
    public X509CertificatePanel(X509Certificate certificate)
    {
        this(Arrays.asList(certificate));
    }

    /**
     * Constructs a X509 certificate panel.
     *
     * @param certificates List of <tt>X509Certificate</tt> objects ordered with
     * the end entity certificate first
     */
    public X509CertificatePanel(final java.util.List<X509Certificate> certificates)
    {
        setLayout(new BorderLayout(5, 5));
        
        // Certificate chain list
        TransparentPanel topPanel = new TransparentPanel(new BorderLayout());
        topPanel.add(new JLabel("Certificate chain:"), BorderLayout.NORTH);
        
        DefaultMutableTreeNode top = new DefaultMutableTreeNode();
        DefaultMutableTreeNode previous = top;
        ListIterator<X509Certificate> it = certificates.listIterator(
                certificates.size());
        while (it.hasPrevious()) {
            X509Certificate cert = it.previous();
            DefaultMutableTreeNode next = new DefaultMutableTreeNode(cert);
            previous.add(next);
            previous = next;
        }
        JTree tree = new JTree(top);
        tree.setRootVisible(false);
        tree.setExpandsSelectedPaths(true);
        tree.getSelectionModel().setSelectionMode(
                TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setCellRenderer(new DefaultTreeCellRenderer() {

            @Override
            public Component getTreeCellRendererComponent(JTree tree, 
                    Object value, boolean sel, boolean expanded, boolean leaf, 
                    int row, boolean hasFocus) {
                JLabel component = (JLabel) super.getTreeCellRendererComponent(
                        tree, value, sel, expanded, leaf, row, hasFocus);
                if (value instanceof DefaultMutableTreeNode) {
                    Object o = ((DefaultMutableTreeNode) value).getUserObject();
                    if (o instanceof X509Certificate) {
                        component.setText(getName((X509Certificate) o));
                    }
                }
                return component;
            }
            
            private String getName(X509Certificate cert) {
                return cert.getSubjectX500Principal().getName();
            }
            
        });
        tree.setSelectionPath(new TreePath(((
                (DefaultTreeModel)tree.getModel()).getPathToRoot(previous))));
        topPanel.add(tree, BorderLayout.CENTER);        
        
        add(topPanel, BorderLayout.NORTH);
        
        // Certificate details pane
        JEditorPane infoTextPane = new JEditorPane();

        /*
         * Make JEditorPane respect our default font because we will be using it
         * to just display text.
         */
        infoTextPane.putClientProperty(
                JEditorPane.HONOR_DISPLAY_PROPERTIES,
                true);

        infoTextPane.setOpaque(false);
        infoTextPane.setEditable(false);
        infoTextPane.setContentType("text/html");

        add(infoTextPane, BorderLayout.CENTER);
        String text = toString(certificates.get(0));
        System.out.println("Text:\n" + text);
        infoTextPane.setText(text);
    }
    
    private String toString(X509Certificate certificate)
    {
        final StringBuilder sb = new StringBuilder();
        ResourceManagementService R = DesktopUtilActivator.getResources();
        X500Principal issuer = certificate.getIssuerX500Principal();
        X500Principal subject = certificate.getSubjectX500Principal();
        
        sb.append("<html><body><table>\n");

        // subject
        addTitle(sb, R.getI18NString("service.gui.CERT_INFO_ISSUED_TO"));
        try
        {
            for(Rdn name : new LdapName(subject.getName()).getRdns())
            {
                String nameType = name.getType();
                String lblKey = "service.gui.CERT_INFO_" + nameType;
                String lbl = R.getI18NString(lblKey);

                if ((lbl == null) || ("!" + lblKey + "!").equals(lbl))
                    lbl = nameType;

                sb.append("<tr>");
                sb.append("<td style='margin-left: 5pt; margin-right: 25pt;'>").append(lbl).append("</td>");
                
                Object nameValue = name.getValue();

                if (nameValue instanceof byte[])
                {
                    byte[] nameValueAsByteArray = (byte[]) nameValue;

                    lbl
                        = getHex(nameValueAsByteArray) + " ("
                            + new String(nameValueAsByteArray) + ")";
                }
                else
                    lbl = nameValue.toString();

                sb.append("<td>").append(lbl).append("</td>");
                sb.append("</tr>\n");
            }
        }
        catch (InvalidNameException ine)
        {
            sb.append("<tr>");
            sb.append("<td style='margin-left: 5pt; margin-right: 25pt;'>").append(R.getI18NString("service.gui.CERT_INFO_CN")).append("</td>");
            sb.append("<td>").append(subject.getName()).append("</td>");
            sb.append("</tr>\n");
        }

        // issuer
        addTitle(sb, R.getI18NString("service.gui.CERT_INFO_ISSUED_BY"));
        try
        {
            for(Rdn name : new LdapName(issuer.getName()).getRdns())
            {
                String nameType = name.getType();
                String lblKey = "service.gui.CERT_INFO_" + nameType;
                String lbl = R.getI18NString(lblKey);

                if ((lbl == null) || ("!" + lblKey + "!").equals(lbl))
                    lbl = nameType;

                sb.append("<tr>");
                sb.append("<td style='margin-left: 5pt; margin-right: 25pt;'>").append(lbl).append("</td>");

                Object nameValue = name.getValue();

                if (nameValue instanceof byte[])
                {
                    byte[] nameValueAsByteArray = (byte[]) nameValue;

                    lbl
                        = getHex(nameValueAsByteArray) + " ("
                            + new String(nameValueAsByteArray) + ")";
                }
                else
                    lbl = nameValue.toString();

                sb.append("<td>").append(lbl).append("</td>");
                sb.append("</tr>");
            }
        }
        catch (InvalidNameException ine)
        {
            addField(sb, R.getI18NString("service.gui.CERT_INFO_CN"), 
                    issuer.getName());
        }

        // validity
        addTitle(sb, R.getI18NString("service.gui.CERT_INFO_VALIDITY"));

        addField(sb, R.getI18NString("service.gui.CERT_INFO_ISSUED_ON"), 
                certificate.getNotBefore().toString());

        addField(sb, R.getI18NString("service.gui.CERT_INFO_EXPIRES_ON"), 
                certificate.getNotAfter().toString());
        
        
        addTitle(sb, R.getI18NString("service.gui.CERT_INFO_FINGERPRINTS"));
        try
        {
            String sha1String = getThumbprint(certificate, "SHA1");
            String md5String = getThumbprint(certificate, "MD5");

            addField(sb, "SHA1:", sha1String);
            addField(sb, "MD5:", md5String);
        }
        catch (Exception e)
        {
            // do nothing as we cannot show this value
        }


        addTitle(sb, R.getI18NString("service.gui.CERT_INFO_CERT_DETAILS"));

        addField(sb, R.getI18NString("service.gui.CERT_INFO_SER_NUM"), 
                String.valueOf(certificate.getVersion()));

        addField(sb, R.getI18NString("service.gui.CERT_INFO_VER"), 
                certificate.getSerialNumber().toString());

        addField(sb, R.getI18NString("service.gui.CERT_INFO_SIGN_ALG"),
                String.valueOf(certificate.getSigAlgName()));


        addTitle(sb, R.getI18NString("service.gui.CERT_INFO_PUB_KEY_INFO"));
            
        addField(sb, R.getI18NString("service.gui.CERT_INFO_ALG"),
            certificate.getPublicKey().getAlgorithm());

        if(certificate.getPublicKey().getAlgorithm().equals("RSA"))
        {
            RSAPublicKey key = (RSAPublicKey)certificate.getPublicKey();
            
            addField(sb, R.getI18NString("service.gui.CERT_INFO_PUB_KEY"),
                    R.getI18NString(
                    "service.gui.CERT_INFO_KEY_BYTES_PRINT",
                    new String[]{
                        String.valueOf(key.getModulus().toByteArray().length-1),
                        key.getModulus().toString(16)
                    }));
            
            addField(sb, R.getI18NString("service.gui.CERT_INFO_EXP"), 
                    key.getPublicExponent().toString());

            addField(sb, R.getI18NString("service.gui.CERT_INFO_KEY_SIZE"),
                    R.getI18NString(
                    "service.gui.CERT_INFO_KEY_BITS_PRINT",
                    new String[]{
                        String.valueOf(key.getModulus().bitLength())}));
        }
        else if(certificate.getPublicKey().getAlgorithm().equals("DSA"))
        {
            DSAPublicKey key =
                (DSAPublicKey)certificate.getPublicKey();

            addField(sb, "Y:", key.getY().toString(16));
        }

        addField(sb, R.getI18NString("service.gui.CERT_INFO_SIGN"),
                R.getI18NString(
                    "service.gui.CERT_INFO_KEY_BYTES_PRINT",
                    new String[]{
                        String.valueOf(certificate.getSignature().length),
                        getHex(certificate.getSignature())
                    }));

        sb.append("</table>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private void addTitle(StringBuilder sb, String title) {
        sb.append("<tr><td rowspan='1' style='margin-top: 5pt'><p><b>").append(title).append("</b></p></td></tr>\n");
    }
    
    private void addField(StringBuilder sb, String field, String value) {
        sb.append("<tr>");
        sb.append("<td style='margin-left: 5pt; margin-right: 25pt;'>").append(field).append("</td>");
        sb.append("<td>").append(value).append("</td>");
        sb.append("</tr>\n");
    }
    
    /**
     * Converts the byte array to hex string.
     * @param raw the data.
     * @return the hex string.
     */
    private String getHex( byte [] raw )
    {
        if (raw == null)
            return null;

        StringBuilder hex = new StringBuilder(2 * raw.length);
        Formatter f = new Formatter(hex);
        try
        {
            for (byte b : raw)
                f.format("%02x", b);
        }
        finally
        {
            f.close();
        }
        return hex.toString();
    }

    /**
     * Calculates the hash of the certificate known as the "thumbprint"
     * and returns it as a string representation.
     *
     * @param cert The certificate to hash.
     * @param algorithm The hash algorithm to use.
     * @return The SHA-1 hash of the certificate.
     * @throws CertificateException
     */
    private static String getThumbprint(X509Certificate cert, String algorithm)
        throws CertificateException
    {
        MessageDigest digest;
        try
        {
            digest = MessageDigest.getInstance(algorithm);
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new CertificateException(e);
        }
        byte[] encodedCert = cert.getEncoded();
        StringBuilder sb = new StringBuilder(encodedCert.length * 2);
        Formatter f = new Formatter(sb);
        try
        {
            for (byte b : digest.digest(encodedCert))
                f.format("%02x", b);
        }
        finally
        {
            f.close();
        }
        return sb.toString();
    }
}
