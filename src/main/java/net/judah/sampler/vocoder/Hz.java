package net.judah.sampler.vocoder;

import judahzone.util.Frequency;

public record Hz(float freq) {

	public Hz{
		freq = Math.max(Frequency.MIN, Math.min(Frequency.MAX, freq));
	}

//    @Test void testCtorClamps() {
//        Hz low = new Hz(1f);
//        assertEquals(Frequency.MIN, low.freq(), EPS, "Constructor should clamp up to MIN");
//        Hz high = new Hz(20000f);
//        assertEquals(Frequency.MAX, high.freq(), EPS, "Constructor should clamp down to MAX");
//        Hz ok = new Hz(440f);
//        assertEquals(440f, ok.freq(), EPS, "Constructor should preserve in-range value"); }

}
