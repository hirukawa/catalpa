package net.osdn.catalpa;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Util {
	
	public static List<String> readAllLines(Reader reader) throws IOException {
		List<String> lines = new ArrayList<String>();
		try(BufferedReader br = new BufferedReader(reader)) {
			String line;
			while((line = br.readLine()) != null) {
				lines.add(line);
			}
		}
		return lines;
	}
	
	public static void writeAllLines(Writer writer, List<String> lines) throws IOException {
		for(String line : lines) {
			writer.write(line);
			writer.write("\r\n");
		}
	}
	
	public static Path replaceFileExtension(Path path, String[] exts, String replacement) {
		String filename = path.getFileName().toString();
		for(String ext : exts) {
			if(filename.toLowerCase().endsWith(ext)) {
				if(replacement != null) {
					filename = filename.substring(0, filename.length() - ext.length()) + replacement;
				} else {
					filename = filename.substring(0, filename.length() - ext.length());
				}
				break;
			}
		}
		return path.getParent().resolve(filename);
	}
	
	/** JavaScriptの文字列として扱えるようにエスケープした文字列を返します。
	 * 
	 * @param text
	 * @return
	 */
	public static String getJavaScriptString(String text) {
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
	
	public static StringBuilder trim(StringBuilder sb) {
		if(sb != null) {
			while(sb.length() >= 1) {
				char c = sb.charAt(0);
				if(c != '\r' && c != '\n') {
					break;
				}
				sb.deleteCharAt(0);
			}
			while(sb.length() >= 1) {
				char c = sb.charAt(sb.length() - 1);
				if(c != '\r' && c != '\n') {
					break;
				}
				sb.deleteCharAt(sb.length() - 1);
			}
		}
		return sb;
	}
	
	public static int[] getApplicationVersion() {
		String s = System.getProperty("java.application.version");
		if(s == null || s.trim().length() == 0) {
			return null;
		}
		
		s = s.trim() + ".0.0.0.0";
		String[] array = s.split("\\.", 5);
		int[] version = new int[4];
		for(int i = 0; i < 4; i++) {
			try {
				version[i] = Integer.parseInt(array[i]);
			} catch(NumberFormatException e) {
				e.printStackTrace();
			}
		}
		if(version[0] == 0 && version[1] == 0 && version[2] == 0 && version[3] == 0) {
			return null;
		}
		return version;
	}
}
