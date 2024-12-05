package onl.oss.catalpa.freemarker;

import com.vladsch.flexmark.ext.attributes.AttributesExtension;
import com.vladsch.flexmark.ext.definition.DefinitionExtension;
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.typographic.TypographicExtension;
import com.vladsch.flexmark.ext.wikilink.WikiLinkExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.data.DataKey;
import com.vladsch.flexmark.util.data.MutableDataSet;
import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;
import onl.oss.catalpa.flexmark.ext.BasicNodeExtension;
import onl.oss.catalpa.flexmark.ext.HighlightExtension;
import onl.oss.catalpa.flexmark.ext.KbdExtension;
import onl.oss.catalpa.flexmark.ext.SampButtonExtension;
import onl.oss.catalpa.html.Typesetting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static onl.oss.catalpa.Logger.INFO;

public class MarkdownDirective implements TemplateDirectiveModel {

    public static final DataKey<String> _ROOTPATH = new DataKey<String>("_ROOTPATH", "");
    public static final DataKey<String> _FILEPATH = new DataKey<String>("_FILEPATH", "");

    private static final Pattern TABLE_BLOCK_PATTERN = Pattern.compile("(^\\|[^\n]*\n)(^(\\||\\s{2})[^\n]*\n)*(^\\|[^\n]*\n)(^\\{[^\n]*}\\s*\n)?(^\\s*)\n", Pattern.MULTILINE | Pattern.DOTALL);
    private static final Pattern RUBY_PATTERN_1 = Pattern.compile("｜(.+?)《(.+?)》");
    private static final Pattern RUBY_PATTERN_2 = Pattern.compile("([\\u4E00-\\u9FFF\\u3005-\\u3007\\u30F6]+)《(.+?)》");

    private final MutableDataSet options;
    private final Parser parser;
    private final HtmlRenderer renderer;

    public MarkdownDirective() {
        MutableDataSet options = new MutableDataSet();
        options.set(HtmlRenderer.FENCED_CODE_NO_LANGUAGE_CLASS, "nohighlight");
        //TODO:
        //options.set(HighlightExtension.REPLACE_YEN_SIGN, true);
        options.set(TaskListExtension.ITEM_DONE_CLASS, "checked");
        options.set(TaskListExtension.ITEM_NOT_DONE_CLASS, "unchecked");
        options.set(TaskListExtension.ITEM_DONE_MARKER, "");
        options.set(TaskListExtension.ITEM_NOT_DONE_MARKER, "");
        options.set(FootnoteExtension.FOOTNOTE_REF_PREFIX, "*");
        options.set(Parser.EXTENSIONS, Arrays.asList(
                AttributesExtension.create(),
                DefinitionExtension.create(),
                FootnoteExtension.create(),
                WikiLinkExtension.create(),
                StrikethroughExtension.create(),
                TaskListExtension.create(),
                TablesExtension.create(),
                TypographicExtension.create(),

                BasicNodeExtension.create(),
                HighlightExtension.create(),
                KbdExtension.create(),
                SampButtonExtension.create()
        ));

        this.options = options;
        this.parser = Parser.builder(options).build();
        this.renderer = HtmlRenderer.builder(options).build();
    }

    @Override
    public void execute(Environment environment, @SuppressWarnings("rawtypes") Map params, TemplateModel[] loopVars, TemplateDirectiveBody body) throws TemplateException, IOException {
        StringWriter sw = new StringWriter();
        body.render(sw);
        String markdown = sw.toString();

        markdown = fixVerticalSpace(markdown);
        markdown = fixTableBlock(markdown);
        markdown = fixRuby(markdown);

        String rootpath = "";
        {
            TemplateModel tm = environment.getDataModel().get("_ROOTPATH");
            if (tm instanceof TemplateScalarModel tsm) {
                String s = tsm.getAsString();
                if (s != null) {
                    rootpath = s;
                }
            }
        }
        INFO("rootpath=" + rootpath);

        String filepath = "";
        {
            TemplateModel tm = environment.getDataModel().get("_FILEPATH");
            if (tm instanceof TemplateScalarModel tsm) {
                String s = tsm.getAsString();
                if (s != null) {
                    filepath = s;
                }
            }
        }
        INFO("filepath=" + filepath);

        //
        // オプション
        //
        Set<String> options = new LinkedHashSet<>();
        {
            TemplateModel tm = environment.getDataModel().get("options");
            if (tm instanceof TemplateSequenceModel seq) {
                for (int i = 0; i < seq.size(); i++) {
                    TemplateModel e = seq.get(i);
                    if (e instanceof TemplateScalarModel tsm) {
                        String s = tsm.getAsString();
                        if (s != null && !s.isBlank()) {
                            options.add(s.trim());
                        }
                    }
                }
            } else if (tm instanceof TemplateScalarModel tsm) {
                String s = tsm.getAsString();
                if (s != null && !s.isBlank()) {
                    options.add(s.trim());
                }
            }
        }

        // font-feature-settings: halt を使用するオプション
        boolean halt = options.contains("halt");
        INFO("option halt=" + halt);

        //
        // Markdown を HTML に変換します。
        //
        Document document = parser.parse(markdown);
        document.set(_ROOTPATH, rootpath);
        document.set(_FILEPATH, filepath);
        String html = renderer.render(document);

        //
        // 日本語組版を適用します。
        //
        html = Typesetting.apply(html, halt);

        //
        // 結果を出力します。
        //
        environment.getOut().write(html);
    }

