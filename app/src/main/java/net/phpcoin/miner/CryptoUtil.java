package net.phpcoin.miner;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.util.encoders.Hex;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;

public class CryptoUtil {

    public static final String NETWORK_PREFIX = "38";


    public static Wallet.Account createAccount() throws IOException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
        Security.addProvider(new BouncyCastleProvider());

        ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256k1");
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDSA", "BC");
        keyPairGenerator.initialize(ecSpec, new SecureRandom());
        java.security.KeyPair keypair = keyPairGenerator.generateKeyPair();
        PublicKey publicKey = keypair.getPublic();
        PrivateKey privateKey = keypair.getPrivate();

        String privateKeyDer = privateKeyToDER(privateKey);
        String privateKeybase58 = pem2coin(privateKeyDer);

        String publicKeyDer = privateKeyToDER(publicKey);
        String publicKeybase58 = pem2coin(publicKeyDer);

        String address = getAddress(publicKeybase58);
        System.out.println(address);

        String wallet = "phpcoin\n";
        wallet += privateKeybase58 + "\n";
        wallet += publicKeybase58;
        return new Wallet.Account(privateKeybase58, publicKeybase58, address);
    }

    public static String privateKeyToDER(Key key) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PEMWriter pemWriter = new PEMWriter(new OutputStreamWriter(bos));
        pemWriter.writeObject(key);
        pemWriter.close();

        return new String(bos.toByteArray());
    }

    private static String pem2coin(String data) {
        data = data.replace("-----BEGIN PUBLIC KEY-----", "");
        data = data.replace("-----END PUBLIC KEY-----", "");
        data = data.replace("-----BEGIN EC PRIVATE KEY-----", "");
        data = data.replace("-----END EC PRIVATE KEY-----", "");
        data = data.replaceAll("\n", "");
        byte[] bytes = org.bouncycastle.util.encoders.Base64.decode(data);
        return Base58.encode(bytes);
    }

    private static String getAddress(String publicKey) {
        if(publicKey == null) {
            return null;
        }
        if(publicKey.length()==0) {
            return null;
        }
        String hash1 = hash("SHA-256", publicKey);
        String hash2 = hash("RIPEMD160", hash1);
        String baseAddress = NETWORK_PREFIX.concat(hash2);
        String checksumCalc1 = hash("SHA-256", baseAddress);
        String checksumCalc2 = hash("SHA-256", checksumCalc1);
        String checksumCalc3 = hash("SHA-256", checksumCalc2);
        String checksum = checksumCalc3.substring(0, 8);
        String addressHex = baseAddress.concat(checksum);
        byte[] addressBin = Hex.decode(addressHex);
        String address = Base58.encode(addressBin);
        return address;
    }

    public static String hash(String algo, String input) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance(algo);
            byte[] encodedhash = digest.digest(
                    input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
