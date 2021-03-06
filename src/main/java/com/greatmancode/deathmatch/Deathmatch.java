/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013-2014, UltimateGames Staff <https://github.com/UltimateGames//>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.greatmancode.deathmatch;

import me.ampayne2.ultimategames.api.UltimateGames;
import me.ampayne2.ultimategames.api.arenas.Arena;
import me.ampayne2.ultimategames.api.arenas.ArenaStatus;
import me.ampayne2.ultimategames.api.arenas.scoreboards.Scoreboard;
import me.ampayne2.ultimategames.api.games.Game;
import me.ampayne2.ultimategames.api.games.GamePlugin;
import me.ampayne2.ultimategames.api.utils.ArmorMaterial;
import me.ampayne2.ultimategames.api.utils.ArmorType;
import me.ampayne2.ultimategames.api.utils.UGUtils;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;

import java.util.List;

public class Deathmatch extends GamePlugin {
    private UltimateGames ultimateGames;
    private Game game;

    private Killcoin killcoin;

    @Override
    public boolean loadGame(UltimateGames ultimateGames, Game game) {
        this.ultimateGames = ultimateGames;
        this.game = game;
        game.setMessages(DMessage.class);

        killcoin = new Killcoin(ultimateGames, game, this);
        ultimateGames.getGameItemManager()
                .registerGameItem(game, killcoin)
                .registerGameItem(game, new Flashbang(ultimateGames));

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
            Player player = Bukkit.getPlayer(playerName);
            ultimateGames.getSpawnpointManager().getRandomSpawnPoint(arena).teleportPlayer(player);
            resetInventory(player);
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
            for (String playerName : players) {
                if (!playerName.equals(highestScorer)) {
                    ultimateGames.getPointManager().addPoint(game, playerName, "store", 5);
                }
            }
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
    @SuppressWarnings("deprecation")
    public boolean addPlayer(Player player, Arena arena) {
        if (arena.getStatus() == ArenaStatus.OPEN && arena.getPlayers().size() >= arena.getMinPlayers() && !ultimateGames.getCountdownManager().hasStartingCountdown(arena)) {
            ultimateGames.getCountdownManager().createStartingCountdown(arena, ultimateGames.getConfigManager().getGameConfig(game).getInt("CustomValues.StartWaitTime"));
        }
        for (PotionEffect potionEffect : player.getActivePotionEffects()) {
            player.removePotionEffect(potionEffect.getType());
        }
        player.setHealth(20.0);
        player.setFoodLevel(20);
        if (ultimateGames.getPointManager().hasPerk(game, player.getName(), "StartWithCoins")) {
            killcoin.addCoins(player.getName(), 5);
        }
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.updateInventory();
        return true;
    }

    @Override
    public void removePlayer(Player player, Arena arena) {
        killcoin.resetCoins(player.getName());
        KillcoinPerk.deactivateAll(ultimateGames, arena, player);
    }

    @Override
    @SuppressWarnings("deprecation")
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
                if (KillcoinPerk.DOUBLE_KILLCOINS.isActivated(killerName)) {
                    killcoin.addCoins(killerName, 2);
                } else {
                    killcoin.addCoin(killerName);
                }
                killcoin.updateCoins(killer);
            } else {
                Scoreboard scoreBoard = ultimateGames.getScoreboardManager().getScoreboard(arena);
                if (scoreBoard != null) {
                    scoreBoard.setScore(playerName, scoreBoard.getScore(playerName) - 1);
                }
                ultimateGames.getMessenger().sendGameMessage(arena, game, DMessage.DEATH, event.getEntity().getName());
            }
            ultimateGames.getPointManager().addPoint(game, event.getEntity().getName(), "death", 1);
        }
        KillcoinPerk.deactivateAll(ultimateGames, arena, event.getEntity());
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
        if (arena.getStatus() == ArenaStatus.RUNNING) {
            if (event.getEntity() instanceof Player) {
                Player player = (Player) event.getEntity();
                switch (event.getCause()) {
                    case FALL:
                    case FIRE:
                    case FIRE_TICK:
                    case POISON:
                    case SUICIDE:
                    case WITHER:
                        player.getWorld().playEffect(player.getLocation(), Effect.STEP_SOUND, Material.REDSTONE_BLOCK);
                        break;
                    default:
                        player.getWorld().playEffect(player.getEyeLocation(), Effect.STEP_SOUND, Material.REDSTONE_BLOCK);
                }
            }
        } else {
            event.setCancelled(true);
        }
    }

    @Override
    public void onEntityDamageByEntity(Arena arena, EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Wolf) {
            AnimalTamer animalTamer = ((Wolf) event.getEntity()).getOwner();
            if (animalTamer != null && animalTamer instanceof Player) {
                Player damager = null;
                if (event.getDamager() instanceof Player) {
                    damager = (Player) event.getDamager();
                } else if (event.getDamager() instanceof Arrow && ((Arrow) event.getDamager()).getShooter() instanceof Player) {
                    damager = (Player) ((Arrow) event.getDamager()).getShooter();
                }
                if (damager != null) {
                    Player owner = (Player) animalTamer;
                    if (owner.getUniqueId().equals(damager.getUniqueId())) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPlayerInteract(Arena arena, PlayerInteractEvent event) {
        if (event.getItem() != null) {
            if (arena.getStatus() == ArenaStatus.RUNNING) {
                Player player = event.getPlayer();
                ItemStack item = event.getItem();
                if (item.getType() == Material.BOW) {
                    if (!player.getInventory().contains(Material.ARROW)) {
                        if (event.getAction().equals(Action.RIGHT_CLICK_AIR)) {
                            ultimateGames.getMessenger().sendGameMessage(player, game, DMessage.OUT_OF_ARROWS);
                        } else if (event.getAction().equals(Action.LEFT_CLICK_AIR)) {
                            if (killcoin.getCoins(player.getName()) >= KillcoinPerk.ARROWS.getCost()) {
                                KillcoinPerk.ARROWS.activate(ultimateGames, this, arena, player);
                            } else {
                                ultimateGames.getMessenger().sendGameMessage(player, game, DMessage.PERK_NOTENOUGHCOINS, KillcoinPerk.ARROWS.getName());
                            }
                        }
                    }
                } else {
                    for (KillcoinPerk killcoinPerk : KillcoinPerk.class.getEnumConstants()) {
                        if (!killcoinPerk.showInMenu() && item.getType() == killcoinPerk.getIcon().getType()) {
                            if (item.getType() == Material.POTION) {
                                Potion potion = Potion.fromItemStack(item);
                                if (!((potion.getType() == PotionType.INSTANT_DAMAGE && killcoinPerk == KillcoinPerk.DAMAGE_POTION) || (potion.getType() == PotionType.POISON && killcoinPerk == KillcoinPerk.POISON_POTION))) {
                                    continue;
                                }
                            }

                            String playerName = player.getName();
                            if (killcoin.getCoins(playerName) < killcoinPerk.getCost()) {
                                ultimateGames.getMessenger().sendGameMessage(player, game, DMessage.PERK_NOTENOUGHCOINS, killcoinPerk.getName());
                            } else if (killcoinPerk.isActivated(playerName)) {
                                ultimateGames.getMessenger().sendGameMessage(player, game, DMessage.PERK_ALREADYACTIVE, killcoinPerk.getName());
                            } else {
                                if (killcoinPerk.canActivate(ultimateGames, this, arena, player)) {
                                    killcoinPerk.activate(ultimateGames, this, arena, player);
                                    killcoin.removeCoins(playerName, killcoinPerk.getCost());
                                    killcoin.updateCoins(player);
                                    return;
                                } else {
                                    ultimateGames.getMessenger().sendGameMessage(player, game, DMessage.PERK_CANNOTACTIVATE, killcoinPerk.getName());
                                }
                            }
                            event.setCancelled(true);
                            player.updateInventory();
                            break;
                        }
                    }
                }
            } else {
                event.setCancelled(true);
                event.getPlayer().updateInventory();
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
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.setItem(0, new ItemStack(Material.IRON_SWORD));
        inventory.setItem(1, new ItemStack(Material.BOW));
        inventory.setItem(3, KillcoinPerk.DAMAGE_POTION.getMenuIcon().clone());
        inventory.setItem(4, KillcoinPerk.POISON_POTION.getMenuIcon().clone());
        killcoin.updateCoins(player);
        inventory.setItem(8, UGUtils.createInstructionBook(game));
        inventory.setItem(9, new ItemStack(Material.ARROW, 32));
        inventory.setArmorContents(new ItemStack[]{new ItemStack(Material.LEATHER_BOOTS), new ItemStack(Material.LEATHER_LEGGINGS), new ItemStack(Material.LEATHER_CHESTPLATE), new ItemStack(Material.LEATHER_HELMET)});
        player.updateInventory();
    }
}
