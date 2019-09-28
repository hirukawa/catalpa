package net.osdn.catalpa.flexmark.ext;

import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.Parser.Builder;
import com.vladsch.flexmark.util.builder.Extension;
import com.vladsch.flexmark.util.data.MutableDataHolder;

public class LineDividableTableExtension implements Parser.ParserExtension {

	private LineDividableTableExtension() {
	}
	
	public static Extension create() {
		return new LineDividableTableExtension();
	}
	
	@Override
	public void parserOptions(MutableDataHolder options) {
	}

	@Override
	public void extend(Builder parserBuilder) {
		parserBuilder.paragraphPreProcessorFactory(new LineDividableTablePreProcessor.Factory());
	}
}
