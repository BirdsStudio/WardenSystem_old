package glorydark.wardensystem.data;

import cn.nukkit.Player;
import cn.nukkit.Server;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PlayerData {

    Player player;
    List<DamageSource> sourceList = new ArrayList<>();

    public PlayerData(Player player) {
        this.player = player;
    }

    public List<DamageSource> getSourceList() {
        refreshSourceList();
        return sourceList;
    }

    public void addDamageSource(Player damager) {
        DamageSource source;
        if (sourceList.size() > 0) {
            List<DamageSource> sources = sourceList.stream().filter(damageSource -> damageSource.damager.equals(damager.getName())).collect(Collectors.toList());
            if (sources.size() > 0) {
                source = sources.get(0);
                sourceList.remove(source);
            } else {
                source = new DamageSource(damager.getName(), System.currentTimeMillis(), 1);
            }
        } else {
            source = new DamageSource(damager.getName(), System.currentTimeMillis(), 1);
        }
        sourceList.add(new DamageSource(damager.getName(), System.currentTimeMillis(), source.getDamageTime() + 1));
    }

    protected void refreshSourceList() {
        sourceList.removeIf(DamageSource::isExpired);
    }

    // 获取以玩家为中心16格内的玩家
    public List<Player> getSurroundedPlayer() {
        return Server.getInstance().getOnlinePlayers().values().stream().filter(p -> !p.getName().equals(player.getName()) && p.getLevel() == player.getLevel() && p.distance(player) < 16).collect(Collectors.toList());
    }

    @Data
    @AllArgsConstructor
    public static class DamageSource {

        String damager;

        long millis;

        int damageTime;

        public boolean isExpired() {
            return System.currentTimeMillis() - millis >= 60000;
        }

    }
}
