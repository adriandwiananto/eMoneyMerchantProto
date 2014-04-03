package emoney.merchant.proto.misc;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import emoney.merchant.proto.Login;
import emoney.merchant.proto.R;
import emoney.merchant.proto.crypto.Hash;
import emoney.merchant.proto.crypto.KeyDerive;
import emoney.merchant.proto.userdata.AppData;
import emoney.merchant.proto.userdata.LogDB;
import emoney.merchant.proto.userdata.LogDB.LogOperation;

public class Network extends AsyncTask<Void, Void, JSONObject> {

	private final static String TAG = "{class} Network";
	private final static int REGISTRATION_MODE = 99;
	private final static int LOG_SYNC_MODE = 49;
	private final static String REG_SERVER = "http://emoney-server.herokuapp.com/register.json";
	private final static String SYNC_SERVER = "http://emoney-server.herokuapp.com/sync.json";
			
	private String hostname;
	private String data,header,logs;
	private String newPassword, newACCN, newIMEI;
	
	private JSONObject jobj_response;
	
	private int param_mode;
	private int error;
	private String errorMessage;
	
	private Context ctx;
	private AppData appdata;
	private KeyDerive key;
	private Activity parentActivity;
	
	private byte[] keyEncryption_key, log_key, balance_key;
	
	/**
	 * USE THIS CONSTRUCTOR TO SEND HTTP POST REGISTRATION DATA
	 * @param parent parent activity
	 * @param context caller context
	 * @param jobj JSON object of registration data
	 * @param NewPass new password inputted in registration activity
	 * @param ACCNtoSend ACCN inputted in registration activity
	 * @param HWID phone IMEI
	 */
	public Network(Activity parent, Context context, JSONObject jobj, String NewPass, String ACCNtoSend, String HWID){
		ctx = context;
		parentActivity = parent;
		hostname = REG_SERVER;
		newPassword = NewPass;
		newACCN = ACCNtoSend;
		newIMEI = HWID;
		key = new KeyDerive();
		error = 0;
		
		param_mode = REGISTRATION_MODE;
		appdata = new AppData(ctx);
		
		data = jobj.toString();
	}
	
	/**
	 * USE THIS CONSTRUCTOR TO SEND HTTP POST LOG SYNCHRONIZATION DATA
	 * @param parent parent activity
	 * @param context caller context
	 * @param keyEncryptionKey key encryption key
	 * @param logKey log key
	 * @param balanceKey balance key
	 */
	public Network(Activity parent, Context context, byte[] keyEncryptionKey, byte[] logKey, byte[] balanceKey) {
		// TODO Auto-generated constructor stub
		ctx = context;
		parentActivity = parent;
		keyEncryption_key = keyEncryptionKey;
		log_key = logKey;
		balance_key = balanceKey;
		param_mode = LOG_SYNC_MODE;
		hostname = SYNC_SERVER;
		error = 0;
		
		appdata = new AppData(ctx);
	}

