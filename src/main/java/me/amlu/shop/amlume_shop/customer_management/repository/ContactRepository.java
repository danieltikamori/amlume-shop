package me.amlu.shop.amlume_shop.customer_management.repository;

import me.amlu.shop.amlume_shop.customer_management.Contact;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactRepository extends CrudRepository<Contact, String> {
	
	
}
