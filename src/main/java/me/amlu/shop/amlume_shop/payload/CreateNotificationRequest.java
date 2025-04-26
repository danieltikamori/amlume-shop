/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.payload;

import me.amlu.shop.amlume_shop.enums.NotificationType;

import java.util.List;
import java.util.Map;

public record CreateNotificationRequest(String recipientEmail, String subject, String message, String actionUrl,
                                        boolean isHtml, List<CreateEmailAttachmentRequest> attachments, boolean emailEnabled,
                                        boolean slackEnabled, NotificationType type,
                                        Map<String, String> additionalData) {

    public static CreateNotificationRequestBuilder builder() {
        return new CreateNotificationRequestBuilder();
    }

    public static class CreateNotificationRequestBuilder {
        private String recipientEmail;
        private String subject;
        private String message;
        private String actionUrl;
        private boolean isHtml;
        private List<CreateEmailAttachmentRequest> attachments;
        private boolean emailEnabled;
        private boolean slackEnabled;
        private NotificationType type;
        private Map<String, String> additionalData;

        CreateNotificationRequestBuilder() {
        }

        public CreateNotificationRequestBuilder recipientEmail(String recipientEmail) {
            this.recipientEmail = recipientEmail;
            return this;
        }

        public CreateNotificationRequestBuilder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public CreateNotificationRequestBuilder message(String message) {
            this.message = message;
            return this;
        }

        public CreateNotificationRequestBuilder actionUrl(String actionUrl) {
            this.actionUrl = actionUrl;
            return this;
        }

        public CreateNotificationRequestBuilder isHtml(boolean isHtml) {
            this.isHtml = isHtml;
            return this;
        }

        public CreateNotificationRequestBuilder attachments(List<CreateEmailAttachmentRequest> attachments) {
            this.attachments = attachments;
            return this;
        }

        public CreateNotificationRequestBuilder emailEnabled(boolean emailEnabled) {
            this.emailEnabled = emailEnabled;
            return this;
        }

        public CreateNotificationRequestBuilder slackEnabled(boolean slackEnabled) {
            this.slackEnabled = slackEnabled;
            return this;
        }

        public CreateNotificationRequestBuilder type(NotificationType type) {
            this.type = type;
            return this;
        }

        public CreateNotificationRequestBuilder additionalData(Map<String, String> additionalData) {
            this.additionalData = additionalData;
            return this;
        }

        public CreateNotificationRequest build() {
            return new CreateNotificationRequest(this.recipientEmail, this.subject, this.message, this.actionUrl,
                    this.isHtml, this.attachments, this.emailEnabled, this.slackEnabled, this.type,
                    this.additionalData);

        }

        public String toString() {
            return "CreateNotificationRequest.CreateNotificationRequestBuilder(recipientEmail=" + this.recipientEmail + ", subject=" + this.subject + ", message=" + this.message + ", actionUrl=" + this.actionUrl + ", isHtml=" + this.isHtml + ", attachments=" + this.attachments + ", emailEnabled=" + this.emailEnabled + ", slackEnabled=" + this.slackEnabled + ", type=" + this.type + ", additionalData=" + this.additionalData + ")";
        }
    }
}
