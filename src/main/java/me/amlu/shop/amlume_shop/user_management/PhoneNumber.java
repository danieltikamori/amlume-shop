/*
 * Copyright (c) 2024-2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.user_management;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotEmpty;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class PhoneNumber implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private String phoneNumber;

  private String locale;

  protected PhoneNumber() { // Required by JPA
  }

  PhoneNumber(@NotEmpty String phoneNumber, @NotEmpty String locale) {
    this.phoneNumber = phoneNumber;
    this.locale = locale;
  }

  public static PhoneNumberBuilder builder() {
    return new PhoneNumberBuilder();
  }

  public @NotEmpty String getPhoneNumber() {
    return this.phoneNumber;
  }

  public @NotEmpty String getLocale() {
    return this.locale;
  }

  @Override
  public boolean equals(final Object o) {
    if (o == this) return true;
    if (!(o instanceof PhoneNumber other)) return false;
    if (!other.canEqual((Object) this)) return false;
    final Object this$value = this.getPhoneNumber();
    final Object other$value = other.getPhoneNumber();
    if (!Objects.equals(this$value, other$value)) return false;
    final Object this$locale = this.getLocale();
    final Object other$locale = other.getLocale();
      return Objects.equals(this$locale, other$locale);
  }

  protected boolean canEqual(final Object other) {
    return other instanceof PhoneNumber;
  }

  @Override
  public int hashCode() {
    final int PRIME = 59;
    int result = 1;
    final Object $value = this.getPhoneNumber();
    result = result * PRIME + ($value == null ? 43 : $value.hashCode());
    final Object $locale = this.getLocale();
    result = result * PRIME + ($locale == null ? 43 : $locale.hashCode());
    return result;
  }

  @Override
  public String toString() {
    return "PhoneNumber(phoneNumber=" + this.getPhoneNumber() + ", locale=" + this.getLocale() + ")";
  }

  public static class PhoneNumberBuilder {
    private @NotEmpty String phoneNumber;
    private @NotEmpty String locale;

    PhoneNumberBuilder() {
    }

    public PhoneNumberBuilder value(@NotEmpty String value) {
      this.phoneNumber = value;
      return this;
    }

    public PhoneNumberBuilder locale(@NotEmpty String locale) {
      this.locale = locale;
      return this;
    }

    public PhoneNumber build() {
      return new PhoneNumber(this.phoneNumber, this.locale);
    }

    @Override
    public String toString() {
      return "PhoneNumber.PhoneNumberBuilder(phoneNumber=" + this.phoneNumber + ", locale=" + this.locale + ")";
    }
  }
}
