package de.bananaco.bananabackup;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The main JavaPlugin class for this simple plugin
 * @author codename_B
 */
public class BananaBackup extends JavaPlugin {
	/**
	 * The interval for this in hours - supports decimal points
	 */
	double interval;
	/**
	 * Should we backup all worlds?
	 */
	boolean allWorlds;
	/**
	 * Should we broadcast a message to the players in a world when it's being backed up?
	 * Static simply to avoid passing a reference to the original class when we don't need to.
	 */
	boolean broadcast = true;
	/**
	 * A list of worlds to backup if allWorlds == false
	 */
	List<String> backupWorlds;
	/**
	 * The number of ticks per hour, this will never change
	 */
	final double tph = 72000;
	
	private boolean plugins = true;
	public static String backupFile = "backups/";
	
	public static int intervalBetween = 100;

	public static Logger log;

	/**
	 * Just your average onDisable();
	 */
	public void onDisable() {
		// Cancel our scheduled task
		getServer().getScheduler().cancelTasks(this);
		// Print disabled message
		log.info("[BananaBackup] Disabled.");
	}

	/**
	 * Nothing special here except a scheduler task.
	 */
	public void onEnable() {
		log = Logger.getLogger("Minecraft");

		// Setup the configuration
		loadConfiguration();
		// Calculate the interval in ticks
		double ticks = tph * interval;
		// Schedule our task
		getServer().getScheduler().scheduleAsyncRepeatingTask(this, doChecks(),
				(long) ticks, (long) ticks);
		// Print enabled message
		log.info("[BananaBackup] Enabled. Backup interval " + interval + " hours.");
	}
	
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(sender instanceof Player) {
			if(!sender.isOp()) {
				sender.sendMessage("Nope.");
				return true;
			}
		}
		getServer().getScheduler().scheduleAsyncDelayedTask(this, doChecks(), 10);
		sender.sendMessage("Backup initiated.");
		return true;
	}
	
	/**
	 * Loads our configuration
	 */
	public void loadConfiguration() {
		// The default config.yml
		FileConfiguration c = this.getConfig();
		if (!new File(this.getDataFolder().toString() + "config.yml").isFile()) saveDefaultConfig();
		
		interval = c.getDouble("backup-interval-hours", 12.0);
		intervalBetween = c.getInt("interval-between", intervalBetween);
		allWorlds = c.getBoolean("backup-all-worlds", true);
		broadcast = c.getBoolean("broadcast-message", true);
		plugins  = c.getBoolean("backup-plugins", true);
		backupFile  = c.getString("backup-file","backups/");
	
		try {
			backupWorlds = c.getStringList("backupWorlds");
		} catch (NullPointerException e) {
			log.warning("[BananaBackup] Configuration failure while loading backupWorlds. Backups will only run in the first world on the server.");
		}
		backupWorlds = c.getStringList("backup-worlds");
		// This is just to make sure there is something in the config as an example
		if (backupWorlds.size() == 0)
			backupWorlds.add(getServer().getWorlds().get(0).getName());
	}
	/**
	 * A simple method to make getting our runnable neater in onEnable();
	 * @return the runnable
	 */
	public Runnable doChecks() {
		return new Runnable() {
			public void run() {
				BackupThread bt;
				// Should we let people know it's starting?
				if(broadcast)
					getServer().broadcastMessage(ChatColor.BLUE
								+ "[BananaBackup] Backup starting. Expect a little lag.");
				// Loop through the worlds
				for (World world : getServer().getWorlds())
					try {
						// Save before the backup
						world.save();
						world.setAutoSave(false);
						// Then backup
						bt = backupWorld(world, null);
						if(bt != null) {
						bt.start();
						while(bt.isAlive()) {
							
						}
						world.setAutoSave(true);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				// How about the plugins?
				if(plugins)
					try {
						bt = backupWorld(null, new File("plugins"));
						if(bt != null);
						bt.start();
						while(bt.isAlive()) {
							
						}
					} catch(Exception e) {
						e.printStackTrace();
					}
				// Should we let people know it's done?
				if(broadcast)
				getServer().broadcastMessage(ChatColor.BLUE
						+ "[BananaBackup] Backup complete.");
			}
		};
	}
	/**
	 * Initiates the backup thread, each backup runs in its own thread
	 * @param world
	 * @throws Exception
	 */
	public BackupThread backupWorld(World world, File file) throws Exception {
		if(world != null) {
		// If allWorlds == true
		if (allWorlds)
			return new BackupThread(new File(world.getName()));
		// Or if the world is in the backup list
		else if (!allWorlds && backupWorlds.contains(world.getName()))
			return new BackupThread(new File(world.getName()));
		// Otherwise print an error message
		else
			log.warning("[BananaBackup] Skipping backup for " + world.getName());
		return null;
		} else if(world == null && file != null) {
			return new BackupThread(file);
		}
		return null;
	}
	/**
	 * Used to format the date for the filename
	 * @return String date
	 */
	public static String format() {
		SimpleDateFormat formatter;
		Date date = new Date();
		formatter = new SimpleDateFormat("HH.mm@dd.MM.yyyy");
		return formatter.format(date);
	}
}
