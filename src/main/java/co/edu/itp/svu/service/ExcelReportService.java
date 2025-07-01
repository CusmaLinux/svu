package co.edu.itp.svu.service;

import java.io.ByteArrayInputStream;

public interface ExcelReportService {
    ByteArrayInputStream generatePqrsReport(String informePqrsId);
}
