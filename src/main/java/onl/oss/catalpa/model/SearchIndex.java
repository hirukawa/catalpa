package onl.oss.catalpa.model;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchIndex {

    private static final Pattern HEADING1 = Pattern.compile("<h1[^>]*>(.+?)</h1>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SEARCH_TARGET = Pattern.compile("<!--start-search-target-->(.+?)<!--end-search-target-->", Pattern.DOTALL);

    public static SearchIndex create(String path, String title, Path target) throws IOException {
        Matcher m;

        String url = URLEncoder.encode(path, StandardCharsets.UTF_8).replace("%2F", "/");
        String html = Files.readString(target, StandardCharsets.UTF_8);
        FileTime lastModifiedTime = Files.getLastModifiedTime(target);

        if (title == null) {
            m = HEADING1.matcher(html);
            if (m.find()) {
                title = m.group(1);
            }
        }

        if (title == null) {
            title = target.getFileName().toString();
        }

        title = normalize(title);
        title = title.replace("\n", " ");
        while (title.contains("  ")) {
            title = title.replace("  ", " ");
        }
        title = title.trim();


        StringBuilder sb = new StringBuilder();
        m = SEARCH_TARGET.matcher(html);
        int start = 0;
        while (m.find(start)) {
            sb.append(m.group(1));
            sb.append('\n');
            start = m.end();
        }

        if (sb.isEmpty()) {
            return null;
        }

        String text = sb.toString();
        text = text.replace("。", "。\n");
        text = normalize(text);

        return new SearchIndex(
                getJavaScriptString(url),
                getJavaScriptString(title),
                getJavaScriptString(text),
                lastModifiedTime
        );
    }

    private static String normalize(String s) {
        s = s.replace("&#8203;", "");
        s = s.replaceAll("</?(a|big|code|em|i|kbd|small|span|strong|tt|wbr).*?>", "");
        s = s.replaceAll("<[^>]*>", "\n");
        s = s.replace("&amp;", "&");
        s = s.replace("&quot;", "\"");
        s = s.replace("&ldquo;", "\"");
        s = s.replace("&rdquo;", "\"");
        s = s.replace("&lsquo;", "'");
        s = s.replace("&rsquo;", "'");
        s = s.replace("&emsp;", " ");
        s = s.replace("&ensp;", " ");
        s = s.replace("&thinsp;", " ");

        while (s.contains("\n\n")) {
            s = s.replace("\n\n", "\n");
        }

        return s.trim();
    }

    /** JavaScriptの文字列として扱えるようにエスケープした文字列を返します。
     *
     * @param text
     * @return
     */
    private static String getJavaScriptString(String text) {
        if(text == null) {
            return "";
        }

        //連続する改行コードをまとめて \n に置き換えます。
        text = text.replace("\r", "\n");
        while(text.contains("\n\n")) {
            text = text.replace("\n\n", "\n");
        }

        //連続するタブ、スペースを単独のスペース1つに置き換えます。
        text = text.replace("\t", " ");
        while(text.contains("  ")) {
            text = text.replace("  ", " ");
        }

        //エスケープ処理
        text = text.replace("\\", "\\\\");
        text = text.replace("\n", "\\n");
        text = text.replace("\"", "\\\"");
        text = text.replace("'", "\\'");

        return text;
    }


    private final String url;
    private final String title;
    private final String text;
    private final FileTime lastModifiedTime;

    private SearchIndex(String url, String title, String text, FileTime lastModifiedTime) {
        this.url = url;
        this.title = title;
        this.text = text;
        this.lastModifiedTime = lastModifiedTime;
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    public FileTime getLastModifiedTime() {
        return lastModifiedTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchIndex that = (SearchIndex) o;
        return Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(url);
    }

    @Override
    public String toString() {
        return "SearchIndex{" +
                "url='" + url + '\'' +
                ", title='" + title + '\'' +
                ", text='" + text + '\'' +
                ", lastModifiedTime=" + lastModifiedTime +
                '}';
    }
}
