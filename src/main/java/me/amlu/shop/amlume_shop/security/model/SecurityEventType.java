/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.model;

public enum SecurityEventType {
    FAILED_LOGIN,
    ACCOUNT_LOCKED,
    ACCOUNT_UNLOCKED,
    PASSWORD_RESET_REQUEST,
    PASSWORD_RESET_SUCCESSFUL,
    SUSPICIOUS_ACTIVITY,
    SUCCESSFUL_LOGIN,
    LOGOUT,
    PERMISSION_CHANGE,
    ACCESS_DENIED_LOCKED, MFA_FAILED, MFA_CHALLENGE_INITIATED, MFA_CHALLENGE_FAILED, MFA_VERIFICATION_FAILED, ROLE_ASSIGNMENT, ROLE_ASSIGNMENT_FAILED, CACHE_CLEARED, REGISTRATION_SUCCESSFUL, PASSWORD_CHANGED, ROLE_CHANGE
}
