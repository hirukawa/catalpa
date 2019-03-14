package net.osdn.catalpa.html;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.osdn.catalpa.html.Char.CharClass;

public class JapaneseTextLayouter {
	// https://www.w3schools.com/tags/
	// https://developer.mozilla.org/en-US/docs/Web/HTML/Element

	//private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<(\"[^\"]*\"|'[^']*'|[^'\">])*>");
	private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<(/?\\w+|!--|!DOCTYPE)(\"[^\"]*\"|'[^']*'|[^'\">])*>", Pattern.CASE_INSENSITIVE);
	private static final Set<String> HTML_TAGS = new HashSet<String>(Arrays.asList(
			"!--", "!DOCTYPE",
			"A", "ABBR", "ACRONYM", "ADDRESS", "APPLET", "AREA", "ARTICLE", "ASIDE", "AUDIO",
			"B", "BASE", "BASEFONT", "BDI", "BDO", "BIG", "BLOCKQUOTE", "BODY", "BR", "BUTTON",
			"CANVAS", "CAPTION", "CENTER", "CITE", "CODE", "COL", "COLGROUP",
			"DATA", "DATALIST", "DD", "DEL", "DETAILS", "DFN", "DIALOG", "DIR", "DIV", "DL", "DT",
			"EM", "EMBED",
			"FIELDSET", "FIGCAPTION", "FIGURE", "FONT", "FOOTER", "FORM", "FRAME", "FRAMESET",
			"H1", "H2", "H3", "H4", "H5", "H6", "HEAD", "HEADER", "HR", "HTML",
			"I", "IFRAME", "IMG", "INPUT", "INS",
			"KBD", "LABEL", "LEGEND",
			"LI", "LINK",
			"MAIN", "MAP", "MARK", "META", "METER",
			"NAV", "NOFRAMES", "NOSCRIPT",
			"OBJECT", "OL", "OPTGROUP", "OPTION", "OUTPUT",
			"P", "PARAM", "PICTURE", "PRE", "PROGRESS",
			"Q",
			"RP", "RT", "RUBY",
			"S", "SAMP", "SCRIPT", "SECTION", "SELECT", "SMALL", "SOURCE", "SPAN", "STRIKE", "STRONG", "STYLE", "SUB", "SUMMARY", "SUP", "SVG",
			"TABLE", "TBODY", "TD", "TEMPLATE", "TEXTAREA", "TFOOT", "TH", "THEAD", "TIME", "TITLE", "TR", "TRACK", "TT",
			"U", "UL",
			"VAR", "VIDEO",
			"WBR"));
	
	private static final Set<String> ELEMENTS_TO_SKIP = new HashSet<String>(Arrays.asList(
			"CODE", "KBD", "PRE", "SAMP", "SCRIPT", "STYLE", "TT"));
	
	private static final Set<String> INLINE_TEXT_TAGS = new HashSet<String>(Arrays.asList(
			"A", "BIG", "EM", "I", "SMALL", "SPAN", "STRONG"));
	
