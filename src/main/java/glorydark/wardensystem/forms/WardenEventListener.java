package glorydark.wardensystem.forms;

import cn.nukkit.AdventureSettings;
import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.ConsoleCommandSender;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.BlockPlaceEvent;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.player.*;
import cn.nukkit.form.response.FormResponseCustom;
import cn.nukkit.form.window.FormWindow;
import cn.nukkit.form.window.FormWindowCustom;
import cn.nukkit.form.window.FormWindowModal;
import cn.nukkit.form.window.FormWindowSimple;
import cn.nukkit.utils.Config;
import glorydark.wardensystem.MainClass;
import glorydark.wardensystem.WardenAPI;
import glorydark.wardensystem.data.OfflineData;
import glorydark.wardensystem.data.PlayerData;
import glorydark.wardensystem.data.WardenData;
import glorydark.wardensystem.data.WardenLevelType;
import glorydark.wardensystem.reports.matters.BugReport;
import glorydark.wardensystem.reports.matters.ByPassReport;
import glorydark.wardensystem.reports.matters.Reward;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

public class WardenEventListener implements Listener {
    public static final HashMap<Player, HashMap<Integer, FormType>> UI_CACHE = new HashMap<>();

    public static void showFormWindow(Player player, FormWindow window, FormType guiType) {
        UI_CACHE.computeIfAbsent(player, i -> new HashMap<>()).put(player.showFormWindow(window), guiType);
    }

    //修复协管飞行状态下饥饿下降的bug
    @EventHandler
    public void PlayerFoodLevelChangeEvent(PlayerFoodLevelChangeEvent event) {
        Player player = event.getPlayer();
        if (player.getGamemode() == 0 && player.getAdventureSettings().get(AdventureSettings.Type.ALLOW_FLIGHT)) {
            event.setFoodLevel(20);
            event.setFoodSaturationLevel(20);
        }
    }

