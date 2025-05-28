   /*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

   package me.amlu.authserver.config;

   import org.springframework.boot.context.properties.ConfigurationProperties;
   import org.springframework.stereotype.Component; // Make it a Spring component

   import java.util.Collections;
   import java.util.Objects;
   import java.util.Set;

   @Component // This allows Spring to discover and manage it as a bean
   @ConfigurationProperties(prefix = "spring.security.webauthn")
   public class WebAuthNProperties {
       private String rpId = "localhost";
       private String rpName = "Amlume Passkeys";
       private Set<String> allowedOrigins = Collections.singleton("http://localhost:8080");

       public WebAuthNProperties() {
       }

       public String getRpId() {
           return this.rpId;
       }

       public void setRpId(String rpId) {
           this.rpId = rpId;
       }

       public String getRpName() {
           return this.rpName;
       }

       public void setRpName(String rpName) {
           this.rpName = rpName;
       }

       public Set<String> getAllowedOrigins() {
           return this.allowedOrigins;
       }

       public void setAllowedOrigins(Set<String> allowedOrigins) {
           this.allowedOrigins = allowedOrigins;
       }

       @Override
       public boolean equals(final Object o) {
           if (o == this) return true;
           if (!(o instanceof WebAuthNProperties other)) return false;
           if (!other.canEqual(this)) return false;
           final Object this$rpId = this.getRpId();
           final Object other$rpId = other.getRpId();
           if (!Objects.equals(this$rpId, other$rpId)) return false;
           final Object this$rpName = this.getRpName();
           final Object other$rpName = other.getRpName();
           if (!Objects.equals(this$rpName, other$rpName)) return false;
           final Object this$allowedOrigins = this.getAllowedOrigins();
           final Object other$allowedOrigins = other.getAllowedOrigins();
           return Objects.equals(this$allowedOrigins, other$allowedOrigins);
       }

       protected boolean canEqual(final Object other) {
           return other instanceof WebAuthNProperties;
       }

       @Override
       public int hashCode() {
           final int PRIME = 59;
           int result = 1;
           final Object $rpId = this.getRpId();
           result = result * PRIME + ($rpId == null ? 43 : $rpId.hashCode());
           final Object $rpName = this.getRpName();
           result = result * PRIME + ($rpName == null ? 43 : $rpName.hashCode());
           final Object $allowedOrigins = this.getAllowedOrigins();
           result = result * PRIME + ($allowedOrigins == null ? 43 : $allowedOrigins.hashCode());
           return result;
       }

       @Override
       public String toString() {
           return "WebAuthNProperties(rpId=" + this.getRpId() + ", rpName=" + this.getRpName() + ", allowedOrigins=" + this.getAllowedOrigins() + ")";
       }
   }
