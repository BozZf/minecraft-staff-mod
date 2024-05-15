package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Collection;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandFunction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.FunctionInstantiationException;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.item.FunctionArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ServerFunctionManager;
import net.minecraft.server.commands.data.DataAccessor;
import net.minecraft.server.commands.data.DataCommands;
import org.apache.commons.lang3.mutable.MutableObject;

public class FunctionCommand {
   private static final DynamicCommandExceptionType ERROR_ARGUMENT_NOT_COMPOUND = new DynamicCommandExceptionType((p_296505_) -> {
      return Component.translatable("commands.function.error.argument_not_compound", p_296505_);
   });
   public static final SuggestionProvider<CommandSourceStack> SUGGEST_FUNCTION = (p_137719_, p_137720_) -> {
      ServerFunctionManager serverfunctionmanager = p_137719_.getSource().getServer().getFunctions();
      SharedSuggestionProvider.suggestResource(serverfunctionmanager.getTagNames(), p_137720_, "#");
      return SharedSuggestionProvider.suggestResource(serverfunctionmanager.getFunctionNames(), p_137720_);
   };

   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
      LiteralArgumentBuilder<CommandSourceStack> literalargumentbuilder = Commands.literal("with");

      for(DataCommands.DataProvider datacommands$dataprovider : DataCommands.SOURCE_PROVIDERS) {
         datacommands$dataprovider.wrap(literalargumentbuilder, (p_296503_) -> {
            return p_296503_.executes((p_296507_) -> {
               return runFunction(p_296507_.getSource(), FunctionArgument.getFunctions(p_296507_, "name"), datacommands$dataprovider.access(p_296507_).getData());
            }).then(Commands.argument("path", NbtPathArgument.nbtPath()).executes((p_296509_) -> {
               return runFunction(p_296509_.getSource(), FunctionArgument.getFunctions(p_296509_, "name"), getArgumentTag(NbtPathArgument.getPath(p_296509_, "path"), datacommands$dataprovider.access(p_296509_)));
            }));
         });
      }

      pDispatcher.register(Commands.literal("function").requires((p_137722_) -> {
         return p_137722_.hasPermission(2);
      }).then(Commands.argument("name", FunctionArgument.functions()).suggests(SUGGEST_FUNCTION).executes((p_296504_) -> {
         return runFunction(p_296504_.getSource(), FunctionArgument.getFunctions(p_296504_, "name"), (CompoundTag)null);
      }).then(Commands.argument("arguments", CompoundTagArgument.compoundTag()).executes((p_296510_) -> {
         return runFunction(p_296510_.getSource(), FunctionArgument.getFunctions(p_296510_, "name"), CompoundTagArgument.getCompoundTag(p_296510_, "arguments"));
      })).then(literalargumentbuilder)));
   }

   private static CompoundTag getArgumentTag(NbtPathArgument.NbtPath pNbtPath, DataAccessor pDataAccessor) throws CommandSyntaxException {
      Tag tag = DataCommands.getSingleTag(pNbtPath, pDataAccessor);
      if (tag instanceof CompoundTag) {
         return (CompoundTag)tag;
      } else {
         throw ERROR_ARGUMENT_NOT_COMPOUND.create(tag.getType().getName());
      }
   }

   private static int runFunction(CommandSourceStack pSource, Collection<CommandFunction> pFunctions, @Nullable CompoundTag pArguments) {
      int i = 0;
      boolean flag = false;
      boolean flag1 = false;

      for(CommandFunction commandfunction : pFunctions) {
         try {
            FunctionCommand.FunctionResult functioncommand$functionresult = runFunction(pSource, commandfunction, pArguments);
            i += functioncommand$functionresult.value();
            flag |= functioncommand$functionresult.isReturn();
            flag1 = true;
         } catch (FunctionInstantiationException functioninstantiationexception) {
            pSource.sendFailure(functioninstantiationexception.messageComponent());
         }
      }

      if (flag1) {
         int j = i;
         if (pFunctions.size() == 1) {
            if (flag) {
               pSource.sendSuccess(() -> {
                  return Component.translatable("commands.function.success.single.result", j, pFunctions.iterator().next().getId());
               }, true);
            } else {
               pSource.sendSuccess(() -> {
                  return Component.translatable("commands.function.success.single", j, pFunctions.iterator().next().getId());
               }, true);
            }
         } else if (flag) {
            pSource.sendSuccess(() -> {
               return Component.translatable("commands.function.success.multiple.result", pFunctions.size());
            }, true);
         } else {
            pSource.sendSuccess(() -> {
               return Component.translatable("commands.function.success.multiple", j, pFunctions.size());
            }, true);
         }
      }

      return i;
   }

   public static FunctionCommand.FunctionResult runFunction(CommandSourceStack pSource, CommandFunction pFunction, @Nullable CompoundTag pArguments) throws FunctionInstantiationException {
      MutableObject<FunctionCommand.FunctionResult> mutableobject = new MutableObject<>();
      int i = pSource.getServer().getFunctions().execute(pFunction, pSource.withSuppressedOutput().withMaximumPermission(2).withReturnValueConsumer((p_296501_) -> {
         mutableobject.setValue(new FunctionCommand.FunctionResult(p_296501_, true));
      }), (ServerFunctionManager.TraceCallbacks)null, pArguments);
      FunctionCommand.FunctionResult functioncommand$functionresult = mutableobject.getValue();
      return functioncommand$functionresult != null ? functioncommand$functionresult : new FunctionCommand.FunctionResult(i, false);
   }

   public static record FunctionResult(int value, boolean isReturn) {
   }
}