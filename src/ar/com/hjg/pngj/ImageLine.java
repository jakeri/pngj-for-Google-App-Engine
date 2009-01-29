package ar.com.hjg.pngj;

/**
 * Wrappea una linea de una imagen
 * 
 * Reusable
 */
public class ImageLine {
	private final static double BIG_VALUE = Double.MAX_VALUE * 0.5;
	private final static double BIG_VALUE_NEG = Double.MAX_VALUE * (-0.5);

	public final ImageInfo imgInfo;

	public final int len; // in bytes
	public final int channels;
	public final int bitDepth;
	public int rown = -1; // optativo
	public int[] scanline = null;

	public ImageLine(ImageInfo imgInfo) {
		this.imgInfo = imgInfo;
		channels = imgInfo.channels;
		len = channels * imgInfo.cols;
		scanline = new int[len];
		this.bitDepth = imgInfo.bitDepth;
	}

	public String toString() {
		return "row=" + rown + " cols=" + imgInfo.cols + " bpc=" + imgInfo.bitDepth + " size=" + len;
	}

	/**
	 * muestra info de primer y ultimo pixel. para debug (does not include
	 * alpha)
	 */
	public String infoPixelsExtremos() {
		return imgInfo.channels == 1 ? String.format("pimero=(%d) ultimo=(%d)", scanline[0], scanline[len - 1]) : String.format(
				"pimero=(%d %d %d) ultimo=(%d %d %d)", scanline[0], scanline[1], scanline[2], scanline[len - imgInfo.channels],
				scanline[len - imgInfo.channels + 1], scanline[len - imgInfo.channels + 2]);
	}

	public String infoFull() {
		return "row=" + rown + " " + computeStats().toString() + "\n  " + infoPixelsExtremos();
	}

	/**
	 * varias estadisticas de una linea.
	 */
	public class ImageLineStats {
		public double[] prom = { 0.0, 0.0, 0.0 }; // promedios de cada canal
		public double[] maxv = { BIG_VALUE_NEG, BIG_VALUE_NEG, BIG_VALUE_NEG }; // maximo de cada canal
		public double[] minv = { BIG_VALUE, BIG_VALUE, BIG_VALUE };
		public double promlum = 0.0; // maximo global (luminancia)
		public double maxlum = BIG_VALUE_NEG; // luminancia maxima
		public double minlum = BIG_VALUE;
		public double[] maxdif = { BIG_VALUE_NEG, BIG_VALUE_NEG, BIG_VALUE_NEG }; // maxima diferencia entre valores vecinos (valor absoluto)

		public String toString() {
			return String.format("prom=%.1f (%.1f %.1f %.1f) max=%.1f (%.1f %.1f %.1f) min=%.1f (%.1f %.1f %.1f)", promlum,
					prom[0], prom[1], prom[2], maxlum, maxv[0], maxv[1], maxv[2], minlum, minv[0], minv[1], minv[2])
					+ String.format(" maxdif=(%.1f %.1f %.1f)", maxdif[0], maxdif[1], maxdif[2]);

		}
	}

	public ImageLineStats computeStats() {
		if (channels < 3)
			throw new PngjException("only for RGB RGBA");
		ImageLineStats stat = new ImageLineStats();
		int ch = 0;
		double x, y, z, d, lum;
		y = z = 0.0;
		for (int i = 0; i < len; i++) {
			x = int2double(scanline[i]);
			stat.prom[ch] += x;
			if (x > stat.maxv[ch])
				stat.maxv[ch] = x;
			if (x < stat.minv[ch])
				stat.minv[ch] = x;
			if (i >= channels) {
				d = Math.abs(x - int2double(scanline[i - channels]));
				if (d > stat.maxdif[ch])
					stat.maxdif[ch] = d;
			}
			if (ch == 2) {
				lum = x + y + z;
				stat.promlum += lum;
				if (lum > stat.maxlum)
					stat.maxlum = lum;
				if (lum < stat.minlum)
					stat.minlum = lum;
				ch = 0;
				if (channels == 4)
					i++; // SALTEO ALPHA
			} else {
				ch++;
			}
			z = y;
			y = x;
		}
		for (ch = 0; ch < 3; ch++) {
			stat.prom[ch] /= imgInfo.cols;
		}
		stat.promlum /= (imgInfo.cols * 3.0);
		stat.maxlum /= 3.0;
		stat.minlum /= 3.0;
		return stat;
	}

	public void setValD(int i, double d) {
		scanline[i] = double2int(d);
	}

	public double int2double(int p) {
		return bitDepth == 16 ? p / 65535.0 : p / 255.0; // tal vez convendria reemplazar por multiplicacion ?
	}

	public double int2doubleClamped(int p) {
		double d = bitDepth == 16 ? p / 65535.0 : p / 255.0; // tal vez convendria reemplazar por multiplicacion ?
		return d <= 0.0 ? 0 : (d >= 1.0 ? 1.0 : d);
	}

	public int double2int(double d) {
		d = d <= 0.0 ? 0 : (d >= 1.0 ? 1.0 : d);
		return bitDepth == 16 ? (int) (d * 65535.0 + 0.5) : (int) (d * 255.0 + 0.5); // 
	}

	public int double2intClamped(double d) {
		d = d <= 0.0 ? 0 : (d >= 1.0 ? 1.0 : d);
		return bitDepth == 16 ? (int) (d * 65535.0 + 0.5) : (int) (d * 255.0 + 0.5); // 
	}

}
