package br.com.phdss;


import java.io.*;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.*;

/**
 * Classe que gera o arquivo Cacerts para todo os estados.
 *
 * @author Pedro H. Lira
 */
public class Cacerts {

    private static final String JSSECACERTS = "NFeCacerts";
    private static final int TIMEOUT_WS = 30;

    /**
     * Construtor padrao.
     */
    private Cacerts() {
    }

    /**
     * Metodo responsavel pela geracao do arquivo cacerts dos estados.
     */
    public static void gerar() {
        try {
            char[] passphrase = "changeit".toCharArray();

            File file = new File(JSSECACERTS);
            if (file.isFile() == false) {
                char SEP = File.separatorChar;
                File dir = new File(System.getProperty("java.home") + SEP + "lib" + SEP + "security");
                file = new File(dir, JSSECACERTS);
                if (file.isFile() == false) {
                    file = new File(dir, "cacerts");
                }
            }

            info("| Loading KeyStore " + file + "...");
            KeyStore ks;
            try (InputStream in = new FileInputStream(file)) {
                ks = KeyStore.getInstance(KeyStore.getDefaultType());
                ks.load(in, passphrase);
            }

            // HOMOLOGACAO
            //get("homnfe.sefaz.am.gov.br", 443, ks);
            //get("hnfe.sefaz.ba.gov.br", 443, ks);
            //get("nfeh.sefaz.ce.gov.br", 443, ks);
            //get("homolog.sefaz.go.gov.br", 443, ks);
            //get("hnfe.fazenda.mg.gov.br", 443, ks);
            //get("homologacao.nfe.ms.gov.br", 443, ks);
            //get("homologacao.sefaz.mt.gov.br", 443, ks);
            //get("nfehomolog.sefaz.pe.gov.br", 443, ks);
            //get("homologacao.nfe.fazenda.pr.gov.br", 443, ks);
            //get("homologacao.nfe.sefaz.rs.gov.br", 443, ks);
            //get("homologacao.nfe.fazenda.sp.gov.br", 443, ks);
            //get("hom.sefazvirtual.fazenda.gov.br", 443, ks);
            //get("homologacao.nfe.sefazvirtual.rs.gov.br", 443, ks);
            //get("hom.svc.fazenda.gov.br", 443, ks);
            //get("homologacao.nfe.sefazvirtual.rs.gov.br", 443, ks);

            // PRODUCAO
            get("nfe.sefaz.am.gov.br", 443, ks);
            get("nfe.sefaz.ba.gov.br", 443, ks);
            get("nfe.sefaz.ce.gov.br", 443, ks);
            get("nfe.sefaz.go.gov.br", 443, ks);
            get("nfe.fazenda.mg.gov.br", 443, ks);
            get("nfe.fazenda.ms.gov.br", 443, ks);
            get("nfe.sefaz.mt.gov.br", 443, ks);
            get("nfe.sefaz.pe.gov.br", 443, ks);
            get("nfe.fazenda.pr.gov.br", 443, ks);
            get("nfe.sefaz.rs.gov.br", 443, ks);
            get("nfe.fazenda.sp.gov.br", 443, ks);
            get("www.sefazvirtual.fazenda.gov.br", 443, ks);
            get("nfe.sefazvirtual.rs.gov.br", 443, ks);
            get("www.svc.fazenda.gov.br", 443, ks);
            get("nfe.sefazvirtual.rs.gov.br", 443, ks);

            File cafile = new File(JSSECACERTS);
            try (OutputStream out = new FileOutputStream(cafile)) {
                ks.store(out, passphrase);
            }
        } catch (Exception e) {
            error(e.getMessage());
        }
    }

    /**
     * Metodo que recupera as informacoes de cada webservices da sefaz.
     *
     * @param host o endereco do WS.
     * @param port a porta do WS.
     * @param ks   a chave gravada.
     *
     * @throws Exception dispara caso nao consiga.
     */
    private static void get(String host, int port, KeyStore ks) throws Exception {
        SSLContext context = SSLContext.getInstance("TLS");
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        X509TrustManager defaultTrustManager = (X509TrustManager) tmf.getTrustManagers()[0];
        SavingTrustManager tm = new SavingTrustManager(defaultTrustManager);
        context.init(null, new TrustManager[]{tm}, null);
        SSLSocketFactory factory = context.getSocketFactory();

        info("| Opening connection to " + host + ":" + port + "...");
        SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
        socket.setSoTimeout(TIMEOUT_WS * 1000);
        try {
            info("| Starting SSL handshake...");
            socket.startHandshake();
            socket.close();
            info("| No errors, certificate is already trusted");
        } catch (SSLHandshakeException e) {
            /**
             * PKIX path building failed:
             * sun.security.provider.certpath.SunCertPathBuilderException:
             * unable to find valid certification path to requested target Não
             * tratado, pois sempre ocorre essa exceção quando o cacerts nao
             * esta gerado.
             */
        } catch (SSLException e) {
            error("| " + e.toString());
        }

        X509Certificate[] chain = tm.chain;
        if (chain == null) {
            info("| Could not obtain server certificate chain");
        }

        info("| Server sent " + chain.length + " certificate(s):");
        MessageDigest sha1 = MessageDigest.getInstance("SHA1");
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        for (int i = 0; i < chain.length; i++) {
            X509Certificate cert = chain[i];
            sha1.update(cert.getEncoded());
            md5.update(cert.getEncoded());

            String alias = host + "-" + (i);
            ks.setCertificateEntry(alias, cert);
            info("| Added certificate to keystore '" + JSSECACERTS + "' using alias '" + alias + "'");
        }
    }

    /**
     * Classe que gerencia o salvamento confiavel.
     */
    private static class SavingTrustManager implements X509TrustManager {

        private final X509TrustManager tm;
        private X509Certificate[] chain;

        SavingTrustManager(X509TrustManager tm) {
            this.tm = tm;
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            this.chain = chain;
            tm.checkServerTrusted(chain, authType);
        }
    }

    /**
     * Metodo que printa na tela as informacoes.
     *
     * @param log o texto a ser mostrado.
     */
    private static void info(String log) {
        System.out.println("INFO: " + log);
    }

    /**
     * Metodo que printa na tela os erros.
     *
     * @param log o texto a ser mostrado.
     */
    private static void error(String log) {
        System.out.println("ERROR: " + log);
    }
}
