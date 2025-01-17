package banano.bananominecraft.bananoeconomy.commands;

import banano.bananominecraft.bananoeconomy.DB;
import banano.bananominecraft.bananoeconomy.Main;
import banano.bananominecraft.bananoeconomy.RPC;
import banano.bananominecraft.bananoeconomy.exceptions.TransactionError;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URL;

public class Tip implements CommandExecutor {
    private final JavaPlugin plugin;

    public Tip(final JavaPlugin plugin) {
        this.plugin = plugin;
    }


    private URL getURL() throws Exception{
        URL url = new URL(plugin.getConfig().getString("exploreblock"));
        return url;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return false;
        }

        final Player player = (Player) sender;

        // Did they call things correctly?
        if (args.length != 2) {
            player.sendMessage("You need to enter an amount to send and a player to send to");
            player.sendMessage("/tip [amount] [playername]");
            return false;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> System.out.println(""));

        // Implementation Note:
        // Because this parses to `double` instead of say an infinite precision `BigDouble` we can
        //  round differently
        final String sAmount = args[0];
        final double amount;
        try {
            amount = Double.parseDouble(sAmount);
            if (amount <= 0) {
                player.sendMessage(String.format("Amount ('%s') has to be greater than 0", sAmount));
                return false;
            }
        } catch (final Exception e) {
            sender.sendMessage(String.format("Amount ('%s') is not a number greater than 0", sAmount));
            return false;
        }

        final String targetPlayerName = args[1];
        final Player target = Bukkit.getPlayerExact(targetPlayerName);
        if (target == player) {
            player.sendMessage("You cannot tip yourself");
            return false;
        } else if (!(target instanceof Player)) {
            player.sendMessage("Player needs to be online for you to tip them");
            return false;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (DB.isFrozen(player)) {
                player.sendMessage("u r frozen!!!!!!!!!");
                return;
            }

            if (DB.isFrozen(target)) {
                player.sendMessage(targetPlayerName + "is frozen!!!!!!!!!");
                return;
            }

            player.sendMessage("Tipping " + target.getDisplayName() + " with " + amount + " bans.");
            final String sWallet = DB.getWallet(player);
            final String tWallet = DB.getWallet(target);
            final String blockHash;
            try {
                blockHash = RPC.sendTransaction(sWallet, tWallet, amount);
            } catch (final TransactionError error) {
                player.sendMessage(String.format("Tip of %s to %s failed with: %s", sAmount, targetPlayerName, error.getUserError()));
                return;
            }

            try{final URL blockURL = new URL ( getURL() + blockHash);
            final TextComponent blocklink = new TextComponent("Click me to view the transaction in the block explorer");
            blocklink.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, blockURL.toString()));
            blocklink.setUnderlined(true);

            final String amountstr = Double.toString(amount);
            player.spigot().sendMessage((new ComponentBuilder("You have sent ").color(ChatColor.YELLOW).append(amountstr).color(ChatColor.WHITE).bold(true).append(" to ").color(ChatColor.YELLOW)
                    .append(target.getDisplayName()).color(ChatColor.WHITE).bold(true).append(" with block ID : ").append(blockHash).color(ChatColor.YELLOW).bold(true).create()));
            player.spigot().sendMessage(blocklink);

            target.spigot().sendMessage((new ComponentBuilder("You have received ").color(ChatColor.YELLOW).append(amountstr).color(ChatColor.WHITE).bold(true).append(" from ").color(ChatColor.YELLOW)
                    .append(player.getDisplayName()).color(ChatColor.WHITE).bold(true).append(" with block ID : ").append(blockHash).color(ChatColor.YELLOW).bold(true).create()));
            target.spigot().sendMessage(blocklink);}
            catch (Exception e){
                System.out.println(e);
            }
        });

        return false;
    }
}
