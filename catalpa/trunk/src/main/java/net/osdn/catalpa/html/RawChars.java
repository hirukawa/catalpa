package net.osdn.catalpa.html;

public class RawChars implements Token {

	public enum Type {
		INLINE_TAG_OPEN,
		INLINE_TAG_CLOSE,
		OTHER
	}
	
	private String value;
	private Type   type;
	private double letterSpacing = -1.0;
	
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
	
	@Override
	public String getHtml() {
		if(letterSpacing <= 0.0) {
			return value;
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append(value);
			sb.append(String.format("<span style=\"font-family:monospace;margin-right:%.2fem;\"> </span>", (letterSpacing - 0.5)));
			return sb.toString();
		}
	}
	
	@Override
	public String toString() {
		return value;
	}
}
