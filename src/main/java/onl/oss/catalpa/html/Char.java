package onl.oss.catalpa.html;

public class Char implements Token {

	private static final String OPENING_BRACKETS = "“‘（〔［｛〈《「『【";
	private static final String CLOSING_BRACKETS = "”’）〕］｝〉》」』】";
	private static final String HYPHENS = "‐―〜";
	private static final String DIVIDING_PUNCTUATION_MARKS = "！？";
	private static final String MIDDLE_DOTS = "・：；";
	private static final String FULL_STOPS = "。．";
	private static final String COMMAS = "、，";
	private static final String INSEPARABLE_CHARACTERS = "―…‥";
	
	private char value;
	private final CharClass characterClass;
	private boolean isLatin;
	private boolean isWhitespace;
	private boolean isEndOfSentence;
	
	private Char previousChar;
	private Char nextChar;

	private double letterSpacing = 0.0;
	private String letterSpacingType;
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

	@SuppressWarnings("unused")
	public boolean isCRLF() {
		return (characterClass == CharClass.CRLF);
	}

	@SuppressWarnings("unused")
	public boolean isCR() {
		return (characterClass == CharClass.CR);
	}

	@SuppressWarnings("unused")
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

	@SuppressWarnings("unused")
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

	public void setLetterSpacingType(String type) {
		letterSpacingType = type;
	}

	public String getLetterSpacingType() {
		return letterSpacingType;
	}

	@Override
	public String getHtml(boolean halt) {
		if(isEndOfSentence) {
			if(isDividingPunctuationMark()) {
				letterSpacing = 1.0;
			}
		}
		if (characterClass == CharClass.EMPTY) {
			return "";
		} else if (characterClass == CharClass.CRLF) {
			return "\r\n";
		} else if (characterClass == CharClass.FULL_STOP && letterSpacing == 0.5) {
			return String.valueOf(value);
		} else if (value != 0 && marginLeft == 0.0 && marginRight == 0.0 && letterSpacing == 0.0) {
			return String.valueOf(value);
		} else {
			StringBuilder sb = new StringBuilder();
			if (value != 0) {
				if (marginLeft == 0.0 && marginRight == 0.0) {
					sb.append(value);
				} else if (halt) {
					// font-feature-settings: "halt" を使用して役物を半角幅にする <span class=h>
					sb.append("<span class=h>");
					sb.append(value);
					sb.append("</span>");
				} else {
					// font-feature-settings: "halt" を使わずに役物を半角幅にする
					// 左を詰める <span class=l>、右を詰める <span class=r>、左右を詰める <span class=lr>
					sb.append("<span class=");
					if (marginLeft != 0.0) {
						sb.append('l');
					}
					if (marginRight != 0.0) {
						sb.append('r');
					}
					sb.append('>');
					sb.append(value);
					sb.append("</span>");
				}
			}
			if (letterSpacing != 0.0) {
				if (letterSpacingType != null && letterSpacingType.contains("JAPANESE:LATIN_WORD_CHARACTER")) {
					// 和字と欧字の間隔 <span class=e>
					sb.append("<span class=e> </span>");
				} else if (letterSpacingType != null && letterSpacingType.contains("LATIN_WORD_CHARACTER:JAPANESE")) {
					// 欧字と和字の間隔 <span class=j>
					sb.append("<span class=j> </span>");
				} else {
					// 二分アキは <span class=s2>、四分アキは <span class=s4>
					sb.append("<span class=s").append((int)(1.0 / letterSpacing)).append("> </span>");
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
