package de.fastcrafter.hg;

public class BlockCoordinates {
	private int X = 0;
	private int Y = 0;
	private int Z = 0;

	public int getX() {
		return X;
	}

	public int getY() {
		return Y;
	}

	public int getZ() {
		return Z;
	}

	public void setX(int para) {
		X = para;
	}

	public void setY(int para) {
		Y = para;
	}

	public void setZ(int para) {
		Z = para;
	}
	
	public boolean equals(BlockCoordinates other) {
		return other.X == X && other.Y == Y && other.Z == Z;
	}
}
