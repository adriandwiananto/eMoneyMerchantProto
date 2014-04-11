package emoney.merchant.proto;

import java.util.Arrays;
import java.util.Random;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.OnNdefPushCompleteCallback;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import emoney.merchant.proto.misc.Converter;
import emoney.merchant.proto.misc.Packet;
import emoney.merchant.proto.misc.Packet.ParseReceivedPacket;
import emoney.merchant.proto.userdata.AppData;
import emoney.merchant.proto.userdata.LogDB;

public class NewTrans extends Activity implements OnClickListener , OnNdefPushCompleteCallback {
	private final static String TAG = "{class} NewTrans";
	private static final boolean debugTextViewVisibility = false;
	
	private NfcAdapter nfcAdapter;
	private PendingIntent mNfcPendingIntent;
	private NdefMessage toSend;
	
	private AppData appdata;
	Button bCancel;
	TextView tDebug, tMsg;
	private byte[] aes_key, log_key, balance_key;
	private int sequence;
	private int sesnInt;
	
	ParseReceivedPacket prp;
	
	private String passExtra;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.newtrans);

		//	sequence 0 - expected to send merchant request
		//	sequence 1 - waiting for transaction data from payer
		//	sequence 2 - accepted transaction data, expected to send receipt
		sequence = 0;
		
		appdata = new AppData(getApplicationContext());
		if(appdata.getError() == true){
			Toast.makeText(this, "APPDATA ERROR!", Toast.LENGTH_LONG).show();
			finish();
		}
		
		Intent myIntent = getIntent();
		aes_key = myIntent.getByteArrayExtra("aesKey");
		log_key = myIntent.getByteArrayExtra("logKey");
		balance_key = myIntent.getByteArrayExtra("balanceKey");
		passExtra = myIntent.getStringExtra("Password");
		
		nfcAdapter = NfcAdapter.getDefaultAdapter(this);
		if (nfcAdapter == null) return; 
		nfcAdapter.setNdefPushMessage(null, this);
		nfcAdapter.setOnNdefPushCompleteCallback(this, this);
		mNfcPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

		tMsg = (TextView)findViewById(R.id.tNewTransSESN);
		tDebug = (TextView)findViewById(R.id.tNewTransDebug);
		bCancel = (Button)findViewById(R.id.bNewTransCancel);
		
		bCancel = (Button)findViewById(R.id.bNewTransCancel);
		bCancel.setOnClickListener(this);

		Random r = new Random();
		int Low = 100; //inclusive
		int High = 1000; //exclusive
		sesnInt = r.nextInt(High-Low) + Low;
		
		long timestamp = System.currentTimeMillis()/1000;
		int timestampInt = Integer.valueOf(Long.valueOf(timestamp).intValue());
		
		Packet packet = new Packet(0, sesnInt, timestampInt, appdata.getACCN(), 0, aes_key);
		byte[] packetArrayToSend = packet.buildTransPacket();
		toSend = packet.createNDEFMessage("emoney/merchantRequest", packetArrayToSend);

		nfcAdapter.setNdefPushMessage(toSend, this);
		
		byte[] plainPayload = new byte[32];
		System.arraycopy(packet.getPlainPacket(), 7, plainPayload, 0, 32);
		
		tDebug.setText("Data packet to send:\n"+Converter.byteArrayToHexString(packetArrayToSend));
		tDebug.append("\nPlain payload:\n"+Converter.byteArrayToHexString(plainPayload));
		tDebug.append("\nCiphered payload:\n"+Converter.byteArrayToHexString(packet.getCipherPayload()));
		tDebug.append("\naes key:\n"+Converter.byteArrayToHexString(aes_key));
		if(debugTextViewVisibility) {
        	tDebug.setVisibility(View.VISIBLE);
        } else {
        	tDebug.setVisibility(View.GONE);
        }
		
		Log.d(TAG,"onCreate called!");
	}

	@Override
    public void onResume(){
    	super.onResume();
		//called when device receive NFC intent
    	nfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, null, null);
    	if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) processIntent(getIntent());
    }
	
	private void processIntent(Intent intent) {
		//debugging purpose
		Log.d(TAG,"process intent");
		tDebug.setText("beam intent found!\n");
		
		Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
		NdefMessage[] msgs;
		if (rawMsgs != null) {
			tDebug.append("ndef message found!\n");
			
			//get payload of received NFC data
			msgs = new NdefMessage[rawMsgs.length];
			for (int i = 0; i < rawMsgs.length; i++) {
				msgs[i] = (NdefMessage) rawMsgs[i];
			}
			byte[] paymentData = msgs[0].getRecords()[0].getPayload();
			
			//parse received NFC data
			prp = new Packet(aes_key).new ParseReceivedPacket(paymentData);
			if(prp.getErrorCode() != 0){
				Toast.makeText(getApplicationContext(), prp.getErrorMsg(), Toast.LENGTH_LONG).show();
			} else {
				if(sequence == 1) {
					sequence = 2;
					int receivedSesn = Converter.byteArrayToInteger(prp.getReceivedSESN());
					
					if(receivedSesn == sesnInt){
						byte[] accnInByteArray = Arrays.copyOfRange(Converter.longToByteArray(appdata.getACCN()), 2, 8);
						LogDB ldb = new LogDB(this, log_key, accnInByteArray);
				    	ldb.insertLastTransToLog(prp.getReceivedPlainPacket());
				    	
				    	appdata.setLastTransTS(System.currentTimeMillis() / 1000);
		
						Toast.makeText(getApplicationContext(), "Transaction Success!", Toast.LENGTH_LONG).show();
	
						//close pay activity and open main activity
						//backToMain();
						
						//PRINT PDF HERE!
						
						tMsg.setText("Transaction Success!!\n" + "Amount: " + 
								Converter.longToRupiah(Converter.byteArrayToLong(prp.getReceivedAMNT())) + "\nPayer ID: " +
								Converter.byteArrayToLong(prp.getReceivedACCN()) +
								"\n\nTap payer device to send receipt" +
								"\nPress Finish to finish transaction without sending transaction receipt");
						bCancel.setText("Finish");
						
						//build packet for sending receipt
						Packet receipt = new Packet(Converter.byteArrayToInteger(prp.getReceivedAMNT()), 
													Converter.byteArrayToInteger(prp.getReceivedSESN()), 
													Converter.byteArrayToInteger(prp.getReceivedTS()), 
													appdata.getACCN(), 
													Converter.byteArrayToLong(prp.getReceivedLATS()), 
													aes_key);
						byte[] receiptPacket = receipt.buildTransPacket();
						toSend = receipt.createNDEFMessage("emoney/merchantReceipt", receiptPacket);
						nfcAdapter.setNdefPushMessage(toSend, this);
						Log.d(TAG,"receipt packet: "+Converter.byteArrayToHexString(receiptPacket));
					}
				}
			}
        }
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch(v.getId()){
			case R.id.bNewTransCancel:
				Log.d(TAG,"cancel!");
//				finish();
				if(sequence != 2){
					exitDialog();
				} else {
					backToMain();
				}
				break;
		}
	}

	@Override
    public void onNewIntent(Intent intent) {
		Log.d(TAG,"new intent");
        setIntent(intent);
    }
	
	@Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
        	nfcAdapter.disableForegroundDispatch(this);
        }
    }
	
	@Override
	public void onNdefPushComplete(NfcEvent arg0) {
		// TODO Auto-generated method stub
		Log.d(TAG,"ndef push complete, sequence:" + sequence);
		if(sequence == 0){
			nfcAdapter.setNdefPushMessage(null, this);
			sequence = 1;
			hand.sendMessage(hand.obtainMessage(1));
		} else if(sequence == 2) {
			nfcAdapter.setNdefPushMessage(null, this);
			hand.sendMessage(hand.obtainMessage(3));
		}
	}
	
	@Override
	public void onBackPressed() {
		exitDialog();
	}
	

	private void exitDialog() {
		// TODO Auto-generated method stub
		new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle("Close New Transaction")
			.setMessage("Are you sure you want to cancel new transaction?")
			.setPositiveButton("Yes", new DialogInterface.OnClickListener()
		{
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		        //finish();
				//close pay activity and open main activity
				backToMain();
		    }
		
		})
		.setNegativeButton("No", null)
		.show();
	}

	@SuppressLint("HandlerLeak")
	Handler hand = new Handler(){
		public void handleMessage(Message msg){
			switch (msg.what){
				case 1:
					tMsg.setText("Waiting payment from payer device");
					break;
				case 3:
			        //popup notification
			    	new AlertDialog.Builder(NewTrans.this)
					.setTitle("Notification")
					.setMessage("Receipt sent!")
					.setNeutralButton("OK", new DialogInterface.OnClickListener()
					{
					    @Override
					    public void onClick(DialogInterface dialog, int which) {
							backToMain();
					    }
					})
					.show();
					break;
			}
		}
	};
	private void backToMain(){
		//close this activity and open main activity with Password in Intent
		Intent newIntent = new Intent(this,MainActivity.class);
		newIntent.putExtra("Password", passExtra);
		startActivity(newIntent);
		finish();
	}
}
