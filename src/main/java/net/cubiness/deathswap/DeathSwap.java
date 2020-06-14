package net.cubiness.deathswap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import net.cubiness.colachampionship.minigame.Minigame;
import net.cubiness.colachampionship.minigame.MinigameAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class DeathSwap extends Minigame {

  private final Main plugin;
  private final HashSet<DeathSwapPlayer> players = new HashSet<>();
  private final World world;

  public DeathSwap(Main plugin, MinigameAPI api) {
    super(api);
    this.plugin = plugin;
    world = Bukkit.getWorld("world");
  }

  @Override
  protected void onReset() {
    players.clear();
  }

  @Override
  protected void onPlayerJoin(Player player) {
    players.add(new DeathSwapPlayer(player));
  }

  @Override
  protected void onPlayerLeave(Player player) {
    for (DeathSwapPlayer p : players) {
      if (p.isPlayer(player)) {
        Bukkit.broadcastMessage(p.getName() + ChatColor.YELLOW + " died!");
        p.spectator();
        players.remove(p);
        if (players.size() <= 1) {
          endGame();
        }
        break;
      }
    }
  }

  @Override
  public void onStart() {
    world.setTime(0);
    spreadPlayers();
    setTimer();
  }

  @Override
  public void forceStop() {
    sendPlayersSpawn();
    players.clear();
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
    sendPlayersSpawn();
    players.clear();
  }

  private void sendPlayersSpawn() {
    Bukkit.getOnlinePlayers().forEach(p -> {
      p.teleport(new Location(Bukkit.getWorld("world"), 0.5, 201, 0.5));
      p.setFoodLevel(20);
      p.setHealth(20);
      p.getInventory().clear();
    });
  }

  private void spreadPlayers() {
    for (DeathSwapPlayer p : players) {
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
    players.forEach(DeathSwapPlayer::safeMessage);
    Bukkit.getScheduler()
        .runTaskLater(plugin, this::warnPlayers, 90 * 20);
    Bukkit.getScheduler()
        .runTaskLater(plugin, this::swapPlayers, (int) (Math.random() * (300 - 90) + 90) * 20);
  }

  private void warnPlayers() {
    players.forEach(DeathSwapPlayer::warn);
  }

  private void swapPlayers() {
    players.forEach(DeathSwapPlayer::setOldLocation);
    List<DeathSwapPlayer> players = new ArrayList<>(this.players);
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
