package com.payments.ingestion.repository;

import com.payments.ingestion.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, String> {
}
