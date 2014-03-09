package com.greatmancode.deathmatch;

import me.ampayne2.ultimategames.api.UltimateGames;
import me.ampayne2.ultimategames.api.arenas.Arena;
import me.ampayne2.ultimategames.api.arenas.ArenaStatus;
import me.ampayne2.ultimategames.api.arenas.scoreboards.Scoreboard;
import me.ampayne2.ultimategames.api.arenas.spawnpoints.PlayerSpawnPoint;
import me.ampayne2.ultimategames.api.games.Game;
import me.ampayne2.ultimategames.api.games.GamePlugin;
import me.ampayne2.ultimategames.api.utils.UGUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.List;

public class Deathmatch extends GamePlugin {
    private UltimateGames ultimateGames;
    private Game game;

    @Override
    public boolean loadGame(UltimateGames ultimateGames, Game game) {
        this.ultimateGames = ultimateGames;
        this.game = game;
        game.setMessages(DMessage.KILL);
        return true;
    }

    @Override
    public void unloadGame() {

    }

    @Override
    public boolean reloadGame() {
        return true;
    }

    @Override
    public boolean stopGame() {
        return true;
    }

    @Override
    public boolean loadArena(Arena arena) {
        ultimateGames.addAPIHandler("/" + game.getName() + "/" + arena.getName(), new DeathmatchWebHandler(ultimateGames, arena));
        return true;
    }

    @Override
    public boolean unloadArena(Arena arena) {
        return true;
    }

    @Override
    public boolean isStartPossible(Arena arena) {
        return arena.getStatus() == ArenaStatus.OPEN;
    }

    @Override
    public boolean startArena(Arena arena) {
        return true;
    }

    @Override
    public boolean beginArena(Arena arena) {
        ultimateGames.getCountdownManager().createEndingCountdown(arena, ultimateGames.getConfigManager().getGameConfig(game).getInt("CustomValues.GameTime"), true);

        Scoreboard scoreBoard = ultimateGames.getScoreboardManager().createScoreboard(arena, "Kills");
        for (String playerName : arena.getPlayers()) {
            scoreBoard.addPlayer(Bukkit.getPlayerExact(playerName));
            scoreBoard.setScore(playerName, 0);
        }
        scoreBoard.setVisible(true);

        return true;
    }

    @Override
    public void endArena(Arena arena) {
        String highestScorer = null;
        Integer highScore = 0;
        List<String> players = arena.getPlayers();
        Scoreboard scoreBoard = ultimateGames.getScoreboardManager().getScoreboard(arena);
        if (scoreBoard != null) {
            for (String playerName : players) {
                Integer playerScore = scoreBoard.getScore(playerName);
                if (playerScore > highScore) {
                    highestScorer = playerName;
                    highScore = playerScore;
                }
            }
        }
        if (highestScorer != null) {
            ultimateGames.getMessenger().sendGameMessage(Bukkit.getServer(), game, DMessage.GAME_END, highestScorer, game.getName(), arena.getName());
            ultimateGames.getPointManager().addPoint(game, highestScorer, "store", 25);
            ultimateGames.getPointManager().addPoint(game, highestScorer, "win", 1);
        }
    }

    @Override
    public boolean openArena(Arena arena) {
        return true;
    }

    @Override
    public boolean stopArena(Arena arena) {
        return true;
    }

    @Override
    public boolean addPlayer(Player player, Arena arena) {
        if (arena.getStatus() == ArenaStatus.OPEN && arena.getPlayers().size() >= arena.getMinPlayers() && !ultimateGames.getCountdownManager().hasStartingCountdown(arena)) {
            ultimateGames.getCountdownManager().createStartingCountdown(arena, ultimateGames.getConfigManager().getGameConfig(game).getInt("CustomValues.StartWaitTime"));
        }
        PlayerSpawnPoint spawnPoint = ultimateGames.getSpawnpointManager().getRandomSpawnPoint(arena);
        spawnPoint.lock(false);
        spawnPoint.teleportPlayer(player);
        for (PotionEffect potionEffect : player.getActivePotionEffects()) {
            player.removePotionEffect(potionEffect.getType());
        }
        player.setHealth(20.0);
        player.setFoodLevel(20);
        resetInventory(player);
        return true;
    }

