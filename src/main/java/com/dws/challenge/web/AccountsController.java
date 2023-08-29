package com.dws.challenge.web;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.dws.challenge.service.AccountsService;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.Optional;

@RestController
@RequestMapping("/v1/accounts")
@Slf4j
public class AccountsController {

    private final AccountsService accountsService;

    @Autowired
    public AccountsController(AccountsService accountsService) {
        this.accountsService = accountsService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> createAccount(@RequestBody @Valid Account account) {
        log.info("Creating account {}", account);

        try {
            this.accountsService.createAccount(account);
        } catch (DuplicateAccountIdException daie) {
            return new ResponseEntity<>(daie.getMessage(), HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping(path = "/{accountId}")
    public Account getAccount(@PathVariable String accountId) {
        log.info("Retrieving account for id {}", accountId);
        Optional<Account> ifAccount = this.accountsService.getAccount(accountId);
        if(ifAccount.isEmpty())
             throw new ServiceException("Not found the account:" + accountId);
        return ifAccount.get();
    }


    @PutMapping(path = "/transfer/{fromAccount}/{toAccount}")
    public ResponseEntity<String> transfer(@PathVariable String fromAccount, @PathVariable String toAccount, @RequestParam("amount") BigDecimal amouunt) {
        accountsService.transfer(fromAccount, toAccount, amouunt);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
