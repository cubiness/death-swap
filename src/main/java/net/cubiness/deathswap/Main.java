package net.cubiness.deathswap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Main extends JavaPlugin implements Listener, CommandExecutor {

  private final HashSet<DeathSwapPlayer> players = new HashSet<>();
  private World world;
  private boolean gameRunning = false;

  @Override
  public void onEnable() {
    getServer().getPluginManager().registerEvents(this, this);
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (label.equals("ds")) {
      if (!sender.hasPermission("deathswap.admin")) {
        sender.sendMessage(ChatColor.RED + "You do not have permission to run this command!");
        return true;
      }
      if (args.length == 1) {
        if (args[0].equals("start")) {
          if (gameRunning) {
            sender.sendMessage(ChatColor.RED + "There is already a game of DeathSwap running!");
          } else {
            if (sender instanceof Player) {
              world = ((Player) sender).getLocation().getWorld();
              gameRunning = true;
              Bukkit.broadcastMessage(ChatColor.YELLOW + "DeathSwap is starting!");
              world.setTime(0);
              Bukkit.getOnlinePlayers().forEach(p -> {
                if (!p.equals(sender)) {
                  players.add(new DeathSwapPlayer(p));
                }
              });
              if (players.size() <= 1) {
                sender.sendMessage(
                    ChatColor.RED + "Cannot start the game with less than two players!");
              } else {
                spreadPlayers();
                setTimer();
              }
            } else {
              sender.sendMessage(ChatColor.RED + "A player has to start the game!");
            }
          }
        } else if (args[0].equals("stop")) {
          if (gameRunning) {
            sendPlayersSpawn();
            players.clear();
            gameRunning = false;
            Bukkit
                .broadcastMessage(ChatColor.YELLOW + "Game has been force stopped by an operator!");
          }
        } else {
          return false;
        }
        return true;
      } else {
        return false;
      }
    }
    return false;
  }

  @EventHandler
  public void onPlayerDeath(PlayerDeathEvent e) {
    for (DeathSwapPlayer p : players) {
      if (p.isPlayer(e.getEntity())) {
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

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent e) {
    for (DeathSwapPlayer p : players) {
      if (p.isPlayer(e.getPlayer())) {
        p.spectator();
        players.remove(p);
        if (players.size() <= 1) {
          endGame();
        }
        break;
      }
    }
  }

  private void endGame() {
    sendPlayersSpawn();
    Bukkit.broadcastMessage(ChatColor.YELLOW + "Game has ended!");
    Bukkit.broadcastMessage(
        players.iterator().next().getName() + ChatColor.YELLOW + " has won the game!");
    players.clear();
    gameRunning = false;
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
        .runTaskLater(this, this::warnPlayers, 90 * 20);
    Bukkit.getScheduler()
        .runTaskLater(this, this::swapPlayers, (int) (Math.random() * (300 - 90) + 90) * 20);
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
