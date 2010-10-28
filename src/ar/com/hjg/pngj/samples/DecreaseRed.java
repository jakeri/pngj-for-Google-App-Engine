package ar.com.hjg.pngj.samples;

import ar.com.hjg.pngj.ImageLine;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;

/**
 * Example: cuts the red channel by two
 */
public class DecreaseRed {

	public static void decreaseRed(String origFilename, String destFilename) {
		PngReader pngr = new PngReader(PngReader.fileToStream(origFilename));
		PngWriter pngw = new PngWriter(destFilename, pngr.imgInfo);
		pngw.setOverrideFile(true);  // allows to override writen file if it already exits
		System.out.println(pngr.toString());
		pngw.prepare(pngr); // not necesary; but this can copy some informational chunks from original 
		int channels = pngr.imgInfo.channels;
		if(channels<3) throw new RuntimeException("Only for truecolour images");
		for (int row = 0; row < pngr.imgInfo.rows; row++) {
			ImageLine l1 = pngr.readRow(row);
			for(int j=0;j<pngr.imgInfo.cols;j++)
				l1.scanline[j*channels]/=2;
			pngw.writeRow(l1);
		}
		pngr.end();
		pngw.end();
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.err.println("Arguments: [pngsrc] [pngdest]");
			System.exit(1);
		}
		decreaseRed(args[0], args[1]);
		System.out.println("Done");
	}
}
