package net.judah.effects.api;

public interface Effect {

    String getName();
    boolean isActive();
    void setActive(boolean active);
    void set(int idx, float value);
    float get(int idx);
    int getParamCount();

}
