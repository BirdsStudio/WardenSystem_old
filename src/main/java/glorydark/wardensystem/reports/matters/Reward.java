package glorydark.wardensystem.reports.matters;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.ConsoleCommandSender;
import cn.nukkit.utils.ConfigSection;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Reward {

    public List<String> messages;

    public List<String> commands;

    public Reward(ConfigSection section) {
        this.messages = new ArrayList<>(section.getStringList("messages"));
        this.commands = new ArrayList<>(section.getStringList("commands"));
    }

    public void execute(Player player) {
        for (String message : new ArrayList<>(messages)) {
            player.sendMessage(message.replace("{player}", player.getName()));
        }

        for (String command : new ArrayList<>(commands)) {
            Server.getInstance().dispatchCommand(new ConsoleCommandSender(), command.replace("{player}", player.getName()));
        }
    }
}
