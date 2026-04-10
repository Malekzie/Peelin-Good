package com.sait.peelin.service;

import com.sait.peelin.model.Customer;
import com.sait.peelin.model.Employee;
import com.sait.peelin.model.EmployeeCustomerLink;
import com.sait.peelin.repository.CustomerRepository;
import com.sait.peelin.repository.EmployeeCustomerLinkRepository;
import com.sait.peelin.repository.EmployeeRepository;
import com.sait.peelin.support.PhoneNumberFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * Keeps linked employee/customer profile fields in sync (names, phones, emails, shared address row).
 * Does not merge {@link com.sait.peelin.model.User} login identities across the two accounts.
 */
@Service
@RequiredArgsConstructor
public class LinkedProfileSyncService {

    private static final ThreadLocal<Boolean> SUPPRESS = new ThreadLocal<>();

    private final EmployeeCustomerLinkRepository linkRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;

    public static void runSuppressed(Runnable r) {
        Boolean prev = SUPPRESS.get();
        try {
            SUPPRESS.set(true);
            r.run();
        } finally {
            if (prev == null) {
                SUPPRESS.remove();
            } else {
                SUPPRESS.set(prev);
            }
        }
    }

    private static boolean isSuppressed() {
        return Boolean.TRUE.equals(SUPPRESS.get());
    }

    @Transactional
    public void afterEmployeeProfilePatch(Employee employee) {
        if (isSuppressed() || employee == null || employee.getId() == null) {
            return;
        }
        linkRepository.findByEmployee_Id(employee.getId()).ifPresent(link -> {
            Customer c = customerRepository.findById(link.getCustomer().getId()).orElse(null);
            if (c == null) {
                return;
            }
            applyEmployeeFieldsToCustomer(employee, c);
            customerRepository.save(c);
        });
    }

    @Transactional
    public void afterCustomerProfilePatch(Customer customer) {
        if (isSuppressed() || customer == null || customer.getId() == null) {
            return;
        }
        linkRepository.findByCustomer_Id(customer.getId()).ifPresent(link -> {
            Employee e = employeeRepository.findById(link.getEmployee().getId()).orElse(null);
            if (e == null) {
                return;
            }
            applyCustomerFieldsToEmployee(customer, e);
            employeeRepository.save(e);
        });
    }

    /**
     * When sign-in email changes on either account, mirror the new value onto the linked profile’s email field.
     */
    /**
     * When a link is first created, treat the employee record as source of truth and copy onto the customer
     * so the shopping profile matches HR data immediately.
     */
    @Transactional
    public void afterLinkCreated(Employee employee, Customer customer) {
        if (isSuppressed() || employee == null || customer == null) {
            return;
        }
        applyEmployeeFieldsToCustomer(employee, customer);
        customerRepository.save(customer);
    }

    @Transactional
    public void afterLinkedUserSignInEmailChanged(UUID userId, String newEmailNormalized) {
        if (isSuppressed() || userId == null || !StringUtils.hasText(newEmailNormalized)) {
            return;
        }
        String email = newEmailNormalized.trim().toLowerCase();
        customerRepository.findByUser_UserId(userId).ifPresent(c ->
                linkRepository.findByCustomer_Id(c.getId()).ifPresent(link -> {
                    Employee e = employeeRepository.findById(link.getEmployee().getId()).orElse(null);
                    if (e != null) {
                        e.setEmployeeWorkEmail(email);
                        employeeRepository.save(e);
                    }
                }));
        employeeRepository.findByUser_UserId(userId).ifPresent(e ->
                linkRepository.findByEmployee_Id(e.getId()).ifPresent(link -> {
                    Customer c = customerRepository.findById(link.getCustomer().getId()).orElse(null);
                    if (c != null) {
                        c.setCustomerEmail(email);
                        customerRepository.save(c);
                    }
                }));
    }

    private void applyEmployeeFieldsToCustomer(Employee e, Customer c) {
        if (StringUtils.hasText(e.getEmployeeFirstName())) {
            c.setCustomerFirstName(e.getEmployeeFirstName());
        }
        if (StringUtils.hasText(e.getEmployeeLastName())) {
            c.setCustomerLastName(e.getEmployeeLastName());
        }
        c.setCustomerMiddleInitial(e.getEmployeeMiddleInitial());
        if (StringUtils.hasText(e.getEmployeeWorkEmail())) {
            c.setCustomerEmail(e.getEmployeeWorkEmail().trim().toLowerCase());
        }
        if (StringUtils.hasText(e.getEmployeePhone())) {
            c.setCustomerPhone(PhoneNumberFormatter.formatStoredPhone(e.getEmployeePhone()));
        }
        c.setCustomerBusinessPhone(PhoneNumberFormatter.formatStoredPhoneOrNull(e.getEmployeeBusinessPhone()));
        if (e.getAddress() != null) {
            c.setAddress(e.getAddress());
        }
    }

    private void applyCustomerFieldsToEmployee(Customer c, Employee e) {
        if (StringUtils.hasText(c.getCustomerFirstName())) {
            e.setEmployeeFirstName(c.getCustomerFirstName());
        }
        if (StringUtils.hasText(c.getCustomerLastName())) {
            e.setEmployeeLastName(c.getCustomerLastName());
        }
        e.setEmployeeMiddleInitial(c.getCustomerMiddleInitial());
        if (StringUtils.hasText(c.getCustomerEmail())) {
            e.setEmployeeWorkEmail(c.getCustomerEmail().trim().toLowerCase());
        }
        if (StringUtils.hasText(c.getCustomerPhone())) {
            e.setEmployeePhone(PhoneNumberFormatter.formatStoredPhone(c.getCustomerPhone()));
        }
        e.setEmployeeBusinessPhone(PhoneNumberFormatter.formatStoredPhoneOrNull(c.getCustomerBusinessPhone()));
        if (c.getAddress() != null) {
            e.setAddress(c.getAddress());
        }
    }
}