    // 防止协管切生存破坏
    @EventHandler
    public void BlockBreakEvent(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (MainClass.wardens.containsKey(player.getName())) {
            if (player.getGamemode() == 1) {
                return;
            }
            if (MainClass.forbid_modify_worlds.contains(player.getLevel().getName())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void BlockPlaceEvent(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (MainClass.wardens.containsKey(player.getName())) {
            if (player.getGamemode() == 1) {
                return;
            }
            if (MainClass.forbid_modify_worlds.contains(player.getLevel().getName())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void PlayerQuitEvent(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        MainClass.offlineData.add(new OfflineData(player));
        if (MainClass.wardens.containsKey(player.getName())) {
            MainClass.wardens.get(player.getName()).setDealing(null);
            MainClass.log.log(Level.INFO, "操作员 [" + player.getName() + "] 退出服务器，飞行状态" + player.getAdventureSettings().get(AdventureSettings.Type.FLYING) + "，游戏模式：" + player.getGamemode() + "！");
        }
    }

    @EventHandler
    public void EntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            PlayerData data = MainClass.playerData.getOrDefault(player, new PlayerData(player));
            data.addDamageSource((Player) event.getDamager());
            MainClass.playerData.put(player, data);
        }
    }

    @EventHandler
    public void PlayerLocallyInitializedEvent(PlayerLocallyInitializedEvent event) {
        Player player = event.getPlayer();
        long bannedRemained = WardenAPI.getRemainedBannedTime(player.getName());
        if (bannedRemained != 0) {
            player.kick("§c您已被封禁\n§e解封时间：" + WardenAPI.getUnBannedDate(player.getName()) + "\n申诉方式：前往服务器群聊申诉（432813576）");
            return;
        }
        long mutedRemained = WardenAPI.getRemainedMutedTime(player.getName());
        if (mutedRemained != 0) {
            player.sendMessage("§c您已被禁言\n§e解封时间：" + WardenAPI.getUnMutedDate(player.getName()) + "\n申诉方式：前往服务器群聊申诉（432813576）");
            return;
        }
        if (MainClass.wardens.containsKey(player.getName())) {
            MainClass.log.log(Level.INFO, "操作员 [" + player.getName() + "] 进入服务器！");
            this.setFlying(player, false);
            if (MainClass.bugReports.size() > 0) {
                player.sendMessage("§e目前有【§c" + MainClass.bugReports.size() + "§e】个bug反馈信息待处理！");
            } else {
                player.sendMessage("§a目前暂无未处理的bug反馈消息！");
            }
            if (MainClass.byPassReports.size() > 0) {
                player.sendMessage("§e目前有【§c" + MainClass.byPassReports.size() + "§e】个举报信息待处理！");
            } else {
                player.sendMessage("§a目前暂无未处理的举报消息！");
            }
            if (MainClass.suspectList.size() > 0) {
                List<String> suspectOnlineList = new ArrayList<>();
                for (String s : MainClass.suspectList.keySet()) {
                    Player p = Server.getInstance().getPlayer(s);
                    if (p != null) {
                        suspectOnlineList.add(p.getName());
                    }
                }
                player.sendMessage("现在在线的嫌疑玩家：" + Arrays.toString(suspectOnlineList.toArray()));
            }
        }
        if (MainClass.suspectList.containsKey(player.getName())) {
            if (MainClass.suspectList.get(player.getName()).checkExpired()) {
                player.sendMessage("§c您已被列为嫌疑玩家，请端正游戏行为！");
                MainClass.suspectList.get(player.getName()).sendSuspectTips();
            } else {
                MainClass.suspectList.remove(player.getName());
            }
        }
        File file = new File(MainClass.path + "/mailbox/" + player.getName() + ".yml");
        if (file.exists()) {
            Config config = new Config(file, Config.YAML);
            List<Map<String, Object>> list = config.get("unclaimed", new ArrayList<>());
            player.sendMessage("§e目前有【§c" + list.size() + "§e】个未读邮件！");
        } else {
            player.sendMessage("§a目前暂无未读邮件！");
        }
    }

    @EventHandler
    public void PlayerChatEvent(PlayerChatEvent event) {
        if (MainClass.muted.contains(event.getPlayer().getName())) {
            if (WardenAPI.getRemainedMutedTime(event.getPlayer().getName()) == 0L) {
                MainClass.muted.remove(event.getPlayer().getName());
            } else {
                event.getPlayer().sendMessage("§c您已被禁言，预计解封时间：" + WardenAPI.getUnMutedDate(event.getPlayer().getName()));
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void PlayerCommandPreprocessEvent(PlayerCommandPreprocessEvent event) {
        if (MainClass.muted.contains(event.getPlayer().getName())) {
            if (event.getMessage().startsWith("me")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void PlayerFormRespondedEvent(PlayerFormRespondedEvent event) {
        Player p = event.getPlayer();
        FormWindow window = event.getWindow();
        if (p == null || window == null) {
            return;
        }
        FormType guiType = UI_CACHE.containsKey(p) ? UI_CACHE.get(p).get(event.getFormID()) : null;
        if (guiType == null) {
            return;
        }
        UI_CACHE.get(p).remove(event.getFormID());
        if (event.getResponse() == null) {
            return;
        }
        if (window instanceof FormWindowSimple) {
            this.formWindowSimpleOnClick(p, (FormWindowSimple) window, guiType);
        }
        if (window instanceof FormWindowCustom) {
            this.formWindowCustomOnClick(p, (FormWindowCustom) window, guiType);
        }
        if (window instanceof FormWindowModal) {
            this.formWindowModalOnClick(p, (FormWindowModal) window, guiType);
        }
    }

    private void formWindowSimpleOnClick(Player player, FormWindowSimple window, FormType guiType) {
        if (window.getResponse() == null) {
            if (MainClass.wardens.containsKey(player.getName())) {
                MainClass.wardens.get(player.getName()).setDealing(null);
            }
            return;
        }
        int id = window.getResponse().getClickedButtonId();
        switch (guiType) {
            case WardenMain:
                switch (id) {
                    case 0:
                        FormMain.showWardenReportTypeList(player);
                        break;
                    case 1:
                        FormMain.showWardenPunishType(player);
                        break;
                    case 2:
                        FormMain.showMailBoxMain(player);
                        break;
                    case 3:
                        FormMain.showReportTypeMenu(player);
                        break;
                    case 4:
                        FormMain.showUsefulTools(player);
                        break;
                    case 5:
                        FormMain.showWardenProfile(player);
                        break;
                    case 6:
                        FormMain.showAdminManage(player);
                        break;
                }
                break;
            case WardenTools:
                switch (id) {
                    case 0:
                        FormMain.showSelectPlayer(player, FormType.WardenTeleportTools);
                        break;
                    case 1:
                        player.getInventory().clearAll();
                        player.sendMessage("§a您的背包已清空！");
                        MainClass.log.log(Level.INFO, "操作员[" + player.getName() + "]使用清空背包功能！");
                        break;
                    case 2:
                        switch (window.getResponse().getClickedButton().getText()) {
                            case "切换至生存模式":
                                player.setGamemode(0);
                                MainClass.log.log(Level.INFO, "操作员[" + player.getName() + "]切换至生存模式！");
                                break;
                            case "切换至冒险模式":
                                player.setGamemode(2);
                                MainClass.log.log(Level.INFO, "操作员[" + player.getName() + "]切换至冒险模式！");
                                break;
                            case "切换至观察者模式":
                                MainClass.wardens.get(player.getName()).setGamemodeBefore(player.getGamemode());
                                player.setGamemode(3);
                                MainClass.log.log(Level.INFO, "操作员[" + player.getName() + "]切换至观察者模式");
                                break;
                        }
                        break;
                    case 3:
                        switch (window.getResponse().getClickedButton().getText()) {
                            case "开启飞行":
                                this.setFlying(player, true);
                                MainClass.log.log(Level.INFO, "操作员[" + player.getName() + "]开启飞行！");
                                break;
                            case "关闭飞行":
                                this.setFlying(player, false);
                                MainClass.log.log(Level.INFO, "操作员[" + player.getName() + "]关闭飞行！");
                                break;
                        }
                        break;
                    case 4:
                        FormMain.showRecentProfile(player);
                        break;
                    case 5:
                        FormMain.showWardenMain(player);
                        break;
                }
                break;
            case WardenDealBugReportList:
                String text = window.getResponse().getClickedButton().getText();
                if (text.equals("返回") || text.equals("")) {
                    FormMain.showWardenReportTypeList(player);
                    return;
                }
                List<BugReport> bugReports = new ArrayList<>(MainClass.bugReports);
                Collections.reverse(bugReports);
                BugReport select = MainClass.bugReports.get(id);
                if (MainClass.wardens.entrySet().stream().anyMatch((s) -> !s.getKey().equals(player.getName()) && s.getValue().getDealing() == select)) {
                    FormMain.showReportReturnMenu("该bug反馈已有人在处理！", player, FormType.DealBugReportReturn);
                    return;
                }
                MainClass.wardens.get(player.getName()).setDealing(select);
                FormMain.showWardenBugReport(player, select);
                break;
            case WardenDealByPassReportList:
                text = window.getResponse().getClickedButton().getText();
                if (text.equals("返回") || text.equals("")) {
                    FormMain.showWardenReportTypeList(player);
                    return;
                }
                List<ByPassReport> byPassReports = new ArrayList<>(MainClass.byPassReports);
                Collections.reverse(byPassReports);
                ByPassReport select1 = byPassReports.get(id);
                if (MainClass.wardens.entrySet().stream().anyMatch((s) -> !s.getKey().equals(player.getName()) && s.getValue().getDealing() == select1)) {
                    FormMain.showReportReturnMenu("该举报已有人在处理！", player, FormType.DealByPassReportReturn);
                    return;
                }
                MainClass.wardens.get(player.getName()).setDealing(select1);
                FormMain.showWardenByPassReport(player, select1);
                break;
            case WardenDealReportMain:
                switch (id) {
                    case 0:
                        FormMain.showWardenReportList(player, FormType.WardenDealBugReportList);
                        break;
                    case 1:
                        FormMain.showWardenReportList(player, FormType.WardenDealByPassReportList);
                        break;
                    case 2:
                        FormMain.showWardenMain(player);
                        break;
                }
                break;
            case WardenTeleportTools:
                String pn = window.getResponse().getClickedButton().getText();
                if (pn.equals("返回")) {
                    FormMain.showWardenMain(player);
                    return;
                }
                Player p = Server.getInstance().getPlayer(pn);
                if (p != null) {
                    player.teleportImmediate(p); // 更改为直接tp
                    player.sendMessage("§a已传送到：" + p.getName());
                    MainClass.log.log(Level.INFO, "操作员[" + player.getName() + "]使用传送功能，传送到玩家" + p.getName() + "！");
                } else {
                    player.sendMessage("§c玩家不在线或不存在！");
                }
                break;
            case PlayerMain:
                switch (id) {
                    case 0:
                        FormMain.showReportTypeMenu(player);
                        break;
                    case 1:
                        FormMain.showMailBoxMain(player);
                        break;
                    case 2:
                        FormMain.showRecentProfile(player);
                        break;
                }
                break;
            case PlayerMailboxMain:
                switch (window.getResponse().getClickedButton().getText()) {
                    case "返回":
                        if (MainClass.wardens.containsKey(player.getName())) {
                            FormMain.showWardenMain(player);
                        } else {
                            FormMain.showPlayerMain(player);
                        }
                        return;
                    case "一键已读":
                        Config mailInfoConfig = new Config(MainClass.path + "/mailbox/" + player.getName() + ".yml", Config.YAML);
                        List<Map<String, Object>> list = (List<Map<String, Object>>) mailInfoConfig.get("unclaimed");
                        List<Map<String, Object>> claimed = new ArrayList<>(mailInfoConfig.get("claimed", new ArrayList<>()));
                        int i = 0;
                        for (Map<String, Object> map : list) {
                            for (String message : new ArrayList<>((List<String>) map.getOrDefault("messages", new ArrayList<>()))) {
                                player.sendMessage(message.replace("{player}", player.getName()));
                            }

                            for (String command : new ArrayList<>((List<String>) map.getOrDefault("commands", new ArrayList<>()))) {
                                Server.getInstance().dispatchCommand(new ConsoleCommandSender(), command.replace("{player}", player.getName()));
                            }
                            claimed.add(list.get(i));
                            i++;
                        }

                        mailInfoConfig.set("unclaimed", new ArrayList<>());
                        mailInfoConfig.set("claimed", claimed);
                        mailInfoConfig.save();
                        player.sendMessage("§a您已一键已读且领取所有邮件！");
                        return;
                }
                FormMain.showMailDetail(player, id - 1);
                break;
            case PlayerMailboxInfo:
                switch (id) {
                    case 0:
                        Config mailInfoConfig = new Config(MainClass.path + "/mailbox/" + player.getName() + ".yml", Config.YAML);
                        List<Map<String, Object>> list = (List<Map<String, Object>>) mailInfoConfig.get("unclaimed");
                        int index = MainClass.mails.get(player);
                        Map<String, Object> map = (list.get(index));
                        for (String message : new ArrayList<>((List<String>) map.getOrDefault("messages", new ArrayList<>()))) {
                            player.sendMessage(message.replace("{player}", player.getName()));
                        }

                        for (String command : new ArrayList<>((List<String>) map.getOrDefault("commands", new ArrayList<>()))) {
                            Server.getInstance().dispatchCommand(new ConsoleCommandSender(), command.replace("{player}", player.getName()));
                        }

                        List<Map<String, Object>> newList = new ArrayList<>();
                        newList.add(list.get(index));

                        list.remove(index);
                        if (list.size() > 0) {
                            mailInfoConfig.set("unclaimed", list);
                        } else {
                            mailInfoConfig.remove("unclaimed");
                        }
                        mailInfoConfig.set("claimed", newList);
                        mailInfoConfig.save();
                        player.sendMessage("§a邮件已查看！");
                        break;
                    case 1:
                        FormMain.showMailBoxMain(player);
                        break;
                }
                break;
            case PlayerReportMain:
                switch (id) {
                    case 0:
                        FormMain.showReportMenu(player, FormType.PlayerBugReport);
                        break;
                    case 1:
                        FormMain.showReportMenu(player, FormType.PlayerByPassReport);
                        break;
                }
                break;
            case RecentProfile:
                Server.getInstance().dispatchCommand(player, "r");
                break;
            case WardenPunishType:
                switch (id) {
                    case 0:
                        FormMain.showWardenPunish(player);
                        break;
                    case 1:
                        FormMain.showWardenPardon(player);
                        break;
                    case 2:
                        FormMain.showCheckPlayerDetails(player);
                        break;
                    case 3:
                        FormMain.showWardenMain(player);
                        break;
                }
                break;
            case AdminManageType:
                switch (id) {
                    case 0:
                        FormMain.showAddWarden(player);
                        break;
                    case 1:
                        FormMain.showRemoveWarden(player);
                        break;
                    case 2:
                        FormMain.showWardenStatistics(player);
                        break;
                    case 3:
                        FormMain.showWardenMain(player);
                        break;
                }
                break;
            case WardenStatistics:
                FormMain.showWardenMain(player);
                break;
        }
    }

    private void formWindowCustomOnClick(Player player, FormWindowCustom window, FormType guiType) {
        FormResponseCustom response = window.getResponse();
        switch (guiType) {
            case WardenDealBugReport:
                if (response == null) {
                    MainClass.wardens.get(player.getName()).setDealing(null);
                    FormMain.showWardenReportList(player, FormType.WardenDealBugReportList);
                    return;
                }
                BugReport bugReport = (BugReport) MainClass.wardens.get(player.getName()).getDealing();
                File bugFile = new File(MainClass.path + "/bugreports/" + bugReport.getMillis() + ".yml");
                String saveName = MainClass.getUltraPrecisionDate(System.currentTimeMillis());
                Config bugConfig = new Config(MainClass.path + "/bugreports/old/" + saveName + ".yml", Config.YAML);
                bugConfig.set("info", bugReport.getInfo());
                bugConfig.set("player", bugReport.getPlayer());
                bugConfig.set("millis", MainClass.getDate(bugReport.getMillis()));
                bugConfig.set("end_millis", saveName);
                boolean acceptBugReport = response.getDropdownResponse(3).getElementID() == 0;
                bugConfig.set("view", acceptBugReport);
                bugConfig.set("comments", response.getInputResponse(4));
                if (acceptBugReport) {
                    String rewardChoice = response.getDropdownResponse(5).getElementContent();
                    bugConfig.set("rewards", rewardChoice);
                    Reward reward = MainClass.rewards.get(rewardChoice);
                    WardenAPI.sendMail("协管团队"
                            , bugReport.getPlayer()
                            , "感谢您向协管团队反馈bug！"
                            , "非常感谢您帮助我们发现服务器中潜在的bug", reward.getCommands(), reward.getMessages());
                } else {
                    WardenAPI.sendMail("协管团队"
                            , bugReport.getPlayer()
                            , "您的反馈已被驳回！"
                            , "您的反馈内容：[" + bugReport.getInfo() + "]，我们暂未查明对应的反馈，请您等待我们的回信！");
                }
                bugConfig.save();
                bugFile.delete();
                MainClass.bugReports.remove(bugReport);
                FormMain.showReportReturnMenu("处理成功", player, FormType.DealBugReportReturn);
                MainClass.wardens.get(player.getName()).addDealBugReportTime();
                MainClass.log.log(Level.INFO, "操作员[" + player.getName() + "]处理bug反馈完毕，具体信息详见：bugreports/" + saveName + ".yml");
                MainClass.wardens.get(player.getName()).setDealing(null);
                break;
            case WardenDealByPassReport:
                if (response == null) {
                    MainClass.wardens.get(player.getName()).setDealing(null);
                    FormMain.showWardenReportList(player, FormType.WardenDealByPassReportList);
                    return;
                }
                ByPassReport byPassReport = (ByPassReport) MainClass.wardens.get(player.getName()).getDealing();
                File bypassFile = new File(MainClass.path + "/bypassreports/" + byPassReport.getMillis() + ".yml");
                String saveName1 = MainClass.getUltraPrecisionDate(System.currentTimeMillis());
                Config bypassConfig = new Config(MainClass.path + "/bypassreports/old/" + saveName1 + ".yml", Config.YAML);
                bypassConfig.set("info", byPassReport.getInfo());
                bypassConfig.set("cheater", byPassReport.getSuspect());
                bypassConfig.set("player", byPassReport.getPlayer());
                bypassConfig.set("millis", MainClass.getDate(byPassReport.getMillis()));
                bypassConfig.set("end_millis", saveName1);
                boolean acceptBypassReport = response.getDropdownResponse(4).getElementID() == 0;
                bypassConfig.set("view", acceptBypassReport);
                bypassConfig.set("comments", response.getInputResponse(5));
                if (acceptBypassReport) {
                    String reward1 = response.getDropdownResponse(6).getElementContent();
                    bypassConfig.set("rewards", reward1);
                    Reward reward = MainClass.rewards.get(reward1);
                    WardenAPI.sendMail("协管团队"
                            , byPassReport.getPlayer()
                            , "感谢您向协管团队举报违规玩家！"
                            , "非常感谢您帮助我们维护本服务器的环境", reward.getCommands(), reward.getMessages());
                    WardenData data = MainClass.wardens.get(byPassReport.getSuspect());
                    FormMain.showWardenPunish(player, (data != null ? (data.getLevelType() == WardenLevelType.ADMIN ? "§6" : "§e") : "") + byPassReport.getSuspect());
                } else {
                    WardenAPI.sendMail("协管团队",
                            byPassReport.getPlayer()
                            , "您的举报已被驳回！"
                            , "您的举报内容:[" + byPassReport.getInfo() + "]，我们暂时未查明该玩家的作弊行为，请您等待我们的回信！");
                    FormMain.showReportReturnMenu("处理成功", player, FormType.DealByPassReportReturn);
                }
                bypassConfig.save();
                bypassFile.delete();
                MainClass.byPassReports.remove(byPassReport);
                MainClass.wardens.get(player.getName()).addDealBypassReportTime();
                MainClass.log.log(Level.INFO, "操作员[" + player.getName() + "]处理举报完毕，具体信息详见：bypassreports/" + saveName1 + ".yml");
                MainClass.wardens.get(player.getName()).setDealing(null);
                break;
            case WardenPersonalInfo:
                if (response == null) {
                    return;
                }
                WardenData data = MainClass.wardens.get(player.getName());
                boolean show = response.getToggleResponse(6);
                if (show != data.isShowWardenPrefix()) {
                    data.setShowWardenPrefix(show);
                    player.sendMessage("§a* 成功设置协管称号显示为：" + show);
                }
                if (response.getDropdownResponse(5).getElementContent().equals(FormMain.noSelectedItemText)) {
                    return;
                }
                List<String> old = data.getPrefixes();
                if (old.size() > 1) {
                    String selectPrefix = response.getDropdownResponse(5).getElementContent();
                    if (!old.get(0).equals(selectPrefix)) {
                        List<String> newRank = new ArrayList<>();
                        newRank.add(selectPrefix);
                        for (String prefix : old) {
                            if (!prefix.equals(selectPrefix)) {
                                newRank.add(prefix);
                            }
                        }
                        Config personalConfig = data.getConfig();
                        personalConfig.set("prefixes", newRank);
                        personalConfig.save();
                        data.setPrefixes(newRank);
                        player.sendMessage("§a已保存称号设置！");
                    }
                }
                break;
            case PlayerBugReport:
                if (response == null) {
                    return;
                }
                String bugInfo = response.getInputResponse(0);
                if (!bugInfo.equals("")) {
                    Config s0 = new Config(MainClass.path + "/bugreports/" + System.currentTimeMillis() + ".yml", Config.YAML);
                    s0.set("player", player.getName());
                    s0.set("info", bugInfo);
                    boolean bugBoolean = response.getToggleResponse(1);
                    long millis = System.currentTimeMillis();
                    s0.set("anonymous", bugBoolean);
                    s0.set("millis", millis);
                    s0.save();
                    BugReport newBugReport = new BugReport(bugInfo, player.getName(), millis, bugBoolean);
                    MainClass.bugReports.add(newBugReport);
                    player.sendMessage("§a感谢您的反馈，我们正在全力核查中...");
                    MainClass.wardens.forEach((s, wardenData) -> {
                        Player p = Server.getInstance().getPlayer(s);
                        if (p != null) {
                            p.sendMessage("§a您有新的bug反馈需处理！");
                        }
                    });
                    MainClass.log.log(Level.INFO, "[" + player.getName() + "]提交bug反馈，具体内容：" + newBugReport);
                } else {
                    player.sendMessage("§c您填写的信息不完整，不予提交，请重试！");
                }
                break;
            case PlayerByPassReport:
                if (response == null) {
                    return;
                }
                String bypassInfo = response.getInputResponse(2);
                String suspect;
                if (response.getResponse(0) != null && response.getInputResponse(0).equals("")) {
                    suspect = response.getDropdownResponse(1).getElementContent();
                } else {
                    suspect = response.getInputResponse(0);
                }
                if(!Server.getInstance().lookupName(suspect).isPresent()){
                    player.sendMessage("§c玩家不存在："+suspect);
                    return;
                }
                if (!bypassInfo.equals("") && !suspect.equals("") && !suspect.equals(FormMain.noSelectedItemText)) {
                    Config s1 = new Config(MainClass.path + "/bypassreports/" + System.currentTimeMillis() + ".yml", Config.YAML);
                    boolean bypassBoolean = response.getToggleResponse(3);
                    long millis1 = System.currentTimeMillis();
                    s1.set("player", player.getName());
                    s1.set("suspect", suspect);
                    s1.set("info", bypassInfo);
                    s1.set("anonymous", bypassBoolean);
                    s1.set("millis", millis1);
                    ByPassReport newBypassReport = new ByPassReport(bypassInfo, player.getName(), suspect, millis1, bypassBoolean);
                    MainClass.byPassReports.add(newBypassReport);
                    s1.save();
                    player.sendMessage("§a感谢您的举报，我们正在全力核查中...");
                    MainClass.wardens.forEach((s, wardenData) -> {
                        Player p = Server.getInstance().getPlayer(s);
                        if (p != null) {
                            p.sendMessage("§a您有新的举报信息需处理！");
                        }
                    });
                    MainClass.log.log(Level.INFO, "[" + player.getName() + "]提交举报信息，具体内容：" + newBypassReport);
                } else {
                    player.sendMessage("§c您填写的信息不完整，不予提交，请重试！");
                }
                break;
            case WardenPardon:
                if (response == null) {
                    return;
                }
                String pardonedPn = response.getInputResponse(0);
                if (pardonedPn.equals("")) {
                    player.sendMessage("§c您填写的信息不完整，不予提交，请重试！");
                    return;
                } else {
                    if (!Server.getInstance().lookupName(pardonedPn).isPresent()) {
                        player.sendMessage("§c玩家" + pardonedPn + "不存在于服务器！");
                        return;
                    }
                }
                switch (response.getDropdownResponse(1).getElementID()) {
                    case 0:
                        WardenAPI.unban(player, pardonedPn);
                        break;
                    case 1:
                        WardenAPI.unmute(player, pardonedPn);
                        break;
                    case 2:
                        WardenAPI.removeSuspect(player, pardonedPn);
                        break;
                }
                break;
            case WardenPunish:
                if (response == null) {
                    return;
                }
                String punishedPn;
                if (response.getResponse(0) != null && response.getInputResponse(0).equals("")) {
                    punishedPn = response.getDropdownResponse(1).getElementContent();
                } else {
                    punishedPn = response.getInputResponse(0);
                }
                punishedPn = punishedPn.replace("§6", "").replace("§e", "");
                if (punishedPn.equals("") || punishedPn.equals(FormMain.noSelectedItemText)) {
                    player.sendMessage("§c您填写的信息不完整，不予提交，请重试！");
                    FormMain.showReportReturnMenu("§c您填写的信息不完整，不予提交，请重试！", player, FormType.WardenPunishReturn);
                    return;
                }
                if (!Server.getInstance().lookupName(punishedPn).isPresent()) {
                    FormMain.showReportReturnMenu("§c找不到玩家！", player, FormType.WardenPunishReturn);
                    return;
                }
                String reason = response.getInputResponse(11);
                if (reason.equals("")) {
                    String selectedDropdownItem = response.getDropdownResponse(10).getElementContent();
                    if (!selectedDropdownItem.equals(FormMain.noSelectedItemText)) {
                        reason = selectedDropdownItem;
                    }
                }
                switch (response.getDropdownResponse(2).getElementID()) {
                    case 0:
                        if (response.getToggleResponse(3)) {
                            WardenAPI.ban(player, punishedPn, reason, -1);
                        } else {
                            Calendar calendar = new Calendar.Builder().setInstant(System.currentTimeMillis()).build();
                            calendar.add(Calendar.YEAR, (int) response.getSliderResponse(4));
                            calendar.add(Calendar.MONTH, (int) response.getSliderResponse(5));
                            calendar.add(Calendar.DATE, (int) response.getSliderResponse(6));
                            calendar.add(Calendar.HOUR, (int) response.getSliderResponse(7));
                            calendar.add(Calendar.MINUTE, (int) response.getSliderResponse(8));
                            calendar.add(Calendar.SECOND, (int) response.getSliderResponse(9));
                            WardenAPI.ban(player, punishedPn, reason, calendar.getTimeInMillis());
                        }
                        break;
                    case 1:
                        if (response.getToggleResponse(3)) {
                            WardenAPI.mute(player, punishedPn, reason, -1);
                        } else {
                            Calendar calendar = new Calendar.Builder().setInstant(System.currentTimeMillis()).build();
                            calendar.add(Calendar.YEAR, (int) response.getSliderResponse(4));
                            calendar.add(Calendar.MONTH, (int) response.getSliderResponse(5));
                            calendar.add(Calendar.DATE, (int) response.getSliderResponse(6));
                            calendar.add(Calendar.HOUR, (int) response.getSliderResponse(7));
                            calendar.add(Calendar.MINUTE, (int) response.getSliderResponse(8));
                            calendar.add(Calendar.SECOND, (int) response.getSliderResponse(9));
                            WardenAPI.mute(player, punishedPn, reason, calendar.getTimeInMillis());
                        }
                        break;
                    case 2:
                        WardenAPI.warn(player, punishedPn, reason);
                        break;
                    case 3:
                        WardenAPI.kick(player, punishedPn);
                        break;
                    case 4:
                        if (response.getToggleResponse(3)) {
                            WardenAPI.suspect(player, punishedPn, reason, -1);
                        } else {
                            Calendar calendar = new Calendar.Builder().setInstant(System.currentTimeMillis()).build();
                            calendar.add(Calendar.YEAR, (int) response.getSliderResponse(4));
                            calendar.add(Calendar.MONTH, (int) response.getSliderResponse(5));
                            calendar.add(Calendar.DATE, (int) response.getSliderResponse(6));
                            calendar.add(Calendar.HOUR, (int) response.getSliderResponse(7));
                            calendar.add(Calendar.MINUTE, (int) response.getSliderResponse(8));
                            calendar.add(Calendar.SECOND, (int) response.getSliderResponse(9));
                            WardenAPI.suspect(player, punishedPn, reason, calendar.getTimeInMillis());
                        }
                        break;
                }
                player.sendMessage("§a处罚成功！");
                break;
            case PlayerStatus:
                if (response == null) {
                    return;
                }
                StringBuilder builder = new StringBuilder();
                String name = response.getInputResponse(0);
                if (!name.equals("") && Server.getInstance().lookupName(name).isPresent()) {
                    long bannedRemained = WardenAPI.getRemainedBannedTime(name);
                    if (bannedRemained <= 0) {
                        if (bannedRemained == -1) {
                            builder.append("封禁状态：§e永久封禁");
                        } else {
                            builder.append("封禁状态：§a未被封禁");
                        }
                    } else {
                        builder.append("封禁状态：§e封禁中【解禁时间：").append(MainClass.getDate(bannedRemained)).append("】");
                    }
                    builder.append("\n").append("§f");
                    long muteRemained = WardenAPI.getRemainedMutedTime(name);
                    if (muteRemained <= 0) {
                        if (muteRemained == -1) {
                            builder.append("禁言状态：§e永久禁言");
                        } else {
                            builder.append("禁言状态：§a未被禁言");
                        }
                    } else {
                        builder.append("禁言状态：§e禁言中【解禁言时间：").append(MainClass.getDate(muteRemained)).append("】");
                    }
                } else {
                    FormMain.showReportReturnMenu("该玩家不存在！", player, FormType.WardenStatusCheckReturn);
                }
                FormMain.showReportReturnMenu(builder.toString(), player, FormType.WardenStatusCheckReturn);
                break;
            case AdminAddWarden:
                if (response == null) {
                    return;
                }
                String pn = response.getInputResponse(0);
                if (!pn.equals("")) {
                    if (!Server.getInstance().lookupName(pn).isPresent()) {
                        FormMain.showReportReturnMenu("§c找不到玩家！", player, FormType.AdminAddWardenReturn);
                        return;
                    }
                    WardenAPI.addWarden(player, pn);
                } else {
                    FormMain.showReportReturnMenu("§c您未输入玩家名字！", player, FormType.AdminAddWardenReturn);
                }
                break;
            case AdminRemoveWarden:
                if (response == null) {
                    return;
                }
                pn = response.getDropdownResponse(0).getElementContent();
                if (pn.equals(FormMain.noSelectedItemText)) {
                    pn = response.getInputResponse(1);
                }
                if (pn.equals("")) {
                    FormMain.showReportReturnMenu("§c您还未选择一个协管！", player, FormType.AdminRemoveWardenReturn);
                    return;
                }
                WardenAPI.removeWarden(player, pn);
                break;
        }
    }

    private void formWindowModalOnClick(Player player, FormWindowModal window, FormType guiType) {
        if (window.getResponse() == null) {
            return;
        }
        if (window.getResponse().getClickedButtonId() != 0) {
            return;
        }
        switch (guiType) {
            case DealBugReportReturn:
                FormMain.showWardenReportList(player, FormType.WardenDealBugReportList);
                break;
            case DealByPassReportReturn:
                FormMain.showWardenReportList(player, FormType.WardenDealByPassReportList);
                break;
            case WardenStatusCheckReturn:
                FormMain.showWardenMain(player);
                break;
            case WardenModifyOperatorReturn:
                FormMain.showAdminManage(player);
                break;
            case AdminAddWardenReturn:
                FormMain.showAddWarden(player);
                break;
            case AdminRemoveWardenReturn:
                FormMain.showRemoveWarden(player);
                break;
            case WardenPunishReturn:
                FormMain.showWardenPunish(player);
                break;
        }
    }

    public void setFlying(Player player, boolean bool) {
        AdventureSettings settings = player.getAdventureSettings();
        settings.set(AdventureSettings.Type.ALLOW_FLIGHT, bool);
        if (!bool) {
            settings.set(AdventureSettings.Type.FLYING, false);
        }
        player.setAdventureSettings(settings);
        player.getAdventureSettings().update();
    }

    public void broadcastMessage(String message) {
        for (Player player : Server.getInstance().getOnlinePlayers().values()) {
            player.sendMessage(message);
        }
    }
}
