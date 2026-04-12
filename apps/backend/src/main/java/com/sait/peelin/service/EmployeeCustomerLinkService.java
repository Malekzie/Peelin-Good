package com.sait.peelin.service;

import com.sait.peelin.model.Customer;
import com.sait.peelin.model.Employee;
import com.sait.peelin.model.EmployeeCustomerLink;
import com.sait.peelin.model.User;
import com.sait.peelin.repository.CustomerRepository;
import com.sait.peelin.repository.EmployeeCustomerLinkRepository;
import com.sait.peelin.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployeeCustomerLinkService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeCustomerLinkService.class);

    /** Fixed employee discount (percent of merchandise after today’s special and tier discounts). */
    public static final BigDecimal EMPLOYEE_DISCOUNT_PERCENT = new BigDecimal("20");

    private final EmployeeCustomerLinkRepository linkRepository;
    private final EmployeeRepository employeeRepository;
    private final CustomerRepository customerRepository;
    private final LinkedProfileSyncService linkedProfileSyncService;

    @Transactional(readOnly = true)
    public boolean isEligibleForEmployeeDiscount(UUID customerId) {
        if (customerId == null) {
            return false;
        }
        return linkRepository.findByCustomer_Id(customerId)
                .map(this::isLinkActiveForDiscount)
                .orElse(false);
    }

    private boolean isLinkActiveForDiscount(EmployeeCustomerLink link) {
        Employee e = link.getEmployee();
        Customer c = link.getCustomer();
        if (e == null || c == null) {
            return false;
        }
        User eu = e.getUser();
        User cu = c.getUser();
        if (eu == null || cu == null) {
            return false;
        }
        return Boolean.TRUE.equals(eu.getActive()) && Boolean.TRUE.equals(cu.getActive());
    }

    /**
     * Creates a link when the customer’s profile email matches exactly one employee (work email) and neither side is already linked.
     * Phone is not used for matching.
     *
     * @return true if a new link row was created
     */
    @Transactional
    @CacheEvict(value = "customers", allEntries = true)
    public boolean tryAutoLinkForCustomer(Customer customer) {
        if (customer == null || customer.getId() == null || customer.getUser() == null) {
            return false;
        }
        if (linkRepository.existsByCustomer_Id(customer.getId())) {
            return false;
        }
        Employee match = findSingleUnlinkedEmployeeByEmail(customer);
        if (match == null) {
            return false;
        }
        if (linkRepository.existsByEmployee_Id(match.getId())) {
            return false;
        }
        EmployeeCustomerLink link = new EmployeeCustomerLink();
        link.setEmployee(match);
        link.setCustomer(customer);
        linkRepository.save(link);
        log.info("Auto-linked customer {} to employee {}", customer.getId(), match.getId());
        Employee employeeFresh = employeeRepository.findById(match.getId()).orElse(match);
        Customer customerFresh = customerRepository.findById(customer.getId()).orElse(customer);
        linkedProfileSyncService.afterLinkCreated(employeeFresh, customerFresh);
        return true;
    }

    private Employee findSingleUnlinkedEmployeeByEmail(Customer customer) {
        if (!StringUtils.hasText(customer.getCustomerEmail())) {
            return null;
        }
        List<Employee> byEmail = employeeRepository.findByWorkEmailNormalized(customer.getCustomerEmail().trim());
        List<Employee> unlinkedEmail = byEmail.stream()
                .filter(e -> !linkRepository.existsByEmployee_Id(e.getId()))
                .toList();
        if (unlinkedEmail.size() == 1) {
            return unlinkedEmail.get(0);
        }
        return null;
    }

    /** True when the two users are the customer and employee sides of the same link row. */
    public boolean areLinkedUserIds(UUID userIdA, UUID userIdB) {
        if (userIdA == null || userIdB == null || userIdA.equals(userIdB)) {
            return false;
        }
        return linkRepository.countLinkBetweenUserIds(userIdA, userIdB) > 0;
    }

    @Transactional
    @CacheEvict(value = "customers", allEntries = true)
    public void createLinkAdmin(UUID employeeId, UUID customerId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
        if (customer.getUser() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer must have a registered user account");
        }
        if (linkRepository.existsByEmployee_Id(employeeId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That employee is already linked to a customer");
        }
        if (linkRepository.existsByCustomer_Id(customerId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That customer is already linked to an employee");
        }
        EmployeeCustomerLink link = new EmployeeCustomerLink();
        link.setEmployee(employee);
        link.setCustomer(customer);
        linkRepository.save(link);
        log.info("Admin linked employee {} to customer {}", employeeId, customerId);
        Employee employeeFresh = employeeRepository.findById(employeeId).orElse(employee);
        Customer customerFresh = customerRepository.findById(customerId).orElse(customer);
        linkedProfileSyncService.afterLinkCreated(employeeFresh, customerFresh);
    }
}
