package vn.hoadon.config;

import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class PasignerHttpsConfig {
    private static final Logger log = LoggerFactory.getLogger(PasignerHttpsConfig.class);

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> pasignerHttpsConnector(
            @Value("${app.pasigner.https.enabled:true}") boolean enabled,
            @Value("${app.pasigner.https.port:443}") int port,
            @Value("${app.pasigner.https.keystore:.local/pasigner/hoadon2er-id-vn.p12}") String keyStore,
            @Value("${app.pasigner.https.password-file:.local/pasigner/certificate.password}") String passwordFile) {
        return factory -> {
            if (!enabled) return;

            Path keyStorePath = Path.of(keyStore).toAbsolutePath().normalize();
            Path passwordPath = Path.of(passwordFile).toAbsolutePath().normalize();
            if (!Files.isRegularFile(keyStorePath) || !Files.isRegularFile(passwordPath)) {
                log.warn("P.A Signer HTTPS connector is disabled because local certificate files are missing");
                return;
            }

            try {
                String password = Files.readString(passwordPath).trim();
                Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
                connector.setPort(port);
                connector.setScheme("https");
                connector.setSecure(true);
                connector.setProperty("SSLEnabled", "true");

                SSLHostConfig sslHostConfig = new SSLHostConfig();
                SSLHostConfigCertificate certificate = new SSLHostConfigCertificate(
                        sslHostConfig, SSLHostConfigCertificate.Type.RSA);
                certificate.setCertificateKeystoreFile(keyStorePath.toString());
                certificate.setCertificateKeystorePassword(password);
                certificate.setCertificateKeystoreType("PKCS12");
                sslHostConfig.addCertificate(certificate);
                connector.addSslHostConfig(sslHostConfig);
                factory.addAdditionalTomcatConnectors(connector);
                log.info("P.A Signer HTTPS connector configured on port {}", port);
            } catch (Exception ex) {
                throw new IllegalStateException("Cannot configure the P.A Signer HTTPS connector", ex);
            }
        };
    }
}
