package net.minecraft.world.scores;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.slf4j.Logger;

public class Scoreboard {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Map<String, Objective> objectivesByName = Maps.newHashMap();
   private final Map<ObjectiveCriteria, List<Objective>> objectivesByCriteria = Maps.newHashMap();
   private final Map<String, Map<Objective, Score>> playerScores = Maps.newHashMap();
   private final Map<DisplaySlot, Objective> displayObjectives = new EnumMap<>(DisplaySlot.class);
   private final Map<String, PlayerTeam> teamsByName = Maps.newHashMap();
   private final Map<String, PlayerTeam> teamsByPlayer = Maps.newHashMap();

   /**
    * Returns a ScoreObjective for the objective name
    */
   @Nullable
   public Objective getObjective(@Nullable String pName) {
      return this.objectivesByName.get(pName);
   }

   public Objective addObjective(String pName, ObjectiveCriteria pCriteria, Component pDisplayName, ObjectiveCriteria.RenderType pRenderType) {
      if (this.objectivesByName.containsKey(pName)) {
         throw new IllegalArgumentException("An objective with the name '" + pName + "' already exists!");
      } else {
         Objective objective = new Objective(this, pName, pCriteria, pDisplayName, pRenderType);
         this.objectivesByCriteria.computeIfAbsent(pCriteria, (p_83426_) -> {
            return Lists.newArrayList();
         }).add(objective);
         this.objectivesByName.put(pName, objective);
         this.onObjectiveAdded(objective);
         return objective;
      }
   }

   public final void forAllObjectives(ObjectiveCriteria pCriteria, String pScoreboardName, Consumer<Score> pAction) {
      this.objectivesByCriteria.getOrDefault(pCriteria, Collections.emptyList()).forEach((p_83444_) -> {
         pAction.accept(this.getOrCreatePlayerScore(pScoreboardName, p_83444_));
      });
   }

   /**
    * Returns if the entity has the given ScoreObjective
    */
   public boolean hasPlayerScore(String pName, Objective pObjective) {
      Map<Objective, Score> map = this.playerScores.get(pName);
      if (map == null) {
         return false;
      } else {
         Score score = map.get(pObjective);
         return score != null;
      }
   }

   /**
    * Get a player's score or create it if it does not exist
    */
   public Score getOrCreatePlayerScore(String pUsername, Objective pObjective) {
      Map<Objective, Score> map = this.playerScores.computeIfAbsent(pUsername, (p_83507_) -> {
         return Maps.newHashMap();
      });
      return map.computeIfAbsent(pObjective, (p_83487_) -> {
         Score score = new Score(this, p_83487_, pUsername);
         score.setScore(0);
         return score;
      });
   }

   /**
    * Returns an array of Score objects, sorting by Score.getScorePoints()
    */
   public Collection<Score> getPlayerScores(Objective pObjective) {
      List<Score> list = Lists.newArrayList();

      for(Map<Objective, Score> map : this.playerScores.values()) {
         Score score = map.get(pObjective);
         if (score != null) {
            list.add(score);
         }
      }

      list.sort(Score.SCORE_COMPARATOR);
      return list;
   }

   public Collection<Objective> getObjectives() {
      return this.objectivesByName.values();
   }

   public Collection<String> getObjectiveNames() {
      return this.objectivesByName.keySet();
   }

   public Collection<String> getTrackedPlayers() {
      return Lists.newArrayList(this.playerScores.keySet());
   }

   /**
    * Remove the given ScoreObjective for the given Entity name.
    */
   public void resetPlayerScore(String pName, @Nullable Objective pObjective) {
      if (pObjective == null) {
         Map<Objective, Score> map = this.playerScores.remove(pName);
         if (map != null) {
            this.onPlayerRemoved(pName);
         }
      } else {
         Map<Objective, Score> map2 = this.playerScores.get(pName);
         if (map2 != null) {
            Score score = map2.remove(pObjective);
            if (map2.size() < 1) {
               Map<Objective, Score> map1 = this.playerScores.remove(pName);
               if (map1 != null) {
                  this.onPlayerRemoved(pName);
               }
            } else if (score != null) {
               this.onPlayerScoreRemoved(pName, pObjective);
            }
         }
      }

   }

   /**
    * Returns all the objectives for the given entity
    */
   public Map<Objective, Score> getPlayerScores(String pName) {
      Map<Objective, Score> map = this.playerScores.get(pName);
      if (map == null) {
         map = Maps.newHashMap();
      }

      return map;
   }

   public void removeObjective(Objective pObjective) {
      this.objectivesByName.remove(pObjective.getName());

      for(DisplaySlot displayslot : DisplaySlot.values()) {
         if (this.getDisplayObjective(displayslot) == pObjective) {
            this.setDisplayObjective(displayslot, (Objective)null);
         }
      }

      List<Objective> list = this.objectivesByCriteria.get(pObjective.getCriteria());
      if (list != null) {
         list.remove(pObjective);
      }

      for(Map<Objective, Score> map : this.playerScores.values()) {
         map.remove(pObjective);
      }

      this.onObjectiveRemoved(pObjective);
   }

   public void setDisplayObjective(DisplaySlot pSlot, @Nullable Objective pObjective) {
      this.displayObjectives.put(pSlot, pObjective);
   }

