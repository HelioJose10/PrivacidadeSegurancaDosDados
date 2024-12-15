import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;

public class EncryptionUtil {
    private static final String ALGORITHM = "AES";

    public static byte[] encrypt(byte[] data, SecretKey key) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    public static byte[] decrypt(byte[] data, SecretKey key) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    public static SecretKey generateKey() throws GeneralSecurityException {
        KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
        keyGen.init(128); // Tamanho da chave AES
        return keyGen.generateKey();
    }

    public static SecretKey loadKey(byte[] keyBytes) {
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }
}
