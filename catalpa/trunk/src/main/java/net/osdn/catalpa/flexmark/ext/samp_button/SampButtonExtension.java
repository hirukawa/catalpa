package net.osdn.catalpa.flexmark.ext.samp_button;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.HtmlRenderer.HtmlRendererExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.Parser.ParserExtension;

import com.vladsch.flexmark.util.data.MutableDataHolder;
import com.vladsch.flexmark.util.misc.Extension;
import net.osdn.catalpa.flexmark.ext.samp_button.internal.SampButtonInlineParserExtension;
import net.osdn.catalpa.flexmark.ext.samp_button.internal.SampButtonNodeRenderer;

public class SampButtonExtension implements ParserExtension, HtmlRendererExtension {

    private SampButtonExtension() {
    }

    public static Extension create() {
        return new SampButtonExtension();
    }

    @Override
    public void rendererOptions(final MutableDataHolder options) {

    }

    @Override
    public void parserOptions(final MutableDataHolder options) {

    }

    @Override
    public void extend(Parser.Builder parserBuilder) {
    	parserBuilder.customInlineParserExtensionFactory(new SampButtonInlineParserExtension.Factory());
    }

    @Override
    public void extend(HtmlRenderer.Builder rendererBuilder, String rendererType) {
        if ("HTML".equals(rendererType)) {
            rendererBuilder.nodeRendererFactory(new SampButtonNodeRenderer.Factory());
        }
    }

}
