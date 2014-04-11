package emoney.merchant.proto.misc;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.Arrays;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.util.Log;
import emoney.merchant.proto.crypto.AES256cipher;

public class Packet {

	private final static String TAG = "{class} Packet";
	
	private int Amount;
	private int SESN;
	private int TS;
	private long ACCN;
	private long LATS;
	private byte[] aesKey;
	private byte[] plainPacket;
	private byte[] cipherPayload;
	
	private int mode = 0;
	private final int SEND_TO_NFC_READER = 10;
	private final int SEND_TO_NFC_PHONE = 20;
	private final int RECEIVE_MODE = 30;
	
	/**
	 * USE THIS CONSTRUCTOR TO PARSE RECEIVED DATA PACKET
	 * @param aes_key AES key
	 */
	public Packet(byte[] aes_key){
		aesKey = aes_key;
		mode = RECEIVE_MODE;
	}
	
	/**
	 * USE THIS CONSTRUCTOR TO CREATE DATA PACKET FOR TRANSACTION WITH NFC READER
	 * @param amount
	 * @param sesn
	 * @param accn
	 * @param lastTS
	 * @param aes_key
	 */
	public Packet(int amount, int sesn, long accn, long lastTS, byte[] aes_key) {
		// TODO Auto-generated constructor stub
		Amount = amount;
		SESN = sesn;
		ACCN = accn;
		LATS = lastTS;
		aesKey = aes_key;
		mode = SEND_TO_NFC_READER;
	}
	
	/**
	 * USE THIS CONSTRUCTOR TO CREATE DATA PACKET FOR TRANSACTION WITH NFC PHONE
	 * @param amount
	 * @param sesn
	 * @param timestamp
	 * @param accn
	 * @param lastTS
	 * @param aes_key
	 */
	public Packet(int amount, int sesn, int timestamp, long accn, long lastTS, byte[] aes_key) {
		// TODO Auto-generated constructor stub
		Amount = amount;
		SESN = sesn;
		ACCN = accn;
		LATS = lastTS;
		aesKey = aes_key;
		TS = timestamp;
		mode = SEND_TO_NFC_PHONE;
	}

	/**
	 * method for building transaction data packet
	 * @return transaction data packet
	 */
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
		trans[2] = 1;	//mercchant (1)
		System.arraycopy(ByteBuffer.allocate(4).putInt(intSESN).array(), 2, trans, 3, 2); //SESN (2)
		trans[5] = 0; //EH (2)
		trans[6] = 0; //EH
		System.arraycopy(ByteBuffer.allocate(8).putLong(longACCN).array(),2, trans, 7, 6); //ACCN(6)
		if(mode == SEND_TO_NFC_READER){
			System.arraycopy(ByteBuffer.allocate(8).putLong(System.currentTimeMillis()/1000).array(),4, trans, 13, 4); //TS(4)
		} else {
			System.arraycopy(Converter.integerToByteArray(TS), 0, trans, 13, 4);
		}
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
			cipherPayload = ciphertext;
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

	/**
	 * create NDEF message with only single NDEF record consisting mime type and payload byte
	 * @param mime
	 * @param payload
	 * @return
	 */
	public NdefMessage createNDEFMessage(String mime, byte[] payload) {
		byte[] mimeb = mime.getBytes(Charset.forName("US-ASCII"));
		NdefRecord mrec = new NdefRecord(NdefRecord.TNF_MIME_MEDIA, mimeb, new byte[0], payload);
		NdefMessage msg = new NdefMessage(new NdefRecord[]{mrec});
		return msg;
	}
	
	/**
	 * Getter method for transaction data packet with unencrypted payload
	 * <br>Call buildTransPacket method first!!
	 * @return transaction data packet with unencrypted payload
	 */
	public byte[] getPlainPacket(){
		return plainPacket;
	}
	
	/**
	 * Getter method for encrypted payload (without header and IV)
	 * <br>Call buildTransPacket method first!!
	 * @return encrypted payload
	 */
	public byte[] getCipherPayload(){
		return cipherPayload;
	}
	
	public class ParseReceivedPacket {
		private byte receivedFL;
		private byte receivedPT;
		private byte receivedFF;
		private byte[] receivedSesnHeader = new byte[2];
		private byte[] receivedEH = new byte[4];
		private byte[] receivedACCN = new byte[6];
		private byte[] receivedTS = new byte[4];
		private byte[] receivedAMNT = new byte[4];
		private byte[] receivedLATS = new byte[4];
		private byte[] receivedSesnPayload = new byte[2];
		private byte[] receivedIV = new byte[16];
		
		private byte[] receivedPlainPacket = new byte[55]; 

		private int errorCode = 0;
		private String[] errorMsg = {"","received packet length is not 55","decrypt function throw exception","decrypt ok, but result is wrong!"};
		
