package net.osdn.catalpa.handler;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import net.osdn.catalpa.Context;
import net.osdn.catalpa.Handler;
import net.osdn.catalpa.Util;

public class BlockHandler implements Handler {

	protected static final String[] APPLICABLE_EXTENSIONS = new String[] {
		".markdown",
		".markdown.txt",
		".md",
		".md.txt",
		".yml"
	};
	
	@Override
	public int getPriority() {
		return 200;
	}

	@Override
	public boolean isApplicable(Path path) {
		String filename = path.getFileName().toString().toLowerCase();
		for(String ext : APPLICABLE_EXTENSIONS) {
			if(filename.endsWith(ext)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void handle(Context context, Reader reader, Writer writer) throws IOException {
		// Block
		LinkedHashMap<String, String> blocks = new LinkedHashMap<String, String>();
		String blockName = null;
		StringBuilder blockBody = new StringBuilder();
		for(String line : Util.readAllLines(reader)) {
			String s = ('^' + line).trim();
			if(s.length() > 6 && s.startsWith("^#--") && s.endsWith("--")) {
				if(blockBody.length() > 0) {
					blocks.put(blockName, blockBody.toString());
				}
				blockName = s.substring(4, s.length() - 2);
				blockBody.setLength(0);
			} else if(blockBody.length() > 0 || line.trim().length() > 0) {
				blockBody.append(line);
				blockBody.append("\r\n");
			}
		}
		if(blockBody.length() > 0) {
			blocks.put(blockName, blockBody.toString());
		}
		if(blocks.containsKey(null) && !blocks.containsKey("content")) {
			blocks.put("content", blocks.remove(null));
		}
		
		for(Entry<String, String> block : blocks.entrySet()) {
			context.addBlock(block.getKey(), block.getValue());
		}
	}
}
