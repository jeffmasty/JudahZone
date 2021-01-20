package net.judah.effects.api;

public interface Effect {

    boolean isActive();
    void setActive(boolean active);
    void set(int idx, float value);
    Number get(int idx);
    int getParamCount();

}
