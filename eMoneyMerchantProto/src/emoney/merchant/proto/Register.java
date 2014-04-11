package emoney.merchant.proto;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import emoney.merchant.proto.misc.Network;
import emoney.merchant.proto.userdata.AppData;

public class Register extends Activity implements OnClickListener {
	private final static String TAG = "{class} Register";
	private static final boolean debugTextViewVisibility = false;
	
	TextView debug;
	
	private AppData appdata;
	private String ACCN, NewPass, ConfPass;
	private ProgressBar spinner;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.register);
		
		//set IMEI in appdata
		//ONLY SET IMEI ONCE IN REGISTRATION!
		appdata = new AppData(this);
		appdata.setIMEI();
		
		//UI purpose
		spinner = (ProgressBar)findViewById(R.id.pReg);
		spinner.setVisibility(View.GONE);
		
		//button listener
		((Button)findViewById(R.id.bRegConfirm)).setOnClickListener(this);
		((Button)findViewById(R.id.bRegCancel)).setOnClickListener(this);
		debug = (TextView)findViewById(R.id.tRegDebug);
		if(debugTextViewVisibility) {
        	debug.setVisibility(View.VISIBLE);
        } else {
        	debug.setVisibility(View.GONE);
        }
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch(v.getId()){
			case R.id.bRegConfirm:
				Log.d(TAG,"Starts register");
				
				//hide soft keyboard
				InputMethodManager inputManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE); 
				inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);
				
				//get string from edit text
				ACCN = ((EditText)findViewById(R.id.eRegACCN)).getText().toString();
				NewPass = ((EditText)findViewById(R.id.eRegNewPass)).getText().toString();
				ConfPass = ((EditText)findViewById(R.id.eRegConfPass)).getText().toString();

				//check if data from edit text is correct
				//ACCN must have length of 14
				if(ACCN.length() < 14){
					Toast.makeText(getApplicationContext(), "Incorrect Account ID length" , Toast.LENGTH_SHORT).show();
					return;
				}
				
				//Password cannot empty
				if(NewPass.length() < 1){
					Toast.makeText(getApplicationContext(), "Please input password", Toast.LENGTH_SHORT).show();
					return;
				}
				
				//Confirm password must have same value with password
				if(NewPass.compareTo(ConfPass) != 0){
					Toast.makeText(getApplicationContext(), "Incorrect password confirmation", Toast.LENGTH_SHORT).show();
					return;
				}

				//UI purpose
				((Button)findViewById(R.id.bRegConfirm)).setEnabled(false);
				((Button)findViewById(R.id.bRegCancel)).setEnabled(false);
				spinner.setVisibility(View.VISIBLE);
				
				//create JSON object of REGISTRATION
				long lACCN = Long.parseLong(ACCN);
				JSONObject json = new JSONObject();
				try {
					json.put("HWID", appdata.getIMEI());
					json.put("ACCN", lACCN);
					
					debug.setText("JSON send:"+json.toString());
					if(debugTextViewVisibility) {
			        	debug.setVisibility(View.VISIBLE);
			        } else {
			        	debug.setVisibility(View.GONE);
			        }
					
				} catch (NumberFormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				//do HTTP POST in separate thread (async task)
				Log.d(TAG,"Create asynctask");
				String IMEI = String.valueOf(appdata.getIMEI());
				Network net = new Network(Register.this ,getApplicationContext(), json, NewPass, ACCN, IMEI);
				net.execute();
				Log.d(TAG,"Finish main thread");
				break;
			case R.id.bRegCancel:
				finish();
				break;
		}
	}
}
