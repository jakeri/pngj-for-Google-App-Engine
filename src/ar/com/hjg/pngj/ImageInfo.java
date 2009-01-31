package ar.com.hjg.pngj;

/**
 * Simple immutable wrapper for basic image info Some parameters are clearly
 * redundant The constructor requires an 'ortogonal' subset
 */
public class ImageInfo {

	/**
	 * image width, in pixels
	 */
	public final int cols;
	/**
	 * image height, in pixels
	 */
	public final int rows;
	/**
	 * Bits per sample in the buffer This is 8-6 for RGB true color and
	 * grayscale imags For indexed images, number of bits per palette index (1 2
	 * 4 8)
	 */
	public final int bitDepth;

	/**
	 * Number of channels, used in the buffer (caution!) This is 3-4 for
	 * rgb/rgba, but 1 for palette/gray !
	 */
	public final int channels;

	/**
	 * Bytes used for each pixel in the buffer equals channel * bitDepth/8 (ONLY
	 * MAKES SENSE FOR bitDepth=8,16!!!!)
	 */
	public final int bytesPixel;
	/**
	 * Samples available for each scanline, in the buffer Equals cols x channels
	 */
	public final int samplesPerRow;

	public final boolean alpha;
	public final boolean greyscale;
	public final boolean indexed;

	private static final int MAX_COLS_ROWS_VAL = 100000; // ridicuous big value

	/**
	 * Constructor default: assumes RGB (truecolor)!
	 */
	public ImageInfo(int cols, int rows, int bitdepth, boolean alpha) {
		this(cols, rows, bitdepth, alpha, false, false);
	}

	/**
	 * Constructor
	 * 
	 * @param cols
	 *            width in pixels
	 * @param rows
	 *            height in pixels
	 * @param bitdepth
	 *            Bits per sample, in the buffer : 8-16 for RGB true color and
	 *            greyscale
	 * @param alpha
	 * @param grayscale
	 * @param palette
	 */
	public ImageInfo(int cols, int rows, int bitdepth, boolean alpha, boolean grayscale, boolean palette) {
		this.cols = cols;
		this.rows = rows;
		this.alpha = alpha;
		this.indexed = palette;
		this.greyscale = grayscale;
		if (greyscale && palette)
			throw new PngjException("palette and greyscale are exclusive");
		this.channels = (grayscale || palette) ? (alpha ? 2 : 1) : (alpha ? 4 : 3);
		this.samplesPerRow = channels * this.cols;
		//http://www.w3.org/TR/PNG/#11IHDR
		this.bitDepth = bitdepth;
		switch (this.bitDepth) {
		case 1:
		case 2:
		case 4:
			if (!(this.indexed || this.greyscale))
				throw new PngjException("only indexed or grayscale can have bitdepth=" + this.bitDepth);
			break;
		case 8:
			break;
		case 16:
			if (this.indexed)
				throw new PngjException("indexed can't have bitdepth=" + this.bitDepth);
			break;
		default:
			throw new PngjException("invalid bitdepth=" + this.bitDepth);
		}
		if (this.bitDepth < 8)
			throw new PngjException("sorry, unsupported (though valid) bitdepth=" + this.bitDepth);
		this.bytesPixel = (channels * this.bitDepth) / 8;
		if (cols < 1 || cols > MAX_COLS_ROWS_VAL)
			throw new PngjException("cols=" + cols + " ???");
		if (rows < 1 || rows > MAX_COLS_ROWS_VAL)
			throw new PngjException("rows=" + rows + " ???");
	}

	/**
	 * basic info. should include all relevant parameters, as equals() relies on
	 * this
	 */
	public String toString() {
		return "cols=" + cols + " rows=" + rows + " bitspc=" + bitDepth + " channels=" + channels;

	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj instanceof ImageInfo)
			return this.toString().equals(((ImageInfo) obj).toString());
		else
			return super.equals(obj);
	}

}
