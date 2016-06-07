package ca.benow.transmission;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * From http://www.wikihow.com/Encode-a-String-to-Base64-With-Java
 * <p/>
 * With modificiations.
 * <p/>
 * Prevents dependance on sun jdk's Base64Encoder
 * 
 * @author andy
 * 
 */
class Base64 {

	private static final String base64code = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
			+ "abcdefghijklmnopqrstuvwxyz" + "0123456789" + "+/";

	private static final int splitLinesAt = 76;

	public static byte[] zeroPad(int length, byte[] bytes) {
		byte[] padded = new byte[length]; // initialized to zero by JVM
		System.arraycopy(bytes, 0, padded, 0, bytes.length);
		return padded;
	}

	public static String encode(String string) {

		byte[] stringArray;
		try {
			stringArray = string.getBytes("UTF-8"); // use appropriate encoding
																							// string!
		} catch (Exception ignored) {
			stringArray = string.getBytes(); // use locale default rather than croak
		}
		return encode(stringArray);
	}

	public static String encode(byte[] bytes) {
		String encoded = "";
		// determine how many padding bytes to add to the output
		int paddingCount = (3 - (bytes.length % 3)) % 3;
		// add any necessary padding to the input
		bytes = zeroPad(bytes.length + paddingCount, bytes);
		// process 3 bytes at a time, churning out 4 output bytes
		// worry about CRLF insertions later
		for (int i = 0; i < bytes.length; i += 3) {
			int j = ((bytes[i] & 0xff) << 16) + ((bytes[i + 1] & 0xff) << 8)
					+ (bytes[i + 2] & 0xff);
			encoded = encoded + base64code.charAt((j >> 18) & 0x3f)
					+ base64code.charAt((j >> 12) & 0x3f)
					+ base64code.charAt((j >> 6) & 0x3f) + base64code.charAt(j & 0x3f);
		}
		// replace encoded padding nulls with "="
		return splitLines(encoded.substring(0, encoded.length() - paddingCount)
				+ "==".substring(0, paddingCount));
	}

	public static String splitLines(String string) {

		String lines = "";
		for (int i = 0; i < string.length(); i += splitLinesAt) {

			lines += string.substring(i, Math.min(string.length(), i + splitLinesAt));
			// lines += "\r\n";

		}
		return lines;

	}

	public static void main(String[] args) {

		for (int i = 0; i < args.length; i++) {

			System.err.println("encoding \"" + args[i] + "\"");
			System.out.println(encode(args[i]));

		}

	}

	public static void encode(InputStream in, OutputStream out)
			throws IOException {
		ByteArrayOutputStream tout = new ByteArrayOutputStream();
		byte[] buff = new byte[512];
		int read = in.read(buff);
		while (read > 0) {
			tout.write(buff, 0, read);
			read = in.read(buff);
		}
		String result = encode(tout.toByteArray());
		out.write(result.getBytes());
	}

}
