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
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.security.interfaces.*;
import java.util.*;

import javax.naming.*;
import javax.naming.ldap.*;
import javax.security.auth.x500.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.tree.*;

import org.jitsi.service.resources.*;

/**
 * Panel that shows the content of an X509Certificate.
 */
public class X509CertificatePanel
    extends TransparentPanel
{
    private static final long serialVersionUID = -8368302061995971947L;

    private final JEditorPane infoTextPane = new JEditorPane();

    private final ResourceManagementService R
            = DesktopUtilActivator.getResources();

    /**
     * Constructs a X509 certificate panel from a single certificate.
     * If a chain is available instead use the second constructor.
     * This constructor is kept for backwards compatibility and for convenience
     * when there is only one certificate of interest.
     *
     * @param certificate <tt>X509Certificate</tt> object
     */
    public X509CertificatePanel(Certificate certificate)
    {
        this(new Certificate[]
        {
            certificate
        });
    }

    /**
     * Constructs a X509 certificate panel.
     *
     * @param certificates <tt>X509Certificate</tt> objects
     */
    public X509CertificatePanel(Certificate[] certificates)
    {
        setLayout(new BorderLayout(5, 5));

        // Certificate chain list
        TransparentPanel topPanel = new TransparentPanel(new BorderLayout());
        topPanel.add(new JLabel("<html><body><b>"
                + R.getI18NString("service.gui.CERT_INFO_CHAIN")
                + "</b></body></html>"), BorderLayout.NORTH);

        DefaultMutableTreeNode top = new DefaultMutableTreeNode();
        DefaultMutableTreeNode previous = top;
        for (int i = certificates.length - 1; i >= 0; i--)
        {
            Certificate cert = certificates[i];
            DefaultMutableTreeNode next = new DefaultMutableTreeNode(cert);
            previous.add(next);
            previous = next;
        }
        JTree tree = new JTree(top);
        tree.setBorder(new BevelBorder(BevelBorder.LOWERED));
        tree.setRootVisible(false);
        tree.setExpandsSelectedPaths(true);
        tree.getSelectionModel().setSelectionMode(
                TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setCellRenderer(new DefaultTreeCellRenderer()
        {

            @Override
            public Component getTreeCellRendererComponent(JTree tree,
                    Object value, boolean sel, boolean expanded, boolean leaf,
                    int row, boolean hasFocus)
            {
                JLabel component = (JLabel) super.getTreeCellRendererComponent(
                        tree, value, sel, expanded, leaf, row, hasFocus);
                if (value instanceof DefaultMutableTreeNode)
                {
                    Object o = ((DefaultMutableTreeNode) value).getUserObject();
                    if (o instanceof X509Certificate)
                    {
                        component.setText(
                                getSimplifiedName((X509Certificate) o));
                    }
                    else
                    {
                        // We don't know how to represent this certificate type,
                        // let's use the first 20 characters
                        String text = o.toString();
                        if (text.length() > 20)
                        {
                            text = text.substring(0, 20);
                        }
                        component.setText(text);
                    }
                }
                return component;
            }

        });
        tree.getSelectionModel().addTreeSelectionListener(
                new TreeSelectionListener()
        {

            @Override
            public void valueChanged(TreeSelectionEvent e)
            {
                valueChangedPerformed(e);
            }
        });
        tree.setSelectionPath(new TreePath(((
                (DefaultTreeModel)tree.getModel()).getPathToRoot(previous))));
        topPanel.add(tree, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);

        // Certificate details pane
        Caret caret = infoTextPane.getCaret();
        if (caret instanceof DefaultCaret)
        {
            ((DefaultCaret) caret).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        }

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
        infoTextPane.setText(toString(certificates[0]));

        final JScrollPane certScroll = new JScrollPane(infoTextPane);
        certScroll.setPreferredSize(new Dimension(300, 500));
        add(certScroll, BorderLayout.CENTER);
    }

    /**
     * Creates a String representation of the given object.
     * @param certificate to print
     * @return the String representation
     */
    private String toString(Object certificate)
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("<html><body>\n");

        if (certificate instanceof X509Certificate)
        {
            renderX509(sb, (X509Certificate) certificate);
        }
        else
        {
            sb.append("<pre>\n");
            sb.append(certificate.toString());
            sb.append("</pre>\n");
        }

        sb.append("</body></html>");
        return sb.toString();
    }

    /**
     * Appends an HTML representation of the given X509Certificate. 
     * @param sb StringBuilder to append to
     * @param certificate to print
     */
    private void renderX509(StringBuilder sb, X509Certificate certificate)
    {
        X500Principal issuer = certificate.getIssuerX500Principal();
        X500Principal subject = certificate.getSubjectX500Principal();

        sb.append("<table cellspacing='1' cellpadding='1'>\n");

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

                final String value;
                Object nameValue = name.getValue();

                if (nameValue instanceof byte[])
                {
                    byte[] nameValueAsByteArray = (byte[]) nameValue;

                    value
                        = getHex(nameValueAsByteArray) + " ("
                            + new String(nameValueAsByteArray) + ")";
                }
                else
                    value = nameValue.toString();

                addField(sb, lbl, value);
            }
        }
        catch (InvalidNameException ine)
        {
            addField(sb, R.getI18NString("service.gui.CERT_INFO_CN"), 
                    subject.getName());
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

                final String value;
                Object nameValue = name.getValue();

                if (nameValue instanceof byte[])
                {
                    byte[] nameValueAsByteArray = (byte[]) nameValue;

                    value
                        = getHex(nameValueAsByteArray) + " ("
                            + new String(nameValueAsByteArray) + ")";
                }
                else
                    value = nameValue.toString();

                addField(sb, lbl, value);
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
        catch (CertificateException e)
        {
            // do nothing as we cannot show this value
        }

        addTitle(sb, R.getI18NString("service.gui.CERT_INFO_CERT_DETAILS"));

        addField(sb, R.getI18NString("service.gui.CERT_INFO_SER_NUM"), 
                certificate.getSerialNumber().toString());

        addField(sb, R.getI18NString("service.gui.CERT_INFO_VER"), 
                String.valueOf(certificate.getVersion()));

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

        sb.append("</table>\n");
    }

    /**
     * Add a title.
     *
     * @param sb StringBuilder to append to
     * @param title to print
     */
    private void addTitle(StringBuilder sb, String title)
    {
        sb.append("<tr><td colspan='2'")
                .append(" style='margin-top: 5pt; white-space: nowrap'><p><b>")
                .append(title).append("</b></p></td></tr>\n");
    }

    /**
     * Add a field.
     * @param sb StringBuilder to append to
     * @param field name of the certificate field
     * @param value to print
     */
    private void addField(StringBuilder sb, String field, String value)
    {
        sb.append("<tr>")
                .append("<td style='margin-left: 5pt; margin-right: 25pt;")
                .append(" white-space: nowrap'>")
                .append(field).append("</td>")
                .append("<td>").append(value).append("</td>")
                .append("</tr>\n");
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

    /**
     * Construct a "simplified name" based on the subject DN from the
     * certificate. The purpose is to have something shorter to display in the
     * list. The name used is one of the following DN parts, if
     * available, otherwise the complete DN:
     * 'CN', 'OU' or else 'O'.
     * @param cert to read subject DN from
     * @return the simplified name
     */
    private static String getSimplifiedName(X509Certificate cert)
    {
        final HashMap<String, String> parts = new HashMap<String, String>();
        try
        {
            for (Rdn name : new LdapName(
                    cert.getSubjectX500Principal().getName()).getRdns())
            {
                if (name.getType() != null && name.getValue() != null)
                {
                    parts.put(name.getType(), name.getValue().toString());
                }
            }
        }
        catch (InvalidNameException ignored) // NOPMD
        {
        }

        String result = parts.get("CN");
        if (result == null)
        {
            result = parts.get("OU");
        }
        if (result == null)
        {
            result = parts.get("O");
        }
        if (result == null)
        {
            result = cert.getSubjectX500Principal().getName();
        }
        return result;
    }

    /**
     * Called when the selection changed in the tree.
     * Loads the selected certificate.
     * @param e the event
     */
    private void valueChangedPerformed(TreeSelectionEvent e)
    {
        Object o = e.getNewLeadSelectionPath().getLastPathComponent();
        if (o instanceof DefaultMutableTreeNode)
        {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) o;
            infoTextPane.setText(toString(node.getUserObject()));
        }
    }
}
