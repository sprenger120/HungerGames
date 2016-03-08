package de.fastcrafter.hg;

public class PlayerInstance {
	private PlayerStatus status = PlayerStatus.INLOBBY;
	private BlockCoordinates SlotCoords = null;
	private String lastSpecPlayer = new String("");
	private long lastTeleport = 0;
	
	public PlayerStatus getPlayerStatus() {
		return status;
	}

	public BlockCoordinates getSlotCoordinates() {
		return SlotCoords;
	}
	
	
	public void setSlotCoordinates(BlockCoordinates para) {
		SlotCoords = para;
	}
	
	public void setPlayerStatus(PlayerStatus para) {
		status = para;
	}
	
	public void setLastSpecPlayer(String para) {
		lastSpecPlayer = para;
	}
	
	public String getLastSpecPlayer() {
		return lastSpecPlayer;
	}
		
	public void updateLastTeleport() { 
		lastTeleport = System.currentTimeMillis();
	}
	
	public boolean isTeleportCooledDown() {
		return lastTeleport + 10000 < System.currentTimeMillis();
	}
}
