/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

/**
 * Using record
 * The record version provides the same functionality but with:
 * <p>
 * Immutability by default
 * More concise syntax
 * Clear intent that this is a data carrier
 * Thread-safety due to immutability
 * Built-in toString(), equals(), hashCode(), and constructor
 * <p>
 * Remember that records:
 * <p>
 * Can't extend other classes
 * Are final by default
 * Can't have mutable fields
 * Can still implement interfaces
 * Can have static methods and fields
 * Can have additional methods (but not additional instance fields)
 */

package me.amlu.shop.amlume_shop.security.model;

public record KeyPair(String privateKey, String publicKey) {
}