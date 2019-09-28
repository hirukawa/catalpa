package net.osdn.catalpa.flexmark.ext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ext.tables.internal.TableParagraphPreProcessor;
import com.vladsch.flexmark.parser.block.ParagraphPreProcessor;
import com.vladsch.flexmark.parser.block.ParagraphPreProcessorFactory;
import com.vladsch.flexmark.parser.block.ParserState;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.sequence.BasedSequence;

public class LineDividableTablePreProcessor implements ParagraphPreProcessor {

	public LineDividableTablePreProcessor(DataHolder options) {
	}
	
	@Override
	public int preProcessBlock(Paragraph block, ParserState state) {
		if(block.getLineCount() >= 2 && block.getLineChars(0).length() > 0 && block.getLineChars(0).charAt(0) == '|') {
			int indent = block.getLineIndent(0);
			BasedSequence indentChars = BasedSequence.NULL;
			for(int i = 0; i < indent; i++) {
				indentChars.append(BasedSequence.SPACE);
			}
			BasedSequence content = BasedSequence.NULL;
			BasedSequence buf = BasedSequence.NULL;

			BasedSequence chars = block.getChars();
			int lineNumber = 0;
			int start = 0;
			for(int i = 0; i < block.getChars().length(); i++) {
				if(chars.charAt(i) == '\n') {
					BasedSequence line = chars.subSequence(start, i + 1).trimStart().trimEOL().trimEOL();
					if(block.getLineIndent(lineNumber) < indent) {
						break;
					}
					if(line.isEmpty()) {
						break;
					}
					if(line.charAt(0) != '|' && block.getLineIndent(lineNumber) <= indent) {
						break;
					}
					
					boolean isNewLine = line.trimEnd().lastChar() == '|';
					int brCount = 0;
					while(line.length() >= 2 && line.charAt(line.length() - 1) == ' ' && line.charAt(line.length() - 2) == ' ') {
						line = line.subSequence(0, line.length() - 2);
						brCount++;
					}
					while(--brCount >= 0) {
						line = line.append("<br>");
					}
					buf = buf.append(line);
					if(isNewLine) {
						content = content.append(indentChars).append(buf).append(BasedSequence.EOL);
						buf = BasedSequence.NULL;
					}
					lineNumber++;
					start = i + 1;
				}
			}
			if(buf != BasedSequence.NULL) {
				content = content.append(indentChars).append(buf).append(BasedSequence.EOL);
			}
			if(start > 0) {
				List<BasedSequence> lineSegments = new ArrayList<BasedSequence>();
				List<Integer> lineIndents = new ArrayList<Integer>();
				int s = 0;
				for(int i = 0; i < content.length(); i++) {
					if(content.charAt(i) == '\n') {
						lineSegments.add(content.subSequence(s, i + 1));
						lineIndents.add(indent);
						s = i + 1;
					}
				}
				Paragraph table = new Paragraph(content, lineSegments, lineIndents);
				block.insertBefore(table);
				state.blockAdded(table);
				return start;
			}
		}
		
		return 0;
	}
	
	public static class Factory implements ParagraphPreProcessorFactory {

		@Override
		public Set<? extends Class<? extends ParagraphPreProcessorFactory>> getAfterDependents() {
			return null;
		}

		@Override
		public Set<? extends Class<? extends ParagraphPreProcessorFactory>> getBeforeDependents() {
            Set<Class<? extends ParagraphPreProcessorFactory>> set = new HashSet<Class<? extends ParagraphPreProcessorFactory>>();
            ParagraphPreProcessorFactory pf = TableParagraphPreProcessor.Factory();
            set.add(pf.getClass());
            return set;
		}

		@Override
		public boolean affectsGlobalScope() {
			return false;
		}

		@Override
		public ParagraphPreProcessor apply(ParserState state) {
			return new LineDividableTablePreProcessor(state.getProperties());
		}
	}
}
