package emoney.merchant.proto.crypto;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import android.util.Log;
import emoney.merchant.proto.misc.Converter;

public class KeyDerive {
	private int keyLength;
	private byte[] balance_key, log_key, keyEncryption_key;
	private final static String TAG = "{class} KeyDerive";
	
	public KeyDerive(){
		keyLength = 256;
	}
	
	private byte[] pbkdf2Derive(String password, String salt, int iteration){
		KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), iteration, keyLength);
		SecretKeyFactory keyFactory;
		byte[] keyBytes = new byte[32];
		try {
			keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
			keyBytes = keyFactory.generateSecret(keySpec).getEncoded();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return keyBytes;
	}
	
	public void deriveKey(String password, String IMEI){
		String salt = IMEI;
		Log.d(TAG,"Start deriving key");
		balance_key = pbkdf2Derive(password, salt, 800);
		Log.d(TAG,"balancekey:"+Converter.byteArrayToHexString(balance_key));
		
		log_key = pbkdf2Derive(password, salt, 900);
		Log.d(TAG,"logkey:"+Converter.byteArrayToHexString(log_key));
		
		keyEncryption_key = pbkdf2Derive(password, salt, 1000);
		Log.d(TAG,"transkey:"+Converter.byteArrayToHexString(keyEncryption_key));
		Log.d(TAG,"Finish deriving key. Check the time!");
	}
	
	public byte[] getBalanceKey(){
		return balance_key;
	}

	public byte[] getLogKey(){
		return log_key;
	}
	
	public byte[] getKeyEncryptionKey(){
		return keyEncryption_key;
	}
}
