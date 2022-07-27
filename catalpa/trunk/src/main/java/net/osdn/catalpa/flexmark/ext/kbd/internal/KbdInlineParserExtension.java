package net.osdn.catalpa.flexmark.ext.kbd.internal;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vladsch.flexmark.parser.InlineParser;
import com.vladsch.flexmark.parser.InlineParserExtension;
import com.vladsch.flexmark.parser.InlineParserExtensionFactory;
import com.vladsch.flexmark.parser.LightInlineParser;
import com.vladsch.flexmark.util.sequence.BasedSequence;

import net.osdn.catalpa.flexmark.ext.kbd.Kbd;

public class KbdInlineParserExtension implements InlineParserExtension {
	private final Pattern PATTERN = Pattern.compile("\\['([^']*)'\\]");

    public KbdInlineParserExtension(LightInlineParser inlineParser) {
    }

    @Override
    public void finalizeDocument(InlineParser inlineParser) {
    }

    @Override
    public void finalizeBlock(InlineParser inlineParser) {
    }

    @Override
    public boolean parse(LightInlineParser inlineParser) {
        if (inlineParser.peek(1) == '\'') {
            BasedSequence input = inlineParser.getInput();
            Matcher matcher = inlineParser.matcher(PATTERN);
            if (matcher != null) {
                BasedSequence tag = input.subSequence(matcher.start(), matcher.end());
                BasedSequence text = input.subSequence(matcher.start(1), matcher.end(1));
                Kbd node = new Kbd(tag.subSequence(0, 2), text, tag.endSequence(2));
                node.setCharsFromContent();
                inlineParser.flushTextNode();
                inlineParser.getBlock().appendChild(node);
                return true;
            }
        }
        return false;
    }

    public static class Factory implements InlineParserExtensionFactory {
        @Override
        public Set<Class<?>> getAfterDependents() {
            return null;
        }

        @Override
        public CharSequence getCharacters() {
            return "['";
        }

        @Override
        public Set<Class<?>> getBeforeDependents() {
            return null;
        }

        /*
        @Override
        public InlineParserExtension create(final InlineParser inlineParser) {
            return new KbdInlineParserExtension(inlineParser);
        }
        */

        @Override
        public InlineParserExtension apply(LightInlineParser inlineParser) {
            return new KbdInlineParserExtension(inlineParser);
        }

        @Override
        public boolean affectsGlobalScope() {
            return false;
        }
    }
}
