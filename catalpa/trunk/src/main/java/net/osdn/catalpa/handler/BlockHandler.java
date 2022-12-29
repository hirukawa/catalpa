package net.osdn.catalpa.handler;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.osdn.catalpa.Context;
import net.osdn.catalpa.Handler;
import net.osdn.catalpa.Util;

public class BlockHandler implements Handler {

	private static Pattern TABLE_BLOCK_PATTERN = Pattern.compile("(^\\|[^\r\n]*\r\n)+(^\\{[^\r\n]*\\}\s*\r\n)?(^\s*)\r\n", Pattern.MULTILINE | Pattern.DOTALL);

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
					blocks.put(blockName, Util.trim(blockBody, "\r\n").toString());
				}
				blockName = s.substring(4, s.length() - 2);
				blockBody.setLength(0);
			} else if(blockBody.length() > 0 || line.trim().length() > 0) {
				blockBody.append(line);
				blockBody.append("\r\n");
			}
		}
		if(blockBody.length() > 0) {
			blockBody = fixTableBlock(blockBody);
			blocks.put(blockName, Util.trim(blockBody, "\r\n").toString());
		}
		if(blocks.containsKey(null) && !blocks.containsKey("content")) {
			blocks.put("content", blocks.remove(null));
		}
		
		for(Entry<String, String> block : blocks.entrySet()) {
			context.addBlock(block.getKey(), block.getValue());
		}
	}

	/** 列が複数行に分かれているテーブルブロックの記述を列が1行にまとまっている形に整形します。
	 *
	 * @param input コンテント
	 * @return テーブルブロック記述が整形されたコンテント
	 */
	private static StringBuilder fixTableBlock(StringBuilder input) {
		StringBuilder output = new StringBuilder();

		Matcher m = TABLE_BLOCK_PATTERN.matcher(input);
		int start = 0;
		while(m.find(start)) {
			if(m.start() > start) {
				String table = input.substring(start, m.start());
				output.append(table);
			}
			for(String line : m.group(0).split("\r\n")) {
				if(line.lastIndexOf('|') == 0 && line.trim().length() > 1) {
					output.append(line);
					continue;
				}
				output.append(line);
				output.append('\r');
				output.append('\n');
			}
			output.append('\r');
			output.append('\n');
			start = m.end();
		}
		if(start < input.length()) {
			output.append(input.substring(start));
		}
		return output;
	}
}
