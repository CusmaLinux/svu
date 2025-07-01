package co.edu.itp.svu.web.rest;

import co.edu.itp.svu.service.ExcelReportService;
import java.io.ByteArrayInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
public class ReportResource {

    private final Logger log = LoggerFactory.getLogger(ReportResource.class);
    private final ExcelReportService excelReportService;

    public ReportResource(ExcelReportService excelReportService) {
        this.excelReportService = excelReportService;
    }

    /**
     * {@code GET /pqrs/{id}/download} : Download an Excel report for a specific
     * InformePqrs.
     *
     * @param id the ID of the InformePqrs to generate the report for.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the Excel
     *         file as the body.
     */
    @GetMapping("/pqrs/{id}/download")
    public ResponseEntity<Resource> downloadPqrsReport(@PathVariable String id) {
        log.debug("REST request to download Excel report for InformePqrs : {}", id);

        ByteArrayInputStream in = excelReportService.generatePqrsReport(id);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=informe-pqrs-" + id + ".xlsx");

        return ResponseEntity.ok()
            .headers(headers)
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(new InputStreamResource(in));
    }
}
