package net.judah.song;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.NoArgsConstructor;
import net.judah.seq.arp.Arp;
import net.judah.seq.arp.ArpInfo;
import net.judah.seq.track.Cycle;

/** Atomic-backed Sched with JSON-compatible getters/setters. */
@NoArgsConstructor
public class PrismState {

	@JsonIgnore
	private final AtomicBoolean active = new AtomicBoolean(false);
	@JsonIgnore
	private final AtomicReference<Cycle> cycle = new AtomicReference<>(Cycle.AB);
	@JsonIgnore
	private final AtomicInteger launch = new AtomicInteger(0);
	@JsonIgnore
	private final AtomicReference<Float> amp = new AtomicReference<>(0.5f);
	@JsonIgnore
	private final AtomicReference<Arp> mode = new AtomicReference<>();
	@JsonIgnore
	private final AtomicReference<String> program = new AtomicReference<>();
	@JsonIgnore
	private final AtomicReference<ArpInfo> arp = new AtomicReference<>();

	public PrismState(PrismState clone) {
		setLaunch(clone.getLaunch());
		setActive(clone.isActive());
		setCycle(clone.getCycle());
		setAmp(clone.getAmp());
		setMode(clone.getMode());
		setProgram(clone.getProgram());
		setArp(clone.getArp() == null ? null : new ArpInfo(clone.getArp()));
	}

	public PrismState(boolean synth) {
		if (synth)
			setArp(new ArpInfo());
	}

	@JsonProperty("active")
	public boolean isActive() {
		return active.get();
	}

	@JsonProperty("active")
	public void setActive(boolean v) {
		active.set(v);
	}

	@JsonProperty("cycle")
	public Cycle getCycle() {
		return cycle.get();
	}

	@JsonProperty("cycle")
	public void setCycle(Cycle c) {
		cycle.set(c);
	}

	@JsonProperty("launch")
	public int getLaunch() {
		return launch.get();
	}

	@JsonProperty("launch")
	public void setLaunch(int v) {
		launch.set(v);
	}

	@JsonProperty("amp")
	public float getAmp() {
		return amp.get();
	}

	@JsonProperty("amp")
	public void setAmp(float v) {
		amp.set(v);
	}

	@JsonProperty("mode")
	public Arp getMode() {
		return mode.get();
	}

	@JsonProperty("mode")
	public void setMode(Arp m) {
		mode.set(m);
	}

	@JsonProperty("program")
	public String getProgram() {
		return program.get();
	}

	@JsonProperty("program")
	public void setProgram(String p) {
		program.set(p);
	}

	@JsonProperty("arp")
	@JsonInclude(Include.NON_NULL)
	public ArpInfo getArp() {
		return arp.get();
	}

	@JsonProperty("arp")
	public void setArp(ArpInfo a) {
		arp.set(a);
	}
}
