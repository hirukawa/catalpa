package net.osdn.catalpa;

import java.util.Arrays;

import lu.luz.jzopfli.Zopfli_lib;
import lu.luz.jzopfli.ZopfliH.ZopfliFormat;
import lu.luz.jzopfli.ZopfliH.ZopfliOptions;

public class Zopfli {
	
	public static byte[] compress(byte[] data) {
		ZopfliOptions options = new ZopfliOptions();
		ZopfliFormat output_type = ZopfliFormat.ZOPFLI_FORMAT_GZIP;
		byte[][] out = { { 0 } };
		int[] outsize = { 0 };

		Zopfli_lib.ZopfliCompress(
				options, output_type, data, data.length, out, outsize);

		return Arrays.copyOf(out[0], outsize[0]);
	}

}
