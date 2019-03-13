package net.osdn.catalpa.html;

public class Char implements Token {

	public enum CharClass {
		EMPTY,
		CRLF,
		CR,
		LF,
		WHITESPACE,
		LATIN_WORD_CHARACTER,
		LATIN_NON_WORD_CHARACTER,
		OPENING_BRACKET,
		CLOSING_BRACKET,
		HYPHEN,
		DIVIDING_PUNCTUATION_MARK,
		MIDDLE_DOT,
		FULL_STOP,
		COMMA,
		INSEPARABLE_CHARACTER,
		JAPANESE
	}
	
	private static final String OPENING_BRACKETS = "“‘（〔［｛〈《「『【";
	private static final String CLOSING_BRACKETS = "”’）〕］｝〉》」』】";
	private static final String HYPHENS = "‐―〜";
	private static final String DIVIDING_PUNCTUATION_MARKS = "！？";
	private static final String MIDDLE_DOTS = "・：；";
	private static final String FULL_STOPS = "。．";
	private static final String COMMAS = "、，";
	private static final String INSEPARABLE_CHARACTERS = "―…‥";
	
	private char value;
	private CharClass characterClass;
	private boolean isLatin;
	private boolean isWhitespace;
	private boolean isEndOfSentence;
	
	private Char previousChar;
	private Char nextChar;
	
	private double letterSpacing = 0.0;
	private double marginLeft    = 0.0;
	private double marginRight   = 0.0;
	
	/* create CR+LF */
	public Char() {
		characterClass = CharClass.CRLF;
		isLatin = true;
		isWhitespace = true;
	}
	
	public Char(char value) {
		this.value = value;
		
		if(value == 0) {
			characterClass = CharClass.EMPTY;
		} else if(value == 0x0D) {
			characterClass = CharClass.CR;
			isLatin = true;
			isWhitespace = true;
		} else if(value == 0x0A) {
			characterClass = CharClass.LF;
			isLatin = true;
			isWhitespace = true;
		} else if(value <= 255) {
			isLatin = true;
			if(Character.isWhitespace(value)) {
				characterClass = CharClass.WHITESPACE;
				isWhitespace = true;
			} else if( ('0' <= value && value <= '9')
					|| ('a' <= value && value <= 'z')
					|| ('A' <= value && value <= 'Z')
					|| (value == '!')
					|| (value == '*') || (value == '+') || (value == '-') || (value == '/')
					|| (value == '.') || (value == ':') || (value == '=') || (value == '?')
					|| (value == '@') || (value == '\\') || (value == '_')) {
				characterClass = CharClass.LATIN_WORD_CHARACTER;
			} else {
				characterClass = CharClass.LATIN_NON_WORD_CHARACTER;
			}
		} else if(OPENING_BRACKETS.indexOf(value) != -1) {
			characterClass = CharClass.OPENING_BRACKET;
			marginLeft = -0.5;
		} else if(CLOSING_BRACKETS.indexOf(value) != -1) {
			characterClass = CharClass.CLOSING_BRACKET;
			marginRight = -0.5;
		} else if(HYPHENS.indexOf(value) != -1) {
			characterClass = CharClass.HYPHEN;
		} else if(DIVIDING_PUNCTUATION_MARKS.indexOf(value) != -1) {
			characterClass = CharClass.DIVIDING_PUNCTUATION_MARK;
		} else if(MIDDLE_DOTS.indexOf(value) != -1) {
			characterClass = CharClass.MIDDLE_DOT;
			marginLeft = -0.25;
			marginRight = -0.25;
		} else if(FULL_STOPS.indexOf(value) != -1) {
			characterClass = CharClass.FULL_STOP;
			marginRight = -0.5;
		} else if(COMMAS.indexOf(value) != -1) {
			characterClass = CharClass.COMMA;
			marginRight = -0.5;
		} else if(INSEPARABLE_CHARACTERS.indexOf(value) != -1) {
			characterClass = CharClass.INSEPARABLE_CHARACTER;
		} else if(Character.isWhitespace(value)) {
			characterClass = CharClass.WHITESPACE;
			isWhitespace = true;
		} else {
			characterClass = CharClass.JAPANESE;
		}
	}
	
	public char getValue() {
		return value;
	}
	
	public CharClass getCharClass() {
		return characterClass;
	}
	
	public boolean isLatin() {
		return isLatin;
	}
	
	public boolean isNewLine() {
		return (characterClass == CharClass.CRLF)
				|| (characterClass == CharClass.CR)
				|| (characterClass == CharClass.LF);
	}
	
	public boolean isCRLF() {
		return (characterClass == CharClass.CRLF);
	}
	
	public boolean isCR() {
		return (characterClass == CharClass.CR);
	}
	
	public boolean isLF() {
		return (characterClass == CharClass.LF);
	}
	
	public boolean isWhitespace() {
		return isWhitespace;
	}
	
	public boolean isDividingPunctuationMark() {
		return (value == '!') || (value == '?') || (DIVIDING_PUNCTUATION_MARKS.indexOf(value) != -1);
	}
	
	public void setEndOfSentence(boolean b) {
		this.isEndOfSentence = b;
	}
	
	public boolean isEndOfSentence() {
		return this.isEndOfSentence;
	}
	
	protected void setPreviousChar(Char previousChar) {
		this.previousChar = previousChar;
	}
	
	public Char getPreviousChar() {
		return previousChar;
	}
	
	protected void setNextChar(Char nextChar) {
		this.nextChar = nextChar;
	}
	
	public Char getNextChar() {
		return nextChar;
	}
	
	public double getLetterSpacing() {
		return letterSpacing;
	}
	
	public void setLetterSpacing(double space) {
		letterSpacing = space;
	}
	
	@Override
	public String getHtml() {
		if(isEndOfSentence) {
			if(value == '!' || value == '?') {
				letterSpacing = 1.0;
			} else if(DIVIDING_PUNCTUATION_MARKS.indexOf(value) != -1) {
				letterSpacing = 0.75;
			}
		}
		if(characterClass == CharClass.EMPTY) {
			return "";
		} else if(characterClass == CharClass.CRLF) {
			return "\r\n";
		} else if(marginLeft == 0.0 && marginRight == 0.0 && letterSpacing == 0.0) {
			return String.valueOf(value);
		} else {
			StringBuilder sb = new StringBuilder();
			if(marginLeft == 0.0 && marginRight == 0.0) {
				sb.append(value);
			} else {
				sb.append("<span style=\"");
				if(marginLeft != 0.0) {
					sb.append(String.format("margin-left:%.2fem;", marginLeft));
				}
				if(marginRight != 0.0) {
					sb.append(String.format("margin-right:%.2fem;", marginRight));
				}
				sb.append("\">");
				sb.append(value);
				sb.append("</span>");
			}
			if(letterSpacing != 0.0) {
				sb.append(String.format("<span style=\"font-family:monospace;margin-right:%.2fem;\"> </span>", (letterSpacing - 0.5)));
			}
			return sb.toString();
		}
	}
	
	@Override
	public String toString() {
		if(characterClass == CharClass.EMPTY) {
			return "";
		} else if(characterClass == CharClass.CRLF) {
			return "\r\n";
		} else {
			return String.valueOf(value);
		}
	}
}
