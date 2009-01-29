package ar.com.hjg.pngj;

/**
 * Un stream a memoria que permite flushear fragmentos a otros destino, cada size bytes
 */
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public abstract class ProgressiveOutputStream extends ByteArrayOutputStream {

	private final int size;

	public ProgressiveOutputStream(int size) {
		this.size = size;
	}

	@Override
	public void close() throws IOException {
		flush();
		super.close();
	}

	@Override
	public void flush() throws IOException {
		super.flush();
		checkFlushBuffer(true);
	}

	@Override
	public void write(byte[] b, int off, int len) {
		super.write(b, off, len);
		checkFlushBuffer(false);
	}

	@Override
	public void write(byte[] b) throws IOException {
		super.write(b);
		checkFlushBuffer(false);
	}

	@Override
	public void write(int arg0) {
		super.write(arg0);
		checkFlushBuffer(false);
	}

	@Override
	public synchronized void reset() {
		super.reset();
	}

	/**
	 * si corresponde flushear datos (o si forcer==true) llama al metodo
	 * abstracto flushBuffer y limpia esos bytes del buffer propio
	 */
	private void checkFlushBuffer(boolean forced) {
		while (forced || count >= size) {
			int nb = size;
			if (nb > count)
				nb = count;
			if (nb == 0)
				return;
			flushBuffer(buf, nb);
			int bytesleft = count - nb;
			count = bytesleft;
			if (bytesleft > 0)
				System.arraycopy(buf, nb, buf, 0, bytesleft);
		}
	}

	public abstract void flushBuffer(byte[] b, int n);

}