	@Override
	protected JSONObject doInBackground(Void... params) {
		try {
			//HTTP POST preparation
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost(hostname);

			// Add data
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			if(param_mode == LOG_SYNC_MODE){
				//build JSON object for sync mode
				LogDB ldb = new LogDB(ctx, log_key);
				Cursor cur = ldb.getLogBlob();
				JSONArray jarray_logs = new JSONArray();
				JSONObject jobj_header = new JSONObject();
				int logNum = cur.getCount();
				int balance = 0;
				
				//decrypt and parse all log, then create json object of each row
				//JSON object for each row will be added to JSON array of SYNC logs
				while((cur.isAfterLast() == false) && (error == 0)) {
					//rowNum = log row number pointed by cursor cur
					int rowNum = cur.getInt(cur.getColumnIndex(ldb.getIDColumnName()));

    				LogOperation lo = ldb.new LogOperation();
//    				byte[] decryptedLog = lo.getDecrpytedLogPerRow(cur, log_key);
    				byte[] decryptedLog = lo.getDecrpytedLogPerRow(cur);
    				if(lo.getError() == true) {
    					error = 1;
    				}
    				
    				if(error == 0){
    					//parse decrypted log to it's respective field
    					byte[] NUM = Arrays.copyOfRange(decryptedLog, 0, 3);
    					byte PT = decryptedLog[3];
    					byte[] binaryID = Arrays.copyOfRange(decryptedLog, 4, 8);
    					byte[] accnM = Arrays.copyOfRange(decryptedLog, 8, 14);
    					byte[] accnP = Arrays.copyOfRange(decryptedLog, 14, 20);
    					byte[] amnt = Arrays.copyOfRange(decryptedLog, 20, 24);
    					byte[] TS = Arrays.copyOfRange(decryptedLog, 24, 28);
    					byte STAT = decryptedLog[28];
    					byte CNL = decryptedLog[29];
    					
    					balance = balance + Converter.byteArrayToInteger(amnt);
						
						//create JSON object from previously parsed log
    					//put newly created JSON object into JSON array
    					int array_index = rowNum - 1;
    					JSONObject json = new JSONObject();
    					
    					try {
    						json.put("NUM", Converter.byteArrayToInteger(NUM));
    						json.put("PT", (int)PT);
    						json.put("Binary ID", Converter.byteArrayToInteger(binaryID));
    						json.put("ACCN-M", Converter.byteArrayToLong(accnM));
    						json.put("ACCN-P", Converter.byteArrayToLong(accnP));
    						json.put("AMNT", Converter.byteArrayToInteger(amnt));
    						json.put("TS", Converter.byteArrayToInteger(TS));
    						json.put("STAT", (int)STAT);
    						json.put("CNL", (int)CNL);
    						jarray_logs.put(array_index, json);
    					} catch (Exception e) {
    						e.printStackTrace();
    						error = 2;
    					}
    				}
    				cur.moveToNext();
				}
				
				//create JSON object of SYNC header
				if(error == 0) {
					try {
						jobj_header.put("ACCN", appdata.getACCN());
						jobj_header.put("HWID", appdata.getIMEI());
						jobj_header.put("numOfLog", logNum);
						String logs_hash = Converter.byteArrayToHexString(Hash.sha256Hash(jarray_logs.toString()));
						jobj_header.put("signature", logs_hash);
						jobj_header.put("last_sync_at", appdata.getLATS());
						jobj_header.put("balance", balance);
					} catch (Exception e) {
						e.printStackTrace();
						error = 3;
					}
				}
				
				//add JSON object of SYNC header to header param of HTTP POST
				//add JSON array of SYNC logs to logs param of HTTP POST
				nameValuePairs.add(new BasicNameValuePair("header", jobj_header.toString()));
				nameValuePairs.add(new BasicNameValuePair("logs", jarray_logs.toString()));
				
				//debugging purpose
				//create JSON object consisting JSON object of SYNC header and JSON array of SYNC logs
				//print to logcat
				JSONObject jobj_print = new JSONObject();
				jobj_print.put("header", jobj_header);
				jobj_print.put("logs", jarray_logs);
				Log.d(TAG,"sync http post param:\n"+jobj_print.toString());
			}
			else if(param_mode == REGISTRATION_MODE){
				//add JSON object of REGISTRATION to data param of HTTP POST
				nameValuePairs.add(new BasicNameValuePair("data", data));
			}
			else{
				error = 5; //unknown param_mode
				return null;
			}
			
			//add name value pairs List to HttpPost 
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			// Execute HTTP Post Request
			HttpResponse response = httpclient.execute(httppost);
			
			// Get response and return it in JSON Object type
			BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
			String json = reader.readLine();
			JSONTokener tokener = new JSONTokener(json);
			JSONObject finalResult = new JSONObject(tokener);
			Log.d(TAG, "return:"+finalResult.toString());
			
			return finalResult;
		} catch (Exception e) {
			e.printStackTrace();
			Log.d(TAG,"error:"+e.getMessage());
			error = 1;
			return null;
		}
	}

	@Override
	protected void onPreExecute() {
		//Toast this message before doInBackground starts
		if(param_mode == REGISTRATION_MODE){
			Toast.makeText(ctx, "Registration Starts", Toast.LENGTH_SHORT).show();	
		} else {
			Toast.makeText(ctx, "Sync Starts", Toast.LENGTH_SHORT).show();
		}
	}
	 
