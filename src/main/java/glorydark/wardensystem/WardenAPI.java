package glorydark.wardensystem;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.CommandSender;
import cn.nukkit.utils.Config;
import glorydark.wardensystem.data.SuspectData;
import glorydark.wardensystem.data.WardenData;

import java.util.*;
import java.util.logging.Level;

public class WardenAPI {

    public static void sendMail(String sender, String receiver, String title, String content, List<String> commands, List<String> messages) {
        if (!Server.getInstance().lookupName(receiver).isPresent()) {
            Player senderPlayer = Server.getInstance().getPlayer(sender);
            if (senderPlayer != null) {
                senderPlayer.sendMessage("§c找不到该玩家！");
            }
            return;
        }
        Config config = new Config(MainClass.path + "/mailbox/" + receiver + ".yml", Config.YAML);
        List<Map<String, Object>> list = config.get("unclaimed", new ArrayList<>());
        Map<String, Object> map = new HashMap<>();
        map.put("sender", sender);
        map.put("title", title);
        map.put("content", content);
        map.put("millis", System.currentTimeMillis());
        map.put("commands", commands);
        map.put("messages", messages);
        list.add(map);
        config.set("unclaimed", list);
        config.save();
    }

    public static void sendMail(String sender, String receiver, String title, String content) {
        sendMail(receiver, sender, title, content, new ArrayList<>(), new ArrayList<>());
    }

    public static void ban(CommandSender operator, String player, String reason, long expire) {
        if (!Server.getInstance().lookupName(player).isPresent()) {
            operator.sendMessage("§c找不到玩家！");
            return;
        }
        Config banCfg = new Config(MainClass.path + "/ban.yml", Config.YAML);
        List<String> banned = new ArrayList<>(banCfg.getKeys(false));
        Map<String, Object> map = new HashMap<>();
        map.put("start", System.currentTimeMillis());
        if (banned.contains(player)) {
            map.put("end", expire == -1 ? "permanent" : (expire - System.currentTimeMillis() + Math.max(banCfg.getLong(player + ".end", System.currentTimeMillis()), System.currentTimeMillis())));
        } else {
            map.put("end", expire == -1 ? "permanent" : expire);
        }
        map.put("operator", operator.getName());
        map.put("reason", reason);
        banCfg.set(player, map);
        banCfg.save();
        Player punished = Server.getInstance().getPlayer(player);
        if (punished != null) {
            punished.kick("您已被封禁");
        }
        String unBannedDate = WardenAPI.getUnBannedDate(player);
        MainClass.log.log(Level.INFO, operator.getName() + "封禁 [" + player + "]，解封日期：" + unBannedDate + "，原因：" + reason);
        operator.sendMessage("§a成功封禁玩家【" + player + "】！\n解封日期:" + WardenAPI.getUnBannedDate(player));
        // 向所有在线玩家广播封禁消息
        broadcastMessage("§e[" + player + "] 因游戏作弊被打入小黑屋！");
    }

    public static void unban(CommandSender operator, String player) {
        Config banCfg = new Config(MainClass.path + "/ban.yml", Config.YAML);
        List<String> banned1 = new ArrayList<>(banCfg.getKeys(false));
        if (banned1.contains(player)) {
            banCfg.remove(player);
            banCfg.save();
            operator.sendMessage("§a成功解封玩家【" + player + "】！");
            MainClass.log.log(Level.INFO, operator.getName() + "为玩家 [" + player + "] 解除封禁");
        } else {
            operator.sendMessage("§c该玩家未被封禁！");
        }
    }

