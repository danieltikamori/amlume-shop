/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import me.amlu.shop.amlume_shop.config.ValidPostalCode;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Filter;
import org.hibernate.proxy.HibernateProxy;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

@Cacheable
@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Table(name = "addresses")
public class Address extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Long addressId;

    @Filter(name = "deletedFilter")
    @NotBlank
    @Size(min = 5, max = 250, message = "Street must be between 5 and 250 characters")
    @Column(name = "street")
    private String street;

    @NotBlank
    @Size(min = 5, max = 250, message = "Building must be between 5 and 250 characters")
    @Column(name = "building_name")
    private String buildingName;

    @NotBlank
    @Size(min = 5, max = 250, message = "City must be between 5 and 250 characters")
    @Column(name = "city")
    private String city;

    @NotBlank
    @Size(min = 5, max = 250, message = "State must be between 5 and 250 characters")
    @Column(name = "state")
    private String state;
    @NotBlank
    @Size(min = 5, max = 250, message = "Country must be between 5 and 250 characters")
    @Column(name = "country")
    private String country;

    @ValidPostalCode
    @NotBlank
    @Size(min = 5, max = 250, message = "Zip code must be between 5 and 250 characters")
    @Column(name = "zip_code")
    private String zipCode;

    @ManyToMany(mappedBy = "addresses")
    @ToString.Exclude
    private List<User> usersList = new CopyOnWriteArrayList<>();

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        HibernateProxy oHibernateProxy = o instanceof HibernateProxy hibernateProxy ? hibernateProxy : null;
        Class<?> oEffectiveClass = oHibernateProxy != null ? oHibernateProxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        HibernateProxy thisHibernateProxy = this instanceof HibernateProxy hibernateProxy ? hibernateProxy : null;
        Class<?> thisEffectiveClass = thisHibernateProxy != null ? thisHibernateProxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        if (!(o instanceof Address address)) return false;
        return getAddressId() != null && Objects.equals(getAddressId(), address.getAddressId());
    }

    @Override
    public final int hashCode() {
        HibernateProxy thisHibernateProxy = this instanceof HibernateProxy hibernateProxy ? hibernateProxy : null;
        return thisHibernateProxy != null ? thisHibernateProxy.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
