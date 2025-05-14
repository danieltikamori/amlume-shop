/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.user_management.dto;

import me.amlu.shop.amlume_shop.user_management.ContactInfo;
import me.amlu.shop.amlume_shop.user_management.UserPassword;
import me.amlu.shop.amlume_shop.user_management.UserRole;

public record UserRegistrationRequest(
        ContactInfo firstName,
        ContactInfo lastName,
        ContactInfo nickname,
        UserPassword password,
        ContactInfo userEmail,
        UserRole roles,
        String captchaResponse) {

    public static UserRegistrationRequestBuilder builder() {
        return new UserRegistrationRequestBuilder();
    }


    public static class UserRegistrationRequestBuilder {
        private ContactInfo firstName;
        private ContactInfo lastName;
        private ContactInfo nickname;
        private UserPassword password;
        private ContactInfo userEmail;
        private UserRole roles;
        private String captchaResponse;

        UserRegistrationRequestBuilder() {
        }

        public UserRegistrationRequestBuilder firstName(ContactInfo firstName) {
            this.firstName = firstName;
            return this;
        }

        public UserRegistrationRequestBuilder lastName(ContactInfo lastName) {
            this.lastName = lastName;
            return this;
        }

        public UserRegistrationRequestBuilder nickname(ContactInfo nickname) {
            this.nickname = nickname;
            return this;
        }


        public UserRegistrationRequestBuilder password(UserPassword password) {
            this.password = password;
            return this;
        }

        public UserRegistrationRequestBuilder userEmail(ContactInfo userEmail) {
            this.userEmail = userEmail;
            return this;
        }

        public UserRegistrationRequestBuilder roles(UserRole roles) {
            this.roles = roles;
            return this;
        }

        public UserRegistrationRequestBuilder captchaResponse(String captchaResponse) {
            this.captchaResponse = captchaResponse;
            return this;
        }

        public UserRegistrationRequest build() {
            return new UserRegistrationRequest(this.firstName, this.lastName, this.nickname, this.password, this.userEmail, this.roles, this.captchaResponse);
        }

        public String toString() {
            return "UserRegistrationRequest.UserRegistrationRequestBuilder(firstName=" + this.firstName + ", lastName=" + this.lastName + ", nickname=" + this.nickname + ", password=" + this.password + ", userEmail=" + this.userEmail + ", roles=" + this.roles + ", captchaResponse=" + this.captchaResponse + ")";
        }
    }

}
