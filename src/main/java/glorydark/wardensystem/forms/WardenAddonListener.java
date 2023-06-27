package glorydark.wardensystem.forms;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import glorydark.nukkit.event.PrefixModifyMessageEvent;
import glorydark.wardensystem.MainClass;
import glorydark.wardensystem.data.WardenData;

public class WardenAddonListener implements Listener {
    @EventHandler
    public void PrefixModifyMessageEvent(PrefixModifyMessageEvent event) {
        Player player = event.getPlayer();
        WardenData data = MainClass.wardens.get(player.getName());
        if (data != null) {
            if (data.isShowWardenPrefix()) {
                switch (data.getLevelType()) {
                    case ADMIN:
                        event.setDisplayedPrefix("§6§l协管主管");
                        break;
                    case NORMAL:
                        event.setDisplayedPrefix("§6§l玩家协管");
                        break;
                }
            }
        }
    }
}
