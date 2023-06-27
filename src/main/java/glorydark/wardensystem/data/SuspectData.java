package glorydark.wardensystem.data;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.utils.Config;
import glorydark.wardensystem.MainClass;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SuspectData {

    String name;

    long startMillis;

    long endMillis;

    public void sendSuspectTips() {
        MainClass.wardens.keySet().forEach(s -> {
            Player player = Server.getInstance().getPlayer(s);
            if (player != null) {
                player.sendMessage("§c嫌疑玩家【" + name + "】已上线！");
            }
        });
    }

    public boolean checkExpired() {
        if (endMillis == -1) {
            return true;
        }
        if (System.currentTimeMillis() >= endMillis) {
            Config suspects = new Config(MainClass.path + "/suspects.yml", Config.YAML);
            suspects.remove(name);
            suspects.save();
            return false;
        }
        return true;
    }

}
