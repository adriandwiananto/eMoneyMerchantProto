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
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost(hostname);

			// Add your data
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			if(param_mode == LOG_SYNC_MODE){
				LogDB ldb = new LogDB(ctx, log_key);
				Cursor cur = ldb.getLogBlob();
				JSONArray jarray_logs = new JSONArray();
				JSONObject jobj_header = new JSONObject();
				int logNum = cur.getCount();
				int balance = 0;
				
				while((cur.isAfterLast() == false) && (error == 0)) {
					int rowNum = cur.getInt(cur.getColumnIndex(ldb.getIDColumnName()));

    				LogOperation lo = ldb.new LogOperation();
    				byte[] decryptedLog = lo.getDecrpytedLogPerRow(cur, log_key);
    				if(lo.getError() == true) {
    					error = 1;
    				}
    				
    				if(error == 0){
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
				nameValuePairs.add(new BasicNameValuePair("header", jobj_header.toString()));
				nameValuePairs.add(new BasicNameValuePair("logs", jarray_logs.toString()));
				JSONObject jobj_print = new JSONObject();
				jobj_print.put("header", jobj_header);
				jobj_print.put("logs", jarray_logs);
				Log.d(TAG,"sync http post param:\n"+jobj_print.toString());
			}
			else if(param_mode == REGISTRATION_MODE){
				nameValuePairs.add(new BasicNameValuePair("data", data));
			}
			else{
				error = 5; //unknown param_mode
				return null;
			}
			
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			// Execute HTTP Post Request
			HttpResponse response = httpclient.execute(httppost);
			
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
		if(param_mode == REGISTRATION_MODE){
			Toast.makeText(ctx, "Registration Starts", Toast.LENGTH_SHORT).show();	
		} else {
			Toast.makeText(ctx, "Sync Starts", Toast.LENGTH_SHORT).show();
		}
	}
	 
	@Override
	protected void onPostExecute(JSONObject result) {
		int returnBalance = 0;
		int returnTS = 0;
		boolean returnRenewKey = false;
		String returnNewKey = "";
		
		if (error == 0) {
			// do something
			jobj_response = result;

			if(param_mode == REGISTRATION_MODE){
				String responseStatus;
				try {
					responseStatus = jobj_response.getString("result");
					
					if(responseStatus.compareTo("Error") == 0){
						String errorMessage = jobj_response.getString("message");
						Toast.makeText(ctx, "Registration failed!! "+errorMessage, Toast.LENGTH_LONG).show();
						appdata.deleteAppData();
						error = 3;
					}
					else{
						String responseKey = jobj_response.getString("key");
						byte[] aesKey = new byte[responseKey.length()/2];
						aesKey = Converter.hexStringToByteArray(responseKey.toString());
						Log.d(TAG,"aesKey byte array:"+Arrays.toString(aesKey));
						
						Log.d(TAG,"Start writing shared pref");
						appdata.setACCN(Long.parseLong(newACCN));
						appdata.setPass(newPassword);
						
						key.deriveKey(newPassword, newIMEI);
						keyEncryption_key = key.getKeyEncryptionKey();
						balance_key = key.getBalanceKey();
						
						returnBalance = jobj_response.getInt("balance");
						returnTS = jobj_response.getInt("last_sync_at");
						appdata.setKey(aesKey, keyEncryption_key);						
						appdata.setLATS(returnTS);
//						appdata.setBalance(returnBalance, balance_key);
//						appdata.setVerifiedBalance(returnBalance, balance_key);
						Log.d(TAG,"Finish writing shared pref");
						
						new AlertDialog.Builder(parentActivity)
							.setTitle("Notification")
							.setMessage("Registration Success\nBalance Rp. "+returnBalance)
							.setNeutralButton("OK", new DialogInterface.OnClickListener()
						{
						    @Override
						    public void onClick(DialogInterface dialog, int which) {
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
					responseStatus = jobj_response.getString("result");
					if(responseStatus.compareTo("Error") == 0){
						error = 3;
						errorMessage = jobj_response.getString("message");
					}
					if(responseStatus.compareTo("error") == 0){
						error = 3;
						errorMessage = jobj_response.getString("message");
					}
					
					returnBalance = jobj_response.getInt("balance");
					returnTS = jobj_response.getInt("last_sync_at");
					JSONObject returnKey = jobj_response.getJSONObject("key");
					returnRenewKey = returnKey.getBoolean("renew");
					if(returnRenewKey == true){
						returnNewKey = returnKey.getString("new_key");
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
		
		
		//if error, new activity isn't called then execute bellow code
		if(param_mode == REGISTRATION_MODE){
			(parentActivity.findViewById(R.id.bRegConfirm)).setEnabled(true);
			(parentActivity.findViewById(R.id.bRegCancel)).setEnabled(true);
			(parentActivity.findViewById(R.id.tRegDebug)).setVisibility(View.INVISIBLE);
			(parentActivity.findViewById(R.id.pReg)).setVisibility(View.GONE);
		} else if (param_mode == LOG_SYNC_MODE) { //error or no error, this will be executed after sync
			if(error == 0){
				Log.d(TAG,"new balance:"+returnBalance);
//				appdata.setBalance(returnBalance, balance_key);
//				appdata.setVerifiedBalance(returnBalance, balance_key);
				appdata.setLATS(returnTS);
				if(returnRenewKey == true){
					appdata.setKey(Converter.hexStringToByteArray(returnNewKey), keyEncryption_key);
					Log.d(TAG,"new key:"+returnNewKey);
				}
				LogDB.deleteDB(ctx);
				
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
			(parentActivity.findViewById(R.id.pMain)).setVisibility(View.GONE);
			(parentActivity.findViewById(R.id.bNewTrans)).setEnabled(true);
			(parentActivity.findViewById(R.id.bHistory)).setEnabled(true);
			(parentActivity.findViewById(R.id.bSettlement)).setEnabled(true);
			(parentActivity.findViewById(R.id.bOption)).setEnabled(true);
		}
	}
    
	public String getData(){
		return data;
	}
	
	public String getHeader(){
		return header;
	}
	
	public String getLogs(){
		return logs;
	}
	
	public int getError(){
		return error;
	}
	
	public JSONObject getResponse(){
		return jobj_response;
	}
}