    /** ブランク行（半角スペース・タブのみで構成されている行）を垂直スペース用の div に変換します。
     * 最初の半角スペースで構成される行は高さ 0 の垂直余白になります。（マージン相殺が無効になるのでこれでも高さが増えます。）
     * さらに半角スペースで構成される行が続くと半角スペース 1つごとに高さ 0.25rem の垂直余白になります。
     *
     * @param input 入力文字列
     * @return 出力文字列
     * @throws IOException 例外
     */
    private static String fixVerticalSpace(String input) throws IOException {
        try (BufferedReader reader = new BufferedReader(new StringReader(input))) {
            StringBuilder output = new StringBuilder();
            List<Integer> vspace = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty() && line.isBlank()) {
                    int space = 0;
                    for (int i = 0; i < line.length(); i++) {
                        if (line.charAt(i) == '\u0020') { // 半角スペース
                            space++;
                        }
                    }
                    if (space > 0) {
                        vspace.add(space);
                    }
                } else {
                    if (!vspace.isEmpty()) {
                        if (vspace.size() == 1) {
                            output.append("\n<div class=\"vspace\" data-length=\"0\" style=\"margin-block-start:-1px;height:1px\"></div>\n\n");
                        } else {
                            vspace.removeFirst();
                            int space = 0;
                            for (int i : vspace) {
                                space += i;
                            }
                            BigDecimal rem = BigDecimal.valueOf(space).divide(BigDecimal.valueOf(4), 3, RoundingMode.HALF_EVEN).stripTrailingZeros();
                            output.append("\n<div class=\"vspace\" data-length=\"").append(space).append("\" style=\"height:").append(rem.toPlainString()).append("rem\"></div>\n\n");
                        }
                        vspace.clear();
                    }
                    output.append(line);
                    output.append('\n');
                }
            }
            return output.toString();
        }
    }

    /** 列が複数行に分かれているテーブルブロックの記述を列が1行にまとまっている形に整形します。
     *
     * @param input 入力文字列
     * @return 出力文字列
     */
    private static String fixTableBlock(String input) {
        StringBuilder output = new StringBuilder();

        Matcher m = TABLE_BLOCK_PATTERN.matcher(input);
        int start = 0;
        while (m.find(start)) {
            if (m.start() > start) {
                String table = input.substring(start, m.start());
                output.append(table);
            }
            for (String line : m.group(0).split("\n")) {
                String trim = line.trim();
                if(trim.length() > 1 && line.charAt(0) == '|' && trim.charAt(trim.length() - 1) != '|') {
                    output.append(line);
                    continue;
                }
                if(line.startsWith("  ")) {
                    output.append(line.substring(2));
                    continue;
                }
                output.append(line);
                output.append('\n');
            }
            output.append('\n');
            start = m.end();
        }

        if(start < input.length()) {
            output.append(input.substring(start));
        }

        return output.toString();
    }

    private static String fixRuby(String input) {
        /* [\u4E00-\u9FFF\u3005-\u3007\u30F6]+ は漢字にマッチするパターンです。（ひらがな・カタカナにはマッチしません）
         *
         *  漢字 \u4E00 ～ \u9FFF
         *  以下の文字も漢字として扱います。
         *  \u3005 々
         *  \u3006 〆
         *  \u3007 〇
         *  \u30F6 ヶ
         *  \u4EDD 仝
         */

        StringBuilder sb = new StringBuilder();
        Matcher m;
        int start;

        m = RUBY_PATTERN_1.matcher(input);
        start = 0;
        while (m.find(start)) {
            sb.append(input.subSequence(start, m.start()));

            if (m.group(1).isBlank()) {
                sb.append(m.group(0));
            } else {
                String rb = m.group(1);
                String rt = m.group(2);
                sb.append("<ruby>");
                sb.append(rb);
                sb.append("<rp>（</rp>");
                sb.append("<rt>").append(rt).append("</rt>");
                sb.append("<rp>）</rp>");
                sb.append("</ruby>");
            }
            start = m.end();
        }
        sb.append(input.subSequence(start, input.length()));

        input = sb.toString();
        sb = new StringBuilder();
        m = RUBY_PATTERN_2.matcher(input);
        start = 0;
        while (m.find(start)) {
            sb.append(input.subSequence(start, m.start()));

            if (m.group(1).isBlank()) {
                sb.append(m.group(0));
            } else {
                String rb = m.group(1);
                String rt = m.group(2);
                sb.append("<ruby>");
                sb.append(rb);
                sb.append("<rp>（</rp>");
                sb.append("<rt>").append(rt).append("</rt>");
                sb.append("<rp>）</rp>");
                sb.append("</ruby>");
            }
            start = m.end();
        }
        sb.append(input.subSequence(start, input.length()));
        return sb.toString();
    }
}
