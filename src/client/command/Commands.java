/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as
 published by the Free Software Foundation version 3 as published by
 the Free Software Foundation. You may not use, modify or distribute
 this program under any other version of the GNU Affero General Public
 License.te

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package client.command;

import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleJob;
import client.MapleStat;
import client.Skill;
import client.SkillFactory;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import constants.ItemConstants;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import net.server.Server;
import net.server.channel.Channel;
import net.server.world.World;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import scripting.npc.NPCScriptManager;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MapleShopFactory;
import server.TimerManager;
import server.events.gm.MapleEvent;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MapleMonsterInformationProvider;
import server.life.MapleNPC;
import server.maps.MapleMap;
import server.maps.MapleMapItem;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import tools.DatabaseConnection;
import tools.MaplePacketCreator;
import tools.Pair;

public class Commands {

    public static boolean executePlayerCommand(MapleClient c, String[] sub, char heading) {
        MapleCharacter chr = c.getPlayer();
        if (heading == '!' && chr.gmLevel() == 0) {
            String command = String.join(" ", sub);
            chr.yellowMessage("You may not use !" + command + ", please try /" + command);
            return false;
        }
        switch (sub[0]) {
            case "dispose":
                NPCScriptManager.getInstance().dispose(c);
                c.announce(MaplePacketCreator.enableActions());
                chr.message("Done.");
                break;
            case "bosshp":
                Collection<MapleMapObject> objects = chr.getMap().getMapObjects();
                DecimalFormat dc = new DecimalFormat("0.0");
                for (MapleMapObject object : objects) {
                    if (object.getType() == MapleMapObjectType.MONSTER) {
                        MapleMonster monster = (MapleMonster) object;
                        if (monster.isBoss()) {
                            int hp = monster.getHp();
                            double maxHp = monster.getMaxHp();

                            if (maxHp == 0)
                                continue;

                            chr.message(monster.getName() + ": " + monster.getHp() + "/" + monster.getMaxHp() + " (" + dc.format(hp / maxHp * 100) + "%)");
                        }
                    }
                }
                break;
            default:
                if (chr.gmLevel() == 0) {
                    chr.yellowMessage("Player Command " + heading + sub[0] + " does not exist");
                }
                return false;
        }
        return true;
    }

