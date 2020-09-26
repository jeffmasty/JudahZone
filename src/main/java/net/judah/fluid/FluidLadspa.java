package net.judah.fluid;

import net.judah.util.JudahException;

/**<pre>
from: https://github.com/FluidSynth/fluidsynth/blob/master/doc/ladspa.md
ladspa_effect: Create a new effect from a plugin library
ladspa_buffer: Create a new buffer
ladspa_link: Link an effect port to a host port or a buffer
ladspa_set: Set the value of an effect control
ladspa_check: Check the effect setup for any problems
ladspa_start: Start the effects unit
ladspa_stop: Stop the effects unit
ladspa_reset: Reset the effects unit

from console help:
ladspa_clear               Resets LADSPA effect unit to bypass state
ladspa_add lib plugin n1 <- p1 n2 -> p2 ... Loads and connects LADSPA plugin
ladspa_start               Starts LADSPA effect unit
ladspa_declnode node value Declares control node `node' with value `value'
ladspa_setnode node value  Assigns `value' to `node'


 </pre>*/
public class FluidLadspa {
	final FluidSynth fluid;

	FluidLadspa(FluidSynth fluid) throws JudahException {
		this.fluid = fluid;

		// loadReverb();
		fluid.sendCommand("ladspa_start");
	}

	/** http://tap-plugins.sourceforge.net/ladspa/reverb.html */
	/* $ analyseplugin /usr/lib/ladspa/tap_reverb.so

		Plugin Name: "TAP Reverberator"
		Plugin Label: "tap_reverb"
		Plugin Unique ID: 2142
		Maker: "Tom Szilagyi"
		Copyright: "GPL"
		Must Run Real-Time: No
		Has activate() Function: Yes
		Has deactivate() Function: No
		Has run_adding() Function: Yes
		Environment: Normal
	    Ports:  "Decay [ms]" input, control, 0 to 10000, default 2500
        "Dry Level [dB]" input, control, -70 to 10, default 0
        "Wet Level [dB]" input, control, -70 to 10, default 0
        "Comb Filters" input, control, toggled, default 1
        "Allpass Filters" input, control, toggled, default 1
        "Bandpass Filter" input, control, toggled, default 1
        "Enhanced Stereo" input, control, toggled, default 1
        "Reverb Type" input, control, 0 to 42.1, default 0, integer
        "Input Left" input, audio
        "Output Left" output, audio
        "Input Right" input, audio
        "Output Right" output, audio */

	@SuppressWarnings("unused")
	private void loadReverb() {
		fluid.sendCommand("ladspa_add /usr/lib/ladspa/tap_reverb.so tap_reverb");
	}


}
