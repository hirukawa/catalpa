package net.osdn.catalpa.handler;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

import net.osdn.catalpa.Context;
import net.osdn.catalpa.Handler;
import net.osdn.catalpa.Util;

public class YamlFrontMatterHandler implements Handler {

	protected static final String[] APPLICABLE_EXTENSIONS = new String[] {
		".less",
		".markdown",
		".markdown.txt",
		".md",
		".md.txt"
	};
	
	@Override
	public int getPriority() {
		return 100;
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
		List<String> lines = Util.readAllLines(reader);

		// 先頭から連続する空行を削除します。
		Iterator<String> iterator = lines.iterator();
		while(iterator.hasNext()) {
			if(iterator.next().trim().length() > 0) {
				break;
			}
			iterator.remove();
		}
		
		if(lines.isEmpty()) {
			return;
		}
		
		// YAML front matter
		String yamlFrontMatter = null;
		if(('^' + lines.get(0)).trim().equals("^---")) {
			for(int i = 1; i < lines.size(); i++) {
				if(('^' + lines.get(i)).trim().equals("^---")) {
					StringBuilder sb = new StringBuilder();
					for(int j = 1; j < i; j++) {
						sb.append(lines.get(j));
						sb.append("\r\n");
					}
					while(i >= 0) {
						lines.remove(i--);
					}
					yamlFrontMatter = sb.toString();
					break;
				}
			}
		}
		if(yamlFrontMatter != null) {
			context.addYamlFrontMatter(yamlFrontMatter);
		}
		Util.writeAllLines(writer, lines);
	}
}