	@Override
	protected void onPostExecute(JSONObject result) {
		//this method will be called after doInBackground finished
		int returnBalance = 0;
		int returnTS = 0;
		boolean returnRenewKey = false;
		String returnNewKey = "";
		
		if (error == 0) {
			jobj_response = result;

			if(param_mode == REGISTRATION_MODE){
				String responseStatus;
				try {
					responseStatus = jobj_response.getString("result");
					
					//if result from HTTP POST response error
					if((responseStatus.compareTo("Error") == 0) || (responseStatus.compareTo("error") == 0)){
						String errorMessage = jobj_response.getString("message");
						Toast.makeText(ctx, "Registration failed!! "+errorMessage, Toast.LENGTH_LONG).show();
						appdata.deleteAppData();
						error = 3;
					} else { 
						//if no error
						//get aes key from HTTP POST response and convert it to byte array
						String responseKey = jobj_response.getString("key");
						byte[] aesKey = new byte[responseKey.length()/2];
						aesKey = Converter.hexStringToByteArray(responseKey.toString());
						Log.d(TAG,"aesKey byte array:"+Arrays.toString(aesKey));
						
						//write ACCN to appdata
						//write password to appdata (hashed)
						Log.d(TAG,"Start writing shared pref");
						appdata.setACCN(Long.parseLong(newACCN));
						appdata.setPass(newPassword);
						
						//derive keys (balance key and key encryption key) from password
						key.deriveKey(newPassword, newIMEI);
						keyEncryption_key = key.getKeyEncryptionKey();
						balance_key = key.getBalanceKey();

						//get balance and last sync timestamp from HTTP post response
						returnBalance = jobj_response.getInt("balance");
						returnTS = jobj_response.getInt("last_sync_at");
						
						//wrap aes key using key encryption key and then write aes key to appdata
						//write last sync timestamp to appdata
						//write encrypted balance to appdata
						//write encrypted verified balance to appdata
						appdata.setKey(aesKey, keyEncryption_key);						
						appdata.setLATS(returnTS);
//						appdata.setBalance(returnBalance, balance_key);
//						appdata.setVerifiedBalance(returnBalance, balance_key);
						Log.d(TAG,"Finish writing shared pref");
	
						//notification dialog for registration success					
						new AlertDialog.Builder(parentActivity)
							.setTitle("Notification")
							.setMessage("Registration Success\nBalance Rp. "+returnBalance)
							.setNeutralButton("OK", new DialogInterface.OnClickListener()
						{
						    @Override
						    public void onClick(DialogInterface dialog, int which) {
								//close registration activity and start login activity
						    	((ProgressBar)parentActivity.findViewById(R.id.pReg)).setVisibility(View.INVISIBLE);
								ctx.startActivity((new Intent(ctx, Login.class)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
								parentActivity.finish();
						    }
						})
						.show();
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					appdata.deleteAppData();
				}
			}
			else if(param_mode == LOG_SYNC_MODE){
				String responseStatus;
				try {
					//if result in http post response is error, set error value
					responseStatus = jobj_response.getString("result");
					if(responseStatus.compareTo("Error") == 0){
						error = 3;
						errorMessage = jobj_response.getString("message");
					}
					if(responseStatus.compareTo("error") == 0){
						error = 3;
						errorMessage = jobj_response.getString("message");
					}
					
					//if no error, continue parse http post response
					if(error == 0){
						//get balance
						//get last sync timestamp
						//check if key renew true
						//if key renew true, get new key
						returnBalance = jobj_response.getInt("balance");
						returnTS = jobj_response.getInt("last_sync_at");
						JSONObject returnKey = jobj_response.getJSONObject("key");
						returnRenewKey = returnKey.getBoolean("renew");
						if(returnRenewKey == true){
							returnNewKey = returnKey.getString("new_key");
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					error = 4;
				}
			}
			else{
				error = 99;
				Log.d(TAG,"WTF -- What a Terible Failure. Param_mode not LOG or REG!");
			}
			
		} else {
			// error occured
			error = 1;
			Log.d(TAG,"Response is empty JSON Object");
			appdata.deleteAppData();
		}
		
		
		if(param_mode == REGISTRATION_MODE){
			//if error in registration mode, new activity isn't called then execute bellow code
			//change in UI
			(parentActivity.findViewById(R.id.bRegConfirm)).setEnabled(true);
			(parentActivity.findViewById(R.id.bRegCancel)).setEnabled(true);
			(parentActivity.findViewById(R.id.tRegDebug)).setVisibility(View.INVISIBLE);
			(parentActivity.findViewById(R.id.pReg)).setVisibility(View.GONE);
		} else if (param_mode == LOG_SYNC_MODE) { 
			//in log sync mode, error or no error, this will be executed
			if(error == 0){
				//if no error in parse http post response, write balance to appdata (both unverified and verified get updated)
				//write last sync timestamp to appdata
				//if server sends new key, wrap new key and write to appdata
				Log.d(TAG,"new balance:"+returnBalance);
//				appdata.setBalance(returnBalance, balance_key);
//				appdata.setVerifiedBalance(returnBalance, balance_key);
				appdata.setLATS(returnTS);
				if(returnRenewKey == true){
					appdata.setKey(Converter.hexStringToByteArray(returnNewKey), keyEncryption_key);
					Log.d(TAG,"new key:"+returnNewKey);
				}
				//if sync success, delete all entries in log database
				LogDB.deleteDB(ctx);
				
				//notification dialog about sync success
				new AlertDialog.Builder(parentActivity)
					.setTitle("Notification")
					.setMessage("Sync Success\nBalance Rp. "+returnBalance)
					.setNeutralButton("OK", new DialogInterface.OnClickListener()
				{
				    @Override
				    public void onClick(DialogInterface dialog, int which) {
				    }
				})
				.show();
			} else {
				//notification dialog about sync failed
				new AlertDialog.Builder(parentActivity)
					.setTitle("Notification")
					.setMessage("Sync Failed\n"+errorMessage)
					.setNeutralButton("OK", new DialogInterface.OnClickListener()
				{
				    @Override
				    public void onClick(DialogInterface dialog, int which) {
				    }
				})
				.show();
				Log.d(TAG,"error:"+error);
			}
			
			//change in UI
			(parentActivity.findViewById(R.id.pMain)).setVisibility(View.GONE);
			(parentActivity.findViewById(R.id.bNewTrans)).setEnabled(true);
			(parentActivity.findViewById(R.id.bHistory)).setEnabled(true);
			(parentActivity.findViewById(R.id.bSettlement)).setEnabled(true);
			(parentActivity.findViewById(R.id.bOption)).setEnabled(true);
		}
	}
}
