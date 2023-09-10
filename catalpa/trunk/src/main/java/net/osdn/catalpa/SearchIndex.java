package net.osdn.catalpa;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchIndex {
	
	private static final Pattern HEADING1 = Pattern.compile("<h1[^>]*>(.+?)</h1>", Pattern.CASE_INSENSITIVE);
	private static final Pattern SEARCH_TARGET = Pattern.compile("<!--start-search-target-->(.+?)<!--end-search-target-->", Pattern.DOTALL);
	
	public static SearchIndex create(Context context, List<String> lines) throws IOException {
		Path relativeOutputPath = context.getRelativeOutputPath();
		if(relativeOutputPath == null) {
			return null;
		}

		String title = null;
		String url = relativeOutputPath.toString().replace('\\', '/');
		String text;
		
		StringBuilder searchTarget = new StringBuilder();
		for(String line : lines) {
			searchTarget.append(line);
		}

		String heading1 = null;
		Matcher m = HEADING1.matcher(searchTarget);
		if(m.find()) {
			heading1 = m.group(1);
		}
		
		StringBuilder sb = new StringBuilder();
		String input = searchTarget.toString().replaceAll("<span[^>]*> </span>", "");
		m = SEARCH_TARGET.matcher(input);
		int start = 0;
		while(m.find(start)) {
			sb.append(m.group(1));
			sb.append('\n');
			start = m.end();
		}
		if(sb.length() == 0) {
			return null;
		}
		text = sb.toString();
		text = text.replaceAll("&#8203;", "");
		text = text.replaceAll("</?(a|big|code|em|i|kbd|small|span|strong|tt|wbr).*?>", "");
		text = text.replaceAll("<[^>]*>", "\n");
		text = text.replaceAll("&amp;", "&");
		/*
		text = text.replaceAll("&lt;", "<");
		text = text.replaceAll("&gt;", ">");
		*/
		text = text.replaceAll("&quot;", "\"");
		text = text.replaceAll("&ldquo;", "\"");
		text = text.replaceAll("&rdquo;", "\"");
		text = text.replaceAll("&lsquo;", "'");
		text = text.replaceAll("&rsquo;", "'");
		text = text.replaceAll("。", "。\n");
		while(text.indexOf("\n\n") != -1) {
			text = text.replace("\n\n", "\n");
		}
		text = text.trim();
		
		Map<String, Object> declaredYamlFrontMatter = context.getDeclaredYamlFrontMatter();
		Object obj = declaredYamlFrontMatter.get("title");
		if(obj instanceof String) {
			String s = ((String)obj).trim();
			if(s.length() > 0) {
				title = s;
			}
		}
		if(title == null) {
			title = heading1;
		}
		if(title == null) {
			title = relativeOutputPath.getFileName().toString();
		}
		title = title.replaceAll("&#8203;", "");
		title = title.replaceAll("<span[^>]*> </span>", "");
		title = title.replaceAll("</?(a|big|code|em|i|kbd|small|span|strong|tt).*?>", "");
		title = title.replaceAll("<[^>]*>", " ");
		title = title.replaceAll("&amp;", "&");
		/*
		title = title.replaceAll("&lt;", "<");
		title = title.replaceAll("&gt;", ">");
		*/
		title = title.replaceAll("&quot;", "\"");
		title = title.replaceAll("&ldquo;", "\"");
		title = title.replaceAll("&rdquo;", "\"");
		title = title.replaceAll("&lsquo;", "'");
		title = title.replaceAll("&rsquo;", "'");
		title = title.replaceAll("\n", " ");
		while(title.indexOf("  ") != -1) {
			title = title.replace("  ", " ");
		}
		title = title.trim();
		
		SearchIndex searchIndex = new SearchIndex();
		searchIndex.title = Util.getJavaScriptString(title);
		searchIndex.url = Util.getJavaScriptString(url);
		searchIndex.text = Util.getJavaScriptString(text);
		searchIndex.lastModifiedTime = context.getLastModifiedTime();
		return searchIndex;
	}

	private String title;
	private String url;
	private String text;
	private FileTime lastModifiedTime;
	
	private SearchIndex() {
	}
	
	public String getTitle() {
		return title;
	}
	
	public String getUrl() {
		return url;
	}
	
	public String getText() {
		return text;
	}

	public FileTime getLastModifiedTime() {
	    return lastModifiedTime;
    }

    public void setLastModifiedTime(FileTime t) {
		lastModifiedTime = t;
	}
}
