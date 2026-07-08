package com.yourname.bansmp;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class BanSMPPlugin extends JavaPlugin implements Listener {

    private NamespacedKey reviveKey;
    private ItemStack reviveBeacon;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        reviveKey = new NamespacedKey(this, "revive_beacon");
        reviveBeacon = createReviveBeacon();

        ShapelessRecipe recipe = new ShapelessRecipe(reviveKey, reviveBeacon.clone());
        for (int i = 0; i < 9; i++) {
            recipe.addIngredient(Material.NETHER_STAR);
        }
        Bukkit.addRecipe(recipe);
        getLogger().info("BanSMP plugin enabled!");
    }

    @Override
    public void onDisable() {
        Bukkit.removeRecipe(reviveKey);
        getLogger().info("BanSMP plugin disabled!");
    }

    private ItemStack createReviveBeacon() {
        ItemStack beacon = new ItemStack(Material.BEACON);
        ItemMeta meta = beacon.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Revive Beacon");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Right-click to revive a banned player.");
        lore.add(ChatColor.DARK_PURPLE + "Consumed on use.");
        meta.setLore(lore);
        beacon.setItemMeta(meta);
        return beacon;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Bukkit.getBanList(BanList.Type.NAME).addBan(player.getName(), "You have been permanently banned from this server!", null, null);
        player.kickPlayer("You have been permanently banned from this server!");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().name().contains("RIGHT_CLICK")) return;
        ItemStack item = event.getItem();
        if (item == null || !item.isSimilar(reviveBeacon)) return;
        event.setCancelled(true);
        openReviveGUI(event.getPlayer());
    }

    private void openReviveGUI(Player player) {
        List<String> bannedPlayers = new ArrayList<>();
        for (String name : Bukkit.getBanList(BanList.Type.NAME).getBanEntries()) {
            if (Bukkit.getBanList(BanList.Type.NAME).isBanned(name)) {
                bannedPlayers.add(name);
            }
        }
        if (bannedPlayers.isEmpty()) {
            player.sendMessage(ChatColor.RED + "There are no banned players to revive.");
            return;
        }
        int size = Math.min(54, ((bannedPlayers.size() / 9) + 1) * 9);
        Inventory gui = Bukkit.createInventory(null, size, ChatColor.DARK_RED + "Revive a Player");
        for (String name : bannedPlayers) {
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = skull.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + name);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Click to revive " + name);
            meta.setLore(lore);
            skull.setItemMeta(meta);
            gui.addItem(skull);
        }
        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (!event.getView().getTitle().equals(ChatColor.DARK_RED + "Revive a Player")) return;
        event.setCancelled(true);
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

        String targetName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
        if (!Bukkit.getBanList(BanList.Type.NAME).isBanned(targetName)) {
            player.sendMessage(ChatColor.RED + targetName + " is not banned anymore.");
            player.closeInventory();
            return;
        }
        Bukkit.getBanList(BanList.Type.NAME).pardon(targetName);
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.isSimilar(reviveBeacon)) {
            held.setAmount(held.getAmount() - 1);
            player.getInventory().setItemInMainHand(held.getAmount() > 0 ? held : null);
        }
        player.closeInventory();
        player.sendMessage(ChatColor.GREEN + targetName + " has been revived successfully!");
    }
}
