package DiskUtility;

import Constants.VALUES;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.KeySpec;
import java.util.Arrays;

public class Crypto {
    private static final int GCM_TAG_LENGTH = VALUES.TAG_SIZE;
    private static final int GCM_IV_LENGTH = VALUES.IV_SIZE;
    private static final int KEY_SIZE = 256;
    private static final int SALT_LENGTH = 16;
    private static final int ITERATIONS = 65536;

    public static void init(){
        Security.addProvider(new BouncyCastleProvider());
    }

    public static SecretKey deriveKeyFromPassword(String password, byte[] salt) throws Exception{
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_SIZE);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }
    public static byte[] encryptBlock(byte[] plaintext, SecretKey key, int BLOCK_SIZE) throws Exception {
        if (plaintext.length != BLOCK_SIZE)
            throw new IllegalArgumentException("Plaintext block must be exactly 4096 bytes");
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);
        byte[] ciphertext = cipher.doFinal(plaintext);
        byte[] encryptedData = new byte[GCM_IV_LENGTH + ciphertext.length];
        System.arraycopy(iv, 0, encryptedData, 0, GCM_IV_LENGTH);
        System.arraycopy(ciphertext, 0, encryptedData, GCM_IV_LENGTH, ciphertext.length);
        return encryptedData;
    }
    public static byte[] decryptBlock(byte[] encryptedData, SecretKey key, int BLOCK_SIZE) throws Exception {
        if (encryptedData.length != GCM_IV_LENGTH + BLOCK_SIZE + GCM_TAG_LENGTH)
            throw new IllegalArgumentException("Encrypted data length is incorrect.");
        byte[] iv = Arrays.copyOfRange(encryptedData, 0, GCM_IV_LENGTH);
        byte[] ciphertext = Arrays.copyOfRange(encryptedData, GCM_IV_LENGTH, encryptedData.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);
        return cipher.doFinal(ciphertext);
    }
}
