/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.config.hazelcast;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import jakarta.annotation.Nonnull;
import me.amlu.authserver.oauth2.model.Authority;
import me.amlu.authserver.user.model.PermissionsEntity;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Custom Hazelcast serializer for {@link Authority} objects.
 * <p>
 * This serializer provides an efficient and secure way to serialize and deserialize
 * {@code Authority} objects, including their associated permissions, for storage in
 * Hazelcast-backed sessions. It is designed to be fault-tolerant by handling null
 * values gracefully and only serializing essential data to minimize session size.
 * <p>
 * <b>Serialization Format:</b>
 * <ol>
 *     <li>{@code long} - The ID of the Authority.</li>
 *     <li>{@code String} (UTF) - The name of the Authority (e.g., "ROLE_ADMIN").</li>
 *     <li>{@code int} - The number of permissions in the associated set.</li>
 *     <li>For each permission:
 *         <ul>
 *             <li>{@code boolean} - A flag indicating if the permission object is non-null.</li>
 *             <li>{@code String} (UTF) - The permission's ID (TSID).</li>
 *             <li>{@code String} (UTF) - The permission's name (e.g., "PROFILE_EDIT_OWN").</li>
 *         </ul>
 *     </li>
 * </ol>
 */
public class AuthoritySerializer implements StreamSerializer<Authority> {

    /**
     * Returns the unique type ID for this serializer.
     * This ID must be unique across all custom serializers registered in Hazelcast.
     *
     * @return The unique type ID for the {@link Authority} class.
     */
    @Override
    public int getTypeId() {
        return 1003;
    }

    /**
     * Writes an {@link Authority} object to the provided {@link ObjectDataOutput} stream.
     *
     * @param out       The output stream to write to.
     * @param authority The {@link Authority} object to serialize.
     * @throws IOException If an I/O error occurs.
     */
    @Override
    public void write(@Nonnull ObjectDataOutput out, @Nonnull Authority authority) throws IOException {
        out.writeLong(authority.getId());
        out.writeString(authority.getAuthority());

        Set<PermissionsEntity> permissions = authority.getPermissions();
        if (permissions == null) {
            out.writeInt(0); // Write 0 for size if the permissions set is null.
        } else {
            out.writeInt(permissions.size());
            for (PermissionsEntity permission : permissions) {
                if (permission != null && StringUtils.hasText(permission.getId()) && StringUtils.hasText(permission.getName())) {
                    out.writeBoolean(true); // Indicate non-null permission
                    out.writeString(permission.getId()); // Assuming PermissionsEntity has getId()
                    out.writeString(permission.getName()); // Assuming PermissionsEntity has getName()
                    // The description is omitted to keep the serialized object lean.
//                    out.writeString(permission.getDescription()); // Assuming PermissionsEntity has getDescription()
                } else {
                    out.writeBoolean(false); // Indicate a null or invalid permission entry.
                }
            }
        }
        // Note: Fields from AbstractAuditableEntity (createdAt, updatedAt, etc.) are not serialized here.
        // If they are needed in the session, they should be added.
    }

    /**
     * Reads an {@link Authority} object from the provided {@link ObjectDataInput} stream.
     *
     * @param in The input stream to read from.
     * @return The deserialized {@link Authority} object.
     * @throws IOException If an I/O error occurs.
     */
    @Nonnull
    @Override
    public Authority read(@Nonnull ObjectDataInput in) throws IOException {
        long id = in.readLong();
        String authorityName = in.readString();

        int permissionsSize = in.readInt();
        Set<PermissionsEntity> permissions;
        if (permissionsSize > 0) {
            permissions = new HashSet<>(permissionsSize);
            for (int i = 0; i < permissionsSize; i++) {
                boolean isPermissionNotNull = in.readBoolean();
                if (isPermissionNotNull) {
                    String permId = in.readString();
                    String permName = in.readString();
                    // Reconstruct the permission with only the essential data.
                    // The description will be null, which is acceptable for session state.
//                String permDescription = in.readString();
                    // Assuming a constructor or builder for PermissionsEntity
                    permissions.add(new PermissionsEntity(permId, permName));
                }
                // If the boolean flag was false, we simply skip adding anything to the set,
                // avoiding the creation of invalid or placeholder objects.
            }
        } else {
            permissions = Collections.emptySet();
        }

        // Reconstruct the Authority object using the deserialized data.
        return new Authority(id, authorityName, permissions);
    }
    // Uses the constructor that accepts permissions


    // destroy() method is optional and can be omitted if no resources need cleanup.
    // @Override
    // public void destroy() {
    //     // Cleanup resources if any
    // }
}
