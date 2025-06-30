package onl.oss.catalpa.html;

public class RawChars implements Token {

	public enum Type {
		INLINE_TAG_OPEN,
		INLINE_TAG_CLOSE,
		OTHER
	}
	
	private final String value;
	private final Type   type;
	private double letterSpacing = -1.0;
	private String letterSpacingType;
	
	public RawChars(CharSequence value) {
		this(value, Type.OTHER);
	}
	
	public RawChars(CharSequence value, Type type) {
		this.value = value.toString();
		this.type = type;
	}

	public Type getType() {
		return type;
	}

	public double getLetterSpacing() {
		return letterSpacing;
	}
	
	public void setLetterSpacing(double space) {
		letterSpacing = space;
	}

	public String getLetterSpacingType() {
		return letterSpacingType;
	}

	public void setLetterSpacingType(String type) {
		letterSpacingType = type;
	}

	@Override
	public String getHtml(boolean halt) {
		if (letterSpacing <= 0.0) {
			return value;
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append(value);

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
			return sb.toString();
		}
	}
	
	@Override
	public String toString() {
		return value;
	}
}
