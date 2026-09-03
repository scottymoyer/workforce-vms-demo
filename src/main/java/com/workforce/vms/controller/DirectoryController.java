package com.workforce.vms.controller;

import com.workforce.vms.config.DemoSecrets;
import com.workforce.vms.model.Approval;
import com.workforce.vms.model.Timesheet;
import com.workforce.vms.model.Vendor;
import com.workforce.vms.model.Worker;
import com.workforce.vms.repository.ApprovalRepository;
import com.workforce.vms.repository.TimesheetRepository;
import com.workforce.vms.repository.VendorRepository;
import com.workforce.vms.repository.WorkerRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Benign, parameterized read endpoints. These drive normal APM traces, Database
 * Monitoring load, and IAST reachability for the safe (non-injectable) code paths.
 */
@RestController
public class DirectoryController {

    private final VendorRepository vendors;
    private final WorkerRepository workers;
    private final TimesheetRepository timesheets;
    private final ApprovalRepository approvals;

    public DirectoryController(VendorRepository vendors, WorkerRepository workers,
                               TimesheetRepository timesheets, ApprovalRepository approvals) {
        this.vendors = vendors;
        this.workers = workers;
        this.timesheets = timesheets;
        this.approvals = approvals;
    }

    @GetMapping("/api/vendors")
    public List<Vendor> listVendors() { return vendors.findAll(); }

    @GetMapping("/api/workers")
    public List<Worker> listWorkers() { return workers.findAll(); }

    @GetMapping("/api/timesheets")
    public List<Timesheet> listTimesheets() { return timesheets.findAll(); }

    @GetMapping("/api/approvals")
    public List<Approval> listApprovals() { return approvals.findAll(); }

    /**
     * Vendor billing status stub — references the hardcoded billing API key so the
     * committed secret is actually reachable at runtime (not just present in source).
     */
    @GetMapping("/api/vendors/{id}/billing-status")
    public Map<String, Object> billingStatus(@PathVariable Long id) {
        // DEMO-VULN: hardcoded secret used at runtime (outbound integration stub).
        String maskedKey = DemoSecrets.BILLING_API_KEY.substring(0, 7) + "…";
        return Map.of(
                "vendorId", id,
                "billingProvider", "acme-payments",
                "authScheme", "Bearer " + maskedKey,
                "status", "CURRENT");
    }
}
