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
		return getHtml(false);
	}

	@Override
	public String getHtml(boolean useCatalpaFont) {
		if(letterSpacing <= 0.0) {
			return value;
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append(value);
			if(useCatalpaFont) {
				// catalpaフォントは半角スペースが四分アキになるように作られています。200% にすると二分アキになります。
				sb.append("<span style=\"font-family:catalpa;font-size:" + (int)(400 * letterSpacing) + "%;line-height:0\"> </span>");
			} else {
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
		return value;
	}
}
