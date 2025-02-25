package gtanks.battles;

import gtanks.StringUtils;
import gtanks.battles.anticheats.AnticheatModel;
import gtanks.battles.bonuses.Bonus;
import gtanks.battles.bonuses.BonusesSpawnService;
import gtanks.battles.bonuses.model.BonusTakeModel;
import gtanks.battles.chat.BattlefieldChatModel;
import gtanks.battles.comands.BattlefieldModelComandsConst;
import gtanks.battles.ctf.CTFModel;
import gtanks.battles.effects.model.EffectsVisualizationModel;
import gtanks.battles.managers.SpawnManager;
import gtanks.battles.maps.MapChecksumModel;
import gtanks.battles.mines.model.BattleMinesModel;
import gtanks.battles.spectator.SpectatorController;
import gtanks.battles.spectator.SpectatorModel;
import gtanks.battles.tanks.Tank;
import gtanks.battles.tanks.math.Vector3;
import gtanks.battles.tanks.statistic.PlayersStatisticModel;
import gtanks.battles.timer.schedulers.bonuses.BonusesScheduler;
import gtanks.battles.timer.schedulers.runtime.TankRespawnScheduler;
import gtanks.collections.FastHashMap;
import gtanks.commands.Type;
import gtanks.json.JSONUtils;
import gtanks.lobby.battles.BattleInfo;
import gtanks.lobby.battles.BattlesList;
import gtanks.logger.Logger;
import gtanks.rmi.payments.sfx.SFX;
import gtanks.services.AutoEntryServices;
import gtanks.services.TanksServices;
import gtanks.services.annotations.ServicesInject;
import gtanks.system.BattlesGC;
import gtanks.system.destroy.Destroyable;
import gtanks.system.quartz.QuartzService;
import gtanks.system.quartz.TimeType;
import gtanks.system.quartz.impl.QuartzServiceImpl;
import java.util.HashMap;
import java.util.Iterator;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class BattlefieldModel extends BattlefieldModelComandsConst implements Destroyable {
   public static final String QUARTZ_GROUP = BattlefieldModel.class.getName();
   public final String QUARTZ_NAME;
   public final String QUARTZ_RESTART_NAME;
   @ServicesInject(
      target = TanksServices.class
   )
   private TanksServices tanksServices = TanksServices.getInstance();
   @ServicesInject(
      target = QuartzService.class
   )
   private QuartzService quartzService = QuartzServiceImpl.inject();
   @ServicesInject(
      target = AutoEntryServices.class
   )
   private AutoEntryServices autoEntryServices = AutoEntryServices.instance();
   public FastHashMap<String, BattlefieldPlayerController> players;
   public HashMap<String, Bonus> activeBonuses;
   public BattleInfo battleInfo;
   public int incration = 0;
   private boolean battleFinish = false;
   private long endBattleTime = 0L;
   private JSONParser jsonParser = new JSONParser();
   public PlayersStatisticModel statistics;
   public TankKillModel tanksKillModel;
   public CTFModel ctfModel;
   public BattlefieldChatModel chatModel;
   public BonusesSpawnService bonusesSpawnService;
   public SpectatorModel spectatorModel;
   public EffectsVisualizationModel effectsModel;
   public BonusTakeModel bonusTakeModel;
   public BattleMinesModel battleMinesModel;
   public MapChecksumModel mapChecksumModel;

   public BattlefieldModel(BattleInfo battleInfo) {
      this.battleInfo = battleInfo;
      this.statistics = new PlayersStatisticModel(this);
      this.tanksKillModel = new TankKillModel(this);
      this.chatModel = new BattlefieldChatModel(this);
      this.spectatorModel = new SpectatorModel(this);
      this.effectsModel = new EffectsVisualizationModel(this);
      this.bonusTakeModel = new BonusTakeModel(this);
      this.battleMinesModel = new BattleMinesModel(this);
      this.mapChecksumModel = new MapChecksumModel(this);
      this.QUARTZ_NAME = "TimeBattle " + this.hashCode() + " battle=" + battleInfo.battleId;
      this.QUARTZ_RESTART_NAME = "RestartBattle  battle=" + battleInfo.battleId;
      if (battleInfo.time > 0) {
         this.startTimeBattle();
      }

      if (battleInfo.battleType.equals("CTF")) {
         this.ctfModel = new CTFModel(this);
      }

      this.players = new FastHashMap();
      this.activeBonuses = new HashMap();
      (new Thread(this.bonusesSpawnService = new BonusesSpawnService(this))).start();
      BattlesGC.addBattleForRemove(this);
   }

   private void startTimeBattle() {
      this.endBattleTime = System.currentTimeMillis() + (long)(this.battleInfo.time * 1000);
      this.quartzService.addJob(this.QUARTZ_NAME, QUARTZ_GROUP, (e) -> {
         Logger.debug("battle end...");
         this.tanksKillModel.restartBattle(true);
      }, TimeType.SEC, (long)this.battleInfo.time);
   }

   public void sendTableMessageToPlayers(String msg) {
      this.sendToAllPlayers(Type.BATTLE, "show_warning_table", msg);
   }

   public void battleRestart() {
      if (this.battleInfo.team) {
         this.sendToAllPlayers(Type.BATTLE, "change_team_scores", "RED", String.valueOf(this.battleInfo.scoreRed));
         this.sendToAllPlayers(Type.BATTLE, "change_team_scores", "BLUE", String.valueOf(this.battleInfo.scoreBlue));
      }

      this.battleFinish = false;
      Iterator var2 = this.players.values().iterator();

      while(var2.hasNext()) {
         BattlefieldPlayerController player = (BattlefieldPlayerController)var2.next();
         if (player != null) {
            player.statistic.clear();
            player.clearEffects();
            this.respawnPlayer(player, false);
         }
      }

      long currentTimeMillis = System.currentTimeMillis();
      int prepareTimeLeft = (int)((currentTimeMillis + (long)(this.battleInfo.time * 1000) - currentTimeMillis) / 1000L);
      this.sendToAllPlayers(Type.BATTLE, "battle_restart", String.valueOf(prepareTimeLeft));
      if (this.battleInfo.time > 0) {
         this.startTimeBattle();
      }

   }

   public void battleFinish() {
      if (this.players != null) {
         this.battleFinish = true;
         if (this.activeBonuses != null) {
            this.activeBonuses.clear();
         }

         this.bonusesSpawnService.battleFinished();
         this.tanksKillModel.setBattleFund(0);
         this.battleInfo.scoreBlue = 0;
         this.battleInfo.scoreRed = 0;
         Iterator var2 = this.players.values().iterator();

         while(var2.hasNext()) {
            BattlefieldPlayerController player = (BattlefieldPlayerController)var2.next();
            if (player != null) {
               TankRespawnScheduler.cancelRespawn(player);
            }
         }

         this.autoEntryServices.battleRestarted(this);
      }
   }

   public int getTimeLeft() {
      return (int)((this.endBattleTime - System.currentTimeMillis()) / 1000L);
   }

   public void fire(BattlefieldPlayerController tank, String json) {
      this.sendToAllPlayers(tank, Type.BATTLE, "fire", tank.tank.id, json);
   }

   public void startFire(BattlefieldPlayerController tank) {
      this.sendToAllPlayers(tank, Type.BATTLE, "start_fire", tank.tank.id);
   }

   public void quickFire(BattlefieldPlayerController tank, String json) {
      this.sendToAllPlayers(tank, Type.BATTLE, "shaft_quick_shot", tank.tank.id, json);
   }

   public void stopFire(BattlefieldPlayerController tank) {
      this.sendToAllPlayers(tank, Type.BATTLE, "stop_fire", tank.tank.id);
   }

   public synchronized void onTakeBonus(BattlefieldPlayerController controller, String json) {
      try {
         JSONObject obj = (JSONObject)this.jsonParser.parse(json);
         JSONObject posObj = (JSONObject)obj.get("real_tank_position");
         String bonusId = (String)obj.get("bonus_id");
         Vector3 realPosTank = new Vector3((float)(double)posObj.get("x"), (float)(double)posObj.get("y"), (float)(double)posObj.get("z"));
         Bonus bonus = (Bonus)this.activeBonuses.get(bonusId);
         if (bonus == null) {
            return;
         }

         if (this.bonusTakeModel.onTakeBonus(bonus, realPosTank, controller)) {
            this.sendToAllPlayers(Type.BATTLE, "take_bonus_by", bonusId);
            this.activeBonuses.remove(bonusId);
         }
      } catch (Exception var8) {
         var8.printStackTrace();
      }

   }

   public void spawnBonus(Bonus bonus, int inc, int disappearingTime) {
      if (bonus.position.x != 0.0F || bonus.position.y != 0.0F || bonus.position.z != 0.0F) {
         String id = StringUtils.concatStrings(bonus.type.toString(), "_", String.valueOf(inc));
         this.activeBonuses.put(id, bonus);
         BonusesScheduler.runRemoveTask(this, id, (long)disappearingTime);
         this.sendToAllPlayers(Type.BATTLE, "spawn_bonus", JSONUtils.parseBonusInfo(bonus, inc, disappearingTime));
      }
   }

   public void respawnPlayer(BattlefieldPlayerController controller, boolean kill) {
      if (!this.battleFinish) {
         controller.send(Type.BATTLE, "local_user_killed");
         this.battleMinesModel.playerDied(controller);
         if (kill) {
            controller.clearEffects();
            this.sendToAllPlayers(Type.BATTLE, "kill_tank", controller.tank.id, "suicide");
            controller.statistic.addDeaths();
            this.statistics.changeStatistic(controller);
            if (this.ctfModel != null && controller.flag != null) {
               this.ctfModel.dropFlag(controller, controller.tank.position);
            }
         }

         controller.tank.state = "suicide";
         TankRespawnScheduler.startRespawn(controller, false);
      }
   }

   public void moveTank(BattlefieldPlayerController controller) {
      String json = JSONUtils.parseMoveCommand(controller);
      this.sendToAllPlayers(Type.BATTLE, "move", json);
   }

   public void spawnPlayer(BattlefieldPlayerController controller, Vector3 pos) {
      if (!this.battleFinish) {
         TankRespawnScheduler.startRespawn(controller, true);
      }
   }

   public void setupTank(BattlefieldPlayerController controller) {
      controller.tank.id = controller.parentLobby.getLocalUser().getNickname();
   }

   public void addPlayer(BattlefieldPlayerController controller) {
      this.setupTank(controller);
      this.players.put(controller.tank.id, controller);
      ++this.incration;
      BattlesGC.cancelRemoving(this);
   }

   public void removeUser(BattlefieldPlayerController controller, boolean cache) {
      controller.clearEffects();
      this.battleMinesModel.playerDied(controller);
      this.players.remove(controller.parentLobby.getLocalUser().getNickname(), controller);
      if (!cache) {
         if (!this.battleInfo.team) {
            --BattlesList.getBattleInfoById(this.battleInfo.battleId).countPeople;
         } else if (controller.playerTeamType.equals("RED")) {
            --BattlesList.getBattleInfoById(this.battleInfo.battleId).redPeople;
         } else {
            --BattlesList.getBattleInfoById(this.battleInfo.battleId).bluePeople;
         }
      }

      if (this.ctfModel != null && controller.flag != null) {
         this.ctfModel.dropFlag(controller, controller.tank.position);
      }

      this.sendToAllPlayers(Type.BATTLE, "remove_user", controller.tank.id);
      if (this.players.size() == 0) {
         BattlesGC.addBattleForRemove(this);
      }

   }

   public void initLocalTank(BattlefieldPlayerController controller) {
      controller.userInited = true;
      Vector3 pos = SpawnManager.getSpawnState(this.battleInfo.map, controller.playerTeamType);
      if (this.battleInfo.battleType.equals("CTF")) {
         controller.send(Type.BATTLE, "init_ctf_model", JSONUtils.parseCTFModelData(this));
      }

      controller.send(Type.BATTLE, "init_gui_model", JSONUtils.parseBattleData(this));
      controller.inventory.init();
      this.battleMinesModel.initModel(controller);
      this.battleMinesModel.sendMines(controller);
      this.initWeaponsLightning(controller);
      this.sendAllTanks(controller);
      this.sendToAllPlayers(Type.BATTLE, "init_tank", JSONUtils.parseTankData(this, controller, controller.parentLobby.getLocalUser().getGarage(), pos, true, this.incration, controller.tank.id, controller.parentLobby.getLocalUser().getNickname(), controller.parentLobby.getLocalUser().getRang()));
      this.statistics.changeStatistic(controller);
      this.effectsModel.sendInitData(controller);
      this.spawnPlayer(controller, pos);
   }

   public void sendUserLogMessage(String idUser, String message) {
      this.sendToAllPlayers(Type.BATTLE, "user_log", idUser, message);
   }

   public void initWeaponsLightning(BattlefieldPlayerController controller) {
      try {
         controller.send(Type.BATTLE, "init_sfx_data", SFX.getSFX());
      } catch (Exception e) {
         System.out.println(e.getMessage());
      }
   }

   public void sendAllTanks(BattlefieldPlayerController controller) {
      Iterator var3 = this.players.values().iterator();

      while(var3.hasNext()) {
         BattlefieldPlayerController player = (BattlefieldPlayerController)var3.next();
         if (player != controller && player.userInited) {
            controller.send(Type.BATTLE, "init_tank", JSONUtils.parseTankData(this, player, player.parentLobby.getLocalUser().getGarage(), player.tank.position, false, this.incration, player.tank.id, player.parentLobby.getLocalUser().getNickname(), player.parentLobby.getLocalUser().getRang()));
            this.statistics.changeStatistic(player);
         }
      }

   }

   public void sendAllTanks(SpectatorController controller) {
      Iterator var3 = this.players.values().iterator();

      while(var3.hasNext()) {
         BattlefieldPlayerController player = (BattlefieldPlayerController)var3.next();
         if (player.userInited) {
            controller.sendCommand(Type.BATTLE, "init_tank", JSONUtils.parseTankData(this, player, player.parentLobby.getLocalUser().getGarage(), player.tank.position, false, this.incration, player.tank.id, player.parentLobby.getLocalUser().getNickname(), player.parentLobby.getLocalUser().getRang()));
            this.statistics.changeStatistic(player);
         }
      }

   }

   public void activateTank(BattlefieldPlayerController player) {
      player.tank.state = "active";
      this.sendToAllPlayers(Type.BATTLE, "activate_tank", player.tank.id);
   }

   public BattlefieldPlayerController getPlayer(String id) {
      return (BattlefieldPlayerController)this.players.get(id);
   }

   public void sendToAllPlayers(Type type, String... args) {
      if (this.players != null) {
         if (this.players.size() != 0) {
            Iterator var4 = this.players.values().iterator();

            while(var4.hasNext()) {
               BattlefieldPlayerController player = (BattlefieldPlayerController)var4.next();
               if (player.userInited) {
                  player.send(type, args);
               }
            }
         }

         this.spectatorModel.sendCommandToSpectators(type, args);
      }
   }

   public void sendToAllPlayers(BattlefieldPlayerController other, Type type, String... args) {
      if (this.players.size() != 0) {
         Iterator var5 = this.players.values().iterator();

         while(var5.hasNext()) {
            BattlefieldPlayerController player = (BattlefieldPlayerController)var5.next();
            if (player.userInited && player != other) {
               player.send(type, args);
            }
         }
      }

      this.spectatorModel.sendCommandToSpectators(type, args);
   }

   public void cheatDetected(BattlefieldPlayerController player, Class<?> anticheat) {
      AnticheatModel[] model = (AnticheatModel[])anticheat.getAnnotationsByType(AnticheatModel.class);
      if (model != null && model.length >= 1) {
         Logger.log("Detected cheater![" + model[0].name() + "] " + player.getUser().getNickname() + " " + player.parentLobby.networker.getIP());
      }

      this.kickPlayer(player);
   }

   public void kickPlayer(BattlefieldPlayerController player) {
      player.send(Type.BATTLE, "kick_for_cheats");
      player.parentLobby.networker.closeConnection();
   }

   public void setTank(BattlefieldPlayerController player, Tank newTank) {
      ((BattlefieldPlayerController)this.players.get(player.parentLobby.getLocalUser().getNickname())).tank = newTank;
   }

   public void destroy() {
      this.players.clear();
      this.activeBonuses.clear();
      this.quartzService.deleteJob(this.QUARTZ_NAME, QUARTZ_GROUP);
      this.tanksKillModel.destroy();
      this.tanksKillModel = null;
      this.players = null;
      this.activeBonuses = null;
      this.battleInfo = null;
      this.spectatorModel = null;
   }
}
