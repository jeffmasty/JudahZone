package net.judah.api;

public interface TimeProvider extends TimeNotifier {
	
	float getTempo();
	/**@return true if the operation is supported and successful */ 
	boolean setTempo(float tempo);

	/**@param intro Beats until Time Transport starts (null, the default, indicates Transport won't be triggered)
	 * @param duration Beats until clicks end. (null, the default, indicates clicktrack has no set ending.)*/
	// void setDuration(Integer intro, Integer duration);
	int getMeasure();
	void setMeasure(int bpb);

	boolean beat(int current);
	
}
