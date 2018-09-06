package com.terrytec.nbiot.locationmap.classes;

import java.security.SecureRandom;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class TrustAllCerts implements X509TrustManager {

    @Override
    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws
            CertificateException {
//        if (x509Certificates == null || x509Certificates.length == 0 || s == null || s.length() == 0)
//            throw new IllegalArgumentException();
//        if (false)
//            throw new CertificateException();
    }

    @Override
    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws
            CertificateException {
//        if (x509Certificates == null || x509Certificates.length == 0 || s == null || s.length() == 0)
//            throw new IllegalArgumentException();
//        if (false)
//            throw new CertificateException();
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }


    public static class TrustAllHostnameVerifier implements HostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }


    public static SSLSocketFactory createSSLSocketFactory() {
        SSLSocketFactory ssfFactory = null;

        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[]{new TrustAllCerts()}, new SecureRandom());

            ssfFactory = sc.getSocketFactory();
        } catch (Exception e) {
        }

        return ssfFactory;
    }
}

