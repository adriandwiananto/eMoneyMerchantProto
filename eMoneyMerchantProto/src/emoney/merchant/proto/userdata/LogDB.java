package emoney.merchant.proto.userdata;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import emoney.merchant.proto.crypto.AES256cipher;
import emoney.merchant.proto.misc.Converter;

public class LogDB extends SQLiteOpenHelper{
	protected static final String TAG = "{class} LogDB";
	private static String DBLNAME = "log";	
	private static String DBLCREATE = "CREATE TABLE TransLog (_id INTEGER PRIMARY KEY AUTOINCREMENT, _log BLOB NOT NULL)";
	private static String TABLE = "TransLog";
	private static String CL_ID = "_id";
	private static String CL_LOG = "_log";
	private byte[] accn_M =  new byte[0];
	
	private byte[] logKey;
	
	/**
	 * Use this constructor if you want to call insertLastTransToLog method without known merchant ACCN  
	 * @param c
	 * @param log_key
	 */
	public LogDB(Context c, byte[] log_key){
		super(c, DBLNAME, null, 1);
		logKey = log_key;
	}
	
	/**
	 * Use this constructor if you want to call insertLastTransToLog method with known merchant ACCN
	 * @param c
	 * @param log_key
	 * @param accnMerchant
	 */
	public LogDB(Context c, byte[] log_key, byte[] accnMerchant){
		super(c, DBLNAME, null, 1);
		logKey = log_key;
		accn_M = accnMerchant;
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(DBLCREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * insert last transaction to log
	 * @param transPacket transaction data packet with unencrypted payload
	 * @return row number in database
	 */
	public long insertLastTransToLog(byte[] transPacket){
		Log.d(TAG,"transaction packet: "+Converter.byteArrayToHexString(transPacket));
		SQLiteDatabase db = getWritableDatabase();
		ContentValues CV =  new ContentValues();
		
		long logNum = this.rowCountLog() + 1;
		byte[] num = new byte[3];
		System.arraycopy(ByteBuffer.allocate(8).putLong(logNum).array(), 5, num, 0, 3);
		byte payloadType = transPacket[1];
		byte[] binaryID = new byte[4];
		Arrays.fill(binaryID, (byte) 0);
		byte[] accnM = new byte[6];
		if(accn_M.length == 0){
			Arrays.fill(accnM, (byte) 0);
		} else {
			System.arraycopy(accn_M, 0, accnM, 0, 6);
		}
		byte[] accnP = new byte[6];
		System.arraycopy(transPacket, 7, accnP, 0, 6);
		byte[] amount = new byte[4];
		System.arraycopy(transPacket, 17, amount, 0, 4);
		byte[] timeStamp = new byte[4];
		System.arraycopy(transPacket, 13, timeStamp, 0, 4);
		byte status = 0;
		byte cancel = 0;
		
		byte[] plainLog = new byte[30];
		ByteBuffer target = ByteBuffer.wrap(plainLog);
		target.put(num);
		target.put(payloadType);
		target.put(binaryID);
		target.put(accnM);
		target.put(accnP);
		target.put(amount);
		target.put(timeStamp);
		target.put(status);
		target.put(cancel);
		Log.d(TAG,"plain log:"+Converter.byteArrayToHexString(plainLog));

		String ciphertext;
		byte[] encryptedLogArray = new byte[32];
		byte[] iv = new byte[16];
		byte[] logIv= new byte[48];

		SecureRandom random = new SecureRandom();		
		random.nextBytes(iv);
		
		System.arraycopy(iv, 0, logIv, 32, 16);
		
		try {
			encryptedLogArray = AES256cipher.encrypt(iv, logKey, plainLog);
			System.arraycopy(encryptedLogArray, 0, logIv, 0, 32);
			ciphertext = Converter.byteArrayToHexString(logIv);
			Log.d(TAG,"Log to write to database:"+ciphertext);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		}
		
		CV.put(CL_LOG, logIv);
		long rowid = db.insert(TABLE, null, CV);
		//db.close();
		return rowid;
	}
	
	/**
	 * @return number of row in database
	 */
	private long rowCountLog(){
		SQLiteDatabase db = getReadableDatabase();
		return DatabaseUtils.queryNumEntries(db, TABLE);
	}
	
	/**
	 * @return cursor to first index in log column
	 */
	public Cursor getLogBlob(){
		SQLiteDatabase db = getReadableDatabase();
		Cursor c = db.query(TABLE, new String[]{CL_ID, CL_LOG}, null, null, null, null, CL_ID);
		if (c != null) c.moveToFirst();
		return c;
	}
	
	public class LogOperation{
		private boolean error;
		private final static String TAG = "{SubClass} LogOperation";
		
		/**
		 * Subclass constructor for log operation
		 */
		public LogOperation(){
			error = false;
		}
		
		/**
		 * get decrypted log in row pointed by Cursor cur
		 * @param cur
		 * @param log_key
		 * @return decrypted log
		 */
//		public byte[] getDecrpytedLogPerRow(Cursor cur, byte[] log_key){
		public byte[] getDecrpytedLogPerRow(Cursor cur){
			byte[] encryptedLog = cur.getBlob(cur.getColumnIndex(CL_LOG));
			Log.d(TAG,"encryptedLog: "+Converter.byteArrayToHexString(encryptedLog));
			byte[] iv = Arrays.copyOfRange(encryptedLog, 32, 48);
			byte[] logOnly = Arrays.copyOfRange(encryptedLog, 0, 32);
			
			byte[] decryptedLog = new byte[32];
			
			try{
				//decryptedLog = AES256cipher.decrypt(iv, log_key, logOnly);
				decryptedLog = AES256cipher.decrypt(iv, logKey, logOnly);
				Log.d(TAG,"decryptedLog: "+Converter.byteArrayToHexString(decryptedLog));
			} catch (Exception e) {
				e.printStackTrace();
				Log.d(TAG,"exception thrown by decrypt log per row method");
				error = true;
			}
			
			return decryptedLog;
		}
		
		/**
		 * change log key to newLogKey
		 * @param newLogKey
		 * @return true if success, otherwise returns false
		 */
		public boolean changeLogKey(byte[] newLogKey){
			SQLiteDatabase db = getWritableDatabase();
			ContentValues CV =  new ContentValues();

			Cursor c = db.query(TABLE, new String[]{CL_ID, CL_LOG}, null, null, null, null, CL_ID);
			int totalRow = 0;
			if(c != null){
				totalRow = c.getCount();
				Log.d(TAG,"total row:"+totalRow);
				
				byte[][] newLogIv = new byte[totalRow][48];
				c.moveToFirst();
				while(c.isAfterLast() == false){
					int rowNum = c.getInt(c.getColumnIndex(CL_ID));
					Log.d(TAG,"current operation on row:"+rowNum);
					try{
//						byte[] decryptedLog = this.getDecrpytedLogPerRow(c, logKey);
						byte[] decryptedLog = this.getDecrpytedLogPerRow(c);
						if(error == true){
							Log.d(TAG,"error decrypt");
							return false;
						}
						
						byte[] newEncryptedLog = new byte[32];
						byte[] newIv = new byte[16];

						SecureRandom random = new SecureRandom();		
						random.nextBytes(newIv);
						
						System.arraycopy(newIv, 0, newLogIv[rowNum-1], 32, 16);
						
						newEncryptedLog = AES256cipher.encrypt(newIv, newLogKey, decryptedLog);
						System.arraycopy(newEncryptedLog, 0, newLogIv[rowNum-1], 0, 32);
						Log.d(TAG,"new log key:"+Converter.byteArrayToHexString(newLogKey));
						Log.d(TAG,"newly encrypted log:"+Converter.byteArrayToHexString(newLogIv[rowNum-1]));

						c.moveToNext();
						
					} catch (Exception e) {
						e.printStackTrace();
						Log.d(TAG,"exception thrown by change log key method");
						return false;
					}
				}
				
				int index = 0;
				for(index = 0; index < totalRow; index = index+1){
					CV.put(CL_LOG, newLogIv[index]);
					db.update(TABLE, CV, CL_ID + "=" + (index+1), null);
				}
				return true;
			} else {
				return false;
			}
		}
		
		public boolean getError(){
			return error;
		}
	}
	
	public static void deleteDB(Context c){
		c.deleteDatabase(DBLNAME);
	}
	
	public String getLOGColumnName(){
		return CL_LOG;
	}

	public String getIDColumnName(){
		return CL_ID;
	}
}
