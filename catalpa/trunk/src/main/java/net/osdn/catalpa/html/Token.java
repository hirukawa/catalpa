package net.osdn.catalpa.html;

public interface Token {

	double getLetterSpacing();
	void setLetterSpacing(double space);
	String getHtml();
	String getHtml(boolean useCatalpaFont);

}
