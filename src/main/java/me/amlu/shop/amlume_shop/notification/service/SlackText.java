/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.notification.service;

import java.util.Objects;

// Made public to resolve visibility issue
public class SlackText {
    private String type;
    private String text;

    public SlackText(String type, String text) {
        this.type = type;
        this.text = text;
    }

    public SlackText() {
    }

    public static SlackTextBuilder builder() {
        return new SlackTextBuilder();
    }

    public String getType() {
        return this.type;
    }

    public String getText() {
        return this.text;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof SlackText other)) return false;
        if (!other.canEqual((Object) this)) return false;
        final Object this$type = this.getType();
        final Object other$type = other.getType();
        if (!Objects.equals(this$type, other$type)) return false;
        final Object this$text = this.getText();
        final Object other$text = other.getText();
        return Objects.equals(this$text, other$text);
    }

    protected boolean canEqual(final Object other) {
        return other instanceof SlackText;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $type = this.getType();
        result = result * PRIME + ($type == null ? 43 : $type.hashCode());
        final Object $text = this.getText();
        result = result * PRIME + ($text == null ? 43 : $text.hashCode());
        return result;
    }

    public String toString() {
        return "SlackText(type=" + this.getType() + ", text=" + this.getText() + ")";
    }

    public static class SlackTextBuilder {
        private String type;
        private String text;

        SlackTextBuilder() {
        }

        public SlackTextBuilder type(String type) {
            this.type = type;
            return this;
        }

        public SlackTextBuilder text(String text) {
            this.text = text;
            return this;
        }

        public SlackText build() {
            return new SlackText(this.type, this.text);
        }

        public String toString() {
            return "SlackText.SlackTextBuilder(type=" + this.type + ", text=" + this.text + ")";
        }
    }
}
