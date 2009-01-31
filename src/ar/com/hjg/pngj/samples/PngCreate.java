package ar.com.hjg.pngj.samples;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLine;
import ar.com.hjg.pngj.PngWriter;

/**
 * Creates a test image (size , bit depth and alpha as arguments) Can choose
 * between simple gray image or test image with colour gradations As an example
 * and test.
 */
public class PngCreate {

	/**
	 * crea imagen de test: primera linea negra, segunda blanca. primera columna
	 * amarilla, ultima verde. Degrade de colores, y alpha transparente abajo a
	 * la izquierda
	 */
	private static void makeTestImage(PngWriter png) throws Exception {

		int cols = png.imgInfo.cols;
		int rows = png.imgInfo.rows;
		boolean alpha = png.imgInfo.alpha;
		//int bitspc = png.imgInfo.bitDepth;
		int channels = png.imgInfo.channels;
		int valuesPerRow = png.imgInfo.samplesPerRow;
		ImageLine iline = new ImageLine(png.imgInfo);
		iline.setRown(0);
		iline.setValD(0, 1.0);
		iline.setValD(1, 1.0); // primer columna amarilla
		iline.setValD(2, 0.0);

		iline.setValD(valuesPerRow - channels, 0);
		iline.setValD(valuesPerRow - channels + 1, 1.0); // ultima columna verde
		iline.setValD(valuesPerRow - channels + 2, 0);

		for (int j = 1; j < cols - 1; j++) { // primera fila: blanca
			iline.setValD(j * channels, 1.0);
			iline.setValD(j * channels + 1, 1.0);
			iline.setValD(j * channels + 2, 1.0);
		}
		if (alpha)
			addAlpha(iline);
		png.writeRow(iline);
		iline.incRown();

		for (int j = 1; j < cols - 1; j++) { // segunda fila: negra
			iline.setValD(j * channels, 0.0);
			iline.setValD(j * channels + 1, 0.0);
			iline.setValD(j * channels + 2, 0.0);
		}
		if (alpha)
			addAlpha(iline);
		png.writeRow(iline);
		iline.incRown();

		for (; iline.getRown()< rows; iline.incRown()) {
			for (int j = 1; j < cols - 1; j++) {
				iline.setValD(j * channels, clamp((2 * j / cols) - 0.3, 0, 1.0));
				iline.setValD(j * channels + 1, clamp((2 * iline.getRown()/ rows) - 0.4, 0, 1.0));
				iline.setValD(j * channels + 2,
						clamp((0.55 * Math.sin(13.0 * iline.getRown() / rows + j * 25.0 / cols) + 0.5), 0, 1.0));
			}
			if (alpha)
				addAlpha(iline);
			png.writeRow(iline);
		}
	}

	private static void addAlpha(ImageLine iline) {
		int cols = iline.imgInfo.cols;
		int rows = iline.imgInfo.rows;
		for (int i = 0; i < iline.imgInfo.cols; i++) {
			double alpha;
			if (i == 0 || i == iline.imgInfo.cols - 1 || iline.getRown()< 2)
				alpha = 1.0;
			else {
				// opaco arriba a la derecha, transparente abajo izquierda
				double d = Math.sqrt(((0.5 * i) / cols + 0.0) + ((0.5 * (rows - iline.getRown())) / rows + 0.0)); // entre 0 y 1
				d = d * 1.3 - 0.2;
				alpha = clamp(d, 0.0, 1.0);
			}
			iline.setValD(i * 4 + 3, clamp(alpha, 0, 1.0)); // asume que son 4 canales!
		}
	}

	private static double clamp(double d, double d0, double d1) {
		return d > d1 ? d1 : (d < d0 ? d0 : d);
	}

	public static void createTest1(String orig, int cols, int rows, int bitspc, int channels) throws Exception {
		if (channels != 3 && channels != 4)
			throw new RuntimeException("bad channels number (must be 3 or 4)");
		PngWriter i2 = new PngWriter(orig, new ImageInfo(cols, rows, bitspc, channels == 4));
		i2.setOverrideFile(true);
		makeTestImage(i2);
		i2.end(); // cierra el archivo
		System.out.println("Done: " + i2.getFilename());
	}

	public static void main(String[] args) throws Exception {
		if (args.length < 5) {
			System.err.println("Arguments: [pngdest] [cols] [rows] [bitsc] [channels] [type]");
			System.err.println(" Where bitsc=8|16, channels=3|4");
			System.err.println("type : 0: gray opaque image  1=test image with colors (default) ");
			System.exit(1);
		}
		int type = args.length == 6 ? Integer.parseInt(args[5]) : 1;
		int cols = Integer.parseInt(args[1]);
		int rows = Integer.parseInt(args[2]);
		int bitspc = Integer.parseInt(args[3]);
		int chan = Integer.parseInt(args[4]);

		if (type == 0)
			throw new RuntimeException("no implementado");
		else
			createTest1(args[0], cols, rows, bitspc, chan);
	}
}
