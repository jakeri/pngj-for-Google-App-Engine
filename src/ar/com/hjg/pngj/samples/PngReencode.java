package ar.com.hjg.pngj.samples;

import ar.com.hjg.pngj.ImageLine;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;

/**
 * reencodes a png image with a given filter and compression level
 */
public class PngReencode {

	public static void reencode(String orig, String dest, int filter, int cLevel) {
		PngReader pngr = new PngReader(PngReader.fileToStream(orig));
		PngWriter pngw = new PngWriter(dest, pngr.imgInfo);
		pngw.setOverrideFile(true);
		System.out.println(pngr.toString());

		pngw.setFilterType(filter);
		pngw.setCompLevel(cLevel);

		pngw.prepare(pngr); // not necesary; but this can copy some informational chunks from original 

		System.out.printf("Creating Image %s  filter=%d compLevel=%d \n", pngw.getFilename(), filter, cLevel);

		for (int row = 0; row < pngr.imgInfo.rows; row++) {
			ImageLine l1 = pngr.readRow(row);
			//pngw.writeRow(l1.vals, row);
			pngw.writeRow(l1);
		}

		pngr.end();
		pngw.end();

		System.out.println("Done");
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 4) {
			System.err.println("Arguments: [pngsrc] [pngdest] [filter] [compressionlevel]");
			System.err.println(" Where filter = 0..4  , compressionLevel = 0 .. 9");
			System.exit(1);
		}
		reencode(args[0], args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]));
	}
}
