package gtanks.battles.maps;

import gtanks.battles.bonuses.BonusRegion;
import gtanks.battles.maps.themes.MapTheme;
import gtanks.battles.tanks.math.Vector3;
import java.util.ArrayList;

public class Map {
   public String name;
   public String id;
   public String skyboxId;
   public String themeId;
   public MapTheme mapTheme;
   public int minRank;
   public int maxRank;
   public int maxPlayers;
   public boolean tdm = false;
   public boolean ctf = false;
   public double angleX;
   public double angleZ;
   public int lightColor;
   public int shadowColor;
   public double fogAlpha;
   public int fogColor;
   public int ssaoColor;
   public ArrayList<Vector3> spawnPositonsDM = new ArrayList();
   public ArrayList<Vector3> spawnPositonsBlue = new ArrayList();
   public ArrayList<Vector3> spawnPositonsRed = new ArrayList();
   public ArrayList<BonusRegion> goldsRegions = new ArrayList();
   public ArrayList<BonusRegion> crystallsRegions = new ArrayList();
   public ArrayList<BonusRegion> healthsRegions = new ArrayList();
   public ArrayList<BonusRegion> armorsRegions = new ArrayList();
   public ArrayList<BonusRegion> damagesRegions = new ArrayList();
   public ArrayList<BonusRegion> nitrosRegions = new ArrayList();
   public int totalCountDrops;
   public Vector3 flagRedPosition;
   public Vector3 flagBluePosition;
   public String md5Hash;

   public Map() {
   }

   public Map(String name, String id, String skyboxId, ArrayList<Vector3> spawnPositionsDM, ArrayList<Vector3> spawnPositionsBlue, ArrayList<Vector3> spawnPositionsRed, ArrayList<BonusRegion> goldsRegions, ArrayList<BonusRegion> crystallsRegions, ArrayList<BonusRegion> dropRegions, int min, int max, int maxPlayers, boolean tdm, boolean ctf, double angleX, double angleZ, int lightColor, int shadowColor, double fogAlpha, int fogColor, int ssaoColor) {
      this.name = name;
      this.id = id;
      this.skyboxId = skyboxId;
      this.spawnPositonsDM = spawnPositionsDM;
      this.spawnPositonsBlue = spawnPositionsBlue;
      this.spawnPositonsRed = spawnPositionsRed;
      this.goldsRegions = goldsRegions;
      this.crystallsRegions = crystallsRegions;
      this.minRank = min;
      this.maxRank = max;
      this.tdm = tdm;
      this.ctf = ctf;
      this.maxPlayers = maxPlayers;
      this.angleX = angleX;
      this.angleZ = angleZ;
      this.lightColor = lightColor;
      this.shadowColor = shadowColor;
      this.fogAlpha = fogAlpha;
      this.fogColor = fogColor;
      this.ssaoColor = ssaoColor;
   }
}
