package io.lettuce.test.settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.lettuce.core.SslOptions;

public class TlsSettings {

    private static final String TRUST_STORE_TYPE = "PKCS12";

    private static final String TEST_WORK_FOLDER = System.getenv().getOrDefault("TEST_WORK_FOLDER", "work/docker");

    private static final String TEST_SERVER_CERT = "redis.crt";

    private static final String TEST_CLIENT_P12 = "client.p12";

    private static final String TEST_CLIENT_CERT = "client.crt";

    private static final String TEST_CLIENT_KEY = "client.key";

    private static final String TEST_CA_CERT = "ca.crt";

    private static final String TEST_TRUSTSTORE = "truststore.jks";

    // mTLS container configurations
    public static final String MTLS_STANDALONE_CONTAINER = "redis-standalone-5-client-cert";

    public static final Path MTLS_STANDALONE_TLS_PATH = Paths.get(MTLS_STANDALONE_CONTAINER + "/work/tls");

    public static final String MTLS_CLUSTER_CONTAINER = "ssl-test-cluster";

    public static final Path MTLS_CLUSTER_TLS_PATH = Paths.get(MTLS_CLUSTER_CONTAINER + "/work/tls");

    /**
     * Client certificate options for mTLS tests.
     */
    public enum ClientCertificate {

        /** Default client cert (client.p12) - CN matches ACL user, mTLS auto-auth succeeds */
        DEFAULT("client.p12"),

        /** Alternative client cert (Client-test-2.p12) - CN has no matching ACL user */
        NO_ACL_USER("Client-test-2.p12");

        private final String filename;

        ClientCertificate(String filename) {
            this.filename = filename;
        }

        public String getFilename() {
            return filename;
        }

    }

    public static Path envClientP12(Path certLocation) {
        return Paths.get(TEST_WORK_FOLDER, certLocation.toString(), TEST_CLIENT_P12);
    }

    public static Path envServerCert(Path certLocation) {
        return Paths.get(TEST_WORK_FOLDER, certLocation.toString(), TEST_SERVER_CERT);
    }

    public static Path envCa(Path certLocation) {
        return Paths.get(TEST_WORK_FOLDER, certLocation.toString(), TEST_CA_CERT);
    }

    public static Path testTruststorePath(String name) {
        return Paths.get(TEST_WORK_FOLDER, name + '-' + TEST_TRUSTSTORE);
    }

