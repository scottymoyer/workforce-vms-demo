package com.workforce.vms.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * SQL INJECTION — the demo centerpiece.
 *
 * A worker/timesheet search that concatenates the request parameter directly into a
 * JDBC query. At runtime this produces:
 *   (a) an IAST vulnerability: tainted HTTP parameter -&gt; SQL query sink, with the exact
 *       file/line below; and
 *   (b) an APM trace where the executed SQL statement AND the user-supplied input are
 *       visible on the db span ("see the input parameter in the trace").
 */
@RestController
public class WorkerSearchController {

    private final JdbcTemplate jdbc;

    public WorkerSearchController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * GET /api/workers/search?name=Ada
     * Benign input returns matching workers; malicious input (e.g. ' OR '1'='1)
     * changes the query semantics.
     */
    @GetMapping("/api/workers/search")
    public List<Map<String, Object>> searchWorkers(@RequestParam("name") String name) {
        // DEMO-VULN: SQL Injection (CWE-89). Request param concatenated into the query.
        String sql = "SELECT id, name, title, vendor_id, status, bill_rate "
                + "FROM workers WHERE name LIKE '%" + name + "%'";
        return jdbc.queryForList(sql);
    }

    /**
     * GET /api/timesheets/search?status=SUBMITTED
     * Second reachable SQLi sink over the timesheets table.
     */
    @GetMapping("/api/timesheets/search")
    public List<Map<String, Object>> searchTimesheets(@RequestParam("status") String status) {
        // DEMO-VULN: SQL Injection (CWE-89). Request param concatenated into the query.
        String sql = "SELECT id, worker_id, week_ending, hours, status "
                + "FROM timesheets WHERE status = '" + status + "'";
        return jdbc.queryForList(sql);
    }
}
