package emoney.merchant.proto;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import emoney.merchant.proto.crypto.KeyDerive;
import emoney.merchant.proto.misc.Converter;
import emoney.merchant.proto.misc.Network;
import emoney.merchant.proto.userdata.AppData;

public class MainActivity extends Activity implements OnClickListener{
	private final static String TAG = "{class} MainActivity";
	private static final boolean debugTextViewVisibility = false;
	
	private NfcAdapter nfcA;
	private AppData appdata;
	TextView tDebug;
	ProgressBar pLoading;
	Button bNewTrans, bNewTransQR,bHistory,bSettlement,bOption;
	private String password;
	private long lIMEI;
	private KeyDerive key;
	private byte[] aes_key, keyEncryption_key, log_key, balance_key;
	
	@SuppressLint("HandlerLeak")
	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Bundle bundle = msg.getData();
			//UI modification
			pLoading.setVisibility(View.GONE);
			bNewTrans.setEnabled(true);
			bNewTransQR.setEnabled(true);
			bHistory.setEnabled(true);
			bSettlement.setEnabled(true);
			bOption.setEnabled(true);
			
			tDebug.setText("KEK:\n"+Converter.byteArrayToHexString(key.getKeyEncryptionKey()));
			tDebug.append("\nBalance Key:\n"+Converter.byteArrayToHexString(key.getBalanceKey()));
			tDebug.append("\nLog Key:\n"+Converter.byteArrayToHexString(key.getLogKey()));
			tDebug.append("\nTransaction Key:\n"+Converter.byteArrayToHexString(appdata.getDecryptedKey(key.getKeyEncryptionKey())));
		}
	};
		 
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
    	//UI Initialization
    	pLoading = (ProgressBar)findViewById(R.id.pMain);
    	tDebug = (TextView)findViewById(R.id.tDebug);
    	bNewTrans = (Button) findViewById(R.id.bNewTrans);
    	bNewTrans.setOnClickListener(this);
    	bNewTrans.setEnabled(false);
    	bNewTransQR = (Button) findViewById(R.id.bNewTransQR);
    	bNewTransQR.setOnClickListener(this);
    	bNewTransQR.setEnabled(false);
        bHistory = (Button) findViewById(R.id.bHistory);
        bHistory.setOnClickListener(this);
        bHistory.setEnabled(false);
        bSettlement = (Button) findViewById(R.id.bSettlement);
        bSettlement.setOnClickListener(this);
        bSettlement.setEnabled(false);
        bOption = (Button) findViewById(R.id.bOption);
        bOption.setOnClickListener(this);
        bOption.setEnabled(false);
        if(debugTextViewVisibility) {
        	tDebug.setVisibility(View.VISIBLE);
        } else {
        	tDebug.setVisibility(View.GONE);
        }

        // Init NFC
        nfcA = NfcAdapter.getDefaultAdapter(this);
        // no nfc device
    	if (nfcA == null){
    		Toast.makeText(this, "No NFC found!", Toast.LENGTH_LONG).show();
        	finish();
        }
    	
        //get device IMEI
    	TelephonyManager T = (TelephonyManager)getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
		String IMEI = T.getDeviceId();
		lIMEI = Long.parseLong(IMEI);
		
        appdata = new AppData(getApplicationContext());
        Log.d(TAG,"create new AppData class successfully");
        if(appdata.getError() == true){
			Toast.makeText(this, "APPDATA ERROR!", Toast.LENGTH_LONG).show();
			finish();
		}
        
        //if ACCN empty, open register activity and close main activity
        if(appdata.getACCN() == 0){
        	startActivity(new Intent(this, Register.class)); 
        	finish();
        }
        else{
        	//check if registered IMEI in appdata is same with current IMEI
        	if(appdata.getIMEI() != lIMEI){
        		Toast.makeText(getApplicationContext(), "Registered device not same with current device", Toast.LENGTH_LONG).show();
        		finish();
        	}

        	//get intent myIntent
        	Intent myIntent = getIntent();
        	
        	//if Password field in myIntent empty, it means this app is just started.
        	//open login activity and close main activity
        	//if login success main activity will be opened again with not empty Password field in myIntent
        	if(myIntent.getStringExtra("Password") == null){
        		startActivity(new Intent(this, Login.class));
        		finish();
        	}else{
        		//get password entered in login activity
	        	password = myIntent.getStringExtra("Password");
	        	Log.d(TAG,"Password:"+password);
	        	
	        	//derive balance key, log key, and key encryption key in separate thread
	        	//get decrypted balance
    			key = new KeyDerive();

	        	Runnable runnable = new Runnable(){
	        		public void run(){
	        			Message msg = handler.obtainMessage();
	        			key.deriveKey(password, String.valueOf(lIMEI));
	        			//int decryptedBalance = appdata.getDecryptedBalance(key.getBalanceKey());
	        			keyEncryption_key = key.getKeyEncryptionKey();
	        			aes_key = appdata.getDecryptedKey(keyEncryption_key);
	        			log_key = key.getLogKey();
	        			balance_key = key.getBalanceKey();
	        			
	        			Bundle bundle = new Bundle();
	        			//bundle.putString("Balance", String.valueOf(decryptedBalance));
	        			msg.setData(bundle);
	        			handler.sendMessage(msg);
	        		}
	        	};
	        	
	        	Thread balanceThread = new Thread(runnable);
	        	balanceThread.start();
        	}
        }
	}
	
	@Override
    protected void onResume() {
        super.onResume();
        //check if nfc enabled. if nfc is disabled, create dialog to offer enabling nfc in wireless setting
        if (nfcA != null) {
            if (!nfcA.isEnabled()) {
                showWirelessSettingsDialog();
            }
        }
    }
	
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()){
			case R.id.bNewTrans:
				//user choose new trans mode
				//this activity will be closed and new trans activity will be opened
				Intent payIntent = new Intent(this, NewTrans.class);
				payIntent.putExtra("Password", password);
				payIntent.putExtra("aesKey", aes_key);
				payIntent.putExtra("logKey", log_key);
				payIntent.putExtra("balanceKey", balance_key);
				startActivity(payIntent);
				finish();
				break;
			case R.id.bNewTransQR:
				//new trans QR goes in here!!!
				Intent payQrIntent = new Intent(this, NewTransQr.class);
				payQrIntent.putExtra("Password", password);
				payQrIntent.putExtra("aesKey", aes_key);
				payQrIntent.putExtra("logKey", log_key);
				payQrIntent.putExtra("balanceKey", balance_key);
				startActivity(payQrIntent);
				finish();
				break;
			case R.id.bHistory:
				//user choose history mode
				//this activity will be closed and history activity will be opened
				Intent historyIntent = new Intent(this, History.class);
				historyIntent.putExtra("Password", password);
				historyIntent.putExtra("logKey", log_key);
				startActivity(historyIntent);
				finish();
				break;
			case R.id.bSettlement:
				//user tap sync mode
				//disable all button, show progress bar
				pLoading.setVisibility(View.VISIBLE);
				bNewTrans.setEnabled(false);
				bNewTransQR.setEnabled(false);
				bHistory.setEnabled(false);
				bSettlement.setEnabled(false);
				bOption.setEnabled(false);
				
				//do sync in separate thread
				Network sync = new Network(MainActivity.this, getApplicationContext(), keyEncryption_key, log_key, balance_key);
				sync.execute();
				break;
			case R.id.bOption:
				//user choose option mode
				//this activity will be closed and option activity will be opened
				Intent optionIntent = new Intent(this, Option.class);
				optionIntent.putExtra("Password", password);
				optionIntent.putExtra("aesKey", aes_key);
				optionIntent.putExtra("logKey", log_key);
				optionIntent.putExtra("balanceKey", balance_key);
				startActivity(optionIntent);
				finish();
				break;
		}
	}
	
	/**
	 * create dialog that prompt user to enable nfc in wireless setting
	 * <br>if possitive button tapped, open wireless setting
	 * <br>if negative button tapped, finish this activity
	 */
	private void showWirelessSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.nfc_disabled);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                startActivity(intent);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        builder.create().show();
        return;
    }
}