    public static boolean executeGMCommand(MapleClient c, String[] sub, char heading) {
        MapleCharacter player = c.getPlayer();
        Channel cserv = c.getChannelServer();
        Server srv = Server.getInstance();
        if (sub[0].equals("ap")) {
            player.setRemainingAp(Integer.parseInt(sub[1]));
        } else if (sub[0].equals("buffme")) {
            final int[] array = {9001000, 9101002, 9101003, 9101008, 2001002, 1101007, 1005, 2301003, 5121009, 1111002, 4111001, 4111002, 4211003, 4211005, 1321000, 2321004, 3121002};
            for (int i : array) {
                SkillFactory.getSkill(i).getEffect(SkillFactory.getSkill(i).getMaxLevel()).applyTo(player);
            }
        } else if (sub[0].equals("spawn")) {
            MapleMonster monster = MapleLifeFactory.getMonster(Integer.parseInt(sub[1]));
            if (monster == null) {
                return true;
            }
            if (sub.length > 2) {
                for (int i = 0; i < Integer.parseInt(sub[2]); i++) {
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(Integer.parseInt(sub[1])), player.getPosition());
                }
            } else {
                player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(Integer.parseInt(sub[1])), player.getPosition());
            }
        } else if (sub[0].equals("cleardrops")) {
            player.getMap().clearDrops(player);
        } else if (sub[0].equals("gmchat")) {
            String message = joinStringFrom(sub, 1);
            Server.getInstance().gmChat(player.getName() + " : " + message, null);
        } else if (sub[0].equals("dc")) {
            MapleCharacter victim = c.getWorldServer().getPlayerStorage().getCharacterByName(sub[1]);
            if (victim == null) {
                victim = c.getChannelServer().getPlayerStorage().getCharacterByName(sub[1]);
                if (victim == null) {
                    victim = player.getMap().getCharacterByName(sub[1]);
                    if (victim != null) {
                        try {//sometimes bugged because the map = null
                            victim.getClient().getSession().close();
                            player.getMap().removePlayer(victim);
                        } catch (Exception e) {
                        }
                    } else {

                        return true;
                    }
                }
            }
            if (player.gmLevel() < victim.gmLevel()) {
                victim = player;
            }
            victim.getClient().disconnect(false, false);
        } else if (sub[0].equals("exprate")) {
            c.getWorldServer().setExpRate(Integer.parseInt(sub[1]));
            for (MapleCharacter mc : c.getWorldServer().getPlayerStorage().getAllCharacters()) {
                mc.setRates();
            }
        } else if (sub[0].equals("fame")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(sub[1]);
            victim.setFame(Integer.parseInt(sub[2]));
            victim.updateSingleStat(MapleStat.FAME, victim.getFame());
        } else if (sub[0].equals("giftnx")) {
            cserv.getPlayerStorage().getCharacterByName(sub[1]).getCashShop().gainCash(1, Integer.parseInt(sub[2]));
            player.message("Done");
        } else if (sub[0].equals("gmshop")) {
            MapleShopFactory.getInstance().getShop(1337).sendShop(c);
        } else if (sub[0].equals("heal")) {
            player.setHpMp(30000);
        } else if (sub[0].equals("id")) {
            try {
                try (BufferedReader dis = new BufferedReader(new InputStreamReader(new URL("http://www.mapletip.com/search_java.php?search_value=" + sub[1] + "&check=true").openConnection().getInputStream()))) {
                    String s;
                    while ((s = dis.readLine()) != null) {
                        player.dropMessage(s);
                    }
                }
            } catch (Exception e) {
            }
        } else if (sub[0].equals("item") || sub[0].equals("drop")) {
            int itemId = Integer.parseInt(sub[1]);
            short quantity = 1;
            try {
                quantity = Short.parseShort(sub[2]);
            } catch (Exception e) {
            }
            if (sub[0].equals("item")) {
                int petid = -1;
                if (ItemConstants.isPet(itemId)) {
                    petid = MaplePet.createPet(itemId);
                }
                MapleInventoryManipulator.addById(c, itemId, quantity, player.getName(), petid, -1);
            } else {
                Item toDrop;
                if (MapleItemInformationProvider.getInstance().getInventoryType(itemId) == MapleInventoryType.EQUIP) {
                    toDrop = MapleItemInformationProvider.getInstance().getEquipById(itemId);
                } else {
                    toDrop = new Item(itemId, (byte) 0, quantity);
                }
                c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), toDrop, c.getPlayer().getPosition(), true, true);
            }
        } else if (sub[0].equals("job")) {
            player.changeJob(MapleJob.getById(Integer.parseInt(sub[1])));
            player.equipChanged();
        } else if (sub[0].equals("kill")) {
            if (sub.length >= 2) {
                cserv.getPlayerStorage().getCharacterByName(sub[1]).setHpMp(0);
            }
        } else if (sub[0].equals("killall")) {
            List<MapleMapObject> monsters = player.getMap().getMapObjectsInRange(player.getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.MONSTER));
            MapleMap map = player.getMap();
            for (MapleMapObject monstermo : monsters) {
                MapleMonster monster = (MapleMonster) monstermo;
                map.killMonster(monster, player, true);
                monster.giveExpToCharacter(player, monster.getExp() * c.getPlayer().getExpRate(), true, 1);
            }
            player.dropMessage("Killed " + monsters.size() + " monsters.");
        } else if (sub[0].equals("monsterdebug")) {
            List<MapleMapObject> monsters = player.getMap().getMapObjectsInRange(player.getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.MONSTER));
            for (MapleMapObject monstermo : monsters) {
                MapleMonster monster = (MapleMonster) monstermo;
                player.message("Monster ID: " + monster.getId());
            }
        } else if (sub[0].equals("unbug")) {
            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.enableActions());
        } else if (sub[0].equals("level")) {
            player.setLevel(Integer.parseInt(sub[1]));
            player.gainExp(-player.getExp(), false, false);
            player.updateSingleStat(MapleStat.LEVEL, player.getLevel());
        } else if (sub[0].equals("levelperson")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(sub[1]);
            victim.setLevel(Integer.parseInt(sub[2]));
            victim.gainExp(-victim.getExp(), false, false);
            victim.updateSingleStat(MapleStat.LEVEL, victim.getLevel());
        } else if (sub[0].equals("levelpro")) {
            while (player.getLevel() < Math.min(255, Integer.parseInt(sub[1]))) {
                player.levelUp(false);
            }
        } else if (sub[0].equals("levelup")) {
            player.levelUp(false);
        } else if (sub[0].equals("maxstat")) {
            final String[] s = {"setall", String.valueOf(Short.MAX_VALUE)};
            executeGMCommand(c, s, heading);
            player.setLevel(255);
            player.setFame(13337);
            player.setMaxHp(30000);
            player.setMaxMp(30000);
            player.updateSingleStat(MapleStat.LEVEL, 255);
            player.updateSingleStat(MapleStat.FAME, 13337);
            player.updateSingleStat(MapleStat.MAXHP, 30000);
            player.updateSingleStat(MapleStat.MAXMP, 30000);
        } else if (sub[0].equals("maxskills")) {
            for (MapleData skill_ : MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/" + "String.wz")).getData("Skill.img").getChildren()) {
                try {
                    Skill skill = SkillFactory.getSkill(Integer.parseInt(skill_.getName()));
                    player.changeSkillLevel(skill, (byte) skill.getMaxLevel(), skill.getMaxLevel(), -1);
                } catch (NumberFormatException nfe) {
                    break;
                } catch (NullPointerException npe) {
                    continue;
                }
            }
        } else if (sub[0].equals("mesoperson")) {
            cserv.getPlayerStorage().getCharacterByName(sub[1]).gainMeso(Integer.parseInt(sub[2]), true);
        } else if (sub[0].equals("mesos")) {
            player.gainMeso(Integer.parseInt(sub[1]), true);
        } else if (sub[0].equals("notice")) {
            Server.getInstance().broadcastMessage(player.getWorld(), MaplePacketCreator.serverNotice(6, "[Notice] " + joinStringFrom(sub, 1)));
        } else if (sub[0].equals("openportal")) {
            player.getMap().getPortal(sub[1]).setPortalState(true);
        } else if (sub[0].equals("closeportal")) {
            player.getMap().getPortal(sub[1]).setPortalState(false);
        } else if (sub[0].equals("startevent")) {
            for (MapleCharacter chr : player.getMap().getCharacters()) {
                player.getMap().startEvent(chr);
            }
            c.getChannelServer().setEvent(null);
        } else if (sub[0].equals("scheduleevent")) {
            if (c.getPlayer().getMap().hasEventNPC()) {
                switch (sub[1]) {
                    case "treasure":
                        c.getChannelServer().setEvent(new MapleEvent(109010000, 50));
                        break;
                    case "ox":
                        c.getChannelServer().setEvent(new MapleEvent(109020001, 50));
                        srv.broadcastMessage(player.getWorld(), MaplePacketCreator.serverNotice(0, "Hello Scania let's play an event in " + player.getMap().getMapName() + " CH " + c.getChannel() + "! " + player.getMap().getEventNPC()));
                        break;
                    case "ola":
                        c.getChannelServer().setEvent(new MapleEvent(109030101, 50)); // Wrong map but still Ola Ola
                        srv.broadcastMessage(player.getWorld(), MaplePacketCreator.serverNotice(0, "Hello Scania let's play an event in " + player.getMap().getMapName() + " CH " + c.getChannel() + "! " + player.getMap().getEventNPC()));
                        break;
                    case "fitness":
                        c.getChannelServer().setEvent(new MapleEvent(109040000, 50));
                        srv.broadcastMessage(player.getWorld(), MaplePacketCreator.serverNotice(0, "Hello Scania let's play an event in " + player.getMap().getMapName() + " CH " + c.getChannel() + "! " + player.getMap().getEventNPC()));
                        break;
                    case "snowball":
                        c.getChannelServer().setEvent(new MapleEvent(109060001, 50));
                        srv.broadcastMessage(player.getWorld(), MaplePacketCreator.serverNotice(0, "Hello Scania let's play an event in " + player.getMap().getMapName() + " CH " + c.getChannel() + "! " + player.getMap().getEventNPC()));
                        break;
                    case "coconut":
                        c.getChannelServer().setEvent(new MapleEvent(109080000, 50));
                        srv.broadcastMessage(player.getWorld(), MaplePacketCreator.serverNotice(0, "Hello Scania let's play an event in " + player.getMap().getMapName() + " CH " + c.getChannel() + "! " + player.getMap().getEventNPC()));
                        break;
                    default:
                        player.message("Wrong Syntax: /scheduleevent treasure, ox, ola, fitness, snowball or coconut");
                        break;
                }
            } else {
                player.message("You can only use this command in the following maps: 60000, 104000000, 200000000, 220000000");
            }

        } else if (sub[0].equals("online")) {
            for (Channel ch : srv.getChannelsFromWorld(player.getWorld())) {
                String s = "Characters online (Channel " + ch.getId() + " Online: " + ch.getPlayerStorage().getAllCharacters().size() + ") : ";
                if (ch.getPlayerStorage().getAllCharacters().size() < 50) {
                    for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                        s += MapleCharacter.makeMapleReadable(chr.getName()) + ", ";
                    }
                    player.dropMessage(s.substring(0, s.length() - 2));
                }
            }
        } else if (sub[0].equals("pap")) {
            player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8500001), player.getPosition());
        } else if (sub[0].equals("pianus")) {
            player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8510000), player.getPosition());
        } else if (sub[0].equalsIgnoreCase("search")) {
            StringBuilder sb = new StringBuilder();
            if (sub.length > 2) {
                String search = joinStringFrom(sub, 2);
                long start = System.currentTimeMillis();//for the lulz
                MapleData data = null;
                MapleDataProvider dataProvider = MapleDataProviderFactory.getDataProvider(new File("wz/String.wz"));
                if (!sub[1].equalsIgnoreCase("ITEM")) {
                    if (sub[1].equalsIgnoreCase("NPC")) {
                        data = dataProvider.getData("Npc.img");
                    } else if (sub[1].equalsIgnoreCase("MOB") || sub[1].equalsIgnoreCase("MONSTER")) {
                        data = dataProvider.getData("Mob.img");
                    } else if (sub[1].equalsIgnoreCase("SKILL")) {
                        data = dataProvider.getData("Skill.img");
                    } else if (sub[1].equalsIgnoreCase("MAP")) {
                        sb.append("#bUse the '/m' command to find a map. If it finds a map with the same name, it will warp you to it.");
                    } else {
                        sb.append("#bInvalid search.\r\nSyntax: '/search [type] [name]', where [type] is NPC, ITEM, MOB, or SKILL.");
                    }
                    if (data != null) {
                        String name;
                        for (MapleData searchData : data.getChildren()) {
                            name = MapleDataTool.getString(searchData.getChildByPath("name"), "NO-NAME");
                            if (name.toLowerCase().contains(search.toLowerCase())) {
                                sb.append("#b").append(Integer.parseInt(searchData.getName())).append("#k - #r").append(name).append("\r\n");
                            }
                        }
                    }
                } else {
                    for (Pair<Integer, String> itemPair : MapleItemInformationProvider.getInstance().getAllItems()) {
                        if (sb.length() < 32654) {//ohlol
                            if (itemPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                                //#v").append(id).append("# #k- 
                                sb.append("#b").append(itemPair.getLeft()).append("#k - #r").append(itemPair.getRight()).append("\r\n");
                            }
                        } else {
                            sb.append("#bCouldn't load all items, there are too many results.\r\n");
                            break;
                        }
                    }
                }
                if (sb.length() == 0) {
                    sb.append("#bNo ").append(sub[1].toLowerCase()).append("s found.\r\n");
                }

                sb.append("\r\n#kLoaded within ").append((double) (System.currentTimeMillis() - start) / 1000).append(" seconds.");//because I can, and it's free

            } else {
                sb.append("#bInvalid search.\r\nSyntax: '/search [type] [name]', where [type] is NPC, ITEM, MOB, or SKILL.");
            }
            c.announce(MaplePacketCreator.getNPCTalk(9010000, (byte) 0, sb.toString(), "00 00", (byte) 0));
        } else if (sub[0].equals("servermessage")) {
            c.getWorldServer().setServerMessage(joinStringFrom(sub, 1));
        } else if (sub[0].equals("warpsnowball")) {
            for (MapleCharacter chr : player.getMap().getCharacters()) {
                chr.changeMap(109060000, chr.getTeam());
            }
        } else if (sub[0].equals("setall")) {
            final int x = Short.parseShort(sub[1]);
            player.setStr(x);
            player.setDex(x);
            player.setInt(x);
            player.setLuk(x);
            player.updateSingleStat(MapleStat.STR, x);
            player.updateSingleStat(MapleStat.DEX, x);
            player.updateSingleStat(MapleStat.INT, x);
            player.updateSingleStat(MapleStat.LUK, x);
        } else if (sub[0].equals("sp")) {
            player.setRemainingSp(Integer.parseInt(sub[1]));
            player.updateSingleStat(MapleStat.AVAILABLESP, player.getRemainingSp());
        } else if (sub[0].equals("unban")) {
            try {
                try (PreparedStatement p = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET banned = -1 WHERE id = " + MapleCharacter.getIdByName(sub[1]))) {
                    p.executeUpdate();
                }
            } catch (Exception e) {
                player.message("Failed to unban " + sub[1]);
                return true;
            }
            player.message("Unbanned " + sub[1]);
        } else {
            return false;
        }
        return true;
    }

    public static void executeAdminCommand(MapleClient c, String[] sub, char heading) {
        MapleCharacter player = c.getPlayer();
        switch (sub[0]) {
            case "horntail":
                player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8810026), player.getPosition());
                break;
            case "packet":
                player.getMap().broadcastMessage(MaplePacketCreator.customPacket(joinStringFrom(sub, 1)));
                break;
            case "warpworld":
                Server server = Server.getInstance();
                byte worldb = Byte.parseByte(sub[1]);
                if (worldb <= (server.getWorlds().size() - 1)) {
                    try {
                        String[] socket = server.getIP(worldb, c.getChannel()).split(":");
                        c.getWorldServer().removePlayer(player);
                        player.getMap().removePlayer(player);//LOL FORGOT THIS ><                    
                        c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION);
                        player.setWorld(worldb);
                        player.saveToDB();//To set the new world :O (true because else 2 player instances are created, one in both worlds)
                        c.announce(MaplePacketCreator.getChannelChange(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1])));
                    } catch (UnknownHostException | NumberFormatException ex) {
                        player.message("Error when trying to change worlds, are you sure the world you are trying to warp to has the same amount of channels?");
                    }

                } else {
                    player.message("Invalid world; highest number available: " + (server.getWorlds().size() - 1));
                }
                break;
            case "dropinfo":
                if (sub.length != 2) {
                    player.message("Usage: dropinfo <monster_id>");
                    break;
                }

                DecimalFormat dc = new DecimalFormat("0.0#####");

                String dropInfoQuery = "SELECT itemid, chance from drop_data where dropperid = ?;";
                try {
                    try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(dropInfoQuery)) {
                        ps.setString(1, sub[1]);
                        ResultSet rs = ps.executeQuery();
                        while(rs.next()) {
                            String itemName = MapleItemInformationProvider.getInstance().getName(Integer.parseInt(rs.getString(1)));
                            if (itemName != null) {
                                int chance = rs.getInt(2);
                                double adjustedChance = MapleMap.adjustDropChance(chance, player.getDropRate());
                                double probability = adjustedChance / 1000000;
                                player.message("(" + dc.format(probability * 100) + "%) " + itemName);
                            }
                        }
                    }
                    player.message("Done.");
                } catch (Exception e) {
                    player.message("Failed to add drop.");
                }

                break;
            case "saveall"://fyi this is a stupid command
                for (World world : Server.getInstance().getWorlds()) {
                    for (MapleCharacter chr : world.getPlayerStorage().getAllCharacters()) {
                        chr.saveToDB();
                    }
                }
                break;
            case "npc":
                if (sub.length < 1) {
                    break;
                }
                MapleNPC npc = MapleLifeFactory.getNPC(Integer.parseInt(sub[1]));
                if (npc != null) {
                    npc.setPosition(player.getPosition());
                    npc.setCy(player.getPosition().y);
                    npc.setRx0(player.getPosition().x + 50);
                    npc.setRx1(player.getPosition().x - 50);
                    npc.setFh(player.getMap().getFootholds().findBelow(c.getPlayer().getPosition()).getId());
                    player.getMap().addMapObject(npc);
                    player.getMap().broadcastMessage(MaplePacketCreator.spawnNPC(npc));
                }
                break;
            case "adddrop":
                if (sub.length != 4) {
                    player.message("Usage: adddrop <monster_id> <monster_drop> <chance (0-1000000)>");
                    break;
                }

                String addQuery = "INSERT INTO drop_data (dropperid, itemid, chance) values (?, ?, ?);";
                try {
                    double adjustedChance = Integer.parseInt(sub[3]);
                    //double adjustedChance = 1000000 * (1 - Math.pow((1000000 - chance) / 1000000, player.getDropRate()));
                    int chance =  (int) (-1000000 * Math.pow(-adjustedChance / 1000000 + 1, 1.0 / player.getDropRate()) + 1000000);

                    try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(addQuery)) {
                        ps.setString(1, sub[1]);
                        ps.setString(2, sub[2]);
                        ps.setInt(3, chance);
                        ps.executeUpdate();
                    }
                    MapleMonsterInformationProvider.getInstance().reloadDropsForMonster(Integer.parseInt(sub[1]));
                    player.message("Done.");
                } catch (Exception e) {
                    player.message("Failed to add drop.");
                }
                break;
            case "removedrop":
                if (sub.length != 3) {
                    player.message("Usage: removedrop <monster_id> <monster_drop>");
                    break;
                }
                String deleteQuery = "DELETE FROM drop_data where dropperid = ? AND itemid = ?;";
                try {
                    try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(deleteQuery)) {
                        ps.setString(1, sub[1]);
                        ps.setString(2, sub[2]);
                        ps.executeUpdate();
                    }
                    MapleMonsterInformationProvider.getInstance().reloadDropsForMonster(Integer.parseInt(sub[1]));
                    player.message("Done.");
                } catch (Exception e) {
                    player.message("Failed to add drop.");
                }
                break;
            case "jobperson": {
                MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(sub[1]);
                victim.changeJob(MapleJob.getById(Integer.parseInt(sub[2])));
                player.equipChanged();
                break;
            }
            case "pinkbean":
                player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8820009), player.getPosition());
                break;
            case "playernpc":
                player.playerNPC(c.getChannelServer().getPlayerStorage().getCharacterByName(sub[1]), Integer.parseInt(sub[2]));
                break;
            case "setgmlevel": {
                MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(sub[1]);
                victim.setGM(Integer.parseInt(sub[2]));
                player.message("Done.");
                victim.getClient().disconnect(false, false);
                break;
            }
            case "shutdown":
            case "shutdownnow":
                int time = 60000;
                if (sub[0].equals("shutdownnow")) {
                    time = 1;
                } else if (sub.length > 1) {
                    time *= Integer.parseInt(sub[1]);
                }
                TimerManager.getInstance().schedule(Server.getInstance().shutdown(false), time);
                break;
            case "sql": {
                final String query = Commands.joinStringFrom(sub, 1);
                try {
                    try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(query)) {
                        ps.executeUpdate();
                    }
                    player.message("Done " + query);
                } catch (SQLException e) {
                    player.message("Query Failed: " + query);
                }
                break;
            }
            case "sqlwithresult": {
                String name = sub[1];
                final String query = Commands.joinStringFrom(sub, 2);
                try {
                    try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(query); ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            player.dropMessage(String.valueOf(rs.getObject(name)));
                        }
                    }
                } catch (SQLException e) {
                    player.message("Query Failed: " + query);
                }
                break;
            }
            case "itemvac":
                List<MapleMapObject> items = player.getMap().getMapObjectsInRange(player.getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.ITEM));
                for (MapleMapObject item : items) {
                    MapleMapItem mapitem = (MapleMapItem) item;
                    if (!MapleInventoryManipulator.addFromDrop(c, mapitem.getItem(), true)) {
                        continue;
                    }
                    mapitem.setPickedUp(true);
                    player.getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 2, player.getId()), mapitem.getPosition());
                    player.getMap().removeMapObject(item);

                }
                break;
            case "zakum":
                player.getMap().spawnFakeMonsterOnGroundBelow(MapleLifeFactory.getMonster(8800000), player.getPosition());
                for (int x = 8800003; x < 8800011; x++) {
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(x), player.getPosition());
                }
                break;
            default:
                player.yellowMessage("Command " + heading + sub[0] + " does not exist.");
                break;
        }
    }

    private static String joinStringFrom(String arr[], int start) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < arr.length; i++) {
            builder.append(arr[i]);
            if (i != arr.length - 1) {
                builder.append(" ");
            }
        }
        return builder.toString();
    }
}