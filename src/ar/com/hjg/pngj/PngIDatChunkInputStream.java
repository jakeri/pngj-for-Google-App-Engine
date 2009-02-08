package ar.com.hjg.pngj;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.zip.CRC32;

/**
 * Lee de chunks idat
 */
public class PngIDatChunkInputStream extends InputStream {

	private final InputStream inputStream;
	private final CRC32 crcEngine;
	private int lenLastChunk;
	private byte[] idLastChunk = new byte[4];
	private int toReadThisChunk = 0;
	private boolean ended = false;
	private long offset; // offset en el inputstream 

	/**
	 **/
	public PngIDatChunkInputStream(InputStream iStream, int lenFirstChunk, int offset) {
		// El constructor debe llamarse justo despues de haber leido el len y el
		// id
		// del primer IDAT chunk
		this.offset = (long) offset;
		inputStream = iStream;
		crcEngine = new CRC32();
		this.lenLastChunk = lenFirstChunk;
		toReadThisChunk = lenFirstChunk;
		System.arraycopy(PngHelper.IDAT, 0, idLastChunk, 0, 4);// sabemos que
		// este es un
		// IDAT
		crcEngine.update(idLastChunk, 0, 4);

		PngHelper.logdebug("Init: len=" + lenLastChunk);
		if (lenFirstChunk == 0)
			endChunkGoForNext(); // raro, pero...
	}

	/**
	 * el close NO cierra el input stream asociado
	 */
	@Override
	public void close() throws IOException {
		super.close(); // nothing
	}

	private void endChunkGoForNext() {
		// se llama tras leer el ultimo byte del chunk. checkea crc y lee id de
		// proximo chuck.
		// En todo caso, en idLastChunk y lenLastChunk quedan esos datos
		// Tambien tiene la inteligencia para ignorar IDATS vacios si los
		// hubiera
		do {
			int crc = PngHelper.readInt4(inputStream); // no se checkea
			int crccalc = (int) crcEngine.getValue();
			if (crc != crccalc)
				throw new PngjBadCrcException("error reading idat; offset: " + offset);
			crcEngine.reset();
			lenLastChunk = PngHelper.readInt4(inputStream);
			if (lenLastChunk < 0)
				throw new PngjInputException("invalid len for chunk: " + lenLastChunk);
			toReadThisChunk = lenLastChunk;
			PngHelper.readBytes(inputStream, idLastChunk, 0, 4);
			offset += 12;
			ended = !Arrays.equals(idLastChunk, PngHelper.IDAT);
			if (!ended)
				crcEngine.update(idLastChunk, 0, 4);
			//PngHelper.logdebug("IdatChunkEnded. Next chunk len= " + lenLastChunk + " idat?" + (!ended));
		} while (lenLastChunk == 0 && !ended); // muy raro que el while se
		// cumpla (IDAT vacio??)
	}

	/**
	 * en algunos casos la lectura de la ultima fila no consume el chunk. 
	 * en esos casos hay que hacer una lectura dummy de los bytes que quedan
	 */
	public void forceChunkEnd() {
		if(!ended) {
			byte[] dummy = new byte[toReadThisChunk];
			PngHelper.readBytes(inputStream, dummy, 0, toReadThisChunk);
			crcEngine.update(dummy, 0, toReadThisChunk);
			endChunkGoForNext();
		}
	}

	
	/**
	 * puede devolver menos que len. pero nunca 0 -1 significa que termino
	 * "pseudo archivo" prematuramente. notar que eso es un error en nuestro
	 * caso
	 */
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (toReadThisChunk == 0)
			throw new RuntimeException("this should not happen");
		int n = inputStream.read(b, off, len >= toReadThisChunk ? toReadThisChunk : len);
		if (n > 0) {
			crcEngine.update(b, off, n);
			this.offset += n;
			toReadThisChunk -= n;
		}
		if (toReadThisChunk == 0) { // end of chunk: prepare for next
			endChunkGoForNext();
		}
		return n;
	}

	@Override
	public int read(byte[] b) throws IOException {
		return this.read(b, 0, b.length);
	}

	@Override
	public int read() throws IOException {
		PngHelper.logdebug("read() No deberia entrara aca");
		// muy ineficiente; pero es de suponer que se usa poco y nada
		byte[] b1 = new byte[1];
		int r = this.read(b1, 0, 1);
		return r < 0 ? -1 : (int) b1[0];
	}

	public int getLenLastChunk() {
		return lenLastChunk;
	}

	public byte[] getIdLastChunk() {
		return idLastChunk;
	}

	public long getOffset() {
		return offset;
	}

	public boolean isEnded() {
		return ended;
	}

}
