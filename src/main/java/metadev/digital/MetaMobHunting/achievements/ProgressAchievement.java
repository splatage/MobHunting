package metadev.digital.MetaMobHunting.achievements;

import metadev.digital.MetaMobHunting.mobs.ExtendedMob;

public interface ProgressAchievement extends Achievement
{
	int getNextLevel();
	
	String inheritFrom();
	
	String nextLevelId();
	
	ExtendedMob getExtendedMob();
}
