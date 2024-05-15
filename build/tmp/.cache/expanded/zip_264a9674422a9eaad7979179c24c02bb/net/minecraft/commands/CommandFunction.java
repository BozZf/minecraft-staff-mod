package net.minecraft.commands;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerFunctionManager;

public class CommandFunction {
   private final CommandFunction.Entry[] entries;
   final ResourceLocation id;

   public CommandFunction(ResourceLocation pId, CommandFunction.Entry[] pEntries) {
      this.id = pId;
      this.entries = pEntries;
   }

   public ResourceLocation getId() {
      return this.id;
   }

   public CommandFunction.Entry[] getEntries() {
      return this.entries;
   }

   public CommandFunction instantiate(@Nullable CompoundTag pArguments, CommandDispatcher<CommandSourceStack> pDispatcher, CommandSourceStack pSource) throws FunctionInstantiationException {
      return this;
   }

   private static boolean shouldConcatenateNextLine(CharSequence pLine) {
      int i = pLine.length();
      return i > 0 && pLine.charAt(i - 1) == '\\';
   }

   public static CommandFunction fromLines(ResourceLocation pId, CommandDispatcher<CommandSourceStack> pDispatcher, CommandSourceStack pSource, List<String> pLines) {
      List<CommandFunction.Entry> list = new ArrayList<>(pLines.size());
      Set<String> set = new ObjectArraySet<>();

      for(int i = 0; i < pLines.size(); ++i) {
         int j = i + 1;
         String s = pLines.get(i).trim();
         String s1;
         if (shouldConcatenateNextLine(s)) {
            StringBuilder stringbuilder = new StringBuilder(s);

            do {
               ++i;
               if (i == pLines.size()) {
                  throw new IllegalArgumentException("Line continuation at end of file");
               }

               stringbuilder.deleteCharAt(stringbuilder.length() - 1);
               String s2 = pLines.get(i).trim();
               stringbuilder.append(s2);
            } while(shouldConcatenateNextLine(stringbuilder));

            s1 = stringbuilder.toString();
         } else {
            s1 = s;
         }

         StringReader stringreader = new StringReader(s1);
         if (stringreader.canRead() && stringreader.peek() != '#') {
            if (stringreader.peek() == '/') {
               stringreader.skip();
               if (stringreader.peek() == '/') {
                  throw new IllegalArgumentException("Unknown or invalid command '" + s1 + "' on line " + j + " (if you intended to make a comment, use '#' not '//')");
               }

               String s3 = stringreader.readUnquotedString();
               throw new IllegalArgumentException("Unknown or invalid command '" + s1 + "' on line " + j + " (did you mean '" + s3 + "'? Do not use a preceding forwards slash.)");
            }

            if (stringreader.peek() == '$') {
               CommandFunction.MacroEntry commandfunction$macroentry = decomposeMacro(s1.substring(1), j);
               list.add(commandfunction$macroentry);
               set.addAll(commandfunction$macroentry.parameters());
            } else {
               try {
                  ParseResults<CommandSourceStack> parseresults = pDispatcher.parse(stringreader, pSource);
                  if (parseresults.getReader().canRead()) {
                     throw Commands.getParseException(parseresults);
                  }

                  list.add(new CommandFunction.CommandEntry(parseresults));
               } catch (CommandSyntaxException commandsyntaxexception) {
                  throw new IllegalArgumentException("Whilst parsing command on line " + j + ": " + commandsyntaxexception.getMessage());
               }
            }
         }
      }

      return (CommandFunction)(set.isEmpty() ? new CommandFunction(pId, list.toArray((p_299604_) -> {
         return new CommandFunction.Entry[p_299604_];
      })) : new CommandFunction.CommandMacro(pId, list.toArray((p_299648_) -> {
         return new CommandFunction.Entry[p_299648_];
      }), List.copyOf(set)));
   }

   @VisibleForTesting
   public static CommandFunction.MacroEntry decomposeMacro(String pLine, int pLineNumber) {
      ImmutableList.Builder<String> builder = ImmutableList.builder();
      ImmutableList.Builder<String> builder1 = ImmutableList.builder();
      int i = pLine.length();
      int j = 0;
      int k = pLine.indexOf(36);

      while(k != -1) {
         if (k != i - 1 && pLine.charAt(k + 1) == '(') {
            builder.add(pLine.substring(j, k));
            int l = pLine.indexOf(41, k + 1);
            if (l == -1) {
               throw new IllegalArgumentException("Unterminated macro variable in macro '" + pLine + "' on line " + pLineNumber);
            }

            String s = pLine.substring(k + 2, l);
            if (!isValidVariableName(s)) {
               throw new IllegalArgumentException("Invalid macro variable name '" + s + "' on line " + pLineNumber);
            }

            builder1.add(s);
            j = l + 1;
            k = pLine.indexOf(36, j);
         } else {
            k = pLine.indexOf(36, k + 1);
         }
      }

      if (j == 0) {
         throw new IllegalArgumentException("Macro without variables on line " + pLineNumber);
      } else {
         if (j != i) {
            builder.add(pLine.substring(j));
         }

         return new CommandFunction.MacroEntry(builder.build(), builder1.build());
      }
   }

