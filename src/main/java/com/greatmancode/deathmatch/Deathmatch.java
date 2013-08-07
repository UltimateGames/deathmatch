package com.greatmancode.deathmatch;

import java.util.ArrayList;
import java.util.List;

import me.ampayne2.ultimategames.UltimateGames;
import me.ampayne2.ultimategames.api.ArenaScoreboard;
import me.ampayne2.ultimategames.api.GamePlugin;
import me.ampayne2.ultimategames.arenas.Arena;
import me.ampayne2.ultimategames.enums.ArenaStatus;
import me.ampayne2.ultimategames.games.Game;
import me.ampayne2.ultimategames.players.SpawnPoint;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Deathmatch extends GamePlugin {

    private UltimateGames ultimateGames;
    private Game game;

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
        ultimateGames.getJettyServer().getHandler().addHandler("/Deathmatch/"+ arena.getName(), new DeathmatchWebHandler(ultimateGames, arena));
        return true;
    }

    @Override
    public Boolean unloadArena(Arena arena) {
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
        ultimateGames.getCountdownManager().createEndingCountdown(arena, ultimateGames.getConfigManager().getGameConfig(game).getConfig().getInt("CustomValues.GameTime"), true);
        for (ArenaScoreboard scoreBoard : new ArrayList<ArenaScoreboard>(ultimateGames.getScoreboardManager().getArenaScoreboards(arena))) {
            ultimateGames.getScoreboardManager().removeArenaScoreboard(arena, scoreBoard.getName());
        }
        ArenaScoreboard scoreBoard = ultimateGames.getScoreboardManager().createArenaScoreboard(arena, "Kills");
        for (String playerName : arena.getPlayers()) {
            scoreBoard.addPlayer(playerName);
            scoreBoard.setScore(playerName, 0);
        }
        scoreBoard.setVisible(true);
        return true;
    }

    @Override
    public Boolean endArena(Arena arena) {
        String highestScorer = "Unknown";
        Integer highScore = 0;
        List<String> players = arena.getPlayers();
        for (ArenaScoreboard scoreBoard : ultimateGames.getScoreboardManager().getArenaScoreboards(arena)) {
            if (scoreBoard.getName().equals("Kills")) {
                for (String playerName : players) {
                    Integer playerScore = scoreBoard.getScore(playerName);
                    if (playerScore > highScore) {
                        highestScorer = playerName;
                        highScore = playerScore;
                    }
                }
            }
        }
        ultimateGames.getScoreboardManager().removeArenaScoreboard(arena, "Kills");
        ultimateGames.getMessageManager().broadcastReplacedGameMessage(game, "GameEnd", highestScorer, game.getGameDescription().getName(), arena.getName());
        for (String playerName : players) {
            ultimateGames.getPlayerManager().removePlayerFromArena(playerName, arena, false);
        }
        ultimateGames.getArenaManager().openArena(arena);
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
            ultimateGames.getCountdownManager().createStartingCountdown(arena, ultimateGames.getConfigManager().getGameConfig(game).getConfig().getInt("CustomValues.StartWaitTime"));
        }
        SpawnPoint spawnPoint = ultimateGames.getSpawnpointManager().getRandomSpawnPoint(arena);
        spawnPoint.lock(false);
        spawnPoint.teleportPlayer(playerName);
        Player player = Bukkit.getPlayer(playerName);
        resetInventory(player);
        return true;
    }

    @Override
    public Boolean removePlayer(Arena arena, String playerName) {
        return true;
    }

    @Override
    public void onPlayerDeath(Arena arena, PlayerDeathEvent event) {
        String killerName = null;
        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            killerName = killer.getName();
            if (ultimateGames.getPlayerManager().isPlayerInArena(killer.getName()) && ultimateGames.getPlayerManager().getPlayerArena(killer.getName()).equals(arena)) {
                killer.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 2, 5));
            }
        }
        for (ArenaScoreboard scoreBoard : ultimateGames.getScoreboardManager().getArenaScoreboards(arena)) {
            if (scoreBoard.getName().equals("Kills") && killerName != null) {
                scoreBoard.setScore(killerName, scoreBoard.getScore(killerName) + 1);
            }
        }
        event.getDrops().clear();
        ultimateGames.getUtils().autoRespawn(event.getEntity());
    }

    @Override
    public void onPlayerRespawn(Arena arena, PlayerRespawnEvent event) {
        event.setRespawnLocation(ultimateGames.getSpawnpointManager().getRandomSpawnPoint(arena).getLocation());
        resetInventory(event.getPlayer());
    }

    @Override
    public void onEntityDamage(Arena arena, EntityDamageEvent event) {
        if (event.getEntity() instanceof Player && event.getCause() != DamageCause.ENTITY_ATTACK && event.getCause() != DamageCause.PROJECTILE) {
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
        player.getInventory().addItem(new ItemStack(Material.IRON_SWORD, 1), new ItemStack(Material.BOW, 1), new ItemStack(Material.ARROW, 32));
        String playerName = player.getName();
        if (ultimateGames.getPlayerManager().isPlayerInArena(playerName)) {
            Arena arena = ultimateGames.getPlayerManager().getPlayerArena(playerName);
            player.getInventory().addItem(ultimateGames.getUtils().createInstructionBook(arena.getGame()));
        }
        player.getInventory().setArmorContents(
                new ItemStack[] { new ItemStack(Material.LEATHER_BOOTS, 1), new ItemStack(Material.LEATHER_LEGGINGS, 1), new ItemStack(Material.LEATHER_CHESTPLATE, 1),
                        new ItemStack(Material.LEATHER_HELMET, 1) });
        player.updateInventory();
    }
}
