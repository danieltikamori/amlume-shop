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

import me.amlu.shop.amlume_shop.user_management.UserRole;

public record UserRegistrationRequest(
        String givenName,
        String surname,
        String nickname,
        String password,
        String userEmail,
        String mobileNumber,
        UserRole roles,
        String captchaResponse) {

    public static UserRegistrationRequestBuilder builder() {
        return new UserRegistrationRequestBuilder();
    }


    public static class UserRegistrationRequestBuilder {
        private String givenName;
        private String surname;
        private String nickname;
        private String password;
        private String userEmail;
        private String mobileNumber;
        private UserRole roles;
        private String captchaResponse;

        UserRegistrationRequestBuilder() {
        }

        public UserRegistrationRequestBuilder givenName(String givenName) {
            this.givenName = givenName;
            return this;
        }

        public UserRegistrationRequestBuilder surname(String surname) {
            this.surname = surname;
            return this;
        }

        public UserRegistrationRequestBuilder nickname(String nickname) {
            this.nickname = nickname;
            return this;
        }


        public UserRegistrationRequestBuilder password(String password) {
            this.password = password;
            return this;
        }

        public UserRegistrationRequestBuilder userEmail(String userEmail) {
            this.userEmail = userEmail;
            return this;
        }

        public UserRegistrationRequestBuilder mobileNumber(String mobileNumber) {
            this.mobileNumber = mobileNumber;
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
            return new UserRegistrationRequest(this.givenName, this.surname, this.nickname, this.password, this.userEmail, this.mobileNumber, this.roles, this.captchaResponse);
        }

        public String toString() {
            return "UserRegistrationRequest.UserRegistrationRequestBuilder(" +
                    "givenName=" + this.givenName + "," +
                    " surname=" + this.surname + "," +
                    " nickname=" + this.nickname + "," +
                    " password=" + this.password + "," +
                    " userEmail=" + this.userEmail + "," +
                    " mobileNumber=" + this.mobileNumber + "," +
                    " roles=" + this.roles + "," +
                    " captchaResponse=" + this.captchaResponse + ")";
        }
    }

}
