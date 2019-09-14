package net.osdn.catalpa.html;

public class Char implements Token {

	public enum CharClass {
		EMPTY,
		OPENING_BOUNDARY,
		CLOSING_BOUNDARY,
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
		JAPANESE,
		OTHER
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
	public Char(CharClass charClass) {
		switch(charClass) {
		case EMPTY:
			characterClass = CharClass.EMPTY;
			value = 0;
			break;
			
		case OPENING_BOUNDARY:
			characterClass = CharClass.OPENING_BOUNDARY;
			value = 0;
			break;
			
		case CLOSING_BOUNDARY:
			characterClass = CharClass.CLOSING_BOUNDARY;
			value = 0;
			break;
			
		case CRLF:
			characterClass = CharClass.CRLF;
			isLatin = true;
			isWhitespace = true;
			break;
			
		default:
			throw new IllegalArgumentException();
		}
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
					|| ('A' <= value && value <= 'Z')) {
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
			marginLeft = 0.0;
			marginRight = 0.0;
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
			if((value == 0x3005) // 3005 々 (漢字の踊り字)
					|| (value == 0x3007) // 3007 〇 (漢数字のゼロ)
					|| (value == 0x303B) // 303B 〻 (漢字の踊り字)
					|| (0x3040 <= value && value <= 0x309F) // Hiragana / 平仮名
					|| (0x30A0 <= value && value <= 0x30FF) // Katakana / 片仮名
					|| (0x31F0 <= value && value <= 0x31FF) // Katakana Phonetic Extensions / 片仮名拡張
					|| (0x4E00 <= value && value <= 0x9FEF) // CJK Unified Ideographs / CJK統合漢字
					|| (0x3400 <= value && value <= 0x4DB5) // CJK Unified Ideographs Extension A / CJK統合漢字拡張A
					|| (0xF900 <= value && value <= 0xFAFF) // CJK Compatibillity Ideographs / CJK互換漢字
					) {
				characterClass = CharClass.JAPANESE;
			} else {
				// 0xFF00～0xFFEFに配置されている全角記号などは和字として分類しません。すなわち、欧字との間にアキを設けません。
				characterClass = CharClass.OTHER;
			}
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
			if(isDividingPunctuationMark()) {
				letterSpacing = 1.0;
			}
		}
		if(characterClass == CharClass.EMPTY) {
			return "";
		} else if(characterClass == CharClass.CRLF) {
			return "\r\n";
		} else if(characterClass == CharClass.FULL_STOP && letterSpacing == 0.5) {
			return String.valueOf(value);
		} else if(value != 0 && marginLeft == 0.0 && marginRight == 0.0 && letterSpacing == 0.0) {
			return String.valueOf(value);
		} else {
			StringBuilder sb = new StringBuilder();
			if(value != 0) {
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
			}
			if(letterSpacing != 0.0) {
				if(letterSpacing <= 0.5) {
					sb.append(String.format("<span style=\"font-family:monospace;font-size:%d%%;\"> </span>", (int)(200 * letterSpacing)));
				} else {
					//letterSpacingが0.5よりも大きい場合、font-sizeが100%を超えます。この場合、行間が広くならないようにline-height:0.1;を付加します。
					sb.append(String.format("<span style=\"font-family:monospace;font-size:%d%%;line-height:0.1;\"> </span>", (int)(200 * letterSpacing)));
				}
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
