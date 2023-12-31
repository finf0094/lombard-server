package com.lombard.service.service;

import com.lombard.service.entities.Client;
import com.lombard.service.entities.Contract;
import com.lombard.service.entities.Product;
import com.lombard.service.entities.User;
import com.lombard.service.exceptions.ContractNotFoundException;
import com.lombard.service.repository.ClientRepository;
import com.lombard.service.repository.ContractRepository;
import com.lombard.service.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ContractService {
    private final ClientRepository clientRepository;
    private final ProductRepository productRepository;
    private final ContractRepository contractRepository;
    private final UserService userService;

    private final ContractStatusService contractStatusService;

    public ContractService(ClientRepository clientRepository, ProductRepository productRepository, ContractRepository contractRepository, UserService userService, ContractStatusService contractStatusService) {
        this.clientRepository = clientRepository;
        this.productRepository = productRepository;
        this.contractRepository = contractRepository;
        this.userService = userService;
        this.contractStatusService = contractStatusService;
    }

    public ResponseEntity<Contract> createContract(Client client, Product product) {

        product.calculateLastPaymentDate();
        product.calculateUpdatedSums();

        client = clientRepository.save(client);
        product = productRepository.save(product);


        Contract contract = new Contract();
        contract.setClient(client);
        contract.setProduct(product);

        contract.setContractStatus(contractStatusService.getFormedStatus());

        contractRepository.save(contract);

        return ResponseEntity.ok(contract);
    }

    public List<Contract> getFormedContract() {
        return contractRepository.findContractByContractStatus(contractStatusService.getFormedStatus());
    }


    public boolean deleteById(Long contractId) {
        Optional<Contract> contract = contractRepository.findById(contractId);
        if (contract != null) {
            contractRepository.delete(contract.get());
            return true;
        }
        return false;
    }

    public Contract getContractById(Long id) {
        Optional<Contract> contract = contractRepository.findById(id);

        return contract.get();
    }

    public Contract toggleIssuedFalseContractToTrue(Long id) {
        Optional<Contract> contract = contractRepository.findById(id);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        User user = userService.findByUserName(authentication.getPrincipal().toString()).orElseThrow();

        user.setTotalAmountIssued((long) (user.getTotalAmountIssued() + contract.get().getProduct().getSum()));

        contract.get().setContractStatus(contractStatusService.getIssuedContract());

        contractRepository.save(contract.get());

        return contract.get();
    }

    public List<Contract> getContractsByIin(String iin) {
        List<Contract> contracts = contractRepository.findByClientIin(iin);


        return Optional.ofNullable(contracts)
                .orElseThrow(() -> new ContractNotFoundException("No contracts found for the given IIN: " + iin));
    }


}
