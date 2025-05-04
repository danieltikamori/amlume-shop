package me.amlu.shop.amlume_shop.customer_management.repository;

import me.amlu.shop.amlume_shop.customer_management.Accounts;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountsRepository extends CrudRepository<Accounts, Long> {

    Accounts findByCustomerId(long customerId);

}