    public static void mute(CommandSender operator, String player, String reason, long expire) {
        if (!Server.getInstance().lookupName(player).isPresent()) {
            operator.sendMessage("§c找不到玩家！");
            return;
        }
        Config muteCfg = new Config(MainClass.path + "/mute.yml", Config.YAML);
        List<String> muted = new ArrayList<>(muteCfg.getKeys(false));
        Map<String, Object> map = new HashMap<>();
        muted.add(player);
        map.put("start", System.currentTimeMillis());
        if (muted.contains(player)) {
            map.put("end", expire == -1 ? "permanent" : (expire - System.currentTimeMillis() + Math.max(muteCfg.getLong(player + ".end", System.currentTimeMillis()), System.currentTimeMillis())));
        } else {
            map.put("end", expire == -1 ? "permanent" : expire);
        }
        map.put("operator", operator.getName());
        map.put("reason", reason);
        muteCfg.set(player, map);
        muteCfg.save();
        MainClass.muted.add(player);
        Player punished = Server.getInstance().getPlayer(player);
        if (punished != null) {
            punished.sendMessage("您已被禁言");
        }
        String unMutedDate = WardenAPI.getUnMutedDate(player);
        MainClass.log.log(Level.INFO, operator.getName() + "禁言玩家 [" + player + "]，解封日期：" + unMutedDate + "，原因：" + reason);
        operator.sendMessage("§a成功禁言玩家【" + player + "】\n解封时间:" + unMutedDate + "！");
        // 向所有在线玩家广播封禁消息
        broadcastMessage("§e[" + player + "] 因违规发言被禁止发言！");
    }

    public static void unmute(CommandSender operator, String player) {
        Config muteCfg1 = new Config(MainClass.path + "/mute.yml", Config.YAML);
        List<String> muted1 = new ArrayList<>(muteCfg1.getKeys(false));
        if (muted1.contains(player)) {
            muteCfg1.remove(player);
            muteCfg1.save();
            operator.sendMessage("§a成功为玩家【" + player + "】解除禁言！");
            MainClass.muted.remove(player);
            MainClass.log.log(Level.INFO, "CONSOLE执行：/warden unmute " + player);
            MainClass.log.log(Level.INFO, operator.getName() + "为玩家 [" + player + "] 解除禁言");
        } else {
            operator.sendMessage("§c该玩家未被禁言！");
        }
    }

    public static void warn(CommandSender operator, String player, String reason) {
        Player to = Server.getInstance().getPlayer(player);
        if (to != null) {
            to.sendMessage("§c您已被警告，请规范您的游戏行为！原因："+reason);
            operator.sendMessage("§a警告已发送！");
            MainClass.log.log(Level.INFO, operator.getName() + "警告玩家 [" + player + "]");
            broadcastMessage("§e[" + player + "] 被警告！原因："+reason);
        } else {
            operator.sendMessage("§c该玩家不存在！");
        }
    }

    public static void kick(CommandSender operator, String player) {
        Player kicked = Server.getInstance().getPlayer(player);
        if (kicked != null) {
            kicked.kick("§c您已被踢出，请规范您的游戏行为！");
            operator.sendMessage("§a已踢出该玩家！");
            broadcastMessage("§e[" + player + "] 被踢出游戏！");
            MainClass.log.log(Level.INFO, operator.getName() + "踢出玩家 [" + player + "]");
        } else {
            operator.sendMessage("§c该玩家不存在！");
        }
    }

    public static void suspect(CommandSender operator, String player, String reason, long expire) {
        Config config = new Config(MainClass.path + "/suspects.yml", Config.YAML);
        Calendar calendar = new Calendar.Builder().setInstant(System.currentTimeMillis()).build();
        calendar.add(Calendar.DATE, 7);
        Map<String, Object> map = new HashMap<>();
        map.put("start", System.currentTimeMillis());
        if (MainClass.suspectList.containsKey(player)) {
            map.put("end", expire == -1 ? "permanent" : (expire - System.currentTimeMillis() + Math.max(config.getLong(player + ".end", System.currentTimeMillis()), System.currentTimeMillis())));
        } else {
            map.put("end", expire == -1 ? "permanent" : expire);
        }
        MainClass.suspectList.put(player, new SuspectData(player, System.currentTimeMillis(), calendar.getTimeInMillis()));
        map.put("operator", operator.getName());
        map.put("reason", reason);
        config.set(player, map);
        config.save();
        operator.sendMessage("成功将玩家 [" + player + "] 列入嫌疑名单！");
        broadcastMessage("§e[" + player + "] 因疑似游戏作弊被加入嫌疑玩家名单！");
        MainClass.wardens.get(operator.getName()).addSuspectTimes();
        MainClass.log.log(Level.INFO, "[" + operator.getName() + "] 成功添加嫌疑玩家 [" + player + "]，原因：" + reason + "，解除时间：" + (calendar.getTimeInMillis() == -1 ? "永久加入嫌疑名单" : MainClass.getDate(calendar.getTimeInMillis())));
        Player p = Server.getInstance().getPlayer(player);
        if (p != null) {
            p.sendMessage("您已被列入嫌疑玩家，请端正您的游戏行为。");
        }
    }