    @Override
    public void removePlayer(Player player, Arena arena) {
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean addSpectator(Player player, Arena arena) {
        ultimateGames.getSpawnpointManager().getSpectatorSpawnPoint(arena).teleportPlayer(player);
        for (PotionEffect potionEffect : player.getActivePotionEffects()) {
            player.removePotionEffect(potionEffect.getType());
        }
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.getInventory().clear();
        player.getInventory().addItem(UGUtils.createInstructionBook(game));
        player.getInventory().setArmorContents(null);
        player.updateInventory();
        return true;
    }

    @Override
    public void removeSpectator(Player player, Arena arena) {
    }

    @Override
    public void onPlayerDeath(Arena arena, PlayerDeathEvent event) {
        if (arena.getStatus() == ArenaStatus.RUNNING) {
            String playerName = event.getEntity().getName();
            Player killer = event.getEntity().getKiller();
            if (killer != null) {
                String killerName = killer.getName();
                Scoreboard scoreBoard = ultimateGames.getScoreboardManager().getScoreboard(arena);
                if (scoreBoard != null) {
                    scoreBoard.setScore(killerName, scoreBoard.getScore(killerName) + 1);
                }
                ultimateGames.getMessenger().sendGameMessage(arena, game, DMessage.KILL, killerName, event.getEntity().getName());
                ultimateGames.getPointManager().addPoint(game, killerName, "kill", 1);
                ultimateGames.getPointManager().addPoint(game, killerName, "store", 2);
            } else {
                Scoreboard scoreBoard = ultimateGames.getScoreboardManager().getScoreboard(arena);
                if (scoreBoard != null) {
                    scoreBoard.setScore(playerName, scoreBoard.getScore(playerName) - 1);
                }
                ultimateGames.getMessenger().sendGameMessage(arena, game, DMessage.DEATH, event.getEntity().getName());
            }
            ultimateGames.getPointManager().addPoint(game, event.getEntity().getName(), "death", 1);
        }
        event.getDrops().clear();
        UGUtils.autoRespawn(ultimateGames.getPlugin(), event.getEntity());
    }

    @Override
    public void onPlayerRespawn(Arena arena, PlayerRespawnEvent event) {
        event.setRespawnLocation(ultimateGames.getSpawnpointManager().getRandomSpawnPoint(arena).getLocation());
        resetInventory(event.getPlayer());
    }

    @Override
    public void onEntityDamage(Arena arena, EntityDamageEvent event) {
        if (arena.getStatus() != ArenaStatus.RUNNING) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onEntityDamageByEntity(Arena arena, EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            Entity damager = event.getDamager();
            if (damager instanceof Arrow) {
                LivingEntity entity = ((Arrow) damager).getShooter();
                if (entity instanceof Player) {
                    String playerName = ((Player) entity).getName();
                    if (ultimateGames.getPlayerManager().isPlayerInArena(playerName) && ultimateGames.getPlayerManager().getPlayerArena(playerName).getStatus() != ArenaStatus.RUNNING) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @Override
    public void onPlayerFoodLevelChange(Arena arena, FoodLevelChangeEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onItemPickup(Arena arena, PlayerPickupItemEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onItemDrop(Arena arena, PlayerDropItemEvent event) {
        event.setCancelled(true);
    }

    @SuppressWarnings("deprecation")
    private void resetInventory(Player player) {
        player.getInventory().clear();
        player.getInventory().addItem(new ItemStack(Material.IRON_SWORD, 1), new ItemStack(Material.BOW, 1), new ItemStack(Material.ARROW, 32), UGUtils.createInstructionBook(game));
        player.getInventory().setArmorContents(new ItemStack[]{new ItemStack(Material.LEATHER_BOOTS, 1), new ItemStack(Material.LEATHER_LEGGINGS, 1), new ItemStack(Material.LEATHER_CHESTPLATE, 1), new ItemStack(Material.LEATHER_HELMET, 1)});
        player.updateInventory();
    }
}
