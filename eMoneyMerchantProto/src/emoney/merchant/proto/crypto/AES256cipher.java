package emoney.merchant.proto.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AES256cipher {
	
	
	/**
	 * Encrypt textBytes using keyBytes and ivBytes
	 * <br>Encryption algorithm used is AES256 Cipher Block Chaining (CBC)
	 * @param ivBytes Initialization Vector (16 bytes)
	 * @param keyBytes AES key (32 bytes)
	 * @param textBytes Data to encrypt (do not write padding manually, padding will be added automatically)
	 * @return Encrypted textBytes with size multiple of 16 bytes
	 */
	public static byte[] encrypt(byte[] ivBytes, byte[] keyBytes, byte[] textBytes) 
			throws java.io.UnsupportedEncodingException, 
				NoSuchAlgorithmException,
				NoSuchPaddingException,
				InvalidKeyException,
				InvalidAlgorithmParameterException,
				IllegalBlockSizeException,
				BadPaddingException {
		
		AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivBytes);
    	SecretKeySpec newKey = new SecretKeySpec(keyBytes, "AES");
    	Cipher cipher = null;
		cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, newKey, ivSpec);
		return cipher.doFinal(textBytes);
	}

	/**
	 * Decrypt textBytes using keyBytes and ivBytes
	 * <br>Decryption algorithm used is AES256 Cipher Block Chaining (CBC)
	 * @param ivBytes Initialization Vector (16 bytes)
	 * @param keyBytes AES key (32 bytes)
	 * @param textBytes Data to encrypt (size multiple of 16 bytes)
	 * @return Decrypted textBytes
	 */
	public static byte[] decrypt(byte[] ivBytes, byte[] keyBytes, byte[] textBytes) 
			throws java.io.UnsupportedEncodingException, 
			NoSuchAlgorithmException,
			NoSuchPaddingException,
			InvalidKeyException,
			InvalidAlgorithmParameterException,
			IllegalBlockSizeException,
			BadPaddingException {
		
		AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivBytes);
		SecretKeySpec newKey = new SecretKeySpec(keyBytes, "AES");
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.DECRYPT_MODE, newKey, ivSpec);
		return cipher.doFinal(textBytes);
	}

	/**
	 * Wrap AES key using key wrap method from java crypto
	 * <br>Key wrapping algorithm used is RFC3394
	 * @param keyWrapper Key to wrap AES key
	 * @param aesKeyToBeWrapped AES key to be wrapped
	 * @param iv Initialization vector
	 * @return Wrapped AES key (size is AES key length + 8)
	 */
	public static byte[] wrapAES( byte[] keyWrapper, byte[] aesKeyToBeWrapped, byte[] iv) throws Exception
	{
		SecretKey key = new SecretKeySpec(keyWrapper, "AES");
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
		IvParameterSpec ivParams = new IvParameterSpec(iv);
		
		SecretKey keyToBeWrapped = new SecretKeySpec(aesKeyToBeWrapped, "AES");
		
		cipher.init(Cipher.WRAP_MODE, key, ivParams);
		return cipher.wrap(keyToBeWrapped);
	}
	
	/**
	 * Unwrap AES key using key wrap method from java crypto
	 * <br>Key wrapping algorithm used is RFC3394
	 * @param keyWrapper Key to unwrap AES key
	 * @param wrappedKey AES key to be unwrapped
	 * @param iv Initialization vector
	 * @return Unwrapped AES key
	 */
	public static byte[] unwrapAES( byte[] keyWrapper, byte[] wrappedKey, byte[] iv) throws Exception
	{
		SecretKey key = new SecretKeySpec(keyWrapper, "AES");
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
		IvParameterSpec ivParams = new IvParameterSpec(iv);

		cipher.init(Cipher.UNWRAP_MODE, key, ivParams);
		Key keyUnwrapped = cipher.unwrap(wrappedKey, "AES", Cipher.SECRET_KEY);
		return keyUnwrapped.getEncoded();
	}
}
