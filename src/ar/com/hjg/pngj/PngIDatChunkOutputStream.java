package ar.com.hjg.pngj;

import java.io.OutputStream;
import java.util.zip.CRC32;

/**
 * para outputear el stream correspondiente al chunk IDAT de PNG, fragmentado en
 * pedazos de tama�o fijo (8192 por defaut).
 */
public class PngIDatChunkOutputStream extends ProgressiveOutputStream {
	private static final int SIZE_DEFAULT = 8192;

	private final OutputStream outputStream;
	private final CRC32 crc;

	public PngIDatChunkOutputStream(OutputStream outputStream) {
		this(outputStream, SIZE_DEFAULT);
	}

	public PngIDatChunkOutputStream(OutputStream outputStream, int size) {
		super(size);
		this.outputStream = outputStream;
		crc = new CRC32();
	}

	@Override
	public void flushBuffer(byte[] b, int n) {
		PngHelper.writeChunk(outputStream, b, 0, n, PngHelper.IDAT, crc);
	}

}
