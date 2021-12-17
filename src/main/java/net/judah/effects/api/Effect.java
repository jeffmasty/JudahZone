package net.judah.effects.api;

public interface Effect {

    String getName();
    boolean isActive();
    void setActive(boolean active);
    /**@param idx
     * @param value scaled from 0 to 100 */
    void set(int idx, int value);
    /**@param idx
     * @return parameter value of idx scaled from 0 to 100*/
    int get(int idx);
    int getParamCount();
    
}
