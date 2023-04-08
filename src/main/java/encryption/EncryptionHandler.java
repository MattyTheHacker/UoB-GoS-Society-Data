package encryption;

import io.github.cdimascio.dotenv.Dotenv;
import members.Member;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Base64;

import static utils.FileHandler.saveStringToFile;
import static utils.FileHandler.loadFromFile;

public class EncryptionHandler {
    private static final String ALGORITHM_AES256 = "AES/CBC/PKCS5Padding";
    private static final byte[] IV = new byte[16];
    private final SecretKeySpec secretKeySpec;
    private final Cipher cipher;
    private IvParameterSpec iv;

    private static final Dotenv dotenv = Dotenv.load();
    private static final String KEYSTORE_NAME = dotenv.get("KEY_STORE_NAME");
    private static final String KEYSTORE_PASSWORD = dotenv.get("KEY_STORE_PASS");
    private static final String KEY_PASSWORD = dotenv.get("KEY_PASS");
    private static final String KEY_ALIAS = dotenv.get("KEY_ALIAS");

    private static Key getKeyFromKeyStore() {
        try {
            InputStream keystoreStream = new FileInputStream(KEYSTORE_NAME);

            KeyStore keystore = KeyStore.getInstance("JCEKS");

            keystore.load(keystoreStream, KEYSTORE_PASSWORD.toCharArray());

            if (!keystore.containsAlias(KEY_ALIAS)) {
                throw new RuntimeException("No key found under the alias '" + KEY_ALIAS + "'");
            }

            return keystore.getKey(KEY_ALIAS, KEY_PASSWORD.toCharArray());

        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException |
                    IOException e) {
            throw new RuntimeException(e);
        }
    }

    private SecretKeySpec getSecretKeySpec() {
        return secretKeySpec;
    }

    private Cipher getCipher(int encryptMode) throws InvalidKeyException, InvalidAlgorithmParameterException {
        cipher.init(encryptMode, getSecretKeySpec(), iv);
        return cipher;
    }

    /**
     * Create cipher based on existing {@link Key}
     *
     * @param key Key
     */
    public EncryptionHandler(Key key){
        this(key.getEncoded());
    }

    /**
     * Create cipher based on existing {@link Key} and Initial Vector (iv) in bytes
     *
     * @param key Key
     */
    public EncryptionHandler(Key key, byte[] iv) {
        this(key.getEncoded(), iv);
    }

    /**
     * Create AESCipher using a byte[] array as a key
     *
     * NOTE: Uses an Initial Vector of 16 0x0 bytes. This should not be used to create strong security.
     *
     * @param key Key
     */
    public EncryptionHandler(byte[] key) {
        this(key, IV);
    }

    private EncryptionHandler(byte[] key, byte[] iv) {
        try {
            this.secretKeySpec = new SecretKeySpec(key, "AES");
            this.iv = new IvParameterSpec(iv);
            this.cipher = Cipher.getInstance(ALGORITHM_AES256);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException();
        }
    }

    /**
     * take input string and encrypt it
     *
     * @param input string to encrypt
     * @return String Base64 encoded
     */
    public String encrypt(String input) {
        try {
            Cipher cipher = getCipher(Cipher.ENCRYPT_MODE);

            byte[] encrypted = cipher.doFinal(input.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(encrypted);

        } catch (InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException |
                    BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * take input string and decrypt it
     *
     * @param input string to decrypt
     * @return String Base64 encoded
     */
    public String decrypt(String input) {
        try {
            Cipher cipher = getCipher(Cipher.DECRYPT_MODE);

            byte[] encrypted = Base64.getDecoder().decode(input);
            byte[] decrypted = cipher.doFinal(encrypted);

            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException |
                    BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void encodeMemberToFile(Member member, String path){
        // encrypt the object
        Key key = getKeyFromKeyStore();
        EncryptionHandler encryptionHandler = new EncryptionHandler(key);
        String encrypted = encryptionHandler.encrypt(member.toString());

        // save the encrypted object to a file
        saveStringToFile(encrypted, path);
    }

    public static Member decodeMembersFromFile(String path){
        // read the encrypted object from a file
        String encrypted = loadFromFile(path);

        if (encrypted == null) {
            System.out.println("[DEBUG] The file returned null bytes... Operation aborted.");
            return null;
        }

        // decrypt the object
        Key key = getKeyFromKeyStore();
        EncryptionHandler encryptionHandler = new EncryptionHandler(key);
        String decrypted = encryptionHandler.decrypt(encrypted);

        // convert the decrypted object to a Member object
        System.out.println("[DEBUG] Decrypted object: " + decrypted);
        return Member.fromString(decrypted);
    }

    public static String encryptId(int id) {
        Key key = getKeyFromKeyStore();
        EncryptionHandler encryptionHandler = new EncryptionHandler(key);
        return encryptionHandler.encrypt(String.valueOf(id));
    }
}
