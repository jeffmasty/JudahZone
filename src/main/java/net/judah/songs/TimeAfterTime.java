package net.judah.songs;

import net.judah.api.Notification.Property;
import net.judah.tracker.Track;

public class TimeAfterTime extends SmashHit {
	int count;
	int measure;
	
	
	@Override
	public void startup() {
		clock.reset();
		clock.writeTempo(118);
		synth1.getSynthPresets().load("TimeAfterTime");
		synth1.setPreset("TimeAfterTime", true);
		guitar.setPreset("TimeAfterGtr", true);
		drumMachine.getDrum1().setKit("TimeAfter");
		
		drum1.setFile("TimeAfter1");
		drum1.getCycle().setCustom(this);
		drum1.setActive(true);

		lead1.setFile("TimeAfterTime");
		lead1.getCycle().setCustom(this);
		
		bass.setFile("TimeAfterBass");
		bass.getCycle().setCustom(this);
		
		
	}

	@Override
	public void cycle(Track t) {
		if (t == lead1)
			synthCycle(t);
		else if (t == bass)
			bassCycle();
		else if (t == drum1) {
			drum1.setCurrent(drum1.get(measure > 0 ? 1 : 0));
		}
	}
	
	private void synthCycle(Track t) {
		if (measure == 5) 
			t.setCurrent(t.get(0));
		else if (measure == 13)
			t.setCurrent(t.get(4));
		else if (measure == 21)
			t.setCurrent(t.get(8));
		else if (t.isActive())
			t.next(true);
	}
	
	private void bassCycle() {
		if (measure > 20 && measure < 28)
			bass.setPattern("Cvamp");
		else if (bass.isActive())
			bass.next(true);
	}
	
	@Override
	public void update(Property prop, Object value) {
		if (prop == Property.BARS) {
			measure = (Integer) value;
			
			if (measure == 1) {
				lead1.setActive(true);
			}
			if (measure == 5) {
				lead1.setCurrent(lead1.get(0));
			}
			else if (measure == 13) {
				bass.setActive(true);
			}
			else if (measure > 20 && measure < 28)
				bass.setPattern("Cvamp");
			
		}

		
	}
}
