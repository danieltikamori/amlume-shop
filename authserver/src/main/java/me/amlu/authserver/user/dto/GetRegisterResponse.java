/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.user.dto;

import org.springframework.validation.ObjectError;

import java.util.List;

/**
 * Represents the response returned after a user registration attempt.
 * This record encapsulates either the details of the newly registered user
 * or a list of errors that occurred during the registration process.
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * // Successful registration
 * GetRegisterResponse successResponse = new GetRegisterResponse(new UserResponse("john.doe@example.com", "John Doe"), null);
 *
 * // Failed registration with errors
 * GetRegisterResponse errorResponse = new GetRegisterResponse(null, List.of(new ObjectError("email", "Email already exists")));
 * }</pre>
 */
public record GetRegisterResponse(
        UserResponse userResponse,
        List<ObjectError> errors
) {
}
