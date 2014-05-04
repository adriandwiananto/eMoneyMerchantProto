package emoney.merchant.proto;

import java.util.Arrays;
import java.util.Random;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;

import emoney.merchant.proto.misc.Converter;
import emoney.merchant.proto.misc.Packet;
import emoney.merchant.proto.misc.Packet.ParseReceivedPacket;
import emoney.merchant.proto.misc.Receipt;
import emoney.merchant.proto.userdata.AppData;
import emoney.merchant.proto.userdata.LogDB;
import emoney.merchant.qrcode.CameraPreview;
import emoney.merchant.qrcode.Contents;
import emoney.merchant.qrcode.QRCodeEncoder;

public class NewTransQr extends Activity implements OnClickListener {
	private final static String TAG = "{class} NewTrans";
	private static final boolean debugTextViewVisibility = false;
	
	private AppData appdata;
	Button bProceed,bCancel, bContinue, bFinish;
	TextView tDebug, tMsg;
	EditText eAmount;
	private byte[] aes_key, log_key, balance_key;
	private int sequence;
	private int sesnInt;
	
	ParseReceivedPacket prp;
	
	private String passExtra;
	private int amountInt;
	
	// qrcode section
	private QRCodeEncoder qrCodeEncoder;
	private int qrCodeDimention;
	private ImageView qrImage;
	
	// scanner
    private Camera mCamera;
    private CameraPreview mPreview;
    private Handler autoFocusHandler;
    
    private FrameLayout preview;
    ImageScanner scanner;
    
    private boolean barcodeScanned = false;
    private boolean previewing = true;
    
