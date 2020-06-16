package net.cubiness.deathswap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.cubiness.colachampionship.minigame.Minigame;
import net.cubiness.colachampionship.minigame.MinigameAPI;
import net.cubiness.colachampionship.scoreboard.section.PointsSection;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

public class DeathSwap extends Minigame {

  private final Main plugin;
  private final Map<UUID, DeathSwapPlayer> players = new HashMap<>();
  private final World world;
  private final PointsSection points;
  private BukkitTask swapPlayerId;

  public DeathSwap(Main plugin, MinigameAPI api) {
    super(api);
    this.plugin = plugin;
    world = Bukkit.getWorld("world");
    points = new PointsSection(13,
        "" + ChatColor.GREEN + ChatColor.BOLD + "Death Swap Points",
        14);
    api.addSection(points);
  }

  @Override
  protected void onReset() {
    players.clear();
  }

  @Override
  protected void onPlayerJoin(Player player) {
    players.put(player.getUniqueId(), new DeathSwapPlayer(player));
  }

  @Override
  protected void onPlayerLeave(Player player) {
    onPlayerDeath(player);
  }

  public void onPlayerDeath(Player player) {
    DeathSwapPlayer p = players.get(player.getUniqueId());
    if (p != null) {
      if (isRunning()) {
        Bukkit.broadcastMessage(p.getName() + ChatColor.YELLOW + " died!");
        p.spectator();
        players.remove(player.getUniqueId());
        players.keySet().forEach(id -> points.addPoints(id, 1));
        api.updateScoreboard();
        if (players.size() <= 1) {
          endGame();
        }
      }
    }
  }

  @Override
  public void onStart() {
    for (UUID id : players.keySet()) {
      points.addPoints(id, 1);
    }
    api.updateScoreboard();
    world.setTime(0);
    spreadPlayers();
    setTimer();
  }

  @Override
  public void onForceStop() {
    sendPlayersSpawn();
    players.clear();
    swapPlayerId.cancel();
  }

  @Override
  public Location getLobby() {
    return new Location(world, 0, 100, 0);
  }

  @Override
  public String getName() {
    return "DeathSwap";
  }

  @Override
  public int getMinimumPlayers() {
    return 2;
  }

  private void endGame() {
    Bukkit.broadcastMessage(getPlayers().next().getName()
        + ChatColor.YELLOW
        + " has wone the game!");
    sendPlayersSpawn();
    players.clear();
    swapPlayerId.cancel();
    api.finish(this);
  }

  private void sendPlayersSpawn() {
    getPlayers().forEachRemaining(p -> {
      p.teleport(new Location(Bukkit.getWorld("world"), 0.5, 201, 0.5));
      p.setFoodLevel(20);
      p.setHealth(20);
      p.getInventory().clear();
    });
  }

  private void spreadPlayers() {
    for (DeathSwapPlayer p : players.values()) {
      Location l = new Location(world, Math.random() * 200000, 0, Math.random() * 200000);
      for (int i = 255; i > 0; i--) {
        l.setY(i);
        if (l.getBlock().getType() == Material.WATER) {
          l.setX(Math.random() * 200000);
          l.setY(Math.random() * 200000);
          i = 255;
        } else if (l.getBlock().getType() != Material.AIR) {
          l.setY(i + 1);
          break;
        }
      }
      p.setup(l);
    }
  }

  private void setTimer() {
    players.values().forEach(DeathSwapPlayer::safeMessage);
    Bukkit.getScheduler()
        .runTaskLater(plugin, this::warnPlayers, 90 * 20);
    swapPlayerId = Bukkit.getScheduler()
        .runTaskLater(plugin, this::swapPlayers, (int) (Math.random() * (300 - 90) + 90) * 20);
  }

  private void warnPlayers() {
    players.values().forEach(DeathSwapPlayer::warn);
  }

  private void swapPlayers() {
    players.values().forEach(DeathSwapPlayer::setOldLocation);
    List<DeathSwapPlayer> players = new ArrayList<>(this.players.values());
    Collections.shuffle(players);
    for (int i = 0; i < players.size(); i++) {
      DeathSwapPlayer prev = i == 0 ? players.get(players.size() - 1) : players.get(i - 1);
      DeathSwapPlayer next = i == players.size() - 1 ? players.get(0) : players.get(i + 1);
      DeathSwapPlayer player = players.get(i);
      player.teleport(next);
      player.sendTeleportMessage(prev, next);
    }
    setTimer();
  }

  private static class DeathSwapPlayer {

    private final Player player;
    private Location oldLocation;

    public DeathSwapPlayer(Player player) {
      this.player = player;
    }

    public void setOldLocation() {
      oldLocation = player.getLocation().clone();
    }

    public void setup(Location loc) {
      player.teleport(loc);
      player.setFoodLevel(20);
      player.setSaturation(5);
      player.setHealth(20);
      player.getInventory().clear();
      player.addPotionEffect(
          new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 30 * 20, 255, false, false));
    }

    public void teleport(DeathSwapPlayer other) {
      player.teleport(other.oldLocation);
    }

    public String getName() {
      return player.getName();
    }

    public void sendTeleportMessage(DeathSwapPlayer prev, DeathSwapPlayer next) {
      player.sendMessage(
          prev.getName() + ChatColor.YELLOW + " teleported to you, and you were teleported to "
              + ChatColor.WHITE + next.getName());
    }

    public void safeMessage() {
      player.sendMessage(ChatColor.YELLOW + "You are safe for the next 90 seconds");
    }

    public void warn() {
      player.sendMessage(ChatColor.YELLOW + "You are no longer safe!");
    }

    public boolean isPlayer(Player p) {
      return p.getUniqueId() == player.getUniqueId();
    }

    public void spectator() {
      if (player.isOnline()) {
        player.setFoodLevel(20);
        player.setHealth(20);
        player.getInventory().clear();
        player.setGameMode(GameMode.SPECTATOR);
      }
    }
  }
}
