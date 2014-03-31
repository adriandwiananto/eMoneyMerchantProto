package emoney.merchant.proto;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import emoney.merchant.proto.crypto.Hash;
import emoney.merchant.proto.misc.Converter;
import emoney.merchant.proto.userdata.AppData;

public class Login extends Activity implements OnClickListener {
	TextView loginDebug;
	
	private final static String TAG = "{class} Login";
	private AppData appdata;
	private String password;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);
		
		appdata = new AppData(this);
		
		Date d = new Date(appdata.getLATS()*1000);
		SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.US);
		
		Log.d(TAG,"Write debug textview!");
		loginDebug = (TextView)findViewById(R.id.tLoginDebug);
		loginDebug.setText("ACCN : "+String.valueOf(appdata.getACCN()));
		loginDebug.append("\nHWID : "+String.valueOf(appdata.getIMEI()));
		loginDebug.append("\nPassword : "+appdata.getPass());
		loginDebug.append("\nBalance : "+appdata.getBalance());
		loginDebug.append("\nLast Sync : "+String.valueOf(appdata.getLATS()));
		loginDebug.append("\nLast Sync(Readable) : "+ df.format(d));
		Log.d(TAG,"key:"+appdata.getEncryptedKey());
		loginDebug.append("\nWrapped Key+Iv : "+ appdata.getEncryptedKey());
		
		((Button)findViewById(R.id.bLoginProceed)).setOnClickListener(this);
		((Button)findViewById(R.id.bLoginCancel)).setOnClickListener(this);
	}

	@Override
    protected void onResume() {
        super.onResume();
    }
	
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		Intent myIntent = new Intent(this, MainActivity.class);
		switch(v.getId()){
			case R.id.bLoginProceed:
				InputMethodManager inputManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE); 
				inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);
				
				password = ((EditText)findViewById(R.id.eLoginPassword)).getText().toString();
				String passSalt = password.concat(String.valueOf(appdata.getIMEI()));
				
				byte[] hashed = Hash.sha256Hash(passSalt);
				String hashedStr = Converter.byteArrayToHexString(hashed);
				
				if(hashedStr.compareTo(appdata.getPass()) == 0){
					myIntent.putExtra("Password", password);
					startActivity(myIntent);
					finish();
				}else{
					Toast.makeText(getApplicationContext(), "Wrong password!", Toast.LENGTH_SHORT).show();
				}
				break;
			case R.id.bLoginCancel:
				finish();
				break;
		}
	}
}
