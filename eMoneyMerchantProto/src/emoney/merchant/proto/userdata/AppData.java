package emoney.merchant.proto.userdata;

import java.security.SecureRandom;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import emoney.merchant.proto.crypto.AES256cipher;
import emoney.merchant.proto.crypto.Hash;
import emoney.merchant.proto.misc.Converter;

public class AppData {
	private final static String TAG = "{class} AppData";
	private SharedPreferences Pref;
	private Context ctx;
	private String IMEI;
	private long lIMEI;
	
	public AppData(Context context) {
		// TODO Auto-generated constructor stub
		ctx = context;
		Pref = PreferenceManager.getDefaultSharedPreferences(ctx);
	}

	public void setACCN(long lACCN) {
		// TODO Auto-generated method stub
		Editor edit = Pref.edit();
		edit.putLong("ACCN", lACCN);
		edit.commit();
	}
	
	public long getACCN() {
		// TODO Auto-generated method stub
		return Pref.getLong("ACCN", 0);
	}

	public void setIMEI() {
		TelephonyManager T = (TelephonyManager)ctx.getSystemService(Context.TELEPHONY_SERVICE);
		IMEI = T.getDeviceId();
		lIMEI = Long.parseLong(IMEI);
		Editor edit = Pref.edit();
		edit.putLong("HWID", lIMEI);
		edit.commit();
	}
	
	public long getIMEI(){
		return Pref.getLong("HWID", 0);
	}

	public void setPass(String newPass) {
		// TODO Auto-generated method stub
		
		//hash trus commit ke Pref
		String hashed = newPass.concat(String.valueOf(this.getIMEI()));
		Log.d(TAG,"Password to hashed:"+hashed);
		Editor edit = Pref.edit();
		edit.putString("Pass", Converter.byteArrayToHexString(Hash.sha256Hash(hashed)));
		edit.commit();
	}
	
	public String getPass(){
		return Pref.getString("Pass", "");
	}

	public void setLATS(long LATS) {
		// TODO Auto-generated method stub
		Editor edit = Pref.edit();
		edit.putLong("LATS", LATS);
		edit.commit();
	}
	
	public long getLATS(){
		return Pref.getLong("LATS", 0);
	}

