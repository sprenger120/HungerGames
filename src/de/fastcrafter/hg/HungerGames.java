package de.fastcrafter.hg;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Difficulty;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.kitteh.tag.PlayerReceiveNameTagEvent;
import org.kitteh.tag.TagAPI;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;

public final class HungerGames extends JavaPlugin implements Listener {
	private World overworld = null;

	private HashMap<String, PlayerInstance> Players = new HashMap<String, PlayerInstance>();
	private Random rnd = new Random();

	private boolean fMatchStarted = false;
	private boolean fCountdown = false;
	private double iTimeLeftTillStart = -1;

	private Configuration HgConf = new Configuration();
	private String ConfigPath = new String("");

	private String chatError = new String("§4[HungerGames] ");
	private String chatWarning = new String("§e[HungerGames] ");
	private String chatSuccess = new String("§a[HungerGames] ");

	private boolean fArenaRadiusSet = false;
	private boolean fArenaCoordsSet = false;

	private Player plrWinner = null;
	private byte Countdown = -1;
	private int iFireworkEffect = 0;
	private double FireworkAngle = 0.0;

	double ArenaRadius = 10; // Firework and low loot chests

	private String MapAuthor = new String("unknownAuthor");
	private String MapName = new String("unknownMap");

	public void onEnable() {
		// Get overworld
		List<World> worldList = Bukkit.getWorlds();
		for (int i = 0; i <= worldList.size() - 1; i++) {
			if (worldList.get(i).getEnvironment() == World.Environment.NORMAL) {
				overworld = worldList.get(i);
				break;
			}
		}

		ConfigPath = (new File("").getAbsolutePath()) + File.separator + overworld.getName() + File.separator + "hungergames.yml";

		// Loading Config
		if (!(new File(ConfigPath).exists())) {// not existing
			HgConf.setLoobySpawnX(overworld.getSpawnLocation().getBlockX());
			HgConf.setLoobySpawnY(overworld.getSpawnLocation().getBlockY());
			HgConf.setLoobySpawnZ(overworld.getSpawnLocation().getBlockZ());
			HgConf.setArenaX(overworld.getSpawnLocation().getBlockX());
			HgConf.setArenaY(overworld.getSpawnLocation().getBlockY());
			HgConf.setArenaZ(overworld.getSpawnLocation().getBlockZ());
			HgConf.setArenaRadius(1);
			Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.RED + "Configuration is missing!");
		} else {
			try {
				Yaml yaml = new Yaml(new CustomClassLoaderConstructor(Configuration.class.getClassLoader()));
				InputStream in = Files.newInputStream(Paths.get(ConfigPath));
				HgConf = (Configuration) yaml.load(in);

				fArenaRadiusSet = true;
				fArenaCoordsSet = true;
			} catch (Exception e) {
				Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.RED + "Error while loading configuration! " + e.toString());
			}

		}

		// Load map info
		String MapInfoPath = (new File("").getAbsolutePath()) + File.separator + overworld.getName() + File.separator + "WorldName.txt";
		File fileMapInfo = new File(MapInfoPath);
		if (fileMapInfo.exists()) {
			do {
				List<String> lines = null;
				try {
					lines = Files.readAllLines(Paths.get(MapInfoPath), StandardCharsets.UTF_8);
				} catch (IOException e) {
					e.printStackTrace();
					break;
				}
				
				if (lines.size() < 2) {
					break;
				}
				
				MapName = lines.get(0);
				MapAuthor = lines.get(1);
				
				getLogger().info("MapName:" + MapName + " Author:" + MapAuthor);
			} while (false);
		}
		
		for (Player pl : Bukkit.getOnlinePlayers()) {
			Players.put(pl.getName(), new PlayerInstance());
			showPlayer(pl);
			if (pl.isOp()) {
				continue;
			}
			pl.setAllowFlight(false);
			teleportToBlockCoordinates(pl, HgConf.getLoobySpawnX(), HgConf.getLoobySpawnY() + 2, HgConf.getLoobySpawnZ());
			pl.setGameMode(GameMode.ADVENTURE);
		}

		overworld.setPVP(false);
		overworld.setDifficulty(Difficulty.PEACEFUL);
		overworld.setSpawnFlags(false, true);
		overworld.setTime(14550);

		new TimingTask(TimingThreadJob.RESETTIME, this).runTaskLater(this, 4000);
		new TimingTask(TimingThreadJob.TIMESCHEDULE, this).runTaskLater(this, 600); // every
																					// 30
																					// secs

		rnd.setSeed(System.currentTimeMillis());

		getLogger().info("Hungergames successfully loaded. Using world " + overworld.getName());
		getServer().getPluginManager().registerEvents(this, this);
	}

	public void onDisable() {
		try {
			FileWriter writer = new FileWriter(ConfigPath);
			DumperOptions options = new DumperOptions();
			options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
			Yaml yaml = new Yaml(options);

			String output = yaml.dump(HgConf);

			writer.write(output);
			writer.close();
		} catch (Exception e) {
			Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.RED + "Error while saving configuration!");
		}
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Players.put(event.getPlayer().getName(), new PlayerInstance());
		event.getPlayer().sendMessage("§2§lWelcome to the HungerGames v5, made by Retus and Godofcode120 aka Sprenger120");

		if (fMatchStarted) {
			event.getPlayer().sendMessage("§4The match already started. Use §b§l/hg spec§r§4 to spectate.");
		} else {
			event.getPlayer().sendMessage("§4Type in §b§l/hg join§r§4 to join the arena");
		}
		
		event.getPlayer().sendMessage("§e§lYou are playing on §r§6" + MapName + "§r§e§l by §r§6" + MapAuthor);

		if (event.getPlayer().isOp()) {
			return;
		}
		teleportToBlockCoordinates(event.getPlayer(), HgConf.getLoobySpawnX(), HgConf.getLoobySpawnY() + 2, HgConf.getLoobySpawnZ());
		if (!event.getPlayer().isOp()) {
			event.getPlayer().setGameMode(GameMode.ADVENTURE);
		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		if (fMatchStarted && Players.get(event.getPlayer().getName()).getPlayerStatus() == PlayerStatus.INARENA) {
			Bukkit.broadcastMessage("§4Player §l" + event.getPlayer().getName() + "§r§4 forfeit");
		}
		Players.remove(event.getPlayer().getName());
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		PlayerInstance instance = null;
		if (event.getTo().distance(event.getFrom()) == 0) {
			return;
		}

		if ((instance = Players.get(event.getPlayer().getName())).getPlayerStatus() == PlayerStatus.INARENA && !fMatchStarted) {
			Location loc = new Location(overworld, (double) instance.getSlotCoordinates().getX() + 0.5,
					(double) instance.getSlotCoordinates().getY(), (double) instance.getSlotCoordinates().getZ() + 0.5);

			event.getPlayer().teleport(loc);
		}

	}

	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		event.setRespawnLocation(new Location(overworld, (double) HgConf.getLoobySpawnX(), (double) HgConf.getLoobySpawnY() + 2, (double) HgConf
				.getLoobySpawnZ()));
	}

	@EventHandler
	public void onEntityDeath(EntityDeathEvent event) {
		if (!(event.getEntity() instanceof Player)) {
			return;
		}
		Player player = (Player) event.getEntity();
		PlayerInstance playerI = Players.get(player.getName());

		if (playerI.getPlayerStatus() != PlayerStatus.INARENA) {
			teleportToBlockCoordinates(player, HgConf.getLoobySpawnX(), HgConf.getLoobySpawnY() + 2, HgConf.getLoobySpawnZ());
			clearPlayerInv(player);
			return;
		}

		if (playerI.getPlayerStatus() == PlayerStatus.INARENA && !(fMatchStarted || fCountdown)) {
			teleportToBlockCoordinates(player, playerI.getSlotCoordinates().getX(), playerI.getSlotCoordinates().getX() + 2, playerI
					.getSlotCoordinates().getX());
			return;
		}

		Players.get(player.getName()).setPlayerStatus(PlayerStatus.SPECTATOR);
		teleportToBlockCoordinates(player, HgConf.getLoobySpawnX(), HgConf.getLoobySpawnY() + 2, HgConf.getLoobySpawnZ());
		clearPlayerInv(player);
		startSpectating(player);
		TagAPI.refreshPlayer(player);

		int iAlive = 0;
		Player lastPlayer = null;
		for (String name : Players.keySet()) {
			if (Players.get(name).getPlayerStatus() == PlayerStatus.INARENA) {
				iAlive++;
				lastPlayer = Bukkit.getPlayer(name);
			}
		}

		if (iAlive == 0) {
			return;
		}

		if (iAlive > 1) {
			Bukkit.broadcastMessage("§9" + player.getName() + " died! There are " + iAlive + " players remaining");
		} else {
			Bukkit.broadcastMessage("§9§l" + lastPlayer.getName() + " won the HungerGames!");

			for (String name : Players.keySet()) {
				Player plr = Bukkit.getPlayer(name);
				if (plr.isOp() || plr.getName().equals(lastPlayer.getName())) {
					continue;
				}

				PlayerInstance inst = Players.get(name);
				if (inst.getSlotCoordinates() == null) {
					teleportToBlockCoordinates(plr, HgConf.getArenaX(), HgConf.getArenaY(), HgConf.getArenaZ());
				} else {
					teleportToBlockCoordinates(plr, inst.getSlotCoordinates());
					teleportToBlockCoordinates(plr, inst.getSlotCoordinates());
				}
				healAndClearPlayer(plr);
			}

//			Location locThrone = new Location(overworld, (double) HgConf.getArenaX(), (double) HgConf.getArenaY() + 30, (double) HgConf.getArenaZ());
//			Location throneBase = locThrone.clone();
//
//			overworld.getBlockAt(throneBase).setType(Material.DIAMOND_BLOCK);
//
//			// first row
//			throneBase.setX(locThrone.getX() - 1);
//			overworld.getBlockAt(throneBase).setType(Material.GOLD_BLOCK);
//
//			throneBase.setZ(locThrone.getZ() - 1);
//			overworld.getBlockAt(throneBase).setType(Material.GOLD_BLOCK);
//
//			throneBase.setZ(locThrone.getZ() + 1);
//			overworld.getBlockAt(throneBase).setType(Material.GOLD_BLOCK);
//
//			throneBase = locThrone.clone();
//
//			// second
//			throneBase.setZ(locThrone.getZ() - 1);
//			overworld.getBlockAt(throneBase).setType(Material.GOLD_BLOCK);
//
//			throneBase.setZ(locThrone.getZ() + 1);
//			overworld.getBlockAt(throneBase).setType(Material.GOLD_BLOCK);
//
//			throneBase = locThrone.clone();
//
//			// third row
//			throneBase.setX(locThrone.getX() + 1);
//			overworld.getBlockAt(throneBase).setType(Material.GOLD_BLOCK);
//
//			throneBase.setZ(locThrone.getZ() - 1);
//			overworld.getBlockAt(throneBase).setType(Material.GOLD_BLOCK);
//
//			throneBase.setZ(locThrone.getZ() + 1);
//			overworld.getBlockAt(throneBase).setType(Material.GOLD_BLOCK);
//
//			locThrone.setY(locThrone.getY() + 2);

			lastPlayer.setAllowFlight(true);
			lastPlayer.sendMessage(chatSuccess + "You can fly now!");
//			lastPlayer.teleport(locThrone);

			healAndClearPlayer(lastPlayer);
			plrWinner = lastPlayer;

			overworld.setDifficulty(Difficulty.PEACEFUL);
			overworld.setTime(14550);

			new TimingTask(TimingThreadJob.FIREWORK, this).runTaskLater(this, 1);
			new TimingTask(TimingThreadJob.FIREWORK, this).runTaskLater(this, 55);
			new TimingTask(TimingThreadJob.FIREWORK, this).runTaskLater(this, 110);
			new TimingTask(TimingThreadJob.FIREWORK, this).runTaskLater(this, 165);
		}
	}

	@EventHandler
	public void onInventoryOpen(InventoryOpenEvent event) {
		// prevent players that are not inarena to open chests
		if (event.getPlayer().isOp()) {
			return;
		}
		if (Players.get(event.getPlayer().getName()).getPlayerStatus() != PlayerStatus.INARENA) {
			event.setCancelled(true);

		}
	}

	@EventHandler
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		if (event.getDamager() instanceof Player) {
			// prevents specs from dealing damage to fighters
			Player damager = (Player) event.getDamager();

			if (damager.isOp()) {
				return;
			}
			if (Players.get(damager.getName()).getPlayerStatus() != PlayerStatus.INARENA) {
				event.setCancelled(true);
			}
		}

		if (event.getDamager() instanceof Arrow && event.getEntity() instanceof Player && ((Arrow) event.getDamager()).getShooter() instanceof Player) {
			// "teleport" arrows through spectator
			Arrow ar = (Arrow) event.getDamager();
			Player target = (Player) event.getEntity();
			Player shooter = (Player) ar.getShooter();

			if (Players.get(target.getName()).getPlayerStatus() == PlayerStatus.INARENA) {
				return;
			}

			target.teleport(target.getLocation().add(0, 3, 0));
			target.setFlying(true);

			Arrow newArr = shooter.launchProjectile(Arrow.class);
			newArr.setBounce(false);
			newArr.setShooter(shooter);
			newArr.setVelocity(ar.getVelocity());

			event.setCancelled(true);
			ar.remove();

		}

	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.getPlayer().isOp()) {
			return;
		}
		event.setCancelled(true);
	}

	@EventHandler
	public void onPlayerAnimation(PlayerAnimationEvent event) {
		// move to next person as spectator
		if (!fMatchStarted || plrWinner != null) {
			return;
		}
		PlayerInstance inst = Players.get(event.getPlayer().getName());
		if (inst.getPlayerStatus() != PlayerStatus.SPECTATOR) {
			return;
		}

		if (!inst.getLastSpecPlayer().equals("")) {
			// Try to find last spectated person and choose the next
			boolean fFound = false;
			for (String name : Players.keySet()) {

				if (fFound && Players.get(name).getPlayerStatus() == PlayerStatus.INARENA) {

					if (!Players.get(name).isTeleportCooledDown()) {
						event.getPlayer().sendMessage(chatWarning + "You have to wait at least 10 seconds between spectating different players");
						return;
					}

					inst.setLastSpecPlayer(name);
					Location plrLoc = Bukkit.getPlayer(name).getLocation();
					event.getPlayer().teleport(plrLoc);

					Players.get(name).updateLastTeleport();
					return;
				}

				if (name.equals(inst.getLastSpecPlayer()) && Players.get(name).getPlayerStatus() == PlayerStatus.INARENA) {
					fFound = true;
					inst.setLastSpecPlayer("");
				}
			}
		}

		// First time clicked/start from the top of the player list
		for (String name : Players.keySet()) {
			// find first person in list who is alive
			if (Players.get(name).getPlayerStatus() == PlayerStatus.INARENA) {

				if (!Players.get(name).isTeleportCooledDown()) {
					event.getPlayer().sendMessage(chatWarning + "You have to wait at least 10 seconds between spectating different players");
					return;
				}

				inst.setLastSpecPlayer(name);
				Location plrLoc = Bukkit.getPlayer(name).getLocation();
				event.getPlayer().teleport(plrLoc);

				Players.get(name).updateLastTeleport();

				return;
			}
		}
	}

	@EventHandler
	public void onEntitiyDamage(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof Player)) {
			return;
		}

		Player plr = (Player) event.getEntity();
		if (Players.get(plr.getName()).getPlayerStatus() != PlayerStatus.INARENA) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onNameTag(PlayerReceiveNameTagEvent event) {
		switch (Players.get(event.getNamedPlayer().getName()).getPlayerStatus()) {
		case INARENA:
			event.setTag(ChatColor.RED + event.getNamedPlayer().getName());
			break;
		case SPECTATOR:
		case INLOBBY:
			event.setTag(event.getNamedPlayer().getName());
			break;
		}
	}

	@EventHandler
	public void onPlayerPickupItem(PlayerPickupItemEvent e) {
		switch (Players.get(e.getPlayer().getName()).getPlayerStatus()) {
		case INLOBBY:
		case INARENA:
			break;
		case SPECTATOR:
			e.setCancelled(true);
			break;
		}
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent e) {
		if (e.getPlayer().isOp()) {
			return;
		}
		e.setCancelled(true);
	}

	public void executeTimedTask(TimingThreadJob job) {
		switch (job) {
		case COUNTDOWN:
			jobCountdown();
			break;
		case ACTIVATE_PVP:
			ActivatePVP();
			break;
		case FIREWORK:
			jobFirework();
			break;
		case RESETTIME:
			if (fMatchStarted) {
				return;
			}
			overworld.setTime(14550);
			new TimingTask(TimingThreadJob.RESETTIME, this).runTaskLater(this, 4000);
			break;
		case TIMESCHEDULE:
			new TimingTask(TimingThreadJob.TIMESCHEDULE, this).runTaskLater(this, 600);
			if (fMatchStarted || fCountdown) {
				return;
			}

			if (iTimeLeftTillStart > -1) {
				iTimeLeftTillStart -= 0.5;
				if (iTimeLeftTillStart < 0.5) {
					new TimingTask(TimingThreadJob.COUNTDOWN, this).runTaskLater(this, 1);
					fCountdown = true;
					prepareStart(true);
					return;
				}
				Bukkit.broadcastMessage("§4§l" + ((int) Math.ceil(iTimeLeftTillStart)) + " minute" + (iTimeLeftTillStart > 1 ? "s" : "")
						+ " left. Use /hg join to participate!");
			} else {
				Player[] plrs = Bukkit.getOnlinePlayers();
				if (plrs.length > 0) {
					for (int i = 0; i <= plrs.length - 1; i++) {
						// prevent log spam
						plrs[i].sendMessage("§4§lUse /hg join to participate!");
					}
				}

			}
			break;
		}
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!cmd.getName().equalsIgnoreCase("hg"))
			return true;

		if (sender instanceof Player) {
			Player player = (Player) sender;

			if (args.length == 0) {
				player.sendMessage(chatWarning + "Missing parameter. Use /hg help to show the help");
				return true;
			}

			switch (args[0].toLowerCase()) {
			case "join":
				commandJoin(player);
				break;
			case "arenamiddle":
				commandArenaMiddle(player);
				break;
			case "scanchests":
				commandScanChests(player);
				break;
			case "arenaradius":
				commandArenaRadius(player, args);
				break;
			case "addslot":
				commandAddSlot(player);
				break;
			case "start":
				commandStart(player);
				break;
			case "leave":
				commandLeave(player);
				break;
			case "setlobby":
				commandSetLobby(player);
				break;
			case "spec":
				commandSpec(player);
				break;
			case "lobby":
				commandLobby(player);
				break;
			case "refillchests":
				commandRefillChests(player);
				break;
			case "help":
				commandHelp(player);
				break;
			case "sd":
				commandSuddenDeath(player);
				break;
			case "firework":
				commandFirework(player);
				break;
			case "alive":
				commandAlive(player);
				break;
			case "arena":
				commandArena(player);
				break;
			case "schedulestart":
				commandScheduleStart(player, args);
				break;
			case "removeslot":
				commandRemoveSlot(player);
				break;
			case "debug":
				commandDebug();
				break;
			default:
				sender.sendMessage(chatError + "Unknown Command.");
				break;
			}
		} else {
			if (args[0].toLowerCase().equals("debug")) {
				commandDebug();
			} else {
				sender.sendMessage(chatError + "/hg can only be executed by players!");
			}
		}

		return true;
	}

	@EventHandler
	public void onPlayerChatEvent(PlayerChatEvent e) {
		e.setCancelled(true);

		if (e.getPlayer().isOp()) {
			Bukkit.broadcastMessage(ChatColor.RED + "[Admin] " + e.getPlayer().getName() + ChatColor.YELLOW + ": " + e.getMessage());
			return;
		}

		switch (Players.get(e.getPlayer().getName()).getPlayerStatus()) {
		case INARENA:
			Bukkit.broadcastMessage(ChatColor.YELLOW + "[Fighter] " + e.getPlayer().getName() + ChatColor.WHITE + ": " + e.getMessage());
			return;
		case INLOBBY:
			Bukkit.broadcastMessage(ChatColor.WHITE + "[Player] " + e.getPlayer().getName() + ChatColor.WHITE + ": " + e.getMessage());
			return;
		case SPECTATOR:
			Bukkit.broadcastMessage(ChatColor.GRAY + "[Spectator] " + e.getPlayer().getName() + ChatColor.GRAY + ": " + e.getMessage());
			return;
		}
	}

	private int scanChests(int MiddleX, int MiddleZ, int Radius) {
		getLogger().info("Start chest scanning...");

		int ScannedBlocks = 0;
		int ScannedChest = 0;
		long swStart = System.currentTimeMillis();

		for (int x = MiddleX - Radius; x <= MiddleX + Radius; x++) {
			for (int z = MiddleZ - Radius; z <= MiddleZ + Radius; z++) {
				int highestBlock = overworld.getHighestBlockYAt(x, z);
				for (int y = 0; y <= highestBlock; y++) {

					ScannedBlocks++;
					Block blk = null;
					if ((blk = overworld.getBlockAt(x, y, z)).getType() == Material.CHEST) {
						BlockState bs = blk.getState();
						Chest chest = (Chest) bs;
						chest.getBlockInventory().clear();
						chest.update();

						HgConf.getChests().put(HgConf.getChests().size() + 1, x + ";" + y + ";" + z);

						ScannedChest++;
					}
				}
			}
		}

		long time = ((System.currentTimeMillis() - swStart) / 1000);
		if (time == 0) {
			time = 1;
		}
		getLogger().info(
				"Scanned " + ScannedBlocks + "Blocks  and " + ScannedChest + " Chest(s) in " + time + " seconds @" + (ScannedBlocks / time)
						+ " per second");

		// refillChests();
		return ScannedChest;
	}

	private int getDistance(int fx, int fy, int tx, int ty) {
		return (int) Math.sqrt((double) ((tx - fx) * (tx - fx) + (ty - fy) * (ty - fy)));
	}

	private BlockCoordinates extractLocation(String str) {
		try {
			String[] arr = str.split(";");
			if (arr.length < 3) {
				Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.RED + "Corrupted coordinate entry " + str);
				return null;
			}

			BlockCoordinates bc = new BlockCoordinates();
			try {
				bc.setX(Integer.parseInt(arr[0]));
				bc.setY(Integer.parseInt(arr[1]));
				bc.setZ(Integer.parseInt(arr[2]));
			} catch (Exception e) {
				return null;
			}

			return bc;
		} catch (Exception e) {
			return null;
		}
	}

	public void MatchStarted() {
		int iFighters = 0;

		for (PlayerInstance inst : Players.values()) {
			if (inst.getPlayerStatus() == PlayerStatus.INARENA) {
				iFighters++;
			}
		}

		if (iFighters < 2) {
			Bukkit.broadcastMessage(ChatColor.RED + "§lThere are not enough players in the arena. Resetting.");
			Bukkit.getServer().reload();
			return;
		}

		Bukkit.broadcastMessage("§2§lThe HungerGames have started!");
		Bukkit.broadcastMessage("§e§lPVP will be activated in 90 seconds! Be prepared.");
		fMatchStarted = true;
		new TimingTask(TimingThreadJob.ACTIVATE_PVP, this).runTaskLater(this, 1800);
	}

	public void ActivatePVP() {
		overworld.setPVP(true);
		overworld.setDifficulty(Difficulty.NORMAL);
		overworld.setTime(0);
		Bukkit.broadcastMessage("§4§lPVP Activated: Let the slaughter begin!");
	}

	private void startSpectating(Player plr) {
		plr.sendMessage(chatSuccess + "Spectator mode activated");
		hidePlayer(plr);
		plr.setAllowFlight(true);
		teleportToBlockCoordinates(plr, HgConf.getArenaX(), HgConf.getArenaY() + 1, HgConf.getArenaZ());
	}

	private void stopSpectating(Player plr) {
		showPlayer(plr);
		plr.setAllowFlight(false);
	}

	private void hidePlayer(Player plr) {
		for (Player pl : Bukkit.getOnlinePlayers()) {
			if (pl.equals(plr)) {
				continue;
			}
			if (pl.isOp()) {
				continue;
			}
			pl.hidePlayer(plr);
		}
	}

	private void showPlayer(Player plr) {
		for (Player pl : Bukkit.getOnlinePlayers()) {
			if (pl.equals(plr)) {
				continue;
			}
			pl.showPlayer(plr);
		}
	}

	private void commandSpec(Player player) {
		if (player.isOp()) {
			player.sendMessage(chatWarning + "Admins are automaticly spectating.");
			return;
		}
		if (Players.get(player.getName()).getPlayerStatus() == PlayerStatus.INARENA) {
			player.sendMessage(chatWarning + "If you want to leave use /hg leave first!");
			return;
		}

		player.sendMessage(chatSuccess + "You are spectating now!");

		Players.get(player.getName()).setPlayerStatus(PlayerStatus.SPECTATOR);
		Players.get(player.getName()).setSlotCoordinates(null);
		if (fMatchStarted) {
			startSpectating(player);
		}
		TagAPI.refreshPlayer(player);
	}

	private void commandSetLobby(Player player) {
		if (!player.isOp()) {
			player.sendMessage(chatError + "You are not allowed to use this command.");
			return;
		}

		HgConf.setLoobySpawnX(player.getLocation().getBlockX());
		HgConf.setLoobySpawnY(player.getLocation().getBlockY());
		HgConf.setLoobySpawnZ(player.getLocation().getBlockZ());

		player.sendMessage(chatSuccess + "Set lobby spawn to X:" + player.getLocation().getBlockX() + " Y:" + player.getLocation().getBlockY()
				+ " Z:" + player.getLocation().getBlockZ());
	}

	private void commandLeave(Player player) {
		if (player.isOp()) {
			player.sendMessage(chatWarning + "Sorry Admins can not use this command");
			return;
		}

		if (Players.get(player.getName()).getPlayerStatus() == PlayerStatus.INLOBBY) {
			player.sendMessage(chatWarning + "You are neither spectating nor in a match.");
			if (fMatchStarted) {
				player.sendMessage(chatSuccess + "Use /hg spec to spectate.");
			} else {
				player.sendMessage(chatSuccess + "Use /hg spec to spectate or /hg join to join the arena");
			}
			return;
		}

		if (Players.get(player.getName()).getPlayerStatus() == PlayerStatus.SPECTATOR) {
			player.sendMessage(chatSuccess + "You left spectator.");

			if (fMatchStarted) {
				player.sendMessage(chatSuccess + "Use /hg spec to spectate again.");
			} else {
				player.sendMessage(chatSuccess + "Use /hg spec to spectate or /hg join to join the arena");
			}
			stopSpectating(player);
		}

		if (Players.get(player.getName()).getPlayerStatus() == PlayerStatus.INARENA) {
			player.sendMessage(chatSuccess + "You left the Arena.");

			if (fMatchStarted || fCountdown) {
				Bukkit.broadcastMessage("§4Player §l" + player.getName() + "§r§4 forfeit");
				player.sendMessage(chatSuccess + "Use /hg spec to spectate.");
			} else {
				Bukkit.broadcastMessage("§6" + player.getName() + " left the Arena");
				player.sendMessage(chatSuccess + "Use /hg spec to spectate or /hg join to join the arena");
			}
		}

		teleportToBlockCoordinates(player, HgConf.getLoobySpawnX(), HgConf.getLoobySpawnY() + 2, HgConf.getLoobySpawnZ());
		Players.get(player.getName()).setPlayerStatus(PlayerStatus.INLOBBY);
		Players.get(player.getName()).setSlotCoordinates(null);
		clearPlayerInv(player);
		TagAPI.refreshPlayer(player);
	}

	private void commandStart(Player player) {
		if (!player.isOp()) {
			player.sendMessage(chatError + "You are not allowed to use this command.");
			return;
		}
		int iAlive = 0;
		for (String name : Players.keySet()) {
			if (Players.get(name).getPlayerStatus() == PlayerStatus.INARENA) {
				iAlive++;
			}
		}
		if (iAlive == 0) {
			player.sendMessage(chatError + "There is nobody in the arena");
			return;
		}
		if (iAlive < 2) {
			player.sendMessage(chatError + "There is a minimum of 2 players!");
			return;
		}

		new TimingTask(TimingThreadJob.COUNTDOWN, this).runTaskLater(this, 1);
		fCountdown = true;

		prepareStart(true);
	}

	private void prepareStart(boolean fClear) {
		for (String name : Players.keySet()) {
			Player plr = Bukkit.getPlayer(name);
			if (plr.isOp()) {
				continue;
			}
			plr.setGameMode(GameMode.ADVENTURE);

			if (fClear) {
				clearPlayerInv(plr);
			}

			healAndClearPlayer(plr);

			if (Players.get(name).getPlayerStatus() == PlayerStatus.INLOBBY) {
				Players.get(name).setPlayerStatus(PlayerStatus.SPECTATOR);
				plr.sendMessage(chatWarning + "You did not join the HungerGames. You are spectating now.");
				plr.sendMessage(chatWarning + "Type /hg leave to return to the lobby");
			}

			if (Players.get(name).getPlayerStatus() == PlayerStatus.SPECTATOR) {
				plr.sendMessage(chatSuccess + "Click to switch between fighters.");
				startSpectating(plr);
				teleportToBlockCoordinates(plr, HgConf.getArenaX(), HgConf.getArenaY(), HgConf.getArenaZ());
			}
			TagAPI.refreshPlayer(plr);
		}
	}

	private void commandAddSlot(Player player) {
		if (!player.isOp()) {
			player.sendMessage(chatError + "You are not allowed to use this command.");
			return;
		}

		// Check if already existing
		for (String entry : HgConf.getSlots().values()) {
			BlockCoordinates loc = extractLocation(entry);
			if (loc == null) {
				continue;
			}

			if (loc.getX() == player.getLocation().getBlockX() && loc.getY() == player.getLocation().getBlockY()
					&& loc.getZ() == player.getLocation().getBlockZ()) {
				player.sendMessage(chatError + "Slot is already registered");
				return;
			}
		}

		HgConf.getSlots().put(HgConf.getSlots().size() + 1,
				player.getLocation().getBlockX() + ";" + player.getLocation().getBlockY() + ";" + player.getLocation().getBlockZ());

		player.sendMessage(chatSuccess + "Added Arena Slot X:" + player.getLocation().getBlockX() + " Y:" + player.getLocation().getBlockY() + " Z:"
				+ player.getLocation().getBlockZ() + " with ID #" + HgConf.getSlots().size());
	}

	private void commandArenaRadius(Player player, String[] args) {
		if (!player.isOp()) {
			player.sendMessage(chatError + "You are not allowed to use this command.");
			return;
		}

		if (args.length < 2) {
			if (!fArenaCoordsSet) {
				player.sendMessage(chatError + "Please define the middle of the arena by /hg arenamiddle");
				return;
			}

			HgConf.setArenaRadius(getDistance(player.getLocation().getBlockX(), player.getLocation().getBlockZ(), HgConf.getArenaX(),
					HgConf.getArenaZ()));
			player.sendMessage(chatSuccess + "Radius set to " + HgConf.getArenaRadius());
		} else {
			try {
				HgConf.setArenaRadius(Integer.parseInt(args[1]));
			} catch (Exception e) {
				player.sendMessage(chatError + "Not a number");
				return;
			}
			player.sendMessage(chatSuccess + "Radius set to " + HgConf.getArenaRadius());
		}
		fArenaRadiusSet = true;
	}

	private void commandScanChests(Player player) {
		if (!fArenaCoordsSet) {
			player.sendMessage(chatError + "Please define the middle of the arena by /hg arenamiddle");
			return;
		}
		if (!fArenaRadiusSet) {
			player.sendMessage(chatError + "Please define the radius of the arena by /hg areanaradius");
			return;
		}
		if (!player.isOp()) {
			player.sendMessage(chatError + "You are not allowed to use this command.");
			return;
		}

		Bukkit.broadcastMessage(chatWarning + "Scanning map! This will cause server lag for a moment.");
		HgConf.getChests().clear();

		player.sendMessage(chatWarning + "Scanning map from X:" + HgConf.getArenaX() + " Y:" + HgConf.getArenaY() + " Z:" + HgConf.getArenaZ()
				+ " with a radius of " + HgConf.getArenaRadius());
		int scannedchests = scanChests(HgConf.getArenaX(), HgConf.getArenaZ(), HgConf.getArenaRadius());

		getLogger().info("Found " + scannedchests + " chest(s)!");
		Bukkit.broadcastMessage(chatSuccess + "Done.");
	}

	private void commandArenaMiddle(Player player) {
		if (!player.isOp()) {
			player.sendMessage(chatError + "You are not allowed to use this command.");
			return;
		}
		HgConf.setArenaX(player.getLocation().getBlockX());
		HgConf.setArenaY(player.getLocation().getBlockY());
		HgConf.setArenaZ(player.getLocation().getBlockZ());
		fArenaCoordsSet = true;

		player.sendMessage(chatSuccess + "Middle successful set to X:" + HgConf.getArenaX() + " Y:" + HgConf.getArenaY() + " Z:" + HgConf.getArenaZ());
	}

	private void commandJoin(Player player) {
		if (player.isOp()) {
			player.sendMessage(chatWarning + "Sorry Admins can not join the HungerGames");
			return;
		}

		if (fMatchStarted || fCountdown) {
			player.sendMessage(chatWarning + "The match already started. Use /hg spec to spectate");
			return;
		}

		if (Players.get(player.getName()).getPlayerStatus() == PlayerStatus.INARENA) {
			player.sendMessage(chatError + "You already joined the HungerGames!");
			return;
		}

		Vector<BlockCoordinates> vFreeSlots = new Vector<BlockCoordinates>();

		for (String str : HgConf.getSlots().values()) {
			boolean fOccupied = false;
			BlockCoordinates blc = extractLocation(str);
			if (blc == null) {
				getLogger().info("Corrupted slot coordinates!");
				continue;
			}

			for (PlayerInstance playerentry : Players.values()) {
				if (playerentry.getSlotCoordinates() == null) {
					continue;
				}
				if (playerentry.getSlotCoordinates().equals(blc)) {
					fOccupied = true;
					break;
				} else {
					continue;
				}
			}
			if (fOccupied) {
				continue;
			}
			vFreeSlots.add(blc); // Create list of free slots
		}

		if (vFreeSlots.size() == 0) {
			player.sendMessage(chatError + "Sorry there is no slot free for you!");
			return;
		}

		// Choose a random slot
		BlockCoordinates blc = vFreeSlots.get(rnd.nextInt(vFreeSlots.size()));

		PlayerInstance instance = Players.get(player.getName());
		instance.setSlotCoordinates(blc);
		instance.setPlayerStatus(PlayerStatus.INARENA);
		Bukkit.broadcastMessage("§6" + player.getName() + " joined the Arena");

		Location loc = new Location(overworld, (double) instance.getSlotCoordinates().getX() + 0.5, (double) instance.getSlotCoordinates().getY(),
				(double) instance.getSlotCoordinates().getZ() + 0.5);

		player.teleport(loc);
		healAndClearPlayer(player);
		clearPlayerInv(player);

		player.sendMessage(chatError + "The match has not started yet. Please wait for an admin to start");

		TagAPI.refreshPlayer(player);

	}

	private void teleportToBlockCoordinates(Player player, int x, int y, int z) {
		player.teleport(new Location(overworld, (double) x, (double) y, (double) z));
	}

	public boolean isMatchStarted() {
		return fMatchStarted;
	}

	public World getArenaWorld() {
		return overworld;
	}

	public void refillChests() {
		Vector<Pair<Material, Integer>> LootPercentages = new Vector<Pair<Material, Integer>>();
		Vector<Pair<Material, Integer>> LootPercentagesLow = new Vector<Pair<Material, Integer>>();
		int iMaxChestItems = 5;

		// weapons
		LootPercentages.add(new Pair<Material, Integer>(Material.WOOD_SWORD, 40));
		LootPercentages.add(new Pair<Material, Integer>(Material.IRON_SWORD, 20));
		LootPercentages.add(new Pair<Material, Integer>(Material.STONE_SWORD, 30));
		LootPercentages.add(new Pair<Material, Integer>(Material.DIAMOND_SWORD, 5));
		LootPercentages.add(new Pair<Material, Integer>(Material.BOW, 10)); // +
																			// arrows
																			// 5-30
		LootPercentages.add(new Pair<Material, Integer>(Material.ARROW, 30));
		LootPercentages.add(new Pair<Material, Integer>(Material.FLINT_AND_STEEL, 10));

		// amor
		LootPercentages.add(new Pair<Material, Integer>(Material.LEATHER_HELMET, 20));
		LootPercentages.add(new Pair<Material, Integer>(Material.LEATHER_CHESTPLATE, 20));
		LootPercentages.add(new Pair<Material, Integer>(Material.LEATHER_LEGGINGS, 20));
		LootPercentages.add(new Pair<Material, Integer>(Material.LEATHER_BOOTS, 20));
		LootPercentages.add(new Pair<Material, Integer>(Material.IRON_HELMET, 10));
		LootPercentages.add(new Pair<Material, Integer>(Material.IRON_CHESTPLATE, 10));
		LootPercentages.add(new Pair<Material, Integer>(Material.IRON_LEGGINGS, 10));
		LootPercentages.add(new Pair<Material, Integer>(Material.IRON_LEGGINGS, 10));
		LootPercentages.add(new Pair<Material, Integer>(Material.DIAMOND_HELMET, 2));
		LootPercentages.add(new Pair<Material, Integer>(Material.DIAMOND_CHESTPLATE, 2));
		LootPercentages.add(new Pair<Material, Integer>(Material.DIAMOND_LEGGINGS, 2));
		LootPercentages.add(new Pair<Material, Integer>(Material.DIAMOND_BOOTS, 2));

		// food
		LootPercentages.add(new Pair<Material, Integer>(Material.GOLDEN_APPLE, 5));
		LootPercentages.add(new Pair<Material, Integer>(Material.BEDROCK, 3)); // gold.
																				// apple
		LootPercentages.add(new Pair<Material, Integer>(Material.PORK, 20));
		LootPercentages.add(new Pair<Material, Integer>(Material.GRILLED_PORK, 10));
		LootPercentages.add(new Pair<Material, Integer>(Material.RAW_BEEF, 20));
		LootPercentages.add(new Pair<Material, Integer>(Material.COOKED_BEEF, 10));
		LootPercentages.add(new Pair<Material, Integer>(Material.RAW_CHICKEN, 20));
		LootPercentages.add(new Pair<Material, Integer>(Material.COOKED_CHICKEN, 10));
		LootPercentages.add(new Pair<Material, Integer>(Material.APPLE, 20));

		LootPercentages.add(new Pair<Material, Integer>(Material.MUSHROOM_SOUP, 20));
		LootPercentages.add(new Pair<Material, Integer>(Material.BREAD, 20));
		LootPercentages.add(new Pair<Material, Integer>(Material.COAL, 10));

		LootPercentagesLow.add(new Pair<Material, Integer>(Material.WOOD_SWORD, 30));
		LootPercentagesLow.add(new Pair<Material, Integer>(Material.LEATHER_HELMET, 15));
		LootPercentagesLow.add(new Pair<Material, Integer>(Material.LEATHER_CHESTPLATE, 15));
		LootPercentagesLow.add(new Pair<Material, Integer>(Material.LEATHER_LEGGINGS, 15));
		LootPercentagesLow.add(new Pair<Material, Integer>(Material.LEATHER_BOOTS, 15));
		LootPercentagesLow.add(new Pair<Material, Integer>(Material.PORK, 40));
		LootPercentagesLow.add(new Pair<Material, Integer>(Material.GRILLED_PORK, 20));
		LootPercentagesLow.add(new Pair<Material, Integer>(Material.RAW_BEEF, 40));
		LootPercentagesLow.add(new Pair<Material, Integer>(Material.COOKED_BEEF, 20));
		LootPercentagesLow.add(new Pair<Material, Integer>(Material.RAW_CHICKEN, 40));
		LootPercentagesLow.add(new Pair<Material, Integer>(Material.COOKED_CHICKEN, 20));
		LootPercentagesLow.add(new Pair<Material, Integer>(Material.APPLE, 40));
		LootPercentagesLow.add(new Pair<Material, Integer>(Material.MUSHROOM_SOUP, 40));
		LootPercentagesLow.add(new Pair<Material, Integer>(Material.BREAD, 40));

		for (String chestCoordsStr : HgConf.getChests().values()) {
			BlockCoordinates chestCoord = extractLocation(chestCoordsStr);
			if (chestCoord == null) {
				continue;
			}

			Block blk = overworld.getBlockAt(chestCoord.getX(), chestCoord.getY(), chestCoord.getZ());

			if (blk.getType() != Material.CHEST) {
				getLogger()
						.info("Block at X:" + chestCoord.getX() + " Y:" + chestCoord.getY() + " Z:" + chestCoord.getZ()
								+ " is not a chest. Please rescan!");
				continue;
			}

			Vector<Pair<Material, Integer>> lootTable = null;
			BlockState bs = blk.getState();
			Chest chest = (Chest) bs;
			
			try {
				chest.getBlockInventory().clear();
			}catch(NullPointerException e) {
				getLogger().info("Skipping chest due error:" + chestCoordsStr);
				continue;
			}
			

			// Chest near arena middle
			if (getDistance(chestCoord.getX(), chestCoord.getZ(), HgConf.getArenaX(), HgConf.getArenaZ()) <= ArenaRadius) {
				lootTable = LootPercentagesLow;
			} else {
				lootTable = LootPercentages;
			}

			for (int itemCount = 0; itemCount <= iMaxChestItems; itemCount++) {
				int iSlot = rndWithoutZero(26);
				int iLootIndex = rnd.nextInt(lootTable.size() - 1);
				Inventory chstInv = chest.getBlockInventory();

				// Look for random slot
				while (iSlot == 0 || chstInv.getItem(iSlot) != null) {
					iSlot++;
					if (iSlot > 26) {
						iSlot = 0;
					}
				}

				// Insert Items
				while (chstInv.getItem(iSlot) == null) {
					if (rndPercentage(lootTable.get(iLootIndex).second)) {
						// Found item that can go into chest

						switch (lootTable.get(iLootIndex).first) {
						// Handle special items
						case BOW: // Bow ; Add arrows next to it;
							chstInv.setItem(iSlot, new ItemStack(Material.BOW, 1));
							chstInv.setItem(findNextEmptyInvSlot(chstInv, iSlot), new ItemStack(Material.ARROW, rndWithoutZero(20)));
							break;
						case BEDROCK:
							chstInv.setItem(iSlot, new ItemStack(Material.GOLDEN_APPLE, 1, (short) 1));
							break;
						case ARROW:
							chstInv.setItem(iSlot, new ItemStack(Material.ARROW, rndWithoutZero(10)));
							break;
						default:
							chstInv.setItem(iSlot, new ItemStack(lootTable.get(iLootIndex).first));
						}
					}
					iLootIndex++;
					if (iLootIndex > lootTable.size() - 1) {
						iLootIndex = 1;
					}
				}
			}

			chest.update();

		}

	}

	public void commandLobby(Player player) {
		if (!player.isOp()) {
			player.sendMessage(chatError + "You are not allowed to use this command.");
			return;
		}
		teleportToBlockCoordinates(player, HgConf.getLoobySpawnX(), HgConf.getLoobySpawnY() + 2, HgConf.getLoobySpawnZ());
	}

	public boolean rndPercentage(int percentage) {
		if (percentage == 0) {
			return false;
		}
		int number = rnd.nextInt(100);

		for (double i = 0.0; i <= 100.0; i += 100.0 / percentage) {
			if ((int) Math.round(i) == number) {
				return true;
			}
		}
		return false;
	}

	public int rndWithoutZero(int max) {
		if (max == 0) {
			max = 1;
		}
		int val = 0;

		while (val == 0) {
			val = rnd.nextInt(max);
		}
		return val;
	}

	public int findNextEmptyInvSlot(Inventory inv, int iSlot) {
		for (; iSlot < inv.getSize(); iSlot++) {
			if (inv.getItem(iSlot) == null) {
				return iSlot;
			}
		}
		return 0;
	}

	public void commandRefillChests(Player player) {
		if (!player.isOp()) {
			player.sendMessage(chatError + "You are not allowed to use this command.");
			return;
		}
		refillChests();
		Bukkit.broadcastMessage(chatSuccess + "All chests have been refilled!");
	}

	public void clearPlayerInv(Player plr) {
		plr.getInventory().clear();
		plr.getInventory().setArmorContents(null);
	}

	public void commandHelp(Player player) {
		player.sendMessage(chatSuccess + "Listing all available commands.");

		player.sendMessage("§3§l/hg join§r§2 Enters the arena");
		player.sendMessage("§3§l/hg spec§r§2 Lets you spectate the match");
		player.sendMessage("§3§l/hg leave§r§2 Removes you from spectator or arena");
		player.sendMessage("§3§l/hg alive§r§2 Shows all players who are currently alive");
		player.sendMessage("§3§l/hg help§r§2 Shows this");

		if (player.isOp()) {
			player.sendMessage(" ");
			player.sendMessage("§4§l Listing Admin commands (only visible to you)");

			player.sendMessage("§e§nMap setup");
			player.sendMessage("§4§l/hg setlobby§r§2 Sets the lobby coordinates (player spawn)");
			player.sendMessage("§4§l/hg arenamiddle§r§2 Sets the arena middle (needed for scanchests)");
			player.sendMessage("§4§l/hg arenaradius§r§2 Sets the radius of the arena (square, accepts second parameter or your position relative to arenamiddle)");
			player.sendMessage("§4§l/hg addslot§r§2 Adds a arena slot for players");

			player.sendMessage("§e§nLoot & Chests");
			player.sendMessage("§4§l/hg scanchests§r§2 Scans the selected part of the map for chests and empties them");
			player.sendMessage("§4§l/hg refillchests§r§2 Fills all scanned chests with random loot");

			player.sendMessage("§e§nGame");
			player.sendMessage("§4§l/hg start§r§2 Starts the match with a countdown");
			player.sendMessage("§4§l/reload§r§2 To restart the HungerGames (don't forget to refill chests)");
			player.sendMessage("§4§l/hg sd§r§2 Teleports all players to the middle of the arena");
			player.sendMessage("§4§l/hg schedulestart§r§2 Schedules the start of the match to given amount of minutes");

			player.sendMessage("§e§nTeleport");
			player.sendMessage("§4§l/hg lobby§r§2 Teleports you to the lobby spawn");
			player.sendMessage("§4§l/hg arena§r§2 Teleports you to the arena middle");
		}

	}

	public void commandSuddenDeath(Player player) {
		int iAlive = 0;
		for (String name : Players.keySet()) {
			if (Players.get(name).getPlayerStatus() == PlayerStatus.INARENA) {
				iAlive++;
			}
		}
		if (iAlive == 0) {
			player.sendMessage(chatError + "Nobody is alive in the arena.");
			return;
		}

		for (String name : Players.keySet()) {
			if (Players.get(name).getPlayerStatus() == PlayerStatus.INARENA) {
				Player plr = Bukkit.getPlayer(name);
				teleportToBlockCoordinates(plr, Players.get(name).getSlotCoordinates());
				healAndClearPlayer(plr);
			}
		}

		overworld.setDifficulty(Difficulty.PEACEFUL);
		Bukkit.broadcastMessage("§4§lSudden death! Fight for your glory!");
	}

	private void teleportToBlockCoordinates(Player plr, BlockCoordinates slotCoordinates) {
		teleportToBlockCoordinates(plr, slotCoordinates.getX(), slotCoordinates.getY(), slotCoordinates.getZ());
	}

	private void healAndClearPlayer(Player plr) {
		plr.setHealth(20.0);
		plr.setSaturation(5.0f);
		plr.setFoodLevel(20);
		plr.setFireTicks(0);
		Collection<PotionEffect> effects = plr.getActivePotionEffects();
		for (PotionEffect effect : effects) {
			plr.removePotionEffect(effect.getType());
		}
	}

	private void commandFirework(Player player) {
		if (!player.isOp()) {
			return;
		}
		new TimingTask(TimingThreadJob.FIREWORK, this).runTaskLater(this, 1);
	}

	private void jobCountdown() {
		if (Countdown == -1) {
			Countdown = 10;
		}

		if (Countdown > 1) {
			Bukkit.broadcastMessage("§o§6" + Countdown + " seconds remaining!");
			new TimingTask(TimingThreadJob.COUNTDOWN, this).runTaskLater(this, 20);
			Countdown--;
		} else {
			Bukkit.broadcastMessage("§o§41 second remaining!");
			Countdown = -1;
			MatchStarted();
		}
	}

	private void jobFirework() {
		Location loc = new Location(overworld, HgConf.getArenaX(), HgConf.getArenaY(), HgConf.getArenaZ());

		FireworkAngle += (Math.PI / 20);

		Location effectLocation = loc.clone();
		effectLocation.setX(effectLocation.getX() + Math.sin(FireworkAngle) * ArenaRadius);
		effectLocation.setZ(effectLocation.getZ() + Math.cos(FireworkAngle) * ArenaRadius);
		firework(overworld, effectLocation);

		effectLocation = loc.clone();
		effectLocation.setX(effectLocation.getX() + Math.sin(Math.PI * 0.666 + FireworkAngle) * ArenaRadius);
		effectLocation.setZ(effectLocation.getZ() + Math.cos(Math.PI * 0.666 + FireworkAngle) * ArenaRadius);
		firework(overworld, effectLocation);

		effectLocation = loc.clone();
		effectLocation.setX(effectLocation.getX() + Math.sin(Math.PI * 1.333 + FireworkAngle) * ArenaRadius);
		effectLocation.setZ(effectLocation.getZ() + Math.cos(Math.PI * 1.333 + FireworkAngle) * ArenaRadius);
		firework(overworld, effectLocation);

		if (FireworkAngle < 2 * Math.PI) {
			new TimingTask(TimingThreadJob.FIREWORK, this).runTaskLater(this, 3);
		} else {
			FireworkAngle = 0;
		}

		if (plrWinner != null) {
			Location plrLoc = plrWinner.getLocation().clone();
			plrLoc.setY(plrLoc.getY() + 2.0);

			overworld.dropItem(plrLoc, new ItemStack(Material.GOLD_INGOT, 1));
			overworld.dropItem(plrLoc, new ItemStack(Material.DIAMOND, 1));

		}

	}

	private void firework(World overworld, Location effectLocation) {
		Firework fw = (Firework) overworld.spawnEntity(effectLocation, EntityType.FIREWORK);
		FireworkMeta fws = fw.getFireworkMeta();

		switch (iFireworkEffect) {
		case 0:
			// fws.addEffect(FireworkEffect.builder().withTrail().withColor(Color.RED).with(Type.BALL_LARGE).build());
			fws.addEffect(FireworkEffect.builder().withTrail().withColor(Color.YELLOW).with(Type.STAR).withFlicker().build());
			break;
		case 1:
			fws.addEffect(FireworkEffect.builder().withTrail().withColor(Color.RED).with(Type.BALL_LARGE).build());
			break;
		case 2:
			fws.addEffect(FireworkEffect.builder().withTrail().withColor(Color.SILVER).with(Type.BURST).build());
			break;
		case 3:
			fws.addEffect(FireworkEffect.builder().withTrail().withColor(Color.ORANGE).with(Type.BALL).build());
			break;
		}
		iFireworkEffect++;
		if (iFireworkEffect > 3) {
			iFireworkEffect = 0;
		}

		fws.setPower(1);
		fw.setFireworkMeta(fws);
	}

	private void commandAlive(Player plr) {
		int iCount = 0;
		String sMsg = new String("");

		for (String key : Players.keySet()) {
			PlayerInstance inst = Players.get(key);
			if (inst.getPlayerStatus() == PlayerStatus.INARENA) {
				iCount++;
				sMsg = sMsg.concat(key + ",");
			}
		}

		if (iCount == 0) {
			plr.sendMessage(chatSuccess + "There are no Players alive.");
		} else {
			plr.sendMessage(chatSuccess + "There are " + iCount + " out of " + Players.size() + " Players alive.");
			plr.sendMessage("§9" + sMsg.substring(0, sMsg.length() - 1));
		}
	}

	private void commandArena(Player plr) {
		if (!plr.isOp()) {
			plr.sendMessage(chatError + "You are not allowed to use this command.");
			return;
		}
		teleportToBlockCoordinates(plr, HgConf.getArenaX(), HgConf.getArenaY(), HgConf.getArenaZ());
	}

	private void commandScheduleStart(Player plr, String[] Time) {
		if (!plr.isOp()) {
			plr.sendMessage(chatError + "You are not allowed to use this command.");
			return;
		}

		if (Time.length < 1) {
			plr.sendMessage(chatError + "No time given");
			return;
		}

		try {
			iTimeLeftTillStart = (double) Integer.parseInt(Time[1]);
		} catch (NumberFormatException e) {
			plr.sendMessage(chatError + "This is not a valid time");
			return;
		}

		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, (int) iTimeLeftTillStart);
		Bukkit.broadcastMessage("§a§lStart scheduled to " + cal.get(Calendar.HOUR_OF_DAY) + ":"
				+ ((new String(cal.get(Calendar.MINUTE) + "").length() == 1 ? "0" : "")) + cal.get(Calendar.MINUTE));
	}

	private void commandRemoveSlot(Player plr) {
		if (!plr.isOp()) {
			plr.sendMessage(chatError + "You are not allowed to use this command.");
			return;
		}

		for (Integer index : HgConf.getSlots().keySet()) {
			BlockCoordinates slot = extractLocation(HgConf.getSlots().get(index));
			if (slot == null) {
				getLogger().info("Corrupted slot coordinates!");
				plr.sendMessage(chatError + "Corrupted slot coordinates!");
				continue;
			}

			if (slot.getX() == plr.getLocation().getBlockX() && slot.getY() == plr.getLocation().getBlockY()
					&& slot.getZ() == plr.getLocation().getBlockZ()) {

				HgConf.getSlots().remove(index);
				plr.sendMessage(chatSuccess + "Successfully removed Slot #" + index);
				return;
			}
		}

		plr.sendMessage(chatError + "This slot is not registered. Use /hg addslot.");
	}

	private void commandDebug() {
		overworld.setPVP(true);
		overworld.setDifficulty(Difficulty.NORMAL);
		overworld.setTime(0);
		fMatchStarted = true;
		fCountdown = true;
		prepareStart(false);
		Bukkit.broadcastMessage(chatError + "Debug match start");
	}
}
