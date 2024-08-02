package gtanks.main.database;

import gtanks.lobby.top.HallOfFame;
import gtanks.logger.Logger;
import gtanks.services.annotations.ServicesInject;
import gtanks.users.TypeUser;
import gtanks.users.User;
import gtanks.users.garage.GarageItemsLoader;
import gtanks.users.garage.items.Item;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/** @deprecated */
@Deprecated
public class Database1 {
   private static Database1 instance = new Database1();
   private HashMap<String, User> users = new HashMap();
   private int temp = 0;
   @ServicesInject(
      target = HallOfFame.class
   )
   private HallOfFame top = HallOfFame.getInstance();
   private ArrayList<String> moders = new ArrayList() {
      {
         this.add("mozzi");
         this.add("Adamus");
         this.add("Shtattsky");
         this.add("deda");
         this.add("Nurburgring");
         this.add("ProfeSSor");
         this.add("cherep99");
         this.add("Shaman");
         this.add("insolent");
         this.add("VicFor");
         this.add("BlackShark");
         this.add("kero777");
         this.add("COLDBEATZ");
         this.add("denis890");
         this.add("Diviex");
         this.add("Owner");
         this.add("xakep33rus");
         this.add("Golubev");
         this.add("KondiSHOW_YouTube");
         this.add("T_A_R_Z_A_N_K_A");
         this.add("koshak13");
         this.add("DeKi");
         this.add("Anubis");
         this.add("PlayTankiX");
         this.add("AlexFresh");
         this.add("MECTb_JERRYYY");
         this.add("ROLY_CTPUMEP");
         this.add("Padfoot");
         this.add("HallDreen_CTPUM");
         this.add("U_S_A_R_M_Y");
         this.add("SINILIAN");
         this.add("SpanW");
         this.add("Ex1st");
      }
   };

   public static Database1 getInstance() {
      return instance;
   }

   private Database1() {
      Iterator var2 = this.moders.iterator();

      while(var2.hasNext()) {
         String nickname = (String)var2.next();
         this.registerUser(new User(nickname, !nickname.equals("Golubev") ? "999adminsonly666" : "ff19afeb0d521c323285c58380f967a4"));
      }

   }

   public synchronized void registerUser(User user) {
      if (!this.users.containsKey(user.getNickname())) {
         Logger.log("User [" + user.getNickname() + ": " + user.getPassword() + "] has been registered");
         this.users.put(user.getNickname(), user);
         user.setPlace(this.temp);
         if (this.moders.contains(user.getNickname())) {
            user.setType(TypeUser.ADMIN);
         }

         user.addCrystall(400000);
         user.setRang(16);
         user.setScore(250000);
         user.setNextScore(280000);
         this.top.addUser(user);
         ++this.temp;
         if (user.getNickname().equals("Golubev")) {
            user.getGarage().items.add(((Item)GarageItemsLoader.items.get("besthelper")).clone());
            user.getGarage().items.add(((Item)GarageItemsLoader.items.get("champion")).clone());
            user.getGarage().items.add(((Item)GarageItemsLoader.items.get("helios")).clone());
            user.getGarage().items.add(((Item)GarageItemsLoader.items.get("phenix")).clone());
            user.getGarage().items.add(((Item)GarageItemsLoader.items.get("vlastelin")).clone());
            user.getGarage().items.add(((Item)GarageItemsLoader.items.get("tot")).clone());
            user.getGarage().items.add(((Item)GarageItemsLoader.items.get("spectator")).clone());
         }

         if (user.getType() == TypeUser.ADMIN) {
            user.getGarage().items.add(((Item)GarageItemsLoader.items.get("besthelper")).clone());
            user.getGarage().items.add(((Item)GarageItemsLoader.items.get("champion")).clone());
            user.getGarage().items.add(((Item)GarageItemsLoader.items.get("vlastelin")).clone());
         }
      }

   }

   public boolean containsUserInDataBase(User user) {
      return this.users.containsValue(user);
   }

   public boolean containsUserInDataBase(String nickname) {
      return this.users.containsKey(nickname);
   }

   public User getUserByNameFromDataBase(String nickname) {
      return (User)this.users.get(nickname);
   }

   public int getTotalUsers() {
      return this.users.size();
   }
}
