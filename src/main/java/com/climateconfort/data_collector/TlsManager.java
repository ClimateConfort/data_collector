package com.climateconfort.data_collector;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Properties;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class TlsManager {
    private final SSLContext sslContext;

    public TlsManager(Properties properties)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException,
            UnrecoverableKeyException, KeyManagementException {
        sslContext = SSLContext.getInstance("TLSv1.3");
        sslContext.init(createKeyManagerFactory(properties).getKeyManagers(), createTrustManagerFactory(properties).getTrustManagers(), null);
    }

    static KeyManagerFactory createKeyManagerFactory(Properties properties) throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        try (InputStream keyFile = new FileInputStream(properties.getProperty("rabbitmq.tls.pkcs12_key_path"))) {
            KeyStore pkcs12KeyStore = KeyStore.getInstance("PKCS12");
            char[] pcks12KeyPassphrase = properties.getProperty("rabbitmq.tls.pcks12_password").toCharArray();
            pkcs12KeyStore.load(keyFile, pcks12KeyPassphrase);
            keyManagerFactory.init(pkcs12KeyStore, pcks12KeyPassphrase);
        }
        return keyManagerFactory;
    }

    static TrustManagerFactory createTrustManagerFactory(Properties properties) throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        try (InputStream trustStoreFile = new FileInputStream(properties.getProperty("rabbitmq.tls.java_key_store_path"))) {
            KeyStore jksKeyStore = KeyStore.getInstance("JKS");
            char[] trustPassphrase = properties.getProperty("rabbitmq.tls.java_key_store_password").toCharArray();
            jksKeyStore.load(trustStoreFile, trustPassphrase);
            trustManagerFactory.init(jksKeyStore);
        }
        return trustManagerFactory;
    }

    public SSLContext getSslContext() {
        return sslContext;
    }
}
