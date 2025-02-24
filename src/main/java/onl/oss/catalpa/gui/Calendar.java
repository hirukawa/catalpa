package onl.oss.catalpa.gui;

import javafx.scene.control.DatePicker;
import javafx.scene.control.skin.DatePickerSkin;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public class Calendar extends StackPane {

	private DatePicker datePicker;
	private DatePickerSkin skin;

	@SuppressWarnings("this-escape")
	public Calendar() {
		datePicker = new DatePicker();
		skin = new DatePickerSkin(datePicker);
		Region node = (Region)skin.getPopupContent();
		node.setEffect(null);
		getChildren().add(node);
	}
	
	public DatePicker getDatePicker() {
		return datePicker;
	}
}