	public void setBalance(int Balance, byte[] balance_key) {
		// TODO Auto-generated method stub
		
		//derive key
		//encrypt balance
		//commit ke sharedpref
		String ciphertext;
		byte[] encryptedBalanceArray = new byte[16];
		byte[] iv = new byte[16];
		byte[] balanceIv= new byte[32];

		SecureRandom random = new SecureRandom();		
		random.nextBytes(iv);
		
		System.arraycopy(iv, 0, balanceIv, 16, 16);
		
		try {
			encryptedBalanceArray = AES256cipher.encrypt(iv, balance_key, Converter.integerToByteArray(Balance));
			System.arraycopy(encryptedBalanceArray, 0, balanceIv, 0, 16);
			ciphertext = Converter.byteArrayToHexString(balanceIv);
			Log.d(TAG,"Balance to write to shared preferences:"+ciphertext);
			Editor edit = Pref.edit();
			edit.putString("Balance", ciphertext);
			edit.commit();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String getBalance(){
		return Pref.getString("Balance", "");
	}
	
	public int getDecryptedBalance(byte[] balance_key){
		
		//ambil dari Pref
		//derive key
		//decrypt balance
		//return balance
		String ciphertext = Pref.getString("Balance", "");
		if(ciphertext.isEmpty()) {
			Log.d(TAG,"Encrypted balance from shared preferences is empty");
			return -1;
		}
		
		byte[] ciphertextArray = Converter.hexStringToByteArray(ciphertext);
		if(ciphertextArray.length != 32) {
			Log.d(TAG,"Encrypted balance (in byte array) length is not 32");
			return -2;
		}
		
		if(balance_key.length != 32){
			Log.d(TAG,"Balance key not yet derived!!");
			return -3;
		}
		
		byte[] iv = new byte[16];
		System.arraycopy(ciphertextArray, 16, iv, 0, 16);
		Log.d(TAG,"IV:"+Converter.byteArrayToHexString(iv));
		
		byte[] encryptedBalance = new byte[16];
		System.arraycopy(ciphertextArray, 0, encryptedBalance, 0, 16);
		Log.d(TAG,"encrypted balance:"+Converter.byteArrayToHexString(encryptedBalance));
		Log.d(TAG,"balance key:"+Converter.byteArrayToHexString(balance_key));
		
		byte[] balance = new byte[16];
		
		try {
			balance = AES256cipher.decrypt(iv, balance_key, encryptedBalance);
			return Converter.byteArrayToInteger(balance);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e(TAG, e.toString());
		}
		
		Log.d(TAG,"WTF! What a terrible failure! Uncaught exception.");
		return -4;
	}
	
	public void setVerifiedBalance(int Balance, byte[] balance_key) {
		// TODO Auto-generated method stub
		
		//derive key
		//encrypt balance
		//commit ke sharedpref
		String ciphertext;
		byte[] encryptedBalanceArray = new byte[16];
		byte[] iv = new byte[16];
		byte[] balanceIv= new byte[32];
		
		SecureRandom random = new SecureRandom();		
		random.nextBytes(iv);
		
		System.arraycopy(iv, 0, balanceIv, 16, 16);
		
		try {
			encryptedBalanceArray = AES256cipher.encrypt(iv, balance_key, Converter.integerToByteArray(Balance));
			System.arraycopy(encryptedBalanceArray, 0, balanceIv, 0, 16);
			ciphertext = Converter.byteArrayToHexString(balanceIv);
			Log.d(TAG,"Balance to write to shared preferences:"+ciphertext);
			Editor edit = Pref.edit();
			edit.putString("verifiedBalance", ciphertext);
			edit.commit();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String getVerifiedBalance(){
		return Pref.getString("verifiedBalance", "");
	}
	
	public int getDecryptedVerifiedBalance(byte[] balance_key){
		
		//ambil dari Pref
		//derive key
		//decrypt balance
		//return balance
		String ciphertext = Pref.getString("verifiedBalance", "");
		if(ciphertext.isEmpty()) {
			Log.d(TAG,"Encrypted balance from shared preferences is empty");
			return -1;
		}
		
		byte[] ciphertextArray = Converter.hexStringToByteArray(ciphertext);
		if(ciphertextArray.length != 32) {
			Log.d(TAG,"Encrypted balance (in byte array) length is not 32");
			return -2;
		}
		
		if(balance_key.length != 32){
			Log.d(TAG,"Balance key not yet derived!!");
			return -3;
		}
		
		byte[] iv = new byte[16];
		System.arraycopy(ciphertextArray, 16, iv, 0, 16);
		Log.d(TAG,"IV:"+Converter.byteArrayToHexString(iv));
		
		byte[] encryptedBalance = new byte[16];
		System.arraycopy(ciphertextArray, 0, encryptedBalance, 0, 16);
		Log.d(TAG,"encrypted balance:"+Converter.byteArrayToHexString(encryptedBalance));
		Log.d(TAG,"balance key:"+Converter.byteArrayToHexString(balance_key));
		
		byte[] balance = new byte[16];
		
		try {
			balance = AES256cipher.decrypt(iv, balance_key, encryptedBalance);
			return Converter.byteArrayToInteger(balance);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e(TAG, e.toString());
		}
		
		Log.d(TAG,"WTF! What a terrible failure! Uncaught exception.");
		return -4;
	}
	
	public void deleteAppData(){
		Editor edit = Pref.edit();
		edit.clear();
		edit.commit();
	}

	public void setKey(byte[] aesKey, byte[] keyEncryption_key) {
		// TODO Auto-generated method stub
		// derive key
		// encrypt key
		// commit to pref
		
		Editor edit = Pref.edit();
		
		if(this.getIMEI() == 0){
			edit.putString("AESKEY", "FAIL! NO IMEI");
			edit.commit();
			return;
		}
		
		if(this.getPass().length() == 0){
			edit.putString("AESKEY", "FAIL! NO Password");
			edit.commit();
			return; 
		}
		
		if(keyEncryption_key.length == 32){
			SecureRandom random = new SecureRandom();
			byte[] iv = new byte[16];
			random.nextBytes(iv);
			
			try {
				byte[] wrappedKey = AES256cipher.wrapAES(keyEncryption_key, aesKey, iv);
				byte[] wrappedKeyIv = new byte[wrappedKey.length+iv.length];
				System.arraycopy(wrappedKey, 0, wrappedKeyIv, 0, wrappedKey.length);
				System.arraycopy(iv, 0, wrappedKeyIv, wrappedKeyIv.length - iv.length, iv.length);
				
				edit.putString("AESKEY", Converter.byteArrayToHexString(wrappedKeyIv));
				edit.commit();
				return;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			edit.putString("AESKEY", "FAIL! WTF! UNCAUGHT EXCEPTION!");
			edit.commit();
		}
	}
	
	public String getEncryptedKey(){
		return Pref.getString("AESKEY", "");
	}
	
	public byte[] getDecryptedKey(byte[] keyEncryption_key){
		String wrappedAll = Pref.getString("AESKEY", "");
		byte[] wrappedAllArr = Converter.hexStringToByteArray(wrappedAll);
		Log.d(TAG,"aeskey from sharedpref:"+wrappedAll);
		
		byte[] wrappedKey = new byte[wrappedAllArr.length - 16];
		System.arraycopy(wrappedAllArr, 0, wrappedKey, 0, wrappedAllArr.length-16);
		
		byte[] iv = new byte[16];
		System.arraycopy(wrappedAllArr, wrappedAllArr.length-16, iv, 0, 16);
		
		byte[] plainKey = new byte[32];
		
		try {
			plainKey = AES256cipher.unwrapAES(keyEncryption_key, wrappedKey, iv);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return plainKey;
	}
}
