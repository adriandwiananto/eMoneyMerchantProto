package emoney.merchant.proto.misc;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.Arrays;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import emoney.merchant.proto.crypto.AES256cipher;

public class Packet {

	private int Amount;
	private int SESN;
	private long ACCN;
	private long LATS;
	private byte[] aesKey;
	private byte[] plainPacket;
	private byte[] cipherPacket;
	
	public Packet(int amount, int sesn, long accn, long lastTS, byte[] aes_key) {
		// TODO Auto-generated constructor stub
		Amount = amount;
		SESN = sesn;
		ACCN = accn;
		LATS = lastTS;
		aesKey = aes_key;
	}

	public byte[] buildTransPacket() {
		// TODO Auto-generated method stub
		int intAmount = Amount;
		int intSESN = SESN;
		long longACCN = ACCN;
		long lLATS = LATS;
		byte[] trans = new byte[55];
		
		byte[] randomIV = new byte[16];
		SecureRandom random = new SecureRandom();		
		random.nextBytes(randomIV);

		trans[0] = 55;	//Frame Length (1)
		trans[1] = 1;	//offline (1)
		trans[2] = 0;	//payer (1)
		System.arraycopy(ByteBuffer.allocate(4).putInt(intSESN).array(), 2, trans, 3, 2); //SESN (2)
		trans[5] = 0; //EH (2)
		trans[6] = 0; //EH
		System.arraycopy(ByteBuffer.allocate(8).putLong(longACCN).array(),2, trans, 7, 6); //ACCN(6)
		System.arraycopy(ByteBuffer.allocate(8).putLong(System.currentTimeMillis()/1000).array(),4, trans, 13, 4); //TS(4)
		System.arraycopy(ByteBuffer.allocate(4).putInt(intAmount).array(), 0, trans, 17, 4); //Amount(4)
		System.arraycopy(ByteBuffer.allocate(8).putLong(lLATS).array(),4, trans, 21, 4); //LATS(4)
		System.arraycopy(ByteBuffer.allocate(4).putInt(intSESN).array(), 2, trans, 25, 2); //SESN(2)
		Arrays.fill(trans, 27, 39, (byte) 12); //PAD(12)
		System.arraycopy(randomIV, 0, trans, 39, 16);
		
		plainPacket = trans;
		
		byte[] encAES = new byte[20];
		System.arraycopy(trans, 7, encAES, 0, 20);
		
//		byte[] keyAES = Converter.hexStringToByteArray(aesKey);
		byte[] keyAES = aesKey;
		
		byte[] ciphertext = null;
		try {
			ciphertext = AES256cipher.encrypt(randomIV, keyAES, encAES);
			cipherPacket = ciphertext;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		byte[] decryptedCiphertext = null;
		try {
			decryptedCiphertext = AES256cipher.decrypt(randomIV, keyAES, ciphertext);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		byte[] transFinal = new byte[55]; 
		if(Arrays.equals(encAES, decryptedCiphertext)) {
			System.arraycopy(trans, 0, transFinal, 0, 7);
			System.arraycopy(ciphertext, 0, transFinal, 7, 32);
			System.arraycopy(randomIV, 0, transFinal, 39, 16);
		}
		
		return transFinal;
	}

	public NdefMessage createNDEFMessage(String mime, byte[] payload) {
		byte[] mimeb = mime.getBytes(Charset.forName("US-ASCII"));
		NdefRecord mrec = new NdefRecord(NdefRecord.TNF_MIME_MEDIA, mimeb, new byte[0], payload);
		NdefMessage msg = new NdefMessage(new NdefRecord[]{mrec});
		return msg;
	}
	
	public byte[] getPlainPacket(){
		return plainPacket;
	}
	
	public byte[] getCipherPacket(){
		return cipherPacket;
	}
}
