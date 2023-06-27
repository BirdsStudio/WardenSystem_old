package glorydark.wardensystem.data;

import cn.nukkit.Player;
import lombok.Data;

@Data
public class OfflineData {

    private String name;

    private String displayName;

    private long millis;

    public OfflineData(Player player) {
        this.name = player.getName();
        this.displayName = player.getDisplayName();
        this.millis = System.currentTimeMillis();
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - millis >= 60000;
    }
}
