package com.gabrielsimmer.swearjar;

import java.util.Arrays;
import java.util.List;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener{

	public void loadConfiguration(){
		//Config defaults
		this.getConfig().addDefault("price", 50);
		List<String> defaultSwears = Arrays.asList("crap");
		List<String> defaultMuted = Arrays.asList("noone");//If a player named noone joins... well crap :p
		this.getConfig().addDefault("swears.words.list", defaultSwears);
		this.getConfig().addDefault("players.muted.list", defaultMuted);

		getConfig().options().copyDefaults(true);
		saveConfig();
	}

	public static Economy economy = null;

	private boolean setupEconomy()
	{
		RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
		if (economyProvider != null) {
			economy = economyProvider.getProvider();
		}

		return (economy != null);
	}

	List<String> swears;
	List<String> mutedPlayers;
	List<String> newMutedPlayers;
	String prefix = "§4[§cSwearJar§4]§c "; //I feel dirty now

	@Override
	public void onEnable(){
		loadConfiguration();
		getServer().getPluginManager().registerEvents(this, this);
		getLogger().info("SwearJar Enabled!"); //Just because
		if (!setupEconomy() ) { //Disable if Vault is not found
			Bukkit.getLogger().warning(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
	}
	@Override
	public void onDisable(){
		getLogger().info("SwearJar Disabled."); //Just because
	}

	//Now to check chat messages!
	//Let's break this down
	@EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void chat(AsyncPlayerChatEvent event){ //Sets the event to the highest priority
		swears = this.getConfig().getStringList("swears.words.list"); //Loads swear words from the config
		mutedPlayers = this.getConfig().getStringList("players.muted.list");

		final Player player = event.getPlayer(); //Set the player
		final String message = event.getMessage(); //Get the message

		if (mutedPlayers.contains(player.getDisplayName())){
			event.setCancelled(true);
			player.sendMessage(prefix + "You're muted! Use /swear pay to pay up bub.");
		}

		for(String word : swears){ //For each word in the swear words array

			if(message.matches("(.* )?"+word+"( .*)?")) { //fk this line in particular

				event.setCancelled(true); //Cancel the message

				player.sendMessage(prefix + "You swore! You are now muted until you do /swear pay..."); //Send the player a message
				
				Bukkit.broadcastMessage(prefix + player.getDisplayName() + " has to put money in the swear jar!"); //Shame the player ;p

				//Add player to the muted list
				List<String> list = mutedPlayers;
				list.add(player.getDisplayName()); //<---- FIND UUID YOU DUMMY
				this.getConfig().set("players.muted.list", list);
				saveConfig();

			} else {

				//Do nothing, no bad words were said
			}

		}

	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){

		if  (cmd.getName().equalsIgnoreCase("swear") && sender instanceof Player){

			Player player = (Player) sender;

			if (args.length == 0){
				//Basic help
				player.sendMessage(prefix + "SwearJar Help:");
				player.sendMessage(prefix + "/swear pay - tip the swear jar.");

			}
			else if (args[0].equalsIgnoreCase("pay")){

				double payment = this.getConfig().getDouble("price"); //Doubles! Woo!

				if (economy.getBalance(player) >= payment){ //Get their balance, check if they have enough money

					EconomyResponse r = economy.withdrawPlayer(player, payment);

					if (r.transactionSuccess()){

						player.sendMessage(prefix + "You've been unmuted!");
						Bukkit.broadcastMessage(prefix + player.getDisplayName() + "§c has paid up!");
						
						//Remove the player from the muted list
						List<String> warnedPlayer = getConfig().getStringList("players.muted.list");
						warnedPlayer.remove(player.getDisplayName()); //<---- UUID!!!!!!!!1111!!!!1!one!!!
						getConfig().set("players.muted.list", Arrays.asList(warnedPlayer));
						saveConfig();

					}
					else{
						//In case something fails
						player.sendMessage(prefix + "Something went wrong! Please report the error to an admin.");
					}
				}

				else {
					//If they don't have enough money
					player.sendMessage(prefix + "You don't have enough money! \nSell some things to earn money.");
				}

			}
		}

		return false;
	}
}
