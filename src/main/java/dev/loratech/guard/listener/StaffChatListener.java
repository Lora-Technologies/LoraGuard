package dev.loratech.guard.listener;

import dev.loratech.guard.LoraGuard;
import dev.loratech.guard.command.StaffChatCommand;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class StaffChatListener implements Listener {

    private final LoraGuard plugin;

    public StaffChatListener(LoraGuard plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        StaffChatCommand staffChatCommand = plugin.getStaffChatCommand();

        if (staffChatCommand != null && staffChatCommand.isToggled(player.getUniqueId())) {
            event.setCancelled(true);
            String message = PlainTextComponentSerializer.plainText().serialize(event.message());
            staffChatCommand.broadcastToStaff(player, message);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        StaffChatCommand staffChatCommand = plugin.getStaffChatCommand();
        if (staffChatCommand != null) {
            staffChatCommand.removeToggle(event.getPlayer().getUniqueId());
        }
    }
}
