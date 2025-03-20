/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.model.address;


import jakarta.persistence.*;
import lombok.*;
import me.amlu.shop.amlume_shop.model.BaseEntity;
import me.amlu.shop.amlume_shop.user_management.User;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Filter;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Cacheable
@Entity
@Getter
@ToString
@AllArgsConstructor
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Table(name = "addresses")
public class Address extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Long addressId;

    @Filter(name = "deletedFilter")
    @Embedded
    private Street street;

    @Embedded
    private Building building;

    @Embedded
    private City city;

    @Embedded
    private State state;

    @Embedded
    private Country country;

    @Embedded
    private ZipCode zipCode;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

//    @ManyToMany(mappedBy = "addresses")
//    @ToString.Exclude
//    private List<User> usersList = new CopyOnWriteArrayList<>();

    protected Address() {
    } // for JPA

    public Address(Street street, Building building, City city,
                   State state, Country country, ZipCode zipCode) {
        this.street = street;
        this.building = building;
        this.city = city;
        this.state = state;
        this.country = country;
        this.zipCode = zipCode;
    }

    // Getters only - make it immutable
    Street getStreet() {
        return street;
    }

    Building getBuilding() {
        return building;
    }

    City getCity() {
        return city;
    }

    State getState() {
        return state;
    }

    Country getCountry() {
        return country;
    }

    ZipCode getZipCode() {
        return zipCode;
    }

    User getUser() {
        return user;
    }


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