   private static boolean isValidVariableName(String pVariableName) {
      for(int i = 0; i < pVariableName.length(); ++i) {
         char c0 = pVariableName.charAt(i);
         if (!Character.isLetterOrDigit(c0) && c0 != '_') {
            return false;
         }
      }

      return true;
   }

   public static class CacheableFunction {
      public static final CommandFunction.CacheableFunction NONE = new CommandFunction.CacheableFunction((ResourceLocation)null);
      @Nullable
      private final ResourceLocation id;
      private boolean resolved;
      private Optional<CommandFunction> function = Optional.empty();

      public CacheableFunction(@Nullable ResourceLocation pId) {
         this.id = pId;
      }

      public CacheableFunction(CommandFunction pFunction) {
         this.resolved = true;
         this.id = null;
         this.function = Optional.of(pFunction);
      }

      public Optional<CommandFunction> get(ServerFunctionManager pFunctionManager) {
         if (!this.resolved) {
            if (this.id != null) {
               this.function = pFunctionManager.get(this.id);
            }

            this.resolved = true;
         }

         return this.function;
      }

      @Nullable
      public ResourceLocation getId() {
         return this.function.map((p_78001_) -> {
            return p_78001_.id;
         }).orElse(this.id);
      }
   }

   public static class CommandEntry implements CommandFunction.Entry {
      private final ParseResults<CommandSourceStack> parse;

      public CommandEntry(ParseResults<CommandSourceStack> pParse) {
         this.parse = pParse;
      }

      public void execute(ServerFunctionManager pFunctionManager, CommandSourceStack pSource, Deque<ServerFunctionManager.QueuedCommand> pQueuedCommands, int pCommandLimit, int pDepth, @Nullable ServerFunctionManager.TraceCallbacks pTracer) throws CommandSyntaxException {
         if (pTracer != null) {
            String s = this.parse.getReader().getString();
            pTracer.onCommand(pDepth, s);
            int i = this.execute(pFunctionManager, pSource);
            pTracer.onReturn(pDepth, s, i);
         } else {
            this.execute(pFunctionManager, pSource);
         }

      }

      private int execute(ServerFunctionManager pFunctionManager, CommandSourceStack pSource) throws CommandSyntaxException {
         return pFunctionManager.getDispatcher().execute(Commands.mapSource(this.parse, (p_242934_) -> {
            return pSource;
         }));
      }

      public String toString() {
         return this.parse.getReader().getString();
      }
   }

   static class CommandMacro extends CommandFunction {
      private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#");
      private final List<String> parameters;
      private static final int MAX_CACHE_ENTRIES = 8;
      private final Object2ObjectLinkedOpenHashMap<List<String>, CommandFunction> cache = new Object2ObjectLinkedOpenHashMap<>(8, 0.25F);

      public CommandMacro(ResourceLocation pId, CommandFunction.Entry[] pEntries, List<String> pParameters) {
         super(pId, pEntries);
         this.parameters = pParameters;
      }

      public CommandFunction instantiate(@Nullable CompoundTag pArguments, CommandDispatcher<CommandSourceStack> pDispatcher, CommandSourceStack pSource) throws FunctionInstantiationException {
         if (pArguments == null) {
            throw new FunctionInstantiationException(Component.translatable("commands.function.error.missing_arguments", this.getId()));
         } else {
            List<String> list = new ArrayList<>(this.parameters.size());

            for(String s : this.parameters) {
               if (!pArguments.contains(s)) {
                  throw new FunctionInstantiationException(Component.translatable("commands.function.error.missing_argument", this.getId(), s));
               }

               list.add(stringify(pArguments.get(s)));
            }

            CommandFunction commandfunction = this.cache.getAndMoveToLast(list);
            if (commandfunction != null) {
               return commandfunction;
            } else {
               if (this.cache.size() >= 8) {
                  this.cache.removeFirst();
               }

               CommandFunction commandfunction1 = this.substituteAndParse(list, pDispatcher, pSource);
               if (commandfunction1 != null) {
                  this.cache.put(list, commandfunction1);
               }

               return commandfunction1;
            }
         }
      }

      private static String stringify(Tag pTag) {
         if (pTag instanceof FloatTag floattag) {
            return DECIMAL_FORMAT.format((double)floattag.getAsFloat());
         } else if (pTag instanceof DoubleTag doubletag) {
            return DECIMAL_FORMAT.format(doubletag.getAsDouble());
         } else if (pTag instanceof ByteTag bytetag) {
            return String.valueOf((int)bytetag.getAsByte());
         } else if (pTag instanceof ShortTag shorttag) {
            return String.valueOf((int)shorttag.getAsShort());
         } else if (pTag instanceof LongTag longtag) {
            return String.valueOf(longtag.getAsLong());
         } else {
            return pTag.getAsString();
         }
      }

