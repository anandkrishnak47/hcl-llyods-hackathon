package com.payments.ingestion.util;

import com.payments.ingestion.model.Account;
import com.payments.ingestion.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AccountDataLoader implements CommandLineRunner {

    private final AccountRepository repo;
    private final ObjectMapper mapper;

    @Override
    public void run(String... args) throws Exception {
        var resource = new ClassPathResource("accounts.json");
        var accounts = mapper.readValue(
                resource.getInputStream(),
                new TypeReference<List<Account>>() {});
        repo.saveAll(accounts);
        System.out.println("✅ Loaded " + accounts.size() + " accounts into H2");
    }
}
