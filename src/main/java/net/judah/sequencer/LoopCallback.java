package net.judah.sequencer;

import lombok.Getter;
import lombok.Setter;
import net.judah.api.Notification;
import net.judah.api.TimeListener;
import net.judah.looper.Loop;

public class LoopCallback implements TimeListener {

    @Setter @Getter Loop receiver;
    @Setter @Getter Loop holder;

    @Override public void update(Notification.Property prop, Object value) {
        if (Notification.Property.LOOP != prop) return;
        receiver.play(true);
        holder.play(false);
        holder.removeListener(this);
    }

    public void configure(Loop current, Loop target) {
        this.holder = current;
        this.receiver = target;
        current.addListener(this);
    }
}