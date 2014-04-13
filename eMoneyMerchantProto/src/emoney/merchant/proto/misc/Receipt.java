package emoney.merchant.proto.misc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;
import crl.android.pdfwriter.PDFWriter;
import crl.android.pdfwriter.PaperSize;
import crl.android.pdfwriter.StandardFonts;

public class Receipt {
	private final static String TAG = "{class} Receipt";
	
	private String receiptPdfInString;
	private Activity actv;
	private long ts, accn_M, accn_P;
	private int amnt;
	
	public Receipt(Activity activity, long timestamp, long accnM, long accnP, int amount){
		receiptPdfInString = "";
		actv = activity;
		ts = timestamp;
		accn_M = accnM;
		accn_P = accnP;
		amnt = amount;
	}
	
	private String pdfContent() {
		PDFWriter mPDFWriter = new PDFWriter(PaperSize.A7_HEIGHT, PaperSize.A7_WIDTH);
		
		AssetManager mngr = actv.getAssets();
		try {
			Bitmap itbPNG = BitmapFactory.decodeStream(mngr.open("ITB_logo_mono.bmp"));
			mPDFWriter.addImage(71, 145, 40, 40, itbPNG);
		} catch (Exception e) {
			e.printStackTrace();
		}
		mPDFWriter.setFont(StandardFonts.SUBTYPE, StandardFonts.TIMES_BOLD);
		mPDFWriter.addText(120, 158, 30, "e-Money");
		
		mPDFWriter.setFont(StandardFonts.SUBTYPE, StandardFonts.TIMES_ROMAN);
		mPDFWriter.addText(20, 116, 12, Converter.timestampToReadable(ts));
		mPDFWriter.addText(20, 99, 12, "Merchant: "+accn_M);
		mPDFWriter.addText(20, 82, 12, "Payer: "+accn_P);

		int digits = (int) Math.log10(amnt) + 1;
		int padding = 124-(digits*18);
		mPDFWriter.setFont(StandardFonts.SUBTYPE, StandardFonts.TIMES_BOLD);
		mPDFWriter.addText(74+padding, 34, 36, Converter.longToRupiah(amnt));
		
		String s = mPDFWriter.asString();
		return s;
	}
	
	//public boolean outputToFile(long timestamp, String pdfContent) {
	public boolean writeReceiptPdfToFile() {
		String pdfContent = pdfContent();
		
		File directory = new File(Environment.getExternalStorageDirectory() + "/eMoney/");
		if(!directory.exists()){
			directory.mkdir();
		}
		
        File newFile = new File(directory.getAbsolutePath()+"/receipt-"+Converter.timestampToString(ts)+".pdf");
        try {
            newFile.createNewFile();
            try {
            	FileOutputStream pdfFile = new FileOutputStream(newFile);
            	pdfFile.write(pdfContent.getBytes("ISO-8859-1"));
                pdfFile.close();
            } catch(FileNotFoundException e) {
            	e.printStackTrace();
            	Log.d(TAG,"File not found exception");
            	return false;
            }
        } catch(IOException e) {
        	e.printStackTrace();
        	Log.d(TAG,"IO exception");
        	return false;
        }
        
        return true;
	}
	
	public String getReceiptPdfInString(){
		return receiptPdfInString;
	}
}
