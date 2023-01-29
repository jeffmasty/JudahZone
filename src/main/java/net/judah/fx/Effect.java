package net.judah.fx;

public interface Effect {

    String getName();
    boolean isActive();
    void setActive(boolean active);
    /**@return value of setting idx scaled from 0 to 100 */
    void set(int idx, int value);
    /**@param idx which setting to change 
     * @return changed value scaled from 0 to 100*/
    int get(int idx);
    int getParamCount();
    
}
