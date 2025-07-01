package co.edu.itp.svu.service.impl;

import co.edu.itp.svu.domain.InformePqrs;
import co.edu.itp.svu.repository.InformePqrsRepository;
import co.edu.itp.svu.service.ExcelReportService;
import co.edu.itp.svu.service.report.PqrsExcelGenerator;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ExcelReportServiceImpl implements ExcelReportService {

    private final Logger LOG = LoggerFactory.getLogger(ExcelReportServiceImpl.class);

    private final InformePqrsRepository informPqrsRepository;

    public ExcelReportServiceImpl(InformePqrsRepository informPqrsRepository) {
        this.informPqrsRepository = informPqrsRepository;
    }

    @Override
    public ByteArrayInputStream generatePqrsReport(String informPqrsId) {
        InformePqrs informPqrs = informPqrsRepository
            .findById(informPqrsId)
            .orElseThrow(() -> new RuntimeException("Inform not found with id: " + informPqrsId));

        try {
            return PqrsExcelGenerator.generate(informPqrs);
        } catch (IOException e) {
            LOG.error("Error generating Excel report for InformePqrs id {}: {}", informPqrsId, e.getMessage());
            throw new RuntimeException("Failed to generate Excel file.", e);
        }
    }
}
