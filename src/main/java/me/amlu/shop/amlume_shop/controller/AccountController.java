package me.amlu.shop.amlume_shop.controller;

import lombok.RequiredArgsConstructor;
import me.amlu.shop.amlume_shop.customer_management.Accounts;
import me.amlu.shop.amlume_shop.customer_management.Customer;
import me.amlu.shop.amlume_shop.customer_management.repository.AccountsRepository;
import me.amlu.shop.amlume_shop.customer_management.repository.CustomerRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class AccountController {

    private final AccountsRepository accountsRepository;
    private final CustomerRepository customerRepository;

    @GetMapping("/myAccount")
    public Accounts getAccountDetails(@RequestParam String email) {
        Optional<Customer> optionalCustomer = customerRepository.findByEmail(email);
        if (optionalCustomer.isPresent()) {
            Accounts accounts = accountsRepository.findByCustomerId(optionalCustomer.get().getId());
            if (accounts != null) {
                return accounts;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

}
