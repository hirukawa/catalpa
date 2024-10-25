package onl.oss.catalpa.html;

public interface Token {

	double getLetterSpacing();
	void setLetterSpacing(double space);
	String getLetterSpacingType();
	void setLetterSpacingType(String type);
	String getHtml(boolean halt);

}
