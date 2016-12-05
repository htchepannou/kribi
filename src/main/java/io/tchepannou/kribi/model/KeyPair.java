package io.tchepannou.kribi.model;

public class KeyPair {
    private final String publicKey;
    private final String privateKey;

    public KeyPair(final String publicKey, final String privateKey) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }
}
