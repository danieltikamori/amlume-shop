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

import me.amlu.authserver.user.model.User;

/**
 * A DTO (Data Transfer Object) record representing a simplified view of a user for response purposes.
 * This record is used to expose only necessary user information (username and email)
 * to clients, avoiding the exposure of sensitive data like passwords or internal IDs.
 *
 * <p>Usage Example:</p>
 * <pre>{@code
 * User user = new User("testuser", new Email("test@example.com"), "hashedpassword");
 * UserResponse userResponse = new UserResponse(user);
 * System.out.println("Username: " + userResponse.username());
 * System.out.println("Email: " + userResponse.userEmail());
 * }</pre>
 *
 * @param username  The username of the user.
 * @param userEmail The email address of the user.
 */
public record UserResponse(String username, String userEmail) {

    /**
     * Constructs a {@code UserResponse} from a {@link User} entity.
     * This constructor maps the relevant fields from the {@code User} object to the DTO.
     *
     * @param user The {@link User} entity from which to create the response. Must not be null.
     */
    public UserResponse(User user) {
        this(user.getUsername(), user.getEmail().getValue());
    }
}
