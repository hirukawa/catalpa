package onl.oss.catalpa.html;

public class RawChars implements Token {

	public enum Type {
		INLINE_TAG_OPEN,
		INLINE_TAG_CLOSE,
		OTHER
	}
	
	private String value;
	private Type   type;
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
				// 和字と欧字の間隔 <i class=e>
				sb.append("<i class=e> </i>");
			} else if (letterSpacingType != null && letterSpacingType.contains("LATIN_WORD_CHARACTER:JAPANESE")) {
				// 欧字と和字の間隔 <i class=j>
				sb.append("<i class=j> </i>");
			} else {
				// 二分アキは <i class=s2>、四分アキは <i class=s4>
				sb.append("<i class=s").append((int)(1.0 / letterSpacing)).append("> </i>");
			}
			return sb.toString();
		}
	}
	
	@Override
	public String toString() {
		return value;
	}
}
