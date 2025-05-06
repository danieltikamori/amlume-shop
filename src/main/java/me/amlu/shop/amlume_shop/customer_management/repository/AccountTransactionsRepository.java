package me.amlu.shop.amlume_shop.customer_management.repository;

import java.util.List;

import me.amlu.shop.amlume_shop.customer_management.AccountTransactions;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountTransactionsRepository extends CrudRepository<AccountTransactions, String> {
	
	List<AccountTransactions> findByCustomerIdOrderByTransactionDtDesc(long customerId);

}
