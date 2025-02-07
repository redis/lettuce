package io.lettuce.test.settings;

import io.lettuce.core.internal.LettuceStrings;
import org.testcontainers.shaded.org.bouncycastle.cert.X509v3CertificateBuilder;
import org.testcontainers.shaded.org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.testcontainers.shaded.org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.testcontainers.shaded.org.bouncycastle.operator.ContentSigner;
import org.testcontainers.shaded.org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.testcontainers.shaded.org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.testcontainers.shaded.org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.testcontainers.shaded.org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.testcontainers.shaded.org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.testcontainers.shaded.org.bouncycastle.util.io.pem.PemObject;
import org.testcontainers.shaded.org.bouncycastle.util.io.pem.PemWriter;
import sun.security.x509.X500Name;

import javax.security.auth.x500.X500Principal;
import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class TlsSettings {

    private static final String TRUST_STORE_TYPE = "PKCS12";

    private static final String TEST_WORK_FOLDER = System.getenv().getOrDefault("TEST_WORK_FOLDER", "/tmp/redis-env-work");

    private static final String TEST_SERVER_CERT = "redis.crt";

    private static final String TEST_CA_CERT = "ca.crt";

    private static final String TEST_TRUSTSTORE = "truststore.jks";

    private static final String TEST_KEYSTORE = "keystore.jks";

    private static final String PASSWORD = "changeit";

    public static Path envServerCert(Path certLocation) {
        return Paths.get(TEST_WORK_FOLDER, certLocation.toString(), TEST_SERVER_CERT);
    }

    public static Path envCa(Path certLocation) {
        return Paths.get(TEST_WORK_FOLDER, certLocation.toString(), TEST_CA_CERT);
    }

    public static Path testTruststorePath(String name) {
        return Paths.get(TEST_WORK_FOLDER, name + '-' + TEST_TRUSTSTORE);
    }

    public static Path testGenCertPath(String keystoreLocation) {
        return Paths.get(TEST_WORK_FOLDER, keystoreLocation);
    }

    public static Path testKeyStorePath(String keystoreLocation) {
        return Paths.get(TEST_WORK_FOLDER, keystoreLocation, TEST_KEYSTORE);
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

    public static void generateCertificates(String caDir, String keystoreFile) throws Exception {
        createDirectories(caDir);

        KeyPair keyPair = generateKeyPair();

        savePrivateKey(keyPair.getPrivate(), caDir);

        PKCS10CertificationRequest csr = generateCSR(keyPair);

        X509Certificate certificate = signCertificate(csr, keyPair);

        saveCertificate(certificate, caDir);

        createPKCS12(keyPair.getPrivate(), certificate, keystoreFile);
    }

    private static void createDirectories(String caDir) throws IOException {
        Files.createDirectories(Paths.get(caDir, "private"));
        Files.createDirectories(Paths.get(caDir, "certs"));
    }

    private static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }

    private static void savePrivateKey(PrivateKey privateKey, String caDir) throws Exception {
        String keyPath = Paths.get(caDir, "private", "client.key.pem").toString();
        try (PemWriter pemWriter = new PemWriter(new FileWriter(keyPath))) {
            pemWriter.writeObject(new PemObject("PRIVATE KEY", privateKey.getEncoded()));
        }

        File keyFile = new File(keyPath);
        keyFile.setReadable(false, false);
        keyFile.setReadable(true, true);
        keyFile.setWritable(false, false);
        keyFile.setExecutable(false, false);
    }

    private static PKCS10CertificationRequest generateCSR(KeyPair keyPair) throws Exception {
        X500Principal subject = new X500Principal("CN=client,O=lettuce,C=NN,ST=Unknown,L=Unknown");
        PKCS10CertificationRequestBuilder csrBuilder = new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic());

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());

        return csrBuilder.build(signer);
    }

    private static X509Certificate signCertificate(PKCS10CertificationRequest csr, KeyPair keyPair) throws Exception {
        org.bouncycastle.asn1.x500.X500Name issuerName = new org.bouncycastle.asn1.x500.X500Name(
                "CN=client,O=lettuce,C=NN,ST=Unknown,L=Unknown");

        BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());
        Instant now = Instant.now();
        Date startDate = Date.from(now);
        Date endDate = Date.from(now.plus(Duration.ofDays(375)));

        JcaPKCS10CertificationRequest jcaCsr = new JcaPKCS10CertificationRequest(csr);

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                org.testcontainers.shaded.org.bouncycastle.asn1.x500.X500Name.getInstance(issuerName), serialNumber, startDate,
                endDate, jcaCsr.getSubject(), jcaCsr.getPublicKey());

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());

        return new JcaX509CertificateConverter().getCertificate(certBuilder.build(signer));
    }

    private static void saveCertificate(X509Certificate certificate, String caDir) throws Exception {
        String certPath = Paths.get(caDir, "certs", "client.cert.pem").toString();
        try (PemWriter pemWriter = new PemWriter(new FileWriter(certPath))) {
            pemWriter.writeObject(new PemObject("CERTIFICATE", certificate.getEncoded()));
        }
    }

    private static void createPKCS12(PrivateKey privateKey, X509Certificate certificate, String keystoreFile) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);

        keyStore.setKeyEntry("client", privateKey, PASSWORD.toCharArray(), new X509Certificate[] { certificate });

        try (OutputStream output = Files.newOutputStream(testKeyStorePath(keystoreFile))) {
            keyStore.store(output, PASSWORD.toCharArray());
        }
    }

}
