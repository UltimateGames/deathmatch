package com.greatmancode.deathmatch;

import java.util.HashMap;

import me.ampayne2.UltimateGames.API.GamePlugin;
import me.ampayne2.UltimateGames.Arenas.Arena;
import me.ampayne2.UltimateGames.Enums.ArenaStatus;
import me.ampayne2.UltimateGames.Games.Game;
import me.ampayne2.UltimateGames.Players.SpawnPoint;
import me.ampayne2.UltimateGames.UltimateGames;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

public class Deathmatch extends GamePlugin {

	private UltimateGames ultimateGames;
	private Game game;
	private HashMap<Arena, ArenaScoreboard> arenaScoreboard = new HashMap<Arena, ArenaScoreboard>();

	@Override
	public Boolean loadGame(UltimateGames ultimateGames, Game game) {
		this.ultimateGames = ultimateGames;
		this.game = game;
		return true;
	}

	@Override
	public Boolean unloadGame() {
		return true;
	}

	@Override
	public Boolean stopGame() {
		return true;
	}

	@Override
	public Boolean loadArena(Arena arena) {
		arenaScoreboard.put(arena, new ArenaScoreboard());
		return true;
	}

	@Override
	public Boolean unloadArena(Arena arena) {
		arenaScoreboard.remove(arena);
		return true;
	}

	@Override
	public Boolean isStartPossible(Arena arena) {
		if (arena.getStatus() == ArenaStatus.OPEN) {
			return true;
		}
		return false;
	}

	@Override
	public Boolean startArena(Arena arena) {
		return true;
	}

	@Override
	public Boolean beginArena(Arena arena) {
		ultimateGames.getCountdownManager().createEndingCountdown(arena, 30, true);
		return true;
	}

	@Override
	public Boolean endArena(Arena arena) {
		arenaScoreboard.get(arena).reset();
		if (ultimateGames.getCountdownManager().isStartingCountdownEnabled(arena)) {
			ultimateGames.getCountdownManager().stopStartingCountdown(arena);
		}
		if (ultimateGames.getCountdownManager().isEndingCountdownEnabled(arena)) {
			ultimateGames.getCountdownManager().stopEndingCountdown(arena);
		}
		arena.setStatus(ArenaStatus.OPEN);
		for (String p : arena.getPlayers()) {
			ultimateGames.getPlayerManager().removePlayerFromArena(p, arena, false);
		}
		ultimateGames.getUGSignManager().updateLobbySignsOfArena(arena);
		return true;
	}

	@Override
	public Boolean resetArena(Arena arena) {
		return true;
	}

	@Override
	public Boolean openArena(Arena arena) {
		return true;
	}

	@Override
	public Boolean stopArena(Arena arena) {
		return true;
	}

	@Override
	public Boolean addPlayer(Arena arena, String playerName) {
		if (arena.getPlayers().size() >= arena.getMinPlayers() && !ultimateGames.getCountdownManager().isStartingCountdownEnabled(arena)) {
			ultimateGames.getCountdownManager().createStartingCountdown(arena, 30);
		}
		SpawnPoint spawnPoint = ultimateGames.getSpawnpointManager().getRandomSpawnPoint(arena);
		spawnPoint.lock(false);
		spawnPoint.teleportPlayer(playerName);
		Player player = Bukkit.getPlayer(playerName);
		resetInventory(player);
		arenaScoreboard.get(arena).addPlayer(player);
		player.setScoreboard(arenaScoreboard.get(arena).scoreboard);
		return true;
	}

	@Override
	public Boolean removePlayer(Arena arena, String playerName) {
		if (ultimateGames.getCountdownManager().isStartingCountdownEnabled(arena) && arena.getPlayers().size() <= arena.getMinPlayers()) {
			ultimateGames.getCountdownManager().stopStartingCountdown(arena);
		}
		arenaScoreboard.get(arena).removePlayer(Bukkit.getPlayer(playerName));
		Bukkit.getPlayer(playerName).setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
		return true;
	}

	@Override
	public void onGameCommand(String s, CommandSender commandSender, String[] strings) {

	}

	@Override
	public void onArenaCommand(Arena arena, String s, CommandSender commandSender, String[] strings) {

	}

	@Override
	public void handleInputSignCreate(Arena arena, Sign sign, String s) {

	}

	@Override
	public void handleInputSignClick(Arena arena, Sign sign, String s, PlayerInteractEvent playerInteractEvent) {

	}

	private void resetInventory(Player player) {
		player.getInventory().clear();
		player.getInventory().addItem(new ItemStack(Material.DIAMOND_SWORD, 1));
		player.updateInventory();
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerDamageByPlayer(EntityDamageByEntityEvent event) {
		if (!(event.getEntity() instanceof Player)) {
			return;
		}

		Player damaged = (Player) event.getEntity();


		Entity damager = event.getDamager();
		if (ultimateGames.getPlayerManager().isPlayerInArena(damaged.getName())) {
			Arena damagedArena = ultimateGames.getPlayerManager().getPlayerArena(damaged.getName());
			if (!damagedArena.getGame().equals(game)) {
				return;
			}

			if (damagedArena.getStatus() != ArenaStatus.RUNNING) {
				event.setCancelled(true);
				return;
			}

			if (!(damager instanceof Player)) {
				event.setCancelled(true);
				return;
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerDeath(PlayerDeathEvent event) {
		Player killed = event.getEntity();
		if (ultimateGames.getPlayerManager().isPlayerInArena(((Player) event.getEntity()).getName())) {
			Arena damagedArena = ultimateGames.getPlayerManager().getPlayerArena(killed.getName());
			if (!damagedArena.getGame().equals(game)) {
				return;
			}

			Player killer = event.getEntity().getKiller();

			if (killer != null) {
				if (ultimateGames.getPlayerManager().isPlayerInArena(killer.getName()) && ultimateGames.getPlayerManager().getPlayerArena(killer.getName()).equals(damagedArena)) {
					//playerList.get(damagedArena).get(killer.getName()).addKill();
				}
			}
		}
		event.getDrops().clear();

	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		if (ultimateGames.getPlayerManager().isPlayerInArena(event.getPlayer().getName())) {
			Arena damagedArena = ultimateGames.getPlayerManager().getPlayerArena(event.getPlayer().getName());
			if (!damagedArena.getGame().equals(game)) {
				return;
			}
			event.setRespawnLocation(ultimateGames.getSpawnpointManager().getRandomSpawnPoint(damagedArena).getLocation());
			resetInventory(event.getPlayer());
		}
	}
}
