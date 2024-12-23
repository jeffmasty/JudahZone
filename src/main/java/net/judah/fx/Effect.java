package net.judah.fx;

public interface Effect {

    String getName();
    
    void setActive(boolean active);

    boolean isActive();

    int getParamCount();

    /**@return value of setting idx scaled from 0 to 100 */
    void set(int idx, int value);
  
    /**@param idx parameter setting to change 
     * @return changed value scaled from 0 to 100*/
    int get(int idx);
    
    
}
