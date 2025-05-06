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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Made public to resolve visibility issue
public class SlackMessage {
    private String text;
    private List<SlackBlock> blocks = new ArrayList<>();

    public SlackMessage(String text) {
        this.text = text;
    }

    public SlackMessage(String text, List<SlackBlock> blocks) {
        this.text = text;
        this.blocks = blocks;
    }

    public SlackMessage() {
    }

    private static List<SlackBlock> $default$blocks() {
        return new ArrayList<>();
    }

    public static SlackMessageBuilder builder() {
        return new SlackMessageBuilder();
    }

    public String getText() {
        return this.text;
    }

    public List<SlackBlock> getBlocks() {
        return this.blocks;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setBlocks(List<SlackBlock> blocks) {
        this.blocks = blocks;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof SlackMessage other)) return false;
        if (!other.canEqual((Object) this)) return false;
        final Object this$text = this.getText();
        final Object other$text = other.getText();
        if (!Objects.equals(this$text, other$text)) return false;
        final Object this$blocks = this.getBlocks();
        final Object other$blocks = other.getBlocks();
        return Objects.equals(this$blocks, other$blocks);
    }

    protected boolean canEqual(final Object other) {
        return other instanceof SlackMessage;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $text = this.getText();
        result = result * PRIME + ($text == null ? 43 : $text.hashCode());
        final Object $blocks = this.getBlocks();
        result = result * PRIME + ($blocks == null ? 43 : $blocks.hashCode());
        return result;
    }

    public String toString() {
        return "SlackMessage(text=" + this.getText() + ", blocks=" + this.getBlocks() + ")";
    }

    public static class SlackMessageBuilder {
        private String text;
        private List<SlackBlock> blocks$value;
        private boolean blocks$set;

        SlackMessageBuilder() {
        }

        public SlackMessageBuilder text(String text) {
            this.text = text;
            return this;
        }

        public SlackMessageBuilder blocks(List<SlackBlock> blocks) {
            this.blocks$value = blocks;
            this.blocks$set = true;
            return this;
        }

        public SlackMessage build() {
            List<SlackBlock> blocks$value = this.blocks$value;
            if (!this.blocks$set) {
                blocks$value = SlackMessage.$default$blocks();
            }
            return new SlackMessage(this.text, blocks$value);
        }

        public String toString() {
            return "SlackMessage.SlackMessageBuilder(text=" + this.text + ", blocks$value=" + this.blocks$value + ")";
        }
    }
}
