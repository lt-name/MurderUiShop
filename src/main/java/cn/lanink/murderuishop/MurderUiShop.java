package cn.lanink.murderuishop;

import cn.lanink.murdermystery.api.Api;
import cn.lanink.murdermystery.event.MurderRoomStartEvent;
import cn.lanink.murdermystery.ui.GuiCreate;
import cn.lanink.murdermystery.utils.Tools;
import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.window.FormWindowModal;
import cn.nukkit.form.window.FormWindowSimple;
import cn.nukkit.item.Item;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.plugin.PluginBase;

import cn.nukkit.scheduler.AsyncTask;
import cn.nukkit.scheduler.Task;
import cn.nukkit.utils.Config;

import java.util.ArrayList;
import java.util.LinkedList;

public class MurderUiShop extends PluginBase implements Listener {

    private static final int DLC_UI_SHOP = 1111856485;
    private static final int DLC_UI_SHOP_OK = 1111856486;
    private ArrayList<String> items = new ArrayList<>();
    private LinkedList<Player> cache = new LinkedList<>();

    @Override
    public void onEnable() {
        if (getServer().getPluginManager().getPlugin("MurderMystery") == null) {
            getLogger().warning("§c未检测到MurderMystery插件，加载失败！");
            return;
        }
        saveDefaultConfig();
        this.items = (ArrayList<String>) new Config(getDataFolder() + "/config.yml", 2).getStringList("items");
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("§a插件加载完成！");
    }

    @Override
    public void onDisable() {
        this.items.clear();
        this.cache.clear();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRoomStart(MurderRoomStartEvent event) {
        getServer().getScheduler().scheduleDelayedTask(this, new Task() {
            @Override
            public void onRun(int i) {
                Item item = Item.get(347, 0, 1);
                item.setNamedTag(new CompoundTag().putBoolean("isMurderUiShop", true));
                item.setCustomName("§a道具商店");
                item.setLore("便携道具商店", "购买各种道具来帮助你获取胜利！");
                for (Player player : event.getRoom().getPlayers().keySet()) {
                    player.getInventory().addItem(item);
                }
            }
        },20);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Item item = event.getItem();
        if (player == null || item == null || item.getNamedTag() == null) return;
        if (Api.isRoomLevel(player.getLevel()) && Api.getRoomByLevel(player.getLevel()).getMode() == 2 &&
                item.getNamedTag().getBoolean("isMurderUiShop") && !this.cache.contains(player)) {
            this.cache.add(player);
            this.showUiShop(player);
            event.setCancelled(true);
            getServer().getScheduler().scheduleDelayedTask(this, new Task() {
                @Override
                public void onRun(int i) {
                    cache.remove(player);
                }
            }, 10);
        }
    }

    public void showUiShop(Player player) {
        getServer().getScheduler().scheduleAsyncTask(this, new AsyncTask() {
            @Override
            public void onRun() {
                int x = 0;
                for (Item item : player.getInventory().getContents().values()) {
                    if (item.getId() == 266) {
                        x += item.getCount();
                    }
                }
                FormWindowSimple simple = new FormWindowSimple(GuiCreate.PLUGIN_NAME, "§a你当前有 §e" + x + " §a块金锭");
                for (String s : items) {
                    String[] item = s.split(":");
                    simple.addButton(new ElementButton(item[1]));
                }
                player.showFormWindow(simple, DLC_UI_SHOP);
            }
        });
    }

    @EventHandler
    public void onFormResponded(PlayerFormRespondedEvent event) {
        Player player = event.getPlayer();
        if (player == null || event.getWindow() == null || event.getResponse() == null) {
            return;
        }
        if (event.getWindow() instanceof FormWindowSimple) {
            if (event.getFormID() == DLC_UI_SHOP) {
                FormWindowSimple simple = (FormWindowSimple) event.getWindow();
                int id = simple.getResponse().getClickedButtonId();
                if (items.get(id) != null) {
                    String[] item = items.get(id).split(":");
                    FormWindowModal modal = new FormWindowModal(GuiCreate.PLUGIN_NAME,
                            "\n§a确定要花费 §e" + Integer.parseInt(item[2]) + " §a块金锭购买 §e" + item[1] + " §a？" +
                                    "§7§k\"" + simple.getResponse().getClickedButtonId() + "\"",
                            "§a购买", "§c返回");
                    player.showFormWindow(modal, DLC_UI_SHOP_OK);
                }
            }
        }else if (event.getWindow() instanceof FormWindowModal) {
            if (event.getFormID() == DLC_UI_SHOP_OK) {
                FormWindowModal modal = (FormWindowModal) event.getWindow();
                if (modal.getResponse().getClickedButtonId() == 0) {
                    getServer().getScheduler().scheduleAsyncTask(this, new AsyncTask() {
                        @Override
                        public void onRun() {
                            String[] s = modal.getContent().split("\"");
                            int id = Integer.parseInt(s[1]);
                            if (items.get(id) != null) {
                                int x = 0;
                                for (Item item : player.getInventory().getContents().values()) {
                                    if (item.getId() == 266) {
                                        x += item.getCount();
                                    }
                                }
                                String[] item = items.get(id).split(":");
                                if (x >= Integer.parseInt(item[2])) {
                                    player.getInventory().removeItem(Item.get(266, 0, Integer.parseInt(item[2])));
                                    Tools.giveItem(player, Integer.parseInt(item[0]));
                                    player.sendMessage("§a成功兑换到: §e" + item[1] + " §a已发放到背包！");
                                }else {
                                    player.sendMessage("§a你的金锭数量不足！");
                                }
                            }
                        }
                    });
                }else {
                    this.showUiShop(player);
                }
            }
        }
    }

}
