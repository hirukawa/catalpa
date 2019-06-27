package net.osdn.catalpa;

public interface ProgressObserver {

	void setProgress(double value);
	void setText(String text);
	
	public static ProgressObserver EMPTY = new ProgressObserver() {
		@Override
		public void setProgress(double value) {
		}
		@Override
		public void setText(String text) {
		}
	};
}