		/**
		 * CONSTRUCTOR FOR PARSING RECEIVED PACKET SUBCLASS
		 * How to call: ParseReceivedPacket prp = new Packet(aes_key).new ParseReceivedPacket(receivedPacket)
		 * @param receivedPacket
		 */
		public ParseReceivedPacket(byte[] receivedPacket){
			if(receivedPacket.length != 55){
				errorCode = 1;
			} else {
				receivedFL = receivedPacket[0];
				receivedPT = receivedPacket[1];
				receivedFF = receivedPacket[2];
				receivedSesnHeader = Arrays.copyOfRange(receivedPacket, 3, 5);
				receivedEH = Arrays.copyOfRange(receivedPacket, 5, 7);
				receivedIV = Arrays.copyOfRange(receivedPacket, 39, 55);
	
				byte[] receivedPayload = Arrays.copyOfRange(receivedPacket, 7, 39);
				
				byte[] decryptedPayload = new byte[32];
				
				try{
					decryptedPayload = AES256cipher.decrypt(receivedIV, aesKey, receivedPayload);
					Log.d(TAG,"decryptedPayload: "+Converter.byteArrayToHexString(decryptedPayload));
					
					receivedACCN = Arrays.copyOfRange(decryptedPayload, 0, 6);
					receivedTS = Arrays.copyOfRange(decryptedPayload, 6, 10);
					receivedAMNT = Arrays.copyOfRange(decryptedPayload, 10, 14);
					receivedLATS = Arrays.copyOfRange(decryptedPayload, 14, 18);
					receivedSesnPayload = Arrays.copyOfRange(decryptedPayload, 18, 20);
					
					Log.d(TAG,"receiveid ACCN: "+Converter.byteArrayToHexString(receivedACCN)
							+"\nreceived TS: "+Converter.byteArrayToHexString(receivedTS)
							+"\nreceived AMNT: "+Converter.byteArrayToHexString(receivedAMNT)
							+"\nreceived LATS: "+Converter.byteArrayToHexString(receivedLATS)
							+"\nreceived SESN Header: "+Converter.byteArrayToHexString(receivedSesnHeader)
							+"\nreceived SESN Payload: "+Converter.byteArrayToHexString(receivedSesnPayload));
					
					if(Arrays.equals(receivedSesnHeader, receivedSesnPayload) == false){
						errorCode = 3;
					}
					
					System.arraycopy(receivedPacket, 0, receivedPlainPacket, 0, 7);
					System.arraycopy(decryptedPayload, 0, receivedPlainPacket, 7, decryptedPayload.length);
					Arrays.fill(receivedPlainPacket, 7+decryptedPayload.length, 39, (byte) (32-decryptedPayload.length));
					System.arraycopy(receivedIV, 0, receivedPlainPacket, 39, 16);
					Log.d(TAG,"received plain packet: "+ Converter.byteArrayToHexString(receivedPlainPacket));
				} catch (Exception e) {
					e.printStackTrace();
					Log.d(TAG,"exception thrown by decrypt log per row method");
					errorCode = 2;
				}
			}
		}
		
		/**
		 * after construct this subclass, you should see if there's an error happened during construction
		 * @return 0 means no error, > 0  means error happened
		 */
		public int getErrorCode(){
			return errorCode;
		}
		
		/**
		 * Error message that relevant with error code
		 * @return error message
		 */
		public String getErrorMsg(){
			return errorMsg[errorCode];
		}
		
		/**
		 * get Frame Length field in received packet
		 * <br>ONLY CALL WHEN ERROR CODE = 0
		 * @return frame length
		 */
		public byte getReceivedFL(){
			return receivedFL;
		}
		
		/**
		 * get Payload Type field in received packet
		 * <br>ONLY CALL WHEN ERROR CODE = 0
		 * @return payload type
		 */
		public byte getReceivedPT(){
			return receivedPT;
		}
		
		/**
		 * get Frame Field field in received packet
		 * <br>ONLY CALL WHEN ERROR CODE = 0
		 * @return frame field
		 */
		public byte getReceivedFF(){
			return receivedFF;
		}
		
		/**
		 * get SESN field in received packet
		 * <br>ONLY CALL WHEN ERROR CODE = 0
		 * @return SESN
		 */
		public byte[] getReceivedSESN(){
			if(Arrays.equals(receivedSesnHeader, receivedSesnPayload) == false){
				return new byte[2];
			}
			return receivedSesnPayload;
		}
		
		/**
		 * get ACCN field in received packet
		 * <br>ONLY CALL WHEN ERROR CODE = 0
		 * @return ACCN
		 */
		public byte[] getReceivedACCN(){
			return receivedACCN;
		}

		/**
		 * get timestamp field in received packet
		 * <br>ONLY CALL WHEN ERROR CODE = 0
		 * @return timestamp
		 */
		public byte[] getReceivedTS(){
			return receivedTS;
		}
		
		/**
		 * get amount field in received packet
		 * <br>ONLY CALL WHEN ERROR CODE = 0
		 * @return amount
		 */
		public byte[] getReceivedAMNT(){
			return receivedAMNT;
		}
		
		/**
		 * get last transaction timestamp field in received packet
		 * <br>ONLY CALL WHEN ERROR CODE = 0
		 * @return last transaction timestamp
		 */
		public byte[] getReceivedLATS(){
			return receivedLATS;
		}
		
		/**
		 * get received packet in plain form (header+decrypted payload+iv)
		 * @return received packet with plain payload
		 */
		public byte[] getReceivedPlainPacket(){
			return receivedPlainPacket;
		}
	}
}
