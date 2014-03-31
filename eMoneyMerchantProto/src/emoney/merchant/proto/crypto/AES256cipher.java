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
	
	public static byte[] wrapAES( byte[] keyWrapper, byte[] aesKeyToBeWrapped, byte[] iv) throws Exception
	{
		SecretKey key = new SecretKeySpec(keyWrapper, "AES");
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
		IvParameterSpec ivParams = new IvParameterSpec(iv);
		
		SecretKey keyToBeWrapped = new SecretKeySpec(aesKeyToBeWrapped, "AES");
		
		cipher.init(Cipher.WRAP_MODE, key, ivParams);
		return cipher.wrap(keyToBeWrapped);
	}
	
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
