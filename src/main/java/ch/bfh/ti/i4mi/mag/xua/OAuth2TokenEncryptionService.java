package ch.bfh.ti.i4mi.mag.xua;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * A service that encrypts and decrypts OAuth2 tokens using AES-GCM.
 *
 * https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html#algorithms
 * https://developers.google.com/tink/encrypt-data?hl=en
 */
public class OAuth2TokenEncryptionService {
    private static final String ALGORITHM = "AES_128/GCM/NoPadding";
    private static final int GCM_LENGTH = 128;
    private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_DECODER = Base64.getDecoder();

    private final SecretKey key;

    public OAuth2TokenEncryptionService(final SecretKey key) {
        this.key = key;
    }

    public String encrypt(final String input) throws NoSuchPaddingException, NoSuchAlgorithmException,
                                                     InvalidAlgorithmParameterException, InvalidKeyException,
                                                     BadPaddingException, IllegalBlockSizeException {
        final GCMParameterSpec parameterSpec = this.generateParameterSpec();
        final var cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, this.key, parameterSpec);
        final byte[] cipherText = cipher.doFinal(input.getBytes());
        final String encrypt64 = BASE64_ENCODER.encodeToString(cipherText);
        final String iv64 = BASE64_ENCODER.encodeToString(parameterSpec.getIV());
        return String.format("%s#%s", encrypt64, iv64);
    }

    public String decrypt(final String cipherText) throws NoSuchPaddingException, NoSuchAlgorithmException,
                                                             InvalidAlgorithmParameterException, InvalidKeyException,
                                                             BadPaddingException, IllegalBlockSizeException {
        final String[] split = cipherText.split("#");
        final byte[] iv = BASE64_DECODER.decode(split[1]);
        final var parameterSpec = new GCMParameterSpec(GCM_LENGTH, iv);

        final var cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, this.key, parameterSpec);
        final byte[] plainText = cipher.doFinal(BASE64_DECODER.decode(split[0]));
        return new String(plainText);
    }

    public GCMParameterSpec generateParameterSpec() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return new GCMParameterSpec(GCM_LENGTH, iv);
    }
}
