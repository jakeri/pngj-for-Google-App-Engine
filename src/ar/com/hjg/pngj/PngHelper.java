package ar.com.hjg.pngj;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.CRC32;

/**
 * algunos metodos estaticos para PngWriter
 */
public class PngHelper {

	public static final byte[] pngIdBytes = { -119, 80, 78, 71, 13, 10, 26, 10 }; // png magic

	public static final String IHDR_TEXT = "IHDR";
	public static final String IDAT_TEXT = "IDAT";
	public static final String IEND_TEXT = "IEND";
	public static final String ITEXT_TEXT = "tEXt"; // case is important!
	public static final String IPHYS_TEXT = "pHYs";

	public static final byte[] IHDR = IHDR_TEXT.getBytes();
	public static final byte[] IDAT = IDAT_TEXT.getBytes();
	public static final byte[] IEND = IEND_TEXT.getBytes();
	public static final byte[] ITEXT = ITEXT_TEXT.getBytes();
	public static final byte[] IPHYS = IPHYS_TEXT.getBytes();

	public static final boolean DEBUG = true;

	public static void writeInt2(OutputStream os, int n) {
		byte[] temp = { (byte) ((n >> 8) & 0xff), (byte) (n & 0xff) };
		writeBytes(os, temp);
	}

	/**
	 * -1 si eof
	 */
	public static int readInt2(InputStream is) {
		try {
			int b1 = is.read();
			int b2 = is.read();
			if (b1 == -1 || b2 == -1)
				return -1;
			return (b1 << 8) + b2;
		} catch (IOException e) {
			throw new PngjInputException("error reading readInt2", e);
		}
	}

	/**
	 * -1 si eof
	 */
	public static int readInt4(InputStream is) {
		try {
			int b1 = is.read();
			int b2 = is.read();
			int b3 = is.read();
			int b4 = is.read();
			if (b1 == -1 || b2 == -1 || b3 == -1 || b4 == -1)
				return -1;
			return (b1 << 24) + (b2 << 16) + (b3 << 8) + b4;
		} catch (IOException e) {
			throw new PngjInputException("error reading readInt4", e);
		}
	}

	public static void writeInt4(OutputStream os, int n) {
		byte[] temp = { (byte) ((n >> 24) & 0xff), (byte) ((n >> 16) & 0xff), (byte) ((n >> 8) & 0xff), (byte) (n & 0xff) };
		writeBytes(os, temp);
	}

	/**
	 * guaranteed to read exactly len bytes. throws error if it cant
	 */
	public static void readBytes(InputStream is, byte[] b, int offset, int len) {
		if (len == 0)
			return;
		try {
			int read = 0;
			while (read < len) {
				int n = is.read(b, offset + read, len - read);
				if (n < 1)
					throw new RuntimeException("error leyendo " + n + " !=" + len);
				read += n;
			}
		} catch (IOException e) {
			throw new PngjInputException("error reading", e);
		}
	}

	public static void writeBytes(OutputStream os, byte[] b) {
		try {
			os.write(b);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void writeBytes(OutputStream os, byte[] b, int offset, int n) {
		try {
			os.write(b, offset, n);
		} catch (IOException e) {
			throw new PngjOutputException(e);
		}
	}

	public static int readByte(InputStream is) {
		try {
			return is.read();
		} catch (IOException e) {
			throw new PngjOutputException(e);
		}
	}

	public static void writeByte(OutputStream os, byte b) {
		try {
			os.write(b);
		} catch (IOException e) {
			throw new PngjOutputException(e);
		}
	}

	public static void writeChunk(OutputStream os, byte[] buf, int offset, int n, byte[] chunkid, CRC32 crc) {
		if (chunkid.length != 4)
			throw new PngjOutputException("bad chunkid [" + new String(chunkid) + "]");
		int chunkLength = n;
		crc.reset();
		PngHelper.writeInt4(os, chunkLength);
		PngHelper.writeBytes(os, chunkid);
		PngHelper.writeBytes(os, buf, 0, n);
		crc.update(chunkid, 0, chunkid.length); // el crc no inlucye los 4 bytes del length, pero si el id
		crc.update(buf, 0, n); // 
		PngHelper.writeInt4(os, (int) crc.getValue());
	}

	/**
	 * Lee un chunk completo Se supone que ya hemos leido el largo y el tipo de
	 * chunk (sigue el dato y el crc) Devuelve copia el contenido en byte[] sin
	 * incluir id ni crc. Usar solo para chunks chicos! Deja el stream al fin
	 * del chunk (crc leido inclusive)
	 */
	public static void readChunk(InputStream is, byte[] buff, int offset, int len, byte[] chunkid, CRC32 crc) {
		crc.reset();
		crc.update(chunkid, 0, chunkid.length); // el crc no inlucye los 4 bytes del length, pero si el id
		readBytes(is, buff, offset, len);
		crc.update(buff, offset, len); //
		int crcval = (int) crc.getValue();
		int crcori = readInt4(is);
		if (crcori != crcval)
			throw new PngjBadCrcException("crc no coincide " + new String(chunkid) + " calc=" + crcval + " read=" + crcori);
	}

	public static int filterPaethPredictor(int a, int b, int c) {
		//from http://www.libpng.org/pub/png/spec/1.2/PNG-Filters.html
		//  a = left, b = above, c = upper left
		int p = a + b - c;//       ; initial estimate
		int pa = Math.abs(p - a); // distances to a, b, c
		int pb = Math.abs(p - b);
		int pc = Math.abs(p - c);
		//; return nearest of a,b,c,
		//; breaking ties in order a,b,c.
		if (pa <= pb && pa <= pc)
			return a;
		else if (pb <= pc)
			return b;
		else
			return c;
	}

	public static void logdebug(String msg) {
		if (DEBUG)
			System.out.println(msg);
	}
}
