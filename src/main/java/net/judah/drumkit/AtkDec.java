package net.judah.drumkit;

public interface AtkDec {

	int getDk(int typeIdx);
	int getAtk(int typeIdx);
	int getPan(int typeIdx);
	int getVol(int typeIdx);

	void setVol(int type, int val);
	void setPan(int type, int val);
	void setAtk(int type, int val);
	void setDk(int type, int val);

}