      private CommandFunction substituteAndParse(List<String> pArguments, CommandDispatcher<CommandSourceStack> pDispatcher, CommandSourceStack pSource) throws FunctionInstantiationException {
         CommandFunction.Entry[] acommandfunction$entry = this.getEntries();
         CommandFunction.Entry[] acommandfunction$entry1 = new CommandFunction.Entry[acommandfunction$entry.length];

         for(int i = 0; i < acommandfunction$entry.length; ++i) {
            CommandFunction.Entry commandfunction$entry = acommandfunction$entry[i];
            if (!(commandfunction$entry instanceof CommandFunction.MacroEntry commandfunction$macroentry)) {
               acommandfunction$entry1[i] = commandfunction$entry;
            } else {
               List<String> list = commandfunction$macroentry.parameters();
               List<String> list1 = new ArrayList<>(list.size());

               for(String s : list) {
                  list1.add(pArguments.get(this.parameters.indexOf(s)));
               }

               String s1 = commandfunction$macroentry.substitute(list1);

               try {
                  ParseResults<CommandSourceStack> parseresults = pDispatcher.parse(s1, pSource);
                  if (parseresults.getReader().canRead()) {
                     throw Commands.getParseException(parseresults);
                  }

                  acommandfunction$entry1[i] = new CommandFunction.CommandEntry(parseresults);
               } catch (CommandSyntaxException commandsyntaxexception) {
                  throw new FunctionInstantiationException(Component.translatable("commands.function.error.parse", this.getId(), s1, commandsyntaxexception.getMessage()));
               }
            }
         }

         ResourceLocation resourcelocation = this.getId();
         return new CommandFunction(new ResourceLocation(resourcelocation.getNamespace(), resourcelocation.getPath() + "/" + pArguments.hashCode()), acommandfunction$entry1);
      }

      static {
         DECIMAL_FORMAT.setMaximumFractionDigits(15);
         DECIMAL_FORMAT.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.US));
      }
   }

   @FunctionalInterface
   public interface Entry {
      void execute(ServerFunctionManager pFunctionManager, CommandSourceStack pSource, Deque<ServerFunctionManager.QueuedCommand> pQueuedCommands, int pCommandLimit, int pDepth, @Nullable ServerFunctionManager.TraceCallbacks pTracer) throws CommandSyntaxException;
   }

   public static class FunctionEntry implements CommandFunction.Entry {
      private final CommandFunction.CacheableFunction function;

      public FunctionEntry(CommandFunction pFunction) {
         this.function = new CommandFunction.CacheableFunction(pFunction);
      }

      public void execute(ServerFunctionManager pFunctionManager, CommandSourceStack pSource, Deque<ServerFunctionManager.QueuedCommand> pQueuedCommands, int pCommandLimit, int pDepth, @Nullable ServerFunctionManager.TraceCallbacks pTracer) {
         Util.ifElse(this.function.get(pFunctionManager), (p_164900_) -> {
            CommandFunction.Entry[] acommandfunction$entry = p_164900_.getEntries();
            if (pTracer != null) {
               pTracer.onCall(pDepth, p_164900_.getId(), acommandfunction$entry.length);
            }

            int i = pCommandLimit - pQueuedCommands.size();
            int j = Math.min(acommandfunction$entry.length, i);

            for(int k = j - 1; k >= 0; --k) {
               pQueuedCommands.addFirst(new ServerFunctionManager.QueuedCommand(pSource, pDepth + 1, acommandfunction$entry[k]));
            }

         }, () -> {
            if (pTracer != null) {
               pTracer.onCall(pDepth, this.function.getId(), -1);
            }

         });
      }

      public String toString() {
         return "function " + this.function.getId();
      }
   }

   public static class MacroEntry implements CommandFunction.Entry {
      private final List<String> segments;
      private final List<String> parameters;

      public MacroEntry(List<String> pSegments, List<String> pParameters) {
         this.segments = pSegments;
         this.parameters = pParameters;
      }

      public List<String> parameters() {
         return this.parameters;
      }

      public String substitute(List<String> pParameters) {
         StringBuilder stringbuilder = new StringBuilder();

         for(int i = 0; i < this.parameters.size(); ++i) {
            stringbuilder.append(this.segments.get(i)).append(pParameters.get(i));
         }

         if (this.segments.size() > this.parameters.size()) {
            stringbuilder.append(this.segments.get(this.segments.size() - 1));
         }

         return stringbuilder.toString();
      }

      public void execute(ServerFunctionManager pFunctionManager, CommandSourceStack pSource, Deque<ServerFunctionManager.QueuedCommand> pQueuedCommands, int pCommandLimit, int pDepth, @Nullable ServerFunctionManager.TraceCallbacks pTracer) throws CommandSyntaxException {
         throw new IllegalStateException("Tried to execute an uninstantiated macro");
      }
   }
}