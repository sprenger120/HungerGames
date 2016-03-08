package de.fastcrafter.hg;

import java.util.HashMap;
import java.util.Map;

public class Configuration {
	private int LoobySpawnX;
	private int LoobySpawnY;
	private int LoobySpawnZ;
	private int ArenaX;
	private int ArenaY;
	private int ArenaZ;
	private int ArenaRadius;
	private Map<Integer, String> Chests = new HashMap<Integer, String>();
	private Map<Integer, String> Slots = new HashMap<Integer, String>();

	public void setLoobySpawnX(int param) {
		this.LoobySpawnX = param;
	}

	public void setLoobySpawnY(int param) {
		this.LoobySpawnY = param;
	}

	public void setLoobySpawnZ(int param) {
		this.LoobySpawnZ = param;
	}

	public void setArenaX(int param) {
		this.ArenaX = param;
	}

	public void setArenaY(int param) {
		this.ArenaY = param;
	}

	public void setArenaZ(int param) {
		this.ArenaZ = param;
	}

	public void setArenaRadius(int param) {
		this.ArenaRadius = param;
	}

	public void setChests(Map<Integer, String> param) {
		this.Chests = param;
	}

	public void setSlots(Map<Integer, String> param) {
		this.Slots = param;
	}

	public int getLoobySpawnX() {
		return this.LoobySpawnX;
	}

	public int getLoobySpawnY() {
		return this.LoobySpawnY;
	}

	public int getLoobySpawnZ() {
		return this.LoobySpawnZ;
	}

	public int getArenaX() {
		return this.ArenaX;
	}

	public int getArenaY() {
		return this.ArenaY;
	}

	public int getArenaZ() {
		return this.ArenaZ;
	}

	public int getArenaRadius() {
		return this.ArenaRadius;
	}

	public Map<Integer, String> getChests() {
		return this.Chests;
	}

	public Map<Integer, String> getSlots() {
		return this.Slots;
	}
}
