package ar.com.hjg.pngj;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.zip.CRC32;
import java.util.zip.InflaterInputStream;

/**
 * Reads a PNG image, line by line
 */
public class PngReader {
	public static final int FILTER_NONE = 0;
	public static final int FILTER_SUB = 1;
	public static final int FILTER_UP = 2;
	public static final int FILTER_AVERAGE = 3;
	public static final int FILTER_PAETH = 4;

	public final ImageInfo imgInfo;
	public final String filename;
	private final InputStream is;
	private final InflaterInputStream idatIstream;
	private final PngIDatChunkInputStream iIdatCstream;
	private int offset = 0;
	private CRC32 crcengine;

	// chunks: agregar exclusivamente con addChunkToList()
	private static final int MAX_BYTES_CHUNKS_TO_LOAD = 64000;
	private int bytesChunksLoaded;
	private List<PngChunk> chunks1 = new ArrayList<PngChunk>(); // pre idat
	private List<PngChunk> chunks2 = new ArrayList<PngChunk>(); // post idat

	private final int bytesPerRow; // sin inlcuir byte de filtro
	private final int valsPerRow; // smaples per row= cols x channels

	private int rowNum = -1; // numero de linea leida (actual)
	private ImageLine imgLine;
	private int[] rowb = null; // linea covnertida a byte; empieza en 1; (el 0 se usara para tipo de filtro)
	private int[] rowbprev = null; // rowb previa
	private byte[] rowbfilter = null; // linea actual filtrada

