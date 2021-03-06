/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.jss.tests;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Enumeration;

import javax.crypto.SecretKey;

import org.mozilla.jss.CryptoManager;
import org.mozilla.jss.crypto.CryptoToken;
import org.mozilla.jss.crypto.KeyGenAlgorithm;
import org.mozilla.jss.crypto.KeyGenerator;
import org.mozilla.jss.crypto.SecretKeyFacade;
import org.mozilla.jss.pkcs11.PK11Token;
import org.mozilla.jss.util.ConsolePasswordCallback;

public class KeyStoreTest {

    public static void printUsage() {
        System.out.println("Usage: KeyStoreTest <dbdir> " +
            "<operation> [<args>...]");
        System.out.println("Operations:\n" +
            "getAliases\n" +
            "deleteEntry <alias> . . .\n" +
            "getCertByName <alias> . . .\n" +
            "getCertByDER <DER cert filename>\n" +
            "getKey <alias>\n" +
            "addKey <alias>\n" +
            "isTrustedCert <alias>\n");
    }

    public static void main(String argv[]) throws Throwable {
        if( argv.length < 2 ) {
            printUsage();
            System.exit(1);
        }

        String nss_db = argv[0];
        String password_file = argv[1];
        String op = argv[2];

        int offset = 3;
        String[] args = new String[argv.length - offset];
        for(int i = offset; i < argv.length; i++) {
            args[i - offset] = argv[i];
        }

        CryptoManager.initialize(nss_db);
        CryptoManager cm = CryptoManager.getInstance();


        // login to the token
        CryptoToken token = cm.getInternalKeyStorageToken();
        //CryptoToken token = cm.getTokenByName("Builtin Object Token");
        token.login(new FilePasswordCallback(password_file));
        cm.setThreadToken(token);

        KeyStore ks = KeyStore.getInstance("PKCS11", "Mozilla-JSS");
        ks.load(null, null);

        if( op.equalsIgnoreCase("getAliases") ) {
            dumpAliases(ks);
        } else if( op.equalsIgnoreCase("deleteEntry") ) {
            for(int j=0; j < args.length; ++j) {
                ks.deleteEntry(args[j]);
            }
        } else if( op.equalsIgnoreCase("getCertByName") ) {
            for(int j=0; j < args.length; ++j) {
                dumpCert(ks, args[j]);
            }
        } else if( op.equalsIgnoreCase("getCertByDER") ) {
            if( args.length < 1 ) {
                printUsage();
                System.exit(1);
            }
            getCertByDER(ks, args[0]);
        } else if( op.equalsIgnoreCase("getKey") ) {
            if( args.length != 1 ) {
                printUsage();
                System.exit(1);
            }
            getKey(ks, args[0]);
        } else if( op.equalsIgnoreCase("isTrustedCert") ) {
            if( args.length != 1 ) {
                printUsage();
                System.exit(1);
            }
            isTrustedCert(ks, args[0]);
        } else if( op.equalsIgnoreCase("addKey") ) {
            if( args.length != 1 ) {
                printUsage();
                System.exit(1);
            }
            addKey(ks, args[0]);
        } else {
            printUsage();
            System.exit(1);
        }
    }

    public static void dumpCert(KeyStore ks, String alias)
        throws Throwable
    {
        Certificate cert = ks.getCertificate(alias);
        if( cert == null ) {
            System.out.println("Certificate with alias \"" + alias +
                "\" not found");
        } else {
            System.out.println(cert.toString());
        }
    }

    public static void dumpAliases(KeyStore ks) throws Throwable {
        Enumeration<String> aliases = ks.aliases();

        System.out.println("Aliases:");
        while( aliases.hasMoreElements() ) {
            String alias = aliases.nextElement();
            System.out.println( "\"" + alias + "\"");
        }
        System.out.println();
    }

    public static void getCertByDER(KeyStore ks, String derCertFilename)
            throws Throwable {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int numRead;

        try (FileInputStream fis = new FileInputStream(derCertFilename)) {
            while ((numRead = fis.read(buf)) != -1) {
                bos.write(buf, 0, numRead);
            }
        }

        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());

        CertificateFactory fact = CertificateFactory.getInstance("X.509");
        Certificate cert = fact.generateCertificate( bis );

        String nick = ks.getCertificateAlias(cert);

        if( nick == null ) {
            System.out.println("No matching certificate was found.");
        } else {
            System.out.println("Found matching certificate \"" + nick + "\"");
        }
    }

    public static void getKey(KeyStore ks, String alias)
            throws Throwable {

        Key key = ks.getKey(alias, null);

        if( key == null ) {
            System.out.println("Could not find key for alias \"" +
                alias + "\"");
            System.exit(1);
        } else {
            String clazz = key.getClass().getName();
            System.out.println("Found " + clazz + " for alias \"" +
                alias + "\"");
        }
    }

    public static void isTrustedCert(KeyStore ks, String alias)
            throws Throwable {

        if( ks.isCertificateEntry(alias) ) {
            System.out.println("\"" + alias + "\" is a trusted certificate" +
                " entry");
        } else {
            System.out.println("\"" + alias + "\" is NOT a trusted certificate"
                + " entry");
        }
    }

    public static void addKey(KeyStore ks, String alias)
            throws Throwable
    {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA",
            "Mozilla-JSS");

        kpg.initialize(1024);
        KeyPair pair = kpg.genKeyPair();
        Certificate [] certs = new Certificate[1];

        ks.setKeyEntry(alias, pair.getPrivate(), null, certs);

        CryptoManager cm = CryptoManager.getInstance();
        CryptoToken tok = cm.getInternalKeyStorageToken();
        KeyGenerator kg = tok.getKeyGenerator( KeyGenAlgorithm.DES3 );
        SecretKey key = new SecretKeyFacade(kg.generate());

        ks.setKeyEntry(alias+"sym", key, null, null);
    }
}
