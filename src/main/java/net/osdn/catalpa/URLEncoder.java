package net.osdn.catalpa;

import java.io.CharArrayWriter;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;

public class URLEncoder {
	private static final BitSet dontNeedEncoding;
	private static final int caseDiff = ('a' - 'A');
	
	static {
		dontNeedEncoding = new BitSet(256);
		int i;
		for (i = 'a'; i <= 'z'; i++) {
			dontNeedEncoding.set(i);
		}
		for (i = 'A'; i <= 'Z'; i++) {
			dontNeedEncoding.set(i);
		}
		for (i = '0'; i <= '9'; i++) {
			dontNeedEncoding.set(i);
		}
		dontNeedEncoding.set('!');
		dontNeedEncoding.set('#');
		dontNeedEncoding.set('$');
		dontNeedEncoding.set('&');
		dontNeedEncoding.set('\'');
		dontNeedEncoding.set('(');
		dontNeedEncoding.set(')');
		dontNeedEncoding.set('*');
		dontNeedEncoding.set(',');
		dontNeedEncoding.set('-');
		dontNeedEncoding.set('.');
		dontNeedEncoding.set('/');
		dontNeedEncoding.set(':');
		dontNeedEncoding.set(';');
		dontNeedEncoding.set(';');
		dontNeedEncoding.set('=');
		dontNeedEncoding.set('?');
		dontNeedEncoding.set('@');
		dontNeedEncoding.set('[');
		dontNeedEncoding.set(']');
		dontNeedEncoding.set('_');
		dontNeedEncoding.set('~');
	}
	
	public static String encode(String s) {
		boolean needToChange = false;
		StringBuilder out = new StringBuilder(s.length());
		CharArrayWriter charArrayWriter = new CharArrayWriter();
		
		for (int i = 0; i < s.length();) {
			int c = (int)s.charAt(i);
			if (dontNeedEncoding.get(c)) {
				out.append((char)c);
				i++;
			} else {
				do {
					charArrayWriter.write(c);
					if (c >= 0xD800 && c <= 0xDBFF) {
						if ((i+1) < s.length()) {
							int d = (int)s.charAt(i+1);
							if (d >= 0xDC00 && d <= 0xDFFF) {
								charArrayWriter.write(d);
								i++;
							}
						}
					}
					i++;
				} while (i < s.length() && !dontNeedEncoding.get((c = (int)s.charAt(i))));
				
				charArrayWriter.flush();
				String str = new String(charArrayWriter.toCharArray());
				byte[] ba = str.getBytes(StandardCharsets.UTF_8);
				for (int j = 0; j < ba.length; j++) {
					out.append('%');
					char ch = Character.forDigit((ba[j] >> 4) & 0xF, 16);
					if (Character.isLetter(ch)) {
						ch -= caseDiff;
					}
					out.append(ch);
					ch = Character.forDigit(ba[j] & 0xF, 16);
					if (Character.isLetter(ch)) {
						ch -= caseDiff;
					}
					out.append(ch);
				}
				charArrayWriter.reset();
				needToChange = true;
			}
		}
		
		return (needToChange ? out.toString() : s);
	}
}