    public static void removeSuspect(CommandSender operator, String player) {
        Config config = new Config(MainClass.path + "/suspect.yml", Config.YAML);
        List<String> muted1 = new ArrayList<>(config.getKeys(false));
        if (muted1.contains(player)) {
            config.remove(player);
            config.save();
            operator.sendMessage("§a成功将玩家【" + player + "】从嫌疑名单移除！");
            MainClass.muted.remove(player);
            MainClass.log.log(Level.INFO, operator.getName() + "将玩家 [" + player + "] 从嫌疑名单中移除!");
        } else {
            operator.sendMessage("§c该玩家未被禁言！");
        }
    }

    public static void addWarden(CommandSender operator, String player) {
        if (!Server.getInstance().lookupName(player).isPresent()) {
            operator.sendMessage("§c找不到玩家！");
            return;
        }
        Config config = new Config(MainClass.path + "/config.yml", Config.YAML);
        List<String> wardens = new ArrayList<>(config.getStringList("wardens"));
        if (wardens.contains(player)) {
            operator.sendMessage("§c该玩家已为协管！");
        } else {
            wardens.add(player);
            config.set("wardens", wardens);
            config.save();
            WardenData data = new WardenData(player, null, new Config(MainClass.path + "/wardens/" + player + ".yml", Config.YAML));
            MainClass.wardens.put(player, data);
            operator.sendMessage("§a成功为赋予玩家【" + player + "】协管权限！");
            MainClass.log.log(Level.INFO, operator.getName() + "为【" + player + "】添加协管权限");
        }
    }

    public static void removeWarden(CommandSender sender, String player) {
        if (!Server.getInstance().lookupName(player).isPresent()) {
            sender.sendMessage("§c找不到玩家！");
            return;
        }
        Config config1 = new Config(MainClass.path + "/config.yml", Config.YAML);
        List<String> wardens1 = new ArrayList<>(config1.getStringList("wardens"));
        if (wardens1.contains(player)) {
            wardens1.remove(player);
            config1.set("wardens", wardens1);
            config1.save();
            MainClass.wardens.remove(player);
            sender.sendMessage("§a成功取消玩家【" + player + "】协管权限！");
            MainClass.log.log(Level.INFO, sender.getName() + "取消【" + player + "】的协管权限");
        } else {
            sender.sendMessage("§c该玩家不是协管！");
        }
    }


    public static void broadcastMessage(String message) {
        for (Player player : Server.getInstance().getOnlinePlayers().values()) {
            player.sendMessage(message);
        }
    }

    public static String getUnBannedDate(String player) {
        Config config = new Config(MainClass.path + "/ban.yml", Config.YAML);
        return !String.valueOf(config.get(player + ".end", "")).equals("permanent") ? MainClass.getDate(config.getLong(player + ".end")) : "永久封禁";
    }

    public static String getUnMutedDate(String player) {
        Config config = new Config(MainClass.path + "/mute.yml", Config.YAML);
        return !String.valueOf(config.get(player + ".end", "")).equals("permanent") ? MainClass.getDate(config.getLong(player + ".end")) : "永久封禁";
    }

    public static long getRemainedBannedTime(String player) {
        Config config = new Config(MainClass.path + "/ban.yml", Config.YAML);
        if (config.exists(player)) {
            if (config.get(player + ".end").toString().equals("permanent")) {
                return -1;
            } else {
                if (System.currentTimeMillis() >= config.getLong(player + ".end")) {
                    config.remove(player);
                    config.save();
                    return 0;
                } else {
                    return config.getLong(player + ".end") - System.currentTimeMillis();
                }
            }
        }
        return 0;
    }

    public static long getRemainedMutedTime(String player) {
        Config config = new Config(MainClass.path + "/mute.yml", Config.YAML);
        if (config.exists(player)) {
            if (config.get(player + ".end").toString().equals("permanent")) {
                return -1;
            } else {
                if (System.currentTimeMillis() >= config.getLong(player + ".end")) {
                    config.remove(player);
                    config.save();
                    return 0;
                } else {
                    return (config.getLong(player + ".end") - System.currentTimeMillis());
                }
            }
        }
        return 0;
    }

}
