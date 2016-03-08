package de.fastcrafter.hg;

import org.bukkit.scheduler.BukkitRunnable;

public class TimingTask extends BukkitRunnable {
	private TimingThreadJob job;
	private HungerGames hg;

	public TimingTask(TimingThreadJob para,HungerGames para2) {
		job = para;
		hg = para2;
	}


	@Override
	public void run() {
		hg.executeTimedTask(job);
	}

}
