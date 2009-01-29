package ar.com.hjg.pngj;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.zip.CRC32;

public class PngChunk {
	public final int len;
	public final String id; // 4 letras
	public final byte[] idbytes; // 4 bytes
	public byte[] data = null; // sin incluir crc
	public Integer crcval = null;
	private final CRC32 crcengine;
	private String textKey; // key y val solo son relevantes para tEXt. computo
	// lazy
	private String textVal;

	public PngChunk(int len, byte[] idbytes, CRC32 crcengine) {
		if (idbytes.length != 4)
			throw new PngjException("bad chunk len");
		this.idbytes = idbytes;
		this.id = new String(idbytes);
		this.len = len;
		this.crcengine = crcengine;
	}

	public static PngChunk createTextChunk(String key, String val, CRC32 crcengine) {
		if (val == null || key == null)
			return null;
		val = val.trim();
		key = key.trim();
		if (val.isEmpty() || key.isEmpty())
			return null;
		try {
			byte[] b = (key + "\0" + val).getBytes("ISO-8859-1");
			PngChunk chunk = new PngChunk(b.length, PngHelper.ITEXT, crcengine);
			chunk.data = b;
			chunk.textKey = key;
			chunk.textVal = val;
			chunk.computeCrc();
			return chunk;
		} catch (UnsupportedEncodingException e) {
			throw new PngjInputException("bad textual data: Only latin1", e);
		}
	}

	/**
	 * solo para tipo text. (si no, devuelve vacio)
	 */
	public String getTextKeyword() {
		if (!id.equals(PngHelper.ITEXT_TEXT))
			return "";
		if (textKey == null) {
			try {
				String[] k = (new String(data, "ISO-8859-1")).split("\0");
				textKey = k[0];
				textVal = k[1];
			} catch (UnsupportedEncodingException e) {
				throw new PngjException(e);
			}
		}
		return textKey;
	}

	public String getTextVal() {
		getTextKeyword(); // fuerza computo si no existia (bueno!)
		return textVal;
	}

	/**
	 * call after setting data, before writing to os
	 */
	public void computeCrc() {
		crcengine.reset();
		crcengine.update(idbytes, 0, 4);
		crcengine.update(data, 0, len); //
		crcval = Integer.valueOf((int) crcengine.getValue());
	}

	public boolean isCritical() { // critical or ancillary chunk?
		// primer letra es mayuscula
		return (idbytes[0] & 0x20) == 0;
	}

	public boolean isPublic() { // public or private chunk?
		// sgunda letra es mayuscula
		return (idbytes[1] & 0x20) == 0;
	}

	public boolean isSafeToCopy() { // safe to copy?
		// cuarta letra es minusucla
		return (idbytes[3] & 0x20) != 0;
	}

	public void writeChunk(OutputStream os) {
		if (idbytes.length != 4)
			throw new PngjException("bad chunkid [" + id + "]");
		PngHelper.writeInt4(os, len);
		PngHelper.writeBytes(os, idbytes);
		PngHelper.writeBytes(os, data, 0, len);
		PngHelper.writeInt4(os, crcval.intValue());
	}

	public String toString() {
		return "chunkid=" + id + " len=" + len + " " + getTextKeyword();
	}

	/**
	 * llamar con is despues del chunkid. ojo: usar solo para chunk cortos!
	 * aloca los bytes (si es null) y checkea crc Queda situaado despues del crc
	 */
	public void readChunk(InputStream is) {
		if (data == null)
			data = new byte[len];
		PngHelper.readBytes(is, data, 0, len);
		int crcori = PngHelper.readInt4(is);
		computeCrc();
		if (crcori != crcval)
			throw new RuntimeException("crc no coincide " + toString() + " calc=" + crcval + " read=" + crcori);
	}

	public ByteArrayInputStream getAsByteStream() {
		return new ByteArrayInputStream(data);
	}
}
