package emoney.merchant.proto.misc;

import java.math.BigInteger;
import java.nio.ByteBuffer;

public class Converter {
	
	/**
	 * Convert hex numbers written in string to it's byte array representation
	 * <br>e.g: (String)1234AABB will be converted to (byte){0x12,0x34,0xAA,0xBB}
	 * @param dataString Data to convert in String
	 * @return Converted data in byte array
	 */
	public static byte[] hexStringToByteArray(String dataString) {
		int len = dataString.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(dataString.charAt(i), 16) << 4)
									+ Character.digit(dataString.charAt(i+1), 16));
		}
		return data;
	}

	/**
	 * Convert byte array to it's hex representation written in String
	 * <br>e.g: (byte){0x12,0x34,0xAA,0xBB} will be converted to (String)1234AABB  
	 * @param dataByte Data to convert in byte array
	 * @return Converted data in String
	 */
	public static String byteArrayToHexString(byte[] dataByte) {
		return String.format("%0" + (dataByte.length*2) + "X", new BigInteger(1, dataByte));
	}

	/**
	 * Convert integer to byte array
	 * <br>e.g: (int)1000 will be converted to (byte){0x00,0x00,0x03,0xE8}
	 * @param dataInt Data to convert in int
	 * @return Converted data in byte array
	 */
	public static byte[] integerToByteArray(int dataInt){
		return ByteBuffer.allocate(4).putInt(dataInt).array();
	}

	/**
	 * Convert byte array to integer
	 * <br>e.g: (byte){0x00,0x00,0x03,0xE8} will be converted to (int)1000  
	 * @param dataByte Data to convert in byte array
	 * @return Converted data in int
	 */
	public static int byteArrayToInteger(byte[] dataByte){
		if(dataByte.length < 4){
			byte[] newDataByte = new byte[4];
			System.arraycopy(dataByte, 0, newDataByte, newDataByte.length - dataByte.length, dataByte.length);
			return ByteBuffer.wrap(newDataByte).getInt();
		} else {
			return ByteBuffer.wrap(dataByte).getInt();
		}
	}

	/**
	 * Convert long to byte array
	 * <br>e.g: (int)1000 will be converted to (byte){0x00,0x00,0x00,0x00,0x00,0x00,0x03,0xE8}    
	 * @param dataLong Data to convert in long
	 * @return Converted data in byte array
	 */
	public static byte[] longToByteArray(long dataLong){
		return ByteBuffer.allocate(8).putLong(dataLong).array();
	}
	
	/**
	 * Convert byte array to long
	 * <br>e.g: (byte){0x00,0x00,0x00,0x00,0x00,0x00,0x03,0xE8} will be converted to (int)1000  
	 * @param dataByte Data to convert in byte array
	 * @return Converted data in long
	 */
	public static long byteArrayToLong(byte[] dataByte){
		if(dataByte.length < 8){
			byte[] newDataByte = new byte[8];
			System.arraycopy(dataByte, 0, newDataByte, newDataByte.length - dataByte.length, dataByte.length);
			return ByteBuffer.wrap(newDataByte).getLong();
		} else {
			return ByteBuffer.wrap(dataByte).getLong();
		}
	}
}