	public static String layout(CharSequence input, boolean isReplaceBackslashToYensign) {
		if(isReplaceBackslashToYensign) {
			input = input.toString().replace("\\", "&yen;");
		}
		List<Token> tokens = tokenize(input);
		removeSpaces(tokens);
		addLetterSpacing(getFirstChar(tokens));
		
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < tokens.size(); i++) {
			Token token = tokens.get(i);

			// </code>などのインラインテキストの閉じタグの前に余白がある場合は、その余白をインラインテキストの閉じタグの後ろに移動します。
			if(token.getLetterSpacing() != 0.0) {
				RawChars lastCloseTag = null;
				for(int j = i + 1; j < tokens.size(); j++) {
					if(tokens.get(j).getLetterSpacing() < 0.0) {
						RawChars next = (RawChars)tokens.get(j);
						if(next.getType() == RawChars.Type.INLINE_TAG_CLOSE) {
							lastCloseTag = next;
							continue;
						}
					}
					break;
				}
				if(lastCloseTag != null) {
					lastCloseTag.setLetterSpacing(token.getLetterSpacing());
					token.setLetterSpacing(0.0);
				}
			}

			sb.append(token.getHtml());
		}
		return sb.toString();
	}
	
	private static List<Token> tokenize(CharSequence input) {
		List<Token> tokens = new LinkedList<Token>();
		Char previousChar = null;
		Char currentChar = null;
		String upperCaseInput = input.toString().toUpperCase();
		
		Matcher m = HTML_TAG_PATTERN.matcher(input);
		int index = 0;
		while(m.find(index)) {
			boolean isEndTag = false;
			String tagName = m.group(1).toUpperCase();
			if(tagName.charAt(0) == '/') {
				tagName = tagName.substring(1);
				isEndTag = true;
			}
			if(HTML_TAGS.contains(tagName)) {
				// ADD CHARS BEFORE TAG
				for(int i = index; i < m.start(); i++) {
					previousChar = currentChar;
					if(input.charAt(i) == 0x0D && (i+1 < m.start()) && input.charAt(i+1) == 0x0A) {
						currentChar = new Char(); // CRLF Character
						i++;
					} else {
						currentChar = new Char(input.charAt(i));
					}
					if(previousChar != null) {
						previousChar.setNextChar(currentChar);
						currentChar.setPreviousChar(previousChar);
					}
					tokens.add(currentChar);
				}
				
				if(!isEndTag && ELEMENTS_TO_SKIP.contains(tagName)) {
					// ADD EMPTY CHAR
					previousChar = currentChar;
					currentChar = new Char((char)0); //EMPTY Character
					if(previousChar != null) {
						previousChar.setNextChar(currentChar);
						currentChar.setPreviousChar(previousChar);
					}
					tokens.add(currentChar);
					
					int end = upperCaseInput.indexOf("/" + tagName, m.end());
					if(end != -1) {
						// ADD CURRENT ELEMENT (START TAG - END TAG)
						end += ("/" + tagName).length() + 1;
						tokens.add(new RawChars(input.subSequence(m.start(), end)));
						index = end;
					} else {
						// ADD CURRENT TAG
						tokens.add(new RawChars(m.group(0)));
						index = m.end();
					}
				} else if(INLINE_TEXT_TAGS.contains(tagName)) {
					// ADD CURRENT TAG
					RawChars.Type type = isEndTag ? RawChars.Type.INLINE_TAG_CLOSE : RawChars.Type.INLINE_TAG_OPEN;
					tokens.add(new RawChars(m.group(0), type));
					index = m.end();
				} else {
					// ADD EMPTY CHAR
					previousChar = currentChar;
					currentChar = new Char((char)0); //EMPTY Character
					if(previousChar != null) {
						previousChar.setNextChar(currentChar);
						currentChar.setPreviousChar(previousChar);
					}
					tokens.add(currentChar);
					
					// ADD CURRENT TAG
					tokens.add(new RawChars(m.group(0)));
					index = m.end();
				}
			} else {
				// ADD CHAR '<'
				previousChar = currentChar;
				currentChar = new Char('<');
				if(previousChar != null) {
					previousChar.setNextChar(currentChar);
					currentChar.setPreviousChar(previousChar);
				}
				tokens.add(currentChar);
				index++;
			}
		}
		// ADD CHARS AFTER LAST TAG
		for(int i = index; i < input.length(); i++) {
			previousChar = currentChar;
			if(input.charAt(i) == 0x0D && (i+1 < input.length()) && input.charAt(i+1) == 0x0A) {
				currentChar = new Char();
				i++;
			} else {
				currentChar = new Char(input.charAt(i));
			}
			if(previousChar != null) {
				previousChar.setNextChar(currentChar);
				currentChar.setPreviousChar(previousChar);
			}
			tokens.add(currentChar);
		}
		
		return tokens;
	}
	
	private static Char getFirstChar(List<Token> tokens) {
		for(Token token : tokens) {
			if(token instanceof Char) {
				return (Char)token;
			}
		}
		return null;
	}

	private static void removeSpaces(List<Token> tokens) {
		Iterator<Token> iterator = tokens.iterator();
		while(iterator.hasNext()) {
			Token token = iterator.next();
			if(token instanceof Char) {
				Char currentChar = (Char)token;
				if(currentChar.isLatin() && currentChar.isWhitespace()) {
					Char previousChar = currentChar.getPreviousChar();
					if(previousChar == null || previousChar.isWhitespace()) {
						continue;
					}
					Char nextChar = currentChar.getNextChar();
					if(nextChar == null || nextChar.isWhitespace()) {
						continue;
					}
					if(previousChar.isDividingPunctuationMark() && currentChar.isNewLine()) {
						previousChar.setEndOfSentence(true);
						iterator.remove();
						previousChar.setNextChar(nextChar);
						nextChar.setPreviousChar(previousChar);
					} else if((previousChar.getCharClass() == CharClass.LATIN_WORD_CHARACTER && nextChar.isLatin() == false)
							|| (previousChar.isLatin() == false && nextChar.getCharClass() == CharClass.LATIN_WORD_CHARACTER)) {
						iterator.remove();
						previousChar.setNextChar(nextChar);
						nextChar.setPreviousChar(previousChar);
					} else if(currentChar.isNewLine() && !previousChar.isLatin() && !nextChar.isLatin()) {
						iterator.remove();
						previousChar.setNextChar(nextChar);
						nextChar.setPreviousChar(previousChar);
					}
				}
			}
		}
	}
	
	private static void addLetterSpacing(Char firstChar) {
		Char currentChar = firstChar;
		while(currentChar != null) {
			Char nextChar = currentChar.getNextChar();
			if(nextChar == null) {
				break;
			}
			switch(currentChar.getCharClass()) {
			case EMPTY:
				break;
			case CRLF:
				break;
			case CR:
				break;
			case LF:
				break;
			case WHITESPACE:
				break;
			case OPENING_BRACKET:
				switch(nextChar.getCharClass()) {
				case EMPTY:
					break;
				case CRLF:
					break;
				case CR:
					break;
				case LF:
					break;
				case WHITESPACE:
					break;
				case OPENING_BRACKET:
					break;
				case CLOSING_BRACKET:
					break;
				case HYPHEN:
					break;
				case DIVIDING_PUNCTUATION_MARK:
					break;
				case MIDDLE_DOT:
					currentChar.setLetterSpacing(0.25);
					break;
				case FULL_STOP:
					break;
				case COMMA:
					break;
				case INSEPARABLE_CHARACTER:
					break;
				case JAPANESE:
					break;
				case LATIN_WORD_CHARACTER:
					break;
				case LATIN_NON_WORD_CHARACTER:
					break;
				}
				break;
			case CLOSING_BRACKET:
				switch(nextChar.getCharClass()) {
				case EMPTY:
					break;
				case CRLF:
					break;
				case CR:
					break;
				case LF:
					break;
				case WHITESPACE:
					break;
				case OPENING_BRACKET:
					currentChar.setLetterSpacing(0.5);
					break;
				case CLOSING_BRACKET:
					break;
				case HYPHEN:
					currentChar.setLetterSpacing(0.5);
					break;
				case DIVIDING_PUNCTUATION_MARK:
					currentChar.setLetterSpacing(0.5);
					break;
				case MIDDLE_DOT:
					currentChar.setLetterSpacing(0.25);
					break;
				case FULL_STOP:
					break;
				case COMMA:
					break;
				case INSEPARABLE_CHARACTER:
					currentChar.setLetterSpacing(0.5);
					break;
				case JAPANESE:
					currentChar.setLetterSpacing(0.5);
					break;
				case LATIN_WORD_CHARACTER:
					currentChar.setLetterSpacing(0.5);
					break;
				case LATIN_NON_WORD_CHARACTER:
					currentChar.setLetterSpacing(0.5);
					break;
				}
				break;
			case HYPHEN:
				switch(nextChar.getCharClass()) {
				case EMPTY:
					break;
				case CRLF:
					break;
				case CR:
					break;
				case LF:
					break;
				case WHITESPACE:
					break;
				case OPENING_BRACKET:
					currentChar.setLetterSpacing(0.5);
					break;
				case CLOSING_BRACKET:
					break;
				case HYPHEN:
					break;
				case DIVIDING_PUNCTUATION_MARK:
					break;
				case MIDDLE_DOT:
					currentChar.setLetterSpacing(0.25);
					break;
				case FULL_STOP:
					break;
				case COMMA:
					break;
				case INSEPARABLE_CHARACTER:
					break;
				case JAPANESE:
					break;
				case LATIN_WORD_CHARACTER:
					break;
				case LATIN_NON_WORD_CHARACTER:
					break;
				}
				break;
			case DIVIDING_PUNCTUATION_MARK:
				switch(nextChar.getCharClass()) {
				case EMPTY:
					break;
				case CRLF:
					break;
				case CR:
					break;
				case LF:
					break;
				case WHITESPACE:
					break;
				case OPENING_BRACKET:
					currentChar.setLetterSpacing(0.5);
					break;
				case CLOSING_BRACKET:
					break;
				case HYPHEN:
					break;
				case DIVIDING_PUNCTUATION_MARK:
					break;
				case MIDDLE_DOT:
					currentChar.setLetterSpacing(0.25);
					break;
				case FULL_STOP:
					break;
				case COMMA:
					break;
				case INSEPARABLE_CHARACTER:
					break;
				case JAPANESE:
					break;
				case LATIN_WORD_CHARACTER:
					break;
				case LATIN_NON_WORD_CHARACTER:
					break;
				}
				break;
			case MIDDLE_DOT:
				switch(nextChar.getCharClass()) {
				case EMPTY:
					break;
				case CRLF:
					break;
				case CR:
					break;
				case LF:
					break;
				case WHITESPACE:
					break;
				case OPENING_BRACKET:
					currentChar.setLetterSpacing(0.25);
					break;
				case CLOSING_BRACKET:
					currentChar.setLetterSpacing(0.25);
					break;
				case HYPHEN:
					currentChar.setLetterSpacing(0.25);
					break;
				case DIVIDING_PUNCTUATION_MARK:
					currentChar.setLetterSpacing(0.25);
					break;
				case MIDDLE_DOT:
					currentChar.setLetterSpacing(0.5);
					break;
				case FULL_STOP:
					currentChar.setLetterSpacing(0.25);
					break;
				case COMMA:
					currentChar.setLetterSpacing(0.25);
					break;
				case INSEPARABLE_CHARACTER:
					currentChar.setLetterSpacing(0.25);
					break;
				case JAPANESE:
					currentChar.setLetterSpacing(0.25);
					break;
				case LATIN_WORD_CHARACTER:
					currentChar.setLetterSpacing(0.25);
					break;
				case LATIN_NON_WORD_CHARACTER:
					currentChar.setLetterSpacing(0.25);
					break;
				}
				break;
			case FULL_STOP:
				switch(nextChar.getCharClass()) {
				case EMPTY:
					break;
				case CRLF:
					break;
				case CR:
					break;
				case LF:
					break;
				case WHITESPACE:
					break;
				case OPENING_BRACKET:
					currentChar.setLetterSpacing(0.5);
					break;
				case CLOSING_BRACKET:
					break;
				case HYPHEN:
					currentChar.setLetterSpacing(0.5);
					break;
				case DIVIDING_PUNCTUATION_MARK:
					currentChar.setLetterSpacing(0.5);
					break;
				case MIDDLE_DOT:
					currentChar.setLetterSpacing(0.25);
					break;
				case FULL_STOP:
					break;
				case COMMA:
					break;
				case INSEPARABLE_CHARACTER:
					currentChar.setLetterSpacing(0.5);
					break;
				case JAPANESE:
					currentChar.setLetterSpacing(0.5);
					break;
				case LATIN_WORD_CHARACTER:
					currentChar.setLetterSpacing(0.5);
					break;
				case LATIN_NON_WORD_CHARACTER:
					currentChar.setLetterSpacing(0.5);
					break;
				}
				break;
			case COMMA:
				switch(nextChar.getCharClass()) {
				case EMPTY:
					break;
				case CRLF:
					break;
				case CR:
					break;
				case LF:
					break;
				case WHITESPACE:
					break;
				case OPENING_BRACKET:
					currentChar.setLetterSpacing(0.5);
					break;
				case CLOSING_BRACKET:
					break;
				case HYPHEN:
					currentChar.setLetterSpacing(0.5);
					break;
				case DIVIDING_PUNCTUATION_MARK:
					currentChar.setLetterSpacing(0.5);
					break;
				case MIDDLE_DOT:
					currentChar.setLetterSpacing(0.25);
					break;
				case FULL_STOP:
					break;
				case COMMA:
					break;
				case INSEPARABLE_CHARACTER:
					currentChar.setLetterSpacing(0.5);
					break;
				case JAPANESE:
					currentChar.setLetterSpacing(0.5);
					break;
				case LATIN_WORD_CHARACTER:
					currentChar.setLetterSpacing(0.5);
					break;
				case LATIN_NON_WORD_CHARACTER:
					currentChar.setLetterSpacing(0.5);
					break;
				}
				break;
			case INSEPARABLE_CHARACTER:
				switch(nextChar.getCharClass()) {
				case EMPTY:
					break;
				case CRLF:
					break;
				case CR:
					break;
				case LF:
					break;
				case WHITESPACE:
					break;
				case OPENING_BRACKET:
					currentChar.setLetterSpacing(0.5);
					break;
				case CLOSING_BRACKET:
					break;
				case HYPHEN:
					break;
				case DIVIDING_PUNCTUATION_MARK:
					break;
				case MIDDLE_DOT:
					currentChar.setLetterSpacing(0.25);
					break;
				case FULL_STOP:
					break;
				case COMMA:
					break;
				case INSEPARABLE_CHARACTER:
					break;
				case JAPANESE:
					break;
				case LATIN_WORD_CHARACTER:
					break;
				case LATIN_NON_WORD_CHARACTER:
					break;
				}
				break;
			case JAPANESE:
				switch(nextChar.getCharClass()) {
				case EMPTY:
					break;
				case CRLF:
					break;
				case CR:
					break;
				case LF:
					break;
				case WHITESPACE:
					break;
				case OPENING_BRACKET:
					currentChar.setLetterSpacing(0.5);
					break;
				case CLOSING_BRACKET:
					break;
				case HYPHEN:
					break;
				case DIVIDING_PUNCTUATION_MARK:
					break;
				case MIDDLE_DOT:
					currentChar.setLetterSpacing(0.25);
					break;
				case FULL_STOP:
					break;
				case COMMA:
					break;
				case INSEPARABLE_CHARACTER:
					break;
				case JAPANESE:
					break;
				case LATIN_WORD_CHARACTER:
					currentChar.setLetterSpacing(0.25);
					break;
				case LATIN_NON_WORD_CHARACTER:
					// no space
					break;
				}
				break;
			case LATIN_WORD_CHARACTER:
				switch(nextChar.getCharClass()) {
				case EMPTY:
					break;
				case CRLF:
					break;
				case CR:
					break;
				case LF:
					break;
				case WHITESPACE:
					break;
				case OPENING_BRACKET:
					currentChar.setLetterSpacing(0.5);
					break;
				case CLOSING_BRACKET:
					break;
				case HYPHEN:
					break;
				case DIVIDING_PUNCTUATION_MARK:
					break;
				case MIDDLE_DOT:
					currentChar.setLetterSpacing(0.25);
					break;
				case FULL_STOP:
					break;
				case COMMA:
					break;
				case INSEPARABLE_CHARACTER:
					break;
				case JAPANESE:
					currentChar.setLetterSpacing(0.25);
					break;
				case LATIN_WORD_CHARACTER:
					break;
				case LATIN_NON_WORD_CHARACTER:
					break;
				}
				break;
			case LATIN_NON_WORD_CHARACTER:
				switch(nextChar.getCharClass()) {
				case EMPTY:
					break;
				case CRLF:
					break;
				case CR:
					break;
				case LF:
					break;
				case WHITESPACE:
					break;
				case OPENING_BRACKET:
					currentChar.setLetterSpacing(0.5);
					break;
				case CLOSING_BRACKET:
					break;
				case HYPHEN:
					break;
				case DIVIDING_PUNCTUATION_MARK:
					break;
				case MIDDLE_DOT:
					currentChar.setLetterSpacing(0.25);
					break;
				case FULL_STOP:
					break;
				case COMMA:
					break;
				case INSEPARABLE_CHARACTER:
					break;
				case JAPANESE:
					// no space
					break;
				case LATIN_WORD_CHARACTER:
					break;
				case LATIN_NON_WORD_CHARACTER:
					break;
				}
				break;
			}
			currentChar = nextChar;
		}
	}
}