    /**
     * Creates an empty truststore.
     *
     * @return An empty KeyStore object.
     * @throws KeyStoreException If there's an error initializing the truststore.
     * @throws IOException If there's an error loading the truststore.
     * @throws NoSuchAlgorithmException If the algorithm used to check the integrity of the truststore cannot be found.
     * @throws CertificateException If any of the certificates in the truststore could not be loaded.
     */
    private static KeyStore createTruststore()
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        KeyStore trustStore = KeyStore.getInstance(TRUST_STORE_TYPE);
        trustStore.load(null, null);
        return trustStore;
    }

    /**
     * Loads an X.509 certificate from the given file path.
     *
     * @param certPath Path to the certificate file.
     * @return An X509Certificate object.
     * @throws Exception If there's an error loading the certificate.
     */
    private static X509Certificate loadCertificate(Path certPath) throws Exception {
        try (FileInputStream fis = new FileInputStream(certPath.toFile())) {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) certFactory.generateCertificate(fis);
        }
    }

    /**
     * Adds a trusted certificate to the given truststore.
     *
     * @param trustStore The KeyStore object.
     * @param alias Alias for the certificate.
     * @param certPath Path to the certificate file.
     * @throws Exception If there's an error adding the certificate.
     */
    private static void addTrustedCertificate(KeyStore trustStore, String alias, Path certPath) throws Exception {
        X509Certificate cert = loadCertificate(certPath);
        trustStore.setCertificateEntry(alias, cert);
    }

    /**
     * Creates a truststore, adds multiple trusted certificates, and saves it to the specified path.
     *
     * @param trustedCertPaths List of certificate file paths to add to the truststore.
     * @param truststorePath Path to save the generated truststore.
     * @param truststorePassword Password for the truststore.
     * @return Path to the saved truststore file.
     */
    public static Path createAndSaveTruststore(List<Path> trustedCertPaths, Path truststorePath, String truststorePassword) {
        try {
            KeyStore trustStore = createTruststore();

            for (Path certPath : trustedCertPaths) {
                addTrustedCertificate(trustStore, "trusted-cert-" + UUID.randomUUID(), certPath);
            }

            try (FileOutputStream fos = new FileOutputStream(truststorePath.toFile())) {
                trustStore.store(fos, truststorePassword.toCharArray());
            } catch (IOException e) {
                throw new RuntimeException("Failed to save truststore to " + truststorePath + ": " + e.getMessage(), e);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create and save truststore: " + e.getMessage(), e);
        }

        return truststorePath;
    }

    public static Path createAndSaveTestTruststore(String trustStoreName, Path certificateLocations,
            String truststorePassword) {
        List<Path> trustedCertPaths = new ArrayList<>();
        trustedCertPaths.add(envCa(certificateLocations).toAbsolutePath());
        trustedCertPaths.add(envServerCert(certificateLocations).toAbsolutePath());

        Path trustStorePath = testTruststorePath(trustStoreName).toAbsolutePath();

        return createAndSaveTruststore(trustedCertPaths, trustStorePath, truststorePassword);
    }

    /**
     * Creates SslOptions with the specified keystore and truststore files.
     *
     * @param keystore the client certificate keystore file
     * @param truststore the truststore file
     * @return SslOptions configured with the provided stores
     */
    public static SslOptions createSslOptions(File keystore, File truststore) {
        return SslOptions.builder().jdkSslProvider().keystore(keystore, "changeit".toCharArray())
                .truststore(truststore, "changeit").build();
    }

    /**
     * Creates SslOptions with client certificate (keystore) and truststore for mTLS authentication.
     *
     * @param containerName the container/environment name for truststore creation
     * @param tlsPath the path to TLS certificates
     * @param clientCert the client certificate to use
     * @return SslOptions configured for mTLS
     */
    public static SslOptions createMtlsSslOptions(String containerName, Path tlsPath, ClientCertificate clientCert) {
        Path truststorePath = createAndSaveTestTruststore(containerName, tlsPath, "changeit");
        File truststoreFile = truststorePath.toFile();
        Path keystorePath = Paths.get(TEST_WORK_FOLDER, tlsPath.toString(), clientCert.getFilename());

        return createSslOptions(keystorePath.toFile(), truststoreFile);
    }

    /**
     * Creates SslOptions with client certificate (keystore) and truststore for mTLS authentication. Uses the default client
     * certificate which has a matching ACL user (mTLS auto-auth succeeds).
     *
     * @param containerName the container/environment name for truststore creation
     * @param tlsPath the path to TLS certificates
     * @return SslOptions configured for mTLS
     */
    public static SslOptions createMtlsSslOptions(String containerName, Path tlsPath) {
        return createMtlsSslOptions(containerName, tlsPath, ClientCertificate.DEFAULT);
    }

    /**
     * Creates SslOptions for the mTLS cluster with default client certificate. The certificate's CN matches an ACL user, so
     * mTLS auto-auth will succeed.
     *
     * @return SslOptions configured for mTLS cluster
     */
    public static SslOptions createMtlsClusterSslOptions() {
        return createMtlsSslOptions(MTLS_CLUSTER_CONTAINER, MTLS_CLUSTER_TLS_PATH, ClientCertificate.DEFAULT);
    }

    /**
     * Creates SslOptions for the mTLS cluster with a client certificate that has no matching ACL user. Use this for tests that
     * expect authentication to fail.
     *
     * @return SslOptions configured for mTLS cluster without ACL user match
     */
    public static SslOptions createMtlsClusterSslOptionsNoAcl() {
        return createMtlsSslOptions(MTLS_CLUSTER_CONTAINER, MTLS_CLUSTER_TLS_PATH, ClientCertificate.NO_ACL_USER);
    }

}
