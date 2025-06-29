/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.notification.service;


import me.amlu.authserver.notification.slack.PublicBlockInterface;

import java.util.Objects;

// Made public to resolve visibility issue
public class SlackBlock implements PublicBlockInterface {

    private String type;

    private SlackText text;

    public SlackBlock(String type, SlackText text) {
        this.type = type;
        this.text = text;
    }

    public SlackBlock() {
    }

    public static SlackBlockBuilder builder() {
        return new SlackBlockBuilder();
    }

    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public SlackText getText() {
        return this.text;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public void setText(SlackText text) {
        this.text = text;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof SlackBlock other)) return false;
        if (!other.canEqual((Object) this)) return false;
        final Object this$type = this.getType();
        final Object other$type = other.getType();
        if (!Objects.equals(this$type, other$type)) return false;
        final Object this$text = this.getText();
        final Object other$text = other.getText();
        return Objects.equals(this$text, other$text);
    }

    protected boolean canEqual(final Object other) {
        return other instanceof SlackBlock;
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
        return "SlackBlock(type=" + this.getType() + ", text=" + this.getText() + ")";
    }

    public static class SlackBlockBuilder {
        private String type;
        private SlackText text;

        SlackBlockBuilder() {
        }

        public SlackBlockBuilder type(String type) {
            this.type = type;
            return this;
        }

        public SlackBlockBuilder text(SlackText text) {
            this.text = text;
            return this;
        }

        public SlackBlock build() {
            return new SlackBlock(this.type, this.text);
        }

        public String toString() {
            return "SlackBlock.SlackBlockBuilder(type=" + this.type + ", text=" + this.text + ")";
        }
    }
}
