package emoney.merchant.proto;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import emoney.merchant.proto.crypto.Hash;
import emoney.merchant.proto.crypto.KeyDerive;
import emoney.merchant.proto.misc.Converter;
import emoney.merchant.proto.userdata.AppData;
import emoney.merchant.proto.userdata.LogDB;
import emoney.merchant.proto.userdata.LogDB.LogOperation;

public class Option extends Activity implements OnClickListener{

	private byte[] aes_key, log_key, balance_key;
	private String curPassStr, newPassStr, confPassStr;
	private Activity currentActivity;
	EditText curPass, newPass, confPass;
	Button proceed, cancel;
	ProgressBar pOption;
	AppData appdata;
	private String passExtra;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.option);
		
		//get extras from Intent
		Intent myIntent = getIntent();
		aes_key = myIntent.getByteArrayExtra("aesKey");
		log_key = myIntent.getByteArrayExtra("logKey");
		balance_key = myIntent.getByteArrayExtra("balanceKey");
		passExtra = myIntent.getStringExtra("Password");
		
		appdata = new AppData(this);
		if(appdata.getError() == true){
			Toast.makeText(this, "APPDATA ERROR!", Toast.LENGTH_LONG).show();
			finish();
		}
		
		currentActivity = Option.this;
		
		//UI purpose
		proceed = (Button)findViewById(R.id.bOptionProceed);
		proceed.setOnClickListener(this);
		cancel = (Button)findViewById(R.id.bOptionCancel);
		cancel.setOnClickListener(this);
		curPass = (EditText)findViewById(R.id.eOptionCurPass);
		newPass = (EditText)findViewById(R.id.eOptionNewPass);
		confPass = (EditText)findViewById(R.id.eOptionConfPass);
		pOption = (ProgressBar)findViewById(R.id.pOption);
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch(v.getId()){
			case R.id.bOptionProceed:
				//hide soft keyboard
				InputMethodManager inputManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE); 
				inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);
				
				//get string from edit text
				curPassStr = curPass.getText().toString();
				newPassStr = newPass.getText().toString();
				confPassStr = confPass.getText().toString();
				
				//make sure new password is not empty
				if(newPassStr.length() < 1){
					Toast.makeText(getApplicationContext(), "Please input new password", Toast.LENGTH_SHORT).show();
					return;
				}
				
				//make sure confirm password is same with new password
				if(newPassStr.compareTo(confPassStr) != 0){
					Toast.makeText(getApplicationContext(), "Incorrect new password confirmation", Toast.LENGTH_SHORT).show();
					return;
				}
				
				//get hashed value of current password
				String passSalt = curPassStr.concat(String.valueOf(appdata.getIMEI()));
				byte[] hashed = Hash.sha256Hash(passSalt);
				String hashedStr = Converter.byteArrayToHexString(hashed);
				
				//make sure current password hashed value same with value stored in appdata
				if((hashedStr.compareTo(appdata.getPass()) != 0) && (curPassStr.compareTo(passExtra) != 0)){
					Toast.makeText(getApplicationContext(), "Incorrect password", Toast.LENGTH_SHORT).show();
					return;
				}
				
				//UI purpose
				proceed.setEnabled(false);
				cancel.setEnabled(false);
				pOption.setVisibility(View.VISIBLE);
				
				//run in separate thread
				//derive keys from new password
				//decrypt aes key, balance, log with old key then encrypt with new key
				Runnable runnable = new Runnable(){
					public void run(){
						Message msg = handler.obtainMessage();
						
						//derive keys with new password
						KeyDerive kd = new KeyDerive();
						kd.deriveKey(newPassStr, String.valueOf(appdata.getIMEI()));
						byte[] newBalance_key = kd.getBalanceKey();
						byte[] newLog_key = kd.getLogKey();
						byte[] newKeyEncryption_key = kd.getKeyEncryptionKey();
						
						Bundle bundle = new Bundle();

						//change log key
						LogDB ldb = new LogDB(getApplicationContext(), log_key);
						LogOperation lo = ldb.new LogOperation();
						if(lo.changeLogKey(newLog_key) == true)
						{
							//if no error in change log key
							//write new hashed password value to appdata
							//encrypt balance with new key
							//encrypt aes key with new key
							int balance = appdata.getDecryptedBalance(balance_key);
							int verifiedBalance = appdata.getDecryptedVerifiedBalance(balance_key);
							
							appdata.setPass(newPassStr);
							appdata.setBalance(balance, newBalance_key);
							appdata.setVerifiedBalance(verifiedBalance, newBalance_key);
							appdata.setKey(aes_key, newKeyEncryption_key);
							bundle.putBoolean("error", false);
						} else {
							bundle.putBoolean("error", true);
						}
						msg.setData(bundle);
						handler.sendMessage(msg);
					}
				};
				Thread changePassword = new Thread(runnable);
				changePassword.start();
				break;
			case R.id.bOptionCancel:
				backToMain();
				break;
		}
	}
	
	@Override
	public void onBackPressed() {
		backToMain();
	}
	
	private void backToMain(){
		//close this activity and open main activity with old password in Intent
		Intent newIntent = new Intent(this,MainActivity.class);
		newIntent.putExtra("Password", passExtra);
		startActivity(newIntent);
		finish();
	}
	
	@SuppressLint("HandlerLeak")
	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			//UI purpose
			pOption.setVisibility(View.GONE);
			proceed.setEnabled(true);
			cancel.setEnabled(true);
			
			Bundle bundle = msg.getData();
			boolean error = bundle.getBoolean("error", true);
			if(error == true){
				Toast.makeText(getApplicationContext(), "Password changing error!", Toast.LENGTH_LONG).show();
			} else {
				//if no error in changing password, close this option activity and open login activity
				//user need to input newly changed password in login activity
				Toast.makeText(getApplicationContext(), "Password changed successfully!", Toast.LENGTH_LONG).show();
	    		startActivity(new Intent(currentActivity, Login.class));
				currentActivity.finish();
			}
		}
	};
}