    static {
        System.loadLibrary("iconv");
    }
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.newtransqr);

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

		tMsg = (TextView)findViewById(R.id.tNewTransMsg);
		tDebug = (TextView)findViewById(R.id.tNewTransDebug);
		eAmount = (EditText)findViewById(R.id.eNewTransAmount);
		bCancel = (Button)findViewById(R.id.bNewTransCancel);
		bCancel.setOnClickListener(this);
		bProceed = (Button)findViewById(R.id.bNewTransProceed);
		bProceed.setOnClickListener(this);
		bContinue = (Button)findViewById(R.id.newTransContinue);
		bContinue.setOnClickListener(this);
		bFinish = (Button)findViewById(R.id.bNewTransFinish);
		bFinish.setOnClickListener(this);
		
		preview = (FrameLayout)findViewById(R.id.newTransCameraView);
		
		if(debugTextViewVisibility) {
        	tDebug.setVisibility(View.VISIBLE);
        } else {
        	tDebug.setVisibility(View.GONE);
        }
		
		Log.d(TAG,"onCreate called!");
	}


	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch(v.getId()){
			case R.id.bNewTransProceed:
				if(eAmount.getText().toString().length() > 0){
					//hide soft keyboard
					InputMethodManager inputManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
					inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);
					
					String amount = eAmount.getText().toString();
					amountInt = Integer.parseInt(amount);

					bProceed.setEnabled(false);
					tMsg.append(" "+Converter.longToRupiah(amountInt));
					eAmount.setVisibility(View.GONE);
					
					Random r = new Random();
					int Low = 100; //inclusive
					int High = 1000; //exclusive
					sesnInt = r.nextInt(High-Low) + Low;
					
					long timestamp = System.currentTimeMillis()/1000;
					int timestampInt = Integer.valueOf(Long.valueOf(timestamp).intValue());
					
					Packet packet = new Packet(amountInt, sesnInt, timestampInt, appdata.getACCN(), 0, aes_key);
					byte[] packetArrayToSend = packet.buildTransPacket();
					
					qrCodeDimention = 400;
					qrCodeEncoder = new QRCodeEncoder(Converter.byteArrayToHexString(packetArrayToSend), null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), qrCodeDimention);
					qrImage = (ImageView) findViewById(R.id.newTransQr);
					setQrCodeImage(qrCodeEncoder);
					bContinue.setVisibility(View.VISIBLE);
					Toast.makeText(this, "Scan this QRCode on payer's device", Toast.LENGTH_LONG).show();
	
					byte[] plainPayload = new byte[32];
					System.arraycopy(packet.getPlainPacket(), 7, plainPayload, 0, 32);
					
					tDebug.setText("Data packet to send:\n"+Converter.byteArrayToHexString(packetArrayToSend));
					tDebug.append("\nPlain payload:\n"+Converter.byteArrayToHexString(plainPayload));
					tDebug.append("\nCiphered payload:\n"+Converter.byteArrayToHexString(packet.getCipherPayload()));
					tDebug.append("\naes key:\n"+Converter.byteArrayToHexString(aes_key));
				}
				break;
			case R.id.bNewTransCancel:
				Log.d(TAG,"cancel!");
				if(sequence != 2){
					exitDialog();
				} else {
					backToMain();
				}
				break;
			case R.id.newTransContinue:
				Log.d(TAG, "continue");
				sequence = 1;
				qrImage.setVisibility(View.GONE);
				bContinue.setVisibility(View.GONE);
				
				autoFocusHandler = new Handler();
				mCamera = getCameraInstance();
				
				scanner = new ImageScanner();
				
		        scanner.setConfig(0, Config.X_DENSITY, 3);
		        scanner.setConfig(0, Config.Y_DENSITY, 3);
		        
		        mPreview = new CameraPreview(this, mCamera, previewCb, autoFocusCB);
		        preview.addView(mPreview);
		        preview.setVisibility(View.VISIBLE);
				
				break;
			case R.id.bNewTransFinish:
				backToMain();
				break;
		}
	}

	@Override
    public void onNewIntent(Intent intent) {
		Log.d(TAG,"new intent");
        setIntent(intent);
    }
	
	 /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception e){
        }
        return c;
    }
    
    private Runnable doAutoFocus = new Runnable() {
        public void run() {
            if (previewing)
                mCamera.autoFocus(autoFocusCB);
        }
    };

    PreviewCallback previewCb = new PreviewCallback() {
        public void onPreviewFrame(byte[] data, Camera camera) {
            Camera.Parameters parameters = camera.getParameters();
            Size size = parameters.getPreviewSize();

            Image barcode = new Image(size.width, size.height, "Y800");
            barcode.setData(data);

            int result = scanner.scanImage(barcode);
            
            if (result != 0) {
                previewing = false;
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
                
                SymbolSet syms = scanner.getResults();
                for (Symbol sym : syms) {
                    Log.d(TAG, sym.getData());
                    barcodeScanned = true;
					processQr(sym.getData());
                }
            }
        }
    };

    // Mimic continuous auto-focusing
    AutoFocusCallback autoFocusCB = new AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            autoFocusHandler.postDelayed(doAutoFocus, 1000);
        }
    };
    
	private void processQr(String qr){
		Log.d(TAG,"process intent");
		if (qr != null){	
			byte[] receivedPacket = Converter.hexStringToByteArray(qr);
			prp = new Packet(aes_key).new ParseReceivedPacket(receivedPacket);	
			if(prp.getErrorCode() != 0){
				Log.d(TAG, prp.getErrorMsg());
			} else {
				if(sequence == 1) {
					sequence = 2;
					int receivedSesn = Converter.byteArrayToInteger(prp.getReceivedSESN());
					
					if(receivedSesn == sesnInt){
						byte[] accnInByteArray = Arrays.copyOfRange(Converter.longToByteArray(appdata.getACCN()), 2, 8);
						LogDB ldb = new LogDB(this, log_key, accnInByteArray);
				    	ldb.insertLastTransToLog(prp.getReceivedPlainPacket());
				    	
				    	appdata.setLastTransTS(System.currentTimeMillis() / 1000);
		
						Log.d(TAG, "Transaction Success!");
						
						//PRINT PDF HERE!
						Receipt rcp = new Receipt(NewTransQr.this, Converter.byteArrayToLong(prp.getReceivedTS()), 
				        		appdata.getACCN(), Converter.byteArrayToLong(prp.getReceivedACCN()),  
				        		Converter.byteArrayToInteger(prp.getReceivedAMNT()));
						
						if(!rcp.writeReceiptPdfToFile()){
							Toast.makeText(this, "Error creating receipt, external storage not found", Toast.LENGTH_LONG).show();
						}
						
						tMsg.setText("Transaction Success!!\n" + "Amount: " + 
								Converter.longToRupiah(Converter.byteArrayToLong(prp.getReceivedAMNT())) + "\nPayer ID: " +
								Converter.byteArrayToLong(prp.getReceivedACCN()) +
								"\n\nScan QR code below to accept receipt" +
								"\nPress Finish to finish transaction without sending transaction receipt");
						preview.setVisibility(View.GONE);
						bCancel.setText("Finish");
						bFinish.setEnabled(true);
						//build packet for sending receipt
						Packet receipt = new Packet(Converter.byteArrayToInteger(prp.getReceivedAMNT()), 
													Converter.byteArrayToInteger(prp.getReceivedSESN()), 
													Converter.byteArrayToInteger(prp.getReceivedTS()), 
													appdata.getACCN(), 
													Converter.byteArrayToLong(prp.getReceivedLATS()), 
													aes_key);
						byte[] receiptPacket = receipt.buildTransPacket();
						Log.d(TAG,"receipt packet: "+Converter.byteArrayToHexString(receiptPacket));
						qrCodeEncoder = new QRCodeEncoder(Converter.byteArrayToHexString(receiptPacket), null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), qrCodeDimention);
						qrImage = (ImageView) findViewById(R.id.newTransQr);
						setQrCodeImage(qrCodeEncoder);
						preview.setVisibility(View.GONE);
						qrImage.setVisibility(View.VISIBLE);
					}
				}
			}
			
			return;
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
					eAmount.setVisibility(View.GONE);
					break;
				case 3:
			        //popup notification
			    	new AlertDialog.Builder(NewTransQr.this)
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
	
	private void setQrCodeImage(QRCodeEncoder input){
		try {
		    Bitmap bitmap = input.encodeAsBitmap();
		    qrImage.setImageBitmap(bitmap);
		} catch (WriterException e) {
		    e.printStackTrace();
		}
    }
}
