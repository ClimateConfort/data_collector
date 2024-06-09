package com.climateconfort.data_collector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
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
import javax.net.ssl.TrustManagerFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

class TlsManagerTest {

    @Mock
    Properties properties;

    @Mock
    private KeyManagerFactory mockKeyManagerFactory;

    @Mock
    private KeyStore mockKeyStore;

    @Mock
    private TrustManagerFactory mockTrustManagerFactory;

    @BeforeEach
    public void setUp() throws KeyManagementException, NoSuchAlgorithmException, CertificateException, IOException,
            UnrecoverableKeyException, KeyStoreException {
        MockitoAnnotations.openMocks(this);
        doNothing().when(mockKeyManagerFactory).init(any(KeyStore.class), any(char[].class));
        doNothing().when(mockKeyStore).load(any(InputStream.class), any(char[].class));
        doNothing().when(mockTrustManagerFactory).init(any(KeyStore.class));
        doReturn("String").when(properties).getProperty(anyString());
    }

    @Test
    void createKeyManagerFactoryTest() throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException,
            CertificateException, IOException {
        try (MockedStatic<KeyManagerFactory> mockedKeyManagerFactoryStatic = mockStatic(KeyManagerFactory.class);
                MockedConstruction<FileInputStream> mockedConstruction = mockConstruction(FileInputStream.class);
                MockedStatic<KeyStore> mockedKeyStoreStatic = mockStatic(KeyStore.class)) {
            mockedKeyManagerFactoryStatic.when(() -> KeyManagerFactory.getInstance(anyString()))
                    .thenReturn(mockKeyManagerFactory);
            mockedKeyStoreStatic.when(() -> KeyStore.getInstance(anyString())).thenReturn(mockKeyStore);
            assertEquals(mockKeyManagerFactory, TlsManager.createKeyManagerFactory(properties));
        }
    }

    @Test
    void createTrustManagerFactoryTest()
            throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException {
        try (MockedStatic<TrustManagerFactory> mockedTrustManagerFactoryStatic = mockStatic(TrustManagerFactory.class);
                MockedConstruction<FileInputStream> mockedConstruction = mockConstruction(FileInputStream.class);
                MockedStatic<KeyStore> mockedKeyStoreStatic = mockStatic(KeyStore.class)) {
            mockedTrustManagerFactoryStatic.when(() -> TrustManagerFactory.getInstance(anyString()))
                    .thenReturn(mockTrustManagerFactory);
            mockedKeyStoreStatic.when(() -> KeyStore.getInstance(anyString())).thenReturn(mockKeyStore);
            assertEquals(mockTrustManagerFactory, TlsManager.createTrustManagerFactory(properties));
        }
    }
}