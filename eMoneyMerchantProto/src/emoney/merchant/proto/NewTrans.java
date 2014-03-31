package emoney.merchant.proto;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.OnNdefPushCompleteCallback;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import emoney.merchant.proto.misc.Converter;
import emoney.merchant.proto.userdata.AppData;

public class NewTrans extends Activity implements OnClickListener , OnNdefPushCompleteCallback {
	private final static String TAG = "{class} NewTrans";
	
	private NfcAdapter nfcAdapter;
	private PendingIntent mNfcPendingIntent;
	private NdefMessage toSend;
	
	private AppData appdata;
	Button bCancel;
	TextView tDebug, tSESN;
	private byte[] aes_key, log_key, balance_key;
	private byte[] plainTransPacket;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.newtrans);

		appdata = new AppData(getApplicationContext());
		Intent myIntent = getIntent();
		aes_key = myIntent.getByteArrayExtra("aesKey");
		log_key = myIntent.getByteArrayExtra("logKey");
		balance_key = myIntent.getByteArrayExtra("balanceKey");
		
		//UI Init
		
		nfcAdapter = NfcAdapter.getDefaultAdapter(this);
		if (nfcAdapter == null) return; 
		nfcAdapter.setNdefPushMessage(null, this);
		nfcAdapter.setOnNdefPushCompleteCallback(this, this);
		
		tSESN = (TextView)findViewById(R.id.tNewTransSESNDigit);
		tDebug = (TextView)findViewById(R.id.tNewTransDebug);
		bCancel = (Button)findViewById(R.id.bNewTransCancel);
		
		tDebug.setText("NDEF payload");
	}

	@Override
    public void onResume(){
    	super.onResume();
    	if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) processIntent(getIntent());
    }
	
	private void processIntent(Intent intent) {
		Log.d(TAG,"process intent");
		tDebug.setText("beam intent found!\n");
		Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
		NdefMessage[] msgs;
		if (rawMsgs != null) {
			tDebug.append("ndef message found!\n");
			msgs = new NdefMessage[rawMsgs.length];
			for (int i = 0; i < rawMsgs.length; i++) {
				msgs[i] = (NdefMessage) rawMsgs[i];
			}
			tDebug.append(new String(msgs[0].getRecords()[0].getPayload()));
        }
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch(v.getId()){
			case R.id.bNewTransCancel:
				finish();
				break;
		}
	}

	@Override
    public void onNewIntent(Intent intent) {
		Log.d(TAG,"new intent");
        setIntent(intent);
    }
	
	@Override
	public void onNdefPushComplete(NfcEvent arg0) {
		// TODO Auto-generated method stub
		Log.d(TAG,"ndef push complete");
	}
}