   @Nullable
   public Objective getDisplayObjective(DisplaySlot pSlot) {
      return this.displayObjectives.get(pSlot);
   }

   /**
    * Retrieve the ScorePlayerTeam instance identified by the passed team name
    */
   @Nullable
   public PlayerTeam getPlayerTeam(String pTeamName) {
      return this.teamsByName.get(pTeamName);
   }

   public PlayerTeam addPlayerTeam(String pName) {
      PlayerTeam playerteam = this.getPlayerTeam(pName);
      if (playerteam != null) {
         LOGGER.warn("Requested creation of existing team '{}'", (Object)pName);
         return playerteam;
      } else {
         playerteam = new PlayerTeam(this, pName);
         this.teamsByName.put(pName, playerteam);
         this.onTeamAdded(playerteam);
         return playerteam;
      }
   }

   /**
    * Removes the team from the scoreboard, updates all player memberships and broadcasts the deletion to all players
    */
   public void removePlayerTeam(PlayerTeam pPlayerTeam) {
      this.teamsByName.remove(pPlayerTeam.getName());

      for(String s : pPlayerTeam.getPlayers()) {
         this.teamsByPlayer.remove(s);
      }

      this.onTeamRemoved(pPlayerTeam);
   }

   public boolean addPlayerToTeam(String pPlayerName, PlayerTeam pTeam) {
      if (this.getPlayersTeam(pPlayerName) != null) {
         this.removePlayerFromTeam(pPlayerName);
      }

      this.teamsByPlayer.put(pPlayerName, pTeam);
      return pTeam.getPlayers().add(pPlayerName);
   }

   public boolean removePlayerFromTeam(String pPlayerName) {
      PlayerTeam playerteam = this.getPlayersTeam(pPlayerName);
      if (playerteam != null) {
         this.removePlayerFromTeam(pPlayerName, playerteam);
         return true;
      } else {
         return false;
      }
   }

   /**
    * Removes the given username from the given ScorePlayerTeam. If the player is not on the team then an
    * IllegalStateException is thrown.
    */
   public void removePlayerFromTeam(String pUsername, PlayerTeam pPlayerTeam) {
      if (this.getPlayersTeam(pUsername) != pPlayerTeam) {
         throw new IllegalStateException("Player is either on another team or not on any team. Cannot remove from team '" + pPlayerTeam.getName() + "'.");
      } else {
         this.teamsByPlayer.remove(pUsername);
         pPlayerTeam.getPlayers().remove(pUsername);
      }
   }

   /**
    * Retrieve all registered ScorePlayerTeam names
    */
   public Collection<String> getTeamNames() {
      return this.teamsByName.keySet();
   }

   /**
    * Retrieve all registered ScorePlayerTeam instances
    */
   public Collection<PlayerTeam> getPlayerTeams() {
      return this.teamsByName.values();
   }

   /**
    * Gets the ScorePlayerTeam object for the given username.
    */
   @Nullable
   public PlayerTeam getPlayersTeam(String pUsername) {
      return this.teamsByPlayer.get(pUsername);
   }

   public void onObjectiveAdded(Objective pObjective) {
   }

   public void onObjectiveChanged(Objective pObjective) {
   }

   public void onObjectiveRemoved(Objective pObjective) {
   }

   public void onScoreChanged(Score pScore) {
   }

   public void onPlayerRemoved(String pScoreName) {
   }

   public void onPlayerScoreRemoved(String pScoreName, Objective pObjective) {
   }

   public void onTeamAdded(PlayerTeam pPlayerTeam) {
   }

   public void onTeamChanged(PlayerTeam pPlayerTeam) {
   }

   public void onTeamRemoved(PlayerTeam pPlayerTeam) {
   }

   public void entityRemoved(Entity pEntity) {
      if (!(pEntity instanceof Player) && !pEntity.isAlive()) {
         String s = pEntity.getStringUUID();
         this.resetPlayerScore(s, (Objective)null);
         this.removePlayerFromTeam(s);
      }
   }

   protected ListTag savePlayerScores() {
      ListTag listtag = new ListTag();
      this.playerScores.values().stream().map(Map::values).forEach((p_297210_) -> {
         p_297210_.forEach((p_166096_) -> {
            CompoundTag compoundtag = new CompoundTag();
            compoundtag.putString("Name", p_166096_.getOwner());
            compoundtag.putString("Objective", p_166096_.getObjective().getName());
            compoundtag.putInt("Score", p_166096_.getScore());
            compoundtag.putBoolean("Locked", p_166096_.isLocked());
            listtag.add(compoundtag);
         });
      });
      return listtag;
   }

   protected void loadPlayerScores(ListTag pTag) {
      for(int i = 0; i < pTag.size(); ++i) {
         CompoundTag compoundtag = pTag.getCompound(i);
         String s = compoundtag.getString("Name");
         String s1 = compoundtag.getString("Objective");
         Objective objective = this.getObjective(s1);
         if (objective == null) {
            LOGGER.error("Unknown objective {} for name {}, ignoring", s1, s);
         } else {
            Score score = this.getOrCreatePlayerScore(s, objective);
            score.setScore(compoundtag.getInt("Score"));
            if (compoundtag.contains("Locked")) {
               score.setLocked(compoundtag.getBoolean("Locked"));
            }
         }
      }

   }
}