	/**
	 * The constructor loads the header and first chunks, 
	 * stopping at the beginning of the image data (IDAT chunks)
	 * @param filename   Path of image file
	 */
	public PngReader(String filename) {
		this.filename = filename;
		crcengine = new CRC32();
		File file = new File(filename);
		if (!file.exists() || !file.canRead())
			throw new PngjInputException("Can open file for reading (" + filename + ")");
		try {
			is = new BufferedInputStream(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			throw new PngjInputException("Can open file for reading (" + filename + ")");
		}
		// reads header (magic bytes)
		byte[] pngid = new byte[PngHelper.pngIdBytes.length];
		PngHelper.readBytes(is, pngid, 0, pngid.length);
		offset += pngid.length;
		if (!Arrays.equals(pngid, PngHelper.pngIdBytes))
			throw new PngjInputException("Bad file id (" + filename + ")");
		// reads first chunks
		int clen = PngHelper.readInt4(is);
		offset += 4;
		if (clen != 13)
			throw new RuntimeException("IDHR chunk len != 13 ?? " + clen);
		byte[] chunkid = new byte[4];
		PngHelper.readBytes(is, chunkid, 0, 4);
		offset += 4;
		PngChunk ihdr = new PngChunk(clen, chunkid, crcengine);
		if (!ihdr.id.equals(PngHelper.IHDR_TEXT))
			throw new PngjInputException("IHDR not found as first chunk??? [" + ihdr + "]");
		ihdr.readChunk(is);
		offset += ihdr.len + 4;
		addChunkToList(ihdr, chunks1, true);
		ByteArrayInputStream ihdr_s = ihdr.getAsByteStream();
		int cols = PngHelper.readInt4(ihdr_s);
		int rows = PngHelper.readInt4(ihdr_s);
		int bitspc = PngHelper.readByte(ihdr_s);// bit depth: number of bits per channel
		int colormodel = PngHelper.readByte(ihdr_s); // 6 (alpha) 2 sin alpha
		int compmeth = PngHelper.readByte(ihdr_s); // 
		int filmeth = PngHelper.readByte(ihdr_s); //
		int interlaced = PngHelper.readByte(ihdr_s);
		if (interlaced != 0)
			throw new PngjInputException("Interlaced no implemented");
		if (filmeth != 0 || compmeth != 0)
			throw new PngjInputException("compmethod o filtermethod unrecognized");
		boolean alpha = (colormodel & 0x04) != 0;
		boolean palette = (colormodel & 0x01) != 0;
		boolean grayscale = (colormodel == 0 || colormodel == 4);
		if (bitspc != 8 && bitspc != 16)
			throw new RuntimeException("Bit depth not supported " + bitspc);
		imgInfo = new ImageInfo(cols, rows, bitspc, alpha, grayscale, palette);
		imgLine = new ImageLine(imgInfo);
		this.bytesPerRow = imgInfo.bytesPixel * imgInfo.cols;
		this.valsPerRow = imgInfo.cols * imgInfo.channels;
		// allocation
		rowb = new int[bytesPerRow + 1];
		rowbprev = new int[bytesPerRow + 1];
		rowbfilter = new byte[bytesPerRow + 1];
		int idatLen = readFirstChunks();
		if (idatLen < 0)
			throw new PngjInputException("first idat chunk not found!");
		iIdatCstream = new PngIDatChunkInputStream(is, idatLen, offset);
		idatIstream = new InflaterInputStream(iIdatCstream);
	}

	/**
	 * devuelve flag overflow ( true si no lo agregamos con datos porque se
	 * excedio capacidad en memoria)
	 */
	private boolean addChunkToList(PngChunk chunk, List<PngChunk> list, boolean includedata) {
		boolean overflow = false;
		if (includedata && bytesChunksLoaded + chunk.len > MAX_BYTES_CHUNKS_TO_LOAD) {
			overflow = true;
			includedata = false;
		}
		if (includedata) {
			bytesChunksLoaded += chunk.len;
			if (chunk.data == null || chunk.len != chunk.data.length)
				throw new PngjException("error en longitud de chunk a almacenar");
		} else {
			chunk.data = null; // por las dudas
		}
		list.add(chunk);
		return overflow;
	}

	/**
	 * lee (y procesa ?) los chunks anteriores al primer IDAT. Se llama
	 * inmediatamente despues de haber leido IDHR (crc incluido) devuelve el
	 * largo del primer chunk IDAT encontrado. Queda posicionado despues del
	 * IDAT id
	 * */
	private int readFirstChunks() {
		int clen = 0;
		boolean found = false;
		while (!found) {
			clen = PngHelper.readInt4(is);
			offset += 4;
			byte[] chunkid = new byte[4];
			if (clen < 0)
				break;
			PngHelper.readBytes(is, chunkid, 0, 4);
			offset += 4;
			if (Arrays.equals(chunkid, PngHelper.IDAT)) {
				found = true;
				break;
			} else if (Arrays.equals(chunkid, PngHelper.IEND)) { 
				break; //?? 
			}
			PngChunk chunk = new PngChunk(clen, chunkid, crcengine);
			chunk.readChunk(is);
			offset += chunk.len + 4;
			addChunkToList(chunk, chunks1, true);
		}
		return found ? clen : -1;
	}

	/**
	 * lee (y procesa ?) los chunks posteriores al ultimo IDAT (crc incluido).
	 * 
	 * */
	private void readLastChunks() {
		PngHelper.logdebug("idat ended? " + iIdatCstream.isEnded());
		int clen = iIdatCstream.getLenLastChunk();
		byte[] chunkid = iIdatCstream.getIdLastChunk();
		boolean endfound = false;
		boolean first = true;
		while (!endfound) {
			if (!first) {
				clen = PngHelper.readInt4(is);
				offset += 4;
				if (clen < 0)
					throw new PngjInputException("bad len " + clen);
				chunkid = new byte[4];
				PngHelper.readBytes(is, chunkid, 0, 4);
				offset += 4;
			}
			first = false;
			if (Arrays.equals(chunkid, PngHelper.IDAT)) {
				throw new PngjInputException("extra IDA CHUNKS ??");
			} else if (Arrays.equals(chunkid, PngHelper.IEND)) {
				endfound = true;
			}
			PngChunk chunk = new PngChunk(clen, chunkid, crcengine);
			chunk.readChunk(is);
			offset += chunk.len + 4;
			addChunkToList(chunk, chunks2, true);
		}
		if (!endfound)
			throw new PngjInputException("end chunk not found");
		PngHelper.logdebug("end chunk found ok offset=" + offset);
	}

	
	/** 
	 * calls readRow(int[] buffer, int nrow),  usin LineImage as buffer
	 * @return 
	 */
	public ImageLine readRow(int nrow) {
		readRow(imgLine.scanline,nrow);
		imgLine.incRown();
		return imgLine;
	}
	
	/**
	 * Reads a line and returns it as a int array
	 * Buffer can be prealocated (in this case it must have enough len!)
	 * or can be null
	 * See also the other overloaded method
	 * @param buffer  
	 * @param nrow
	 * @return  The same buffer if it was allocated, a newly allocated one otherwise
	 */
	public int[] readRow(int[] buffer, int nrow) {
		if (nrow < 0 || nrow >= imgInfo.rows)
			throw new PngjInputException("invalid line");
		if (nrow != rowNum + 1)
			throw new PngjInputException("invalid line (expected: " + (rowNum + 1));
		rowNum++;
		if(buffer==null)
			buffer = new int[valsPerRow];
		// swap
		int[] tmp = rowb;
		rowb = rowbprev;
		rowbprev = tmp;
		// carga en rowbfilter los bytes "raw", con el filtro
		PngHelper.readBytes(idatIstream, rowbfilter, 0, bytesPerRow + 1);
		rowb[0] = rowbfilter[0];
		unfilterRow();
		convertRowFromBytes(buffer);
		return buffer;
	}



	private void convertRowFromBytes(int[] buffer) {
		// http://www.libpng.org/pub/png/spec/1.2/PNG-DataRep.html
		int i, j;
		if (imgInfo.bitDepth == 8) {
			for (i = 0, j = 1; i < valsPerRow; i++) {
				buffer[i] = (rowb[j++]);
			}
		} else { // 16 bitspc
			for (i = 0, j = 1; i < valsPerRow; i++) {
				buffer[i] = (rowb[j++] << 8) + rowb[j++];
			}
		}
	}

	private void unfilterRow() {
		int filterType = rowbfilter[0];
		switch (filterType) {
		case FILTER_NONE:
			unfilterRowNone();
			break;
		case FILTER_SUB:
			unfilterRowSub();
			break;
		case FILTER_UP:
			unfilterRowUp();
			break;
		case FILTER_AVERAGE:
			unfilterRowAverage();
			break;
		case FILTER_PAETH:
			unfilterRowPaeth();
			break;
		default:
			throw new PngjInputException("Filter type " + filterType + " not implemented");
		}
	}

	private void unfilterRowNone() {
		for (int i = 1; i <= bytesPerRow; i++) {
			rowb[i] = (int) (rowbfilter[i] & 0xFF);
		}
	}

	private void unfilterRowSub() {
		int i, j;
		for (i = 1; i <= imgInfo.bytesPixel; i++) {
			rowb[i] = (int) (rowbfilter[i] & 0xFF);
		}
		for (j = 1, i = imgInfo.bytesPixel + 1; i <= bytesPerRow; i++, j++) {
			rowb[i] = ((int) (rowbfilter[i] & 0xFF) + rowb[j]) & 0xFF;
		}
	}

	private void unfilterRowUp() {
		for (int i = 1; i <= bytesPerRow; i++) {
			rowb[i] = ((int) (rowbfilter[i] & 0xFF) + rowbprev[i]) & 0xFF;
		}
	}

	private void unfilterRowAverage() {
		int i, j, x;
		for (j = 1 - imgInfo.bytesPixel, i = 1; i <= bytesPerRow; i++, j++) {
			x = j > 0 ? rowb[j] : 0;
			rowb[i] = ((int) (rowbfilter[i] & 0xFF) + (x + rowbprev[i]) / 2) & 0xFF;
		}
	}

	private void unfilterRowPaeth() {
		int i, j, x, y;
		for (j = 1 - imgInfo.bytesPixel, i = 1; i <= bytesPerRow; i++, j++) {
			x = j > 0 ? rowb[j] : 0;
			y = j > 0 ? rowbprev[j] : 0;
			rowb[i] = ((int) (rowbfilter[i] & 0xFF) + PngHelper.filterPaethPredictor(x, rowbprev[i], y)) & 0xFF;
		}
	}

	/**
	 * This should be called after having read the last line.
	 */
	public void end() {
		offset = (int) iIdatCstream.getOffset();
		try {
			idatIstream.close();
		} catch (Exception e) {
		}
		readLastChunks();
		try {
			is.close();
		} catch (Exception e) {
			throw new PngjInputException("error closing input stream!", e);
		}
	}

	/**
	 * Get first chunks (before IDAT)
	 */
	public List<PngChunk> getChunks1() {
		return chunks1;
	}

	/**
	 * Get last chunks (after IDAT)
	 */
	public List<PngChunk> getChunks2() {
		return chunks2;
	}

	public void showChunks() {
		for (PngChunk c : chunks1) {
			System.out.println(c);
		}
		System.out.println("-----------------");
		for (PngChunk c : chunks2) {
			System.out.println(c);
		}
	}

	public String toString() { // info basica
		return "filename=" + filename + " " + imgInfo.toString();
	}

	/** para debug */
	public static void showLineInfo(ImageLine line) {
		System.out.println(line);
		System.out.println(line.computeStats());
		System.out.println(line.infoFirstLastPixels());
	}

	public static void main(String[] args) {
		Locale.setDefault(Locale.US);
		PngReader png = new PngReader("resources/test8.png");
		System.out.println(png);
		for (int i = 0; i < png.imgInfo.rows; i++) {
			png.readRow(i);
			if (i % 50 == 0)
				showLineInfo(png.imgLine);
		}

		png.end();
		png.showChunks();
	}
}
