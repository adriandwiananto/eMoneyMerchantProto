package emoney.merchant.proto.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hash {
	
	public static byte[] sha256Hash(String text) {
		MessageDigest digest=null;
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		digest.reset();
		return digest.digest(text.getBytes());
	}
}
