package net.minecraft.network.chat;

import com.mojang.logging.LogUtils;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import net.minecraft.util.SignatureValidator;
import org.slf4j.Logger;

@FunctionalInterface
public interface SignedMessageValidator {
   Logger LOGGER = LogUtils.getLogger();
   SignedMessageValidator ACCEPT_UNSIGNED = (p_296390_) -> {
      if (p_296390_.hasSignature()) {
         LOGGER.error("Received chat message with signature from {}, but they have no chat session initialized", (Object)p_296390_.sender());
         return false;
      } else {
         return true;
      }
   };
   SignedMessageValidator REJECT_ALL = (p_296391_) -> {
      LOGGER.error("Received chat message from {}, but they have no chat session initialized and secure chat is enforced", (Object)p_296391_.sender());
      return false;
   };

   boolean updateAndValidate(PlayerChatMessage pMessage);

   public static class KeyBased implements SignedMessageValidator {
      private final SignatureValidator validator;
      private final BooleanSupplier expired;
      @Nullable
      private PlayerChatMessage lastMessage;
      private boolean isChainValid = true;

      public KeyBased(SignatureValidator pValidator, BooleanSupplier pExpired) {
         this.validator = pValidator;
         this.expired = pExpired;
      }

      private boolean validateChain(PlayerChatMessage pMessage) {
         if (pMessage.equals(this.lastMessage)) {
            return true;
         } else if (this.lastMessage != null && !pMessage.link().isDescendantOf(this.lastMessage.link())) {
            LOGGER.error("Received out-of-order chat message from {}: expected index > {} for session {}, but was {} for session {}", pMessage.sender(), this.lastMessage.link().index(), this.lastMessage.link().sessionId(), pMessage.link().index(), pMessage.link().sessionId());
            return false;
         } else {
            return true;
         }
      }

      private boolean validate(PlayerChatMessage pMessage) {
         if (this.expired.getAsBoolean()) {
            LOGGER.error("Received message from player with expired profile public key: {}", (Object)pMessage);
            return false;
         } else if (!pMessage.verify(this.validator)) {
            LOGGER.error("Received message with invalid signature from {}", (Object)pMessage.sender());
            return false;
         } else {
            return this.validateChain(pMessage);
         }
      }

      public boolean updateAndValidate(PlayerChatMessage pMessage) {
         this.isChainValid = this.isChainValid && this.validate(pMessage);
         if (!this.isChainValid) {
            return false;
         } else {
            this.lastMessage = pMessage;
            return true;
         }
      }
   }
}