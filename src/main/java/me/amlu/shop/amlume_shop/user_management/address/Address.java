/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.user_management.address;


import jakarta.persistence.*;
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
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Table(name = "addresses", indexes = {
        @Index(name = "idx_address_user_id", columnList = "user_id"),
        @Index(name = "idx_address_city", columnList = "city_name"),
        @Index(name = "idx_address_state", columnList = "state_name"),
        @Index(name = "idx_address_country", columnList = "country_code"),
        @Index(name = "idx_address_zip_code", columnList = "zip_code")
})
public class Address extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Long addressId;

    @AttributeOverrides({
//            @AttributeOverride(name = "streetNumber", column = @Column(name = "street_number")),
//            @AttributeOverride(name = "streetType", column = @Column(name = "street_type")),
            @AttributeOverride(name = "streetName", column = @Column(name = "street_name"))
    })
    @Filter(name = "deletedFilter")
    @Embedded
    private Street street;

    @AttributeOverrides({
//            @AttributeOverride(name = "buildingNumber", column = @Column(name = "building_number")),
//            @AttributeOverride(name = "buildingType", column = @Column(name = "building_type")),
            @AttributeOverride(name = "buildingName", column = @Column(name = "building_name"))
    })
    @Embedded
    private Building building;

    @AttributeOverrides({
//            @AttributeOverride(name = "cityCode", column = @Column(name = "city_code")),
            @AttributeOverride(name = "cityName", column = @Column(name = "city_name"))
    })
    @Embedded
    private City city;

    @AttributeOverrides({
//            @AttributeOverride(name = "stateCode", column = @Column(name = "state_code")),
            @AttributeOverride(name = "stateName", column = @Column(name = "state_name"))
    })
    @Embedded
    private State state;

    @AttributeOverrides({
//            @AttributeOverride(name = "countryName", column = @Column(name = "country_name")),
            @AttributeOverride(name = "countryCode", column = @Column(name = "country_code"))
    })
    @Embedded
    private Country country;

    @AttributeOverrides({
//            @AttributeOverride(name = "zipCodeType", column = @Column(name = "zip_code_type")),
            @AttributeOverride(name = "zipCode", column = @Column(name = "zip_code"))
    })
    @Embedded
    private ZipCode zipCode;

    @ManyToOne(fetch = FetchType.LAZY) // Consider LAZY fetch for ManyToOne unless always needed
    @JoinColumn(name = "user_id") // Foreign key in addresses table pointing to user
    private User user; // ManyToOne link back to User

    // Removed commented-out ManyToMany usersList - stick to the ManyToOne/OneToMany bidirectional

    // Protected constructor required by JPA
    protected Address() {
    }

    // Constructor for creating new Address instances (without ID or User initially)
    public Address(Street street, Building building, City city,
                   State state, Country country, ZipCode zipCode) {
        this.street = street;
        this.building = building;
        this.city = city;
        this.state = state;
        this.country = country;
        this.zipCode = zipCode;
        // ID and User are set later (ID by DB, User by the owning User entity's addAddress method)
    }

//    public Address(Long addressId, Street street, Building building, City city, State state, Country country, ZipCode zipCode, User user) {
//        this.addressId = addressId;
//        this.street = street;
//        this.building = building;
//        this.city = city;
//        this.state = state;
//        this.country = country;
//        this.zipCode = zipCode;
//        this.user = user;
//    }

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

    // Originally: Protected setter for the ManyToOne User relationship
    // Made public because of package issues
    // This should ideally only be called from the owning side (User's add/remove methods)
    // It's primarily intended for use by the owning User entity's relationship management methods
    // (e.g., addAddress, removeAddress) to maintain the integrity of the bidirectional link
    public void setUser(User user) {
        this.user = user;
    }


    // --- BaseEntity / Auditable Methods ---
    @Override
    public Long getAuditableId() {
        return this.addressId;
    }

    @Override
    public Long getId() {
        return this.addressId;
    }
    // --- End BaseEntity / Auditable Methods ---

    // --- equals() and hashCode() ---
    // Proxy-aware implementation of equals() based on ID
    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        // Handle Hibernate proxies
        HibernateProxy oHibernateProxy = o instanceof HibernateProxy hibernateProxy ? hibernateProxy : null;
        Class<?> oEffectiveClass = oHibernateProxy != null ? oHibernateProxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        HibernateProxy thisHibernateProxy = this instanceof HibernateProxy hibernateProxy ? hibernateProxy : null;
        Class<?> thisEffectiveClass = thisHibernateProxy != null ? thisHibernateProxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();

        // Compare the effective classes (important for Hibernate proxies)
        if (thisEffectiveClass != oEffectiveClass) return false;

        // Now safe to cast (though instanceof check is slightly redundant now)
        if (!(o instanceof Address address)) return false;

        // Entities are equal if their primary keys are equal and not null
        return getAddressId() != null && Objects.equals(getAddressId(), address.getAddressId());
    }

    // Corrected hashCode() implementation - MUST be based on the ID
    @Override
    public final int hashCode() {
        // Base the hashCode on the primary key (addressId)
        // Use Objects.hash() for conciseness and null handling
        // If addressId is null (for a new, unsaved entity), Objects.hash(null) returns 0.
        // This is acceptable as unsaved entities are not typically stored in Sets/Maps
        // where hashCode is critical, and if they are, they won't equal any other entity.
        return Objects.hash(addressId);

        // --- Alternative using a constant for null ID (also common) ---
        // return addressId == null ? 31 : addressId.hashCode();
    }
    // --- End equals() and hashCode() ---


    // --- toString() (Manual implementation recommended for entities) ---
    @Override
    public String toString() {
        // Exclude relationships (like user) and potentially large embedded objects
        // from toString to avoid lazy loading issues or cycles.
        return "Address(" +
                "addressId=" + addressId +
                ", street=" + (street != null ? street.toString() : "null") + // Include embedded value toString if they are safe
                ", building=" + (building != null ? building.toString() : "null") +
                ", city=" + (city != null ? city.toString() : "null") +
                ", state=" + (state != null ? state.toString() : "null") +
                ", country=" + (country != null ? country.toString() : "null") +
                ", zipCode=" + (zipCode != null ? zipCode.toString() : "null") +
                ", userId=" + (user != null ? user.getUserId() : "null") + // Show user ID instead of whole user
                ')';
    }

    public Long getAddressId() {
        return this.addressId;
    }
    // --- End toString() ---

    // Note: No public setters for address components assuming they are set via constructor
    // and embedded objects themselves are immutable.
}

