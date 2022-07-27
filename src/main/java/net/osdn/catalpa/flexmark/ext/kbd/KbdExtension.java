package net.osdn.catalpa.flexmark.ext.kbd;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.HtmlRenderer.HtmlRendererExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.Parser.ParserExtension;

import com.vladsch.flexmark.util.data.MutableDataHolder;
import com.vladsch.flexmark.util.misc.Extension;
import net.osdn.catalpa.flexmark.ext.kbd.internal.KbdInlineParserExtension;
import net.osdn.catalpa.flexmark.ext.kbd.internal.KbdNodeRenderer;

public class KbdExtension implements ParserExtension, HtmlRendererExtension {

    private KbdExtension() {
    }

    public static Extension create() {
        return new KbdExtension();
    }

    @Override
    public void rendererOptions(final MutableDataHolder options) {

    }

    @Override
    public void parserOptions(final MutableDataHolder options) {

    }

    @Override
    public void extend(Parser.Builder parserBuilder) {
    	parserBuilder.customInlineParserExtensionFactory(new KbdInlineParserExtension.Factory());
    }

    @Override
    public void extend(HtmlRenderer.Builder rendererBuilder, String rendererType) {
        if ("HTML".equals(rendererType)) {
            rendererBuilder.nodeRendererFactory(new KbdNodeRenderer.Factory());
        }
    }

}
