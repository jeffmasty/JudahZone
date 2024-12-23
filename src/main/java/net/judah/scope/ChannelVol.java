package net.judah.scope;

import static net.judah.util.Constants.LEFT;

import java.util.ArrayList;
import java.util.List;

import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.XYStyler;
import org.knowm.xchart.style.markers.None;

import be.tarsos.dsp.AudioEvent;

// time window size of computed (knob) (live vs. wav)
// resolution of computed vol (knob)
public class ChannelVol extends ScopeView {

	private static final String CH1 = "Left/Mono";
	private static final int MAX = 220;

    private final List<Integer> xData = new ArrayList<>();
    private final List<Float> yData = new ArrayList<>();
    private final XYChart chart = new XYChartBuilder().build();
    private final XChartPanel<XYChart> pnl;

    private int insertions;
    private boolean odd;
    private float rms;

	public ChannelVol() {

        XYStyler styler = chart.getStyler();
		styler.setLegendPosition(Styler.LegendPosition.InsideNW);
        styler.setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
        styler.setYAxisMin(-0.1);
        styler.setYAxisMax(0.6);

        for (;insertions < MAX; insertions++) {
        	xData.add(insertions);
        	yData.add(0f);
        }

        XYSeries left = chart.addSeries(CH1, xData, yData);
        left.setMarker(new None());
        pnl = new XChartPanel<>(chart);
        add(pnl);
 	}

	@Override
	public void process(float[][] stereo) {

		float now = (float)AudioEvent.calculateRMS(stereo[LEFT]);

		rms = now > rms ? now : rms;
	    // += 10f * (float)AudioEvent.calculateRMS(stereo[LEFT]);

        odd = !odd;
        if (!odd)
        	return;

        xData.add(insertions++);
        yData.add(rms * 10);

        rms = 0;
        if (xData.size() > MAX) {
            xData.removeFirst();
            yData.removeFirst();
        }
        chart.updateXYSeries(CH1, xData, yData, null);
        pnl.repaint();
	}

	/**
	 * Calculates and returns the root mean square of the signal. Please
	 * cache the result since it is calculated every time.
	 * @param floatBuffer The audio buffer to calculate the RMS for.
	 * @return The <a
	 *         href="http://en.wikipedia.org/wiki/Root_mean_square">RMS</a> of
	 *         the signal present in the current buffer.
	 */
	public static double calculateRMS(float[] floatBuffer){
		double rms = 0.0;
		for (int i = 0; i < floatBuffer.length; i++) {
			rms += floatBuffer[i] * floatBuffer[i];
		}
		rms = rms / Double.valueOf(floatBuffer.length);
		rms = Math.sqrt(rms);
		return rms;
	}

	@Override
	public void knob(int idx, int value) {
		// TODO Auto-generated method stub

	}

}
