package net.minecraft.advancements;

import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonSyntaxException;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;

public record AdvancementRequirements(String[][] requirements) {
   public static final AdvancementRequirements EMPTY = new AdvancementRequirements(new String[0][]);

   public AdvancementRequirements(FriendlyByteBuf pBuffer) {
      this(read(pBuffer));
   }

   private static String[][] read(FriendlyByteBuf pBuffer) {
      String[][] astring = new String[pBuffer.readVarInt()][];

      for(int i = 0; i < astring.length; ++i) {
         astring[i] = new String[pBuffer.readVarInt()];

         for(int j = 0; j < astring[i].length; ++j) {
            astring[i][j] = pBuffer.readUtf();
         }
      }

      return astring;
   }

   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeVarInt(this.requirements.length);

      for(String[] astring : this.requirements) {
         pBuffer.writeVarInt(astring.length);

         for(String s : astring) {
            pBuffer.writeUtf(s);
         }
      }

   }

   public static AdvancementRequirements allOf(Collection<String> pRequirements) {
      return new AdvancementRequirements(pRequirements.stream().map((p_298440_) -> {
         return new String[]{p_298440_};
      }).toArray((p_301148_) -> {
         return new String[p_301148_][];
      }));
   }

   public static AdvancementRequirements anyOf(Collection<String> pCriteria) {
      return new AdvancementRequirements(new String[][]{pCriteria.toArray((p_299867_) -> {
         return new String[p_299867_];
      })});
   }

   public int size() {
      return this.requirements.length;
   }

   public boolean test(Predicate<String> pPredicate) {
      if (this.requirements.length == 0) {
         return false;
      } else {
         for(String[] astring : this.requirements) {
            if (!anyMatch(astring, pPredicate)) {
               return false;
            }
         }

         return true;
      }
   }

   public int count(Predicate<String> pFilter) {
      int i = 0;

      for(String[] astring : this.requirements) {
         if (anyMatch(astring, pFilter)) {
            ++i;
         }
      }

      return i;
   }

   private static boolean anyMatch(String[] pRequirement, Predicate<String> pPredicate) {
      for(String s : pRequirement) {
         if (pPredicate.test(s)) {
            return true;
         }
      }

      return false;
   }

   public static AdvancementRequirements fromJson(JsonArray pJson, Set<String> pCriteria) {
      String[][] astring = new String[pJson.size()][];
      Set<String> set = new ObjectOpenHashSet<>();

      for(int i = 0; i < pJson.size(); ++i) {
         JsonArray jsonarray = GsonHelper.convertToJsonArray(pJson.get(i), "requirements[" + i + "]");
         if (jsonarray.isEmpty() && pCriteria.isEmpty()) {
            throw new JsonSyntaxException("Requirement entry cannot be empty");
         }

         astring[i] = new String[jsonarray.size()];

         for(int j = 0; j < jsonarray.size(); ++j) {
            String s = GsonHelper.convertToString(jsonarray.get(j), "requirements[" + i + "][" + j + "]");
            astring[i][j] = s;
            set.add(s);
         }
      }

      if (!pCriteria.equals(set)) {
         Set<String> set1 = Sets.difference(pCriteria, set);
         Set<String> set2 = Sets.difference(set, pCriteria);
         throw new JsonSyntaxException("Advancement completion requirements did not exactly match specified criteria. Missing: " + set1 + ". Unknown: " + set2);
      } else {
         return new AdvancementRequirements(astring);
      }
   }

   public JsonArray toJson() {
      JsonArray jsonarray = new JsonArray();

      for(String[] astring : this.requirements) {
         JsonArray jsonarray1 = new JsonArray();
         Arrays.stream(astring).forEach(jsonarray1::add);
         jsonarray.add(jsonarray1);
      }

      return jsonarray;
   }

   public boolean isEmpty() {
      return this.requirements.length == 0;
   }

   public String toString() {
      return Arrays.deepToString(this.requirements);
   }

   public Set<String> names() {
      Set<String> set = new ObjectOpenHashSet<>();

      for(String[] astring : this.requirements) {
         Collections.addAll(set, astring);
      }

      return set;
   }

   public interface Strategy {
      AdvancementRequirements.Strategy AND = AdvancementRequirements::allOf;
      AdvancementRequirements.Strategy OR = AdvancementRequirements::anyOf;

      AdvancementRequirements create(Collection<String> pCriteria);
   }
}