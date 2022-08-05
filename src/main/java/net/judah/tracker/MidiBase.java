package net.judah.tracker;

import lombok.Data;
import net.judah.tracker.todo.Key;

@Data 
public class MidiBase {

	private float velocity = 0.9f;
    private int data1;
    private GMDrum drum; // drum grid only

    public void setDrum(GMDrum drum) {
    	this.drum = drum;
    	this.data1 = drum.getMidi();
    }
    
    public MidiBase(int data1) {
        this.data1 = data1;
    }

    public MidiBase(GMDrum drum) {
        this(drum.getMidi());
        this.drum = drum;
    }

    @Override
    public String toString() {
        if (drum == null)
            return Key.values()[data1 % 12].name() + "  " + data1;
        return drum.getDisplay() + "." + data1;
    }
}
