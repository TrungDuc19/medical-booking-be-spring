package com.professionalpractice.medicalbookingbespring.services.impl;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.professionalpractice.medicalbookingbespring.entities.Doctor;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.professionalpractice.medicalbookingbespring.dtos.HealthFormDTO;
import com.professionalpractice.medicalbookingbespring.dtos.request.HealthFormRequest;
import com.professionalpractice.medicalbookingbespring.entities.HealthForm;
import com.professionalpractice.medicalbookingbespring.entities.Shift;
import com.professionalpractice.medicalbookingbespring.entities.User;
import com.professionalpractice.medicalbookingbespring.exceptions.BadRequestException;
import com.professionalpractice.medicalbookingbespring.exceptions.NotFoundException;
import com.professionalpractice.medicalbookingbespring.repositories.HealthFormRepository;
import com.professionalpractice.medicalbookingbespring.repositories.ShiftRepository;
import com.professionalpractice.medicalbookingbespring.repositories.UserRepository;
import com.professionalpractice.medicalbookingbespring.services.HealthFormService;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class HealthFormServiceImpl implements HealthFormService {

    private final HealthFormRepository healthFormRepository;

    private final ModelMapper modelMapper;

    private final UserRepository userRepository;

    private final ShiftRepository shiftRepository;

    private final ServletContext servletContext;

    @Override
    public HealthFormDTO createHealthForm(HealthFormRequest healthFormRequest) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        Shift shift = shiftRepository.findById(healthFormRequest.getShift())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy ca làm việc này này"));

        HealthForm healthForm = HealthForm.builder()
                .user(user)
                .namePatient(healthFormRequest.getNamePatient())
                .email(healthFormRequest.getEmail())
                .phoneNumber(healthFormRequest.getPhoneNumber())
                .address(healthFormRequest.getAddress())
                .shift(shift)
                .reason(healthFormRequest.getReason())
                .cccd(healthFormRequest.getCccdUrl())
                .bhyt(healthFormRequest.getBhytUrl())
                .deniedReason(healthFormRequest.getDeniedReason())
                .status(0)
                .acceptedNumber(0)
                .build();

        HealthForm saveHealthForm = healthFormRepository.save(healthForm);
        return modelMapper.map(saveHealthForm, HealthFormDTO.class);
    }

    @Override
    public Page<HealthFormDTO> getHealthFormByUserId(Long userId, PageRequest pageRequest) {
        Page<HealthForm> healthFormPage = healthFormRepository.queryHealthForm(userId, pageRequest);

        return healthFormPage.map(theHealthForm -> modelMapper.map(theHealthForm, HealthFormDTO.class));
    }

    @Override
    public Page<HealthFormDTO> getHealthForms(PageRequest pageRequest) {
        Page<HealthForm> healthFormPage = healthFormRepository.queryHealthForm(pageRequest);
        return healthFormPage.map(theHealthForm -> modelMapper.map(theHealthForm, HealthFormDTO.class));
    }

    @Override
    public HealthFormDTO updateHealthForm(Long healthFormId, HealthFormRequest healthFormRequest) {
        HealthForm healthForm = healthFormRepository.findById(healthFormId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn khám"));

        if (healthFormRequest.getStatus() != 0) {
            int acceptedNumber = healthFormRepository.countByShiftId(healthForm.getShift().getId());
            if (healthFormRequest.getStatus() == 1) {
                if (healthForm.getShift().getMaxSlot() == acceptedNumber) {
                    throw new BadRequestException("Ca khám đã đầy");
                }
                healthForm.setAcceptedNumber(acceptedNumber + 1);
            }

            healthForm.setStatus(healthFormRequest.getStatus());
        }

        if (healthFormRequest.getDeniedReason() != null) {
            healthForm.setDeniedReason(healthFormRequest.getDeniedReason());
        }

        HealthForm saveHealthForm = healthFormRepository.save(healthForm);
        return modelMapper.map(saveHealthForm, HealthFormDTO.class);
    }

    @Override
    public void deleteHealthFormById(Long healthFormId) {
        HealthForm healthForm = healthFormRepository.findById(healthFormId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn khám"));
        healthFormRepository.deleteById(healthFormId);
    }

    @Override
    public Page<HealthFormDTO> getHistory(String userEmail, Integer status, PageRequest pageRequest) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));

        HealthFormRequest healthFormRequest = new HealthFormRequest();
        healthFormRequest.setUserId(user.getId());
        healthFormRequest.setStatus(status);

        return queryHealthForms(healthFormRequest, pageRequest);
    }

    @Override
    public Page<HealthFormDTO> getHealthFormsByStatus(Integer status, PageRequest pageRequest) {
        HealthFormRequest healthFormRequest = new HealthFormRequest();
        healthFormRequest.setStatus(status);

        return queryHealthForms(healthFormRequest, pageRequest);
    }

    @Override
    public HealthFormDTO updateStatusOfHealthForm(Long heathFormId, HealthFormRequest healthFormRequest) {
        HealthFormDTO healthFormDTO = updateHealthForm(heathFormId, healthFormRequest);

        return healthFormDTO;
    }

    @Override
    public Page<HealthFormDTO> queryHealthForms(HealthFormRequest healthFormRequest, PageRequest pageRequest) {
        Specification<HealthForm> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (healthFormRequest.getUserId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("user").get("id"), healthFormRequest.getUserId()));
            }
            if (healthFormRequest.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), healthFormRequest.getStatus()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<HealthForm> healthFormPage = healthFormRepository.findAll(specification, pageRequest);
        return healthFormPage.map(healthForm -> modelMapper.map(healthForm, HealthFormDTO.class));
    }

    @Override
    public void exportHealthForm(HttpServletResponse response) throws IOException {
        Workbook workbook = new XSSFWorkbook();

        Sheet sheet = workbook.createSheet("Phiếu khám bệnh");

        Row headerRow = sheet.createRow(0);

        CellStyle style = workbook.createCellStyle();
        XSSFFont font = (XSSFFont) workbook.createFont();
        font.setBold(true);
        font.setFontHeight(16);
        style.setFont(font);

        CellStyle styleRow = workbook.createCellStyle();
        XSSFFont fontRow = (XSSFFont) workbook.createFont();
        fontRow.setFontHeight(14);
        styleRow.setFont(fontRow);

        createCell(headerRow, 0, "STT", style);
        sheet.autoSizeColumn(0);
        createCell(headerRow, 1, "Họ và tên", style);
        sheet.autoSizeColumn(1);
        createCell(headerRow, 2, "Tên bệnh nhân", style);
        sheet.autoSizeColumn(2);
        createCell(headerRow, 3, "Email", style);
        sheet.autoSizeColumn(3);
        createCell(headerRow, 4, "Sđt", style);
        sheet.autoSizeColumn(4);
        createCell(headerRow, 5, "Địa chỉ", style);
        sheet.autoSizeColumn(5);
        createCell(headerRow, 6, "Ngày khám", style);
        sheet.autoSizeColumn(6);
        createCell(headerRow, 7, "Giờ khám", style);
        sheet.autoSizeColumn(7);
        createCell(headerRow, 8, "Ví trị khám", style);
        sheet.autoSizeColumn(8);
        createCell(headerRow, 9, "Bác sĩ", style);
        sheet.autoSizeColumn(9);
        createCell(headerRow, 10, "Lý do", style);
        sheet.autoSizeColumn(10);
        createCell(headerRow, 11, "Tình trạng", style);
        sheet.autoSizeColumn(11);
        createCell(headerRow, 12, "Thứ tự khám", style);
        sheet.autoSizeColumn(12);


        List<HealthForm> healthForms = this.healthFormRepository.findAll();
        int rowCount =1;
        for (HealthForm healthForm : healthForms){
            Row row = sheet.createRow(rowCount++);
            int columnCount = 0;
            createCell(row, columnCount++, healthForm.getId(), styleRow);
            sheet.autoSizeColumn(columnCount);
            createCell(row, columnCount++, healthForm.getUser().getFullName(), styleRow);
            sheet.autoSizeColumn(columnCount);
            createCell(row, columnCount++, healthForm.getNamePatient(), styleRow);
            sheet.autoSizeColumn(columnCount);
            createCell(row, columnCount++, healthForm.getEmail(), styleRow);
            sheet.autoSizeColumn(columnCount);
            createCell(row, columnCount++, healthForm.getPhoneNumber(), styleRow);
            sheet.autoSizeColumn(columnCount);
            createCell(row, columnCount++, healthForm.getAddress(), styleRow);
            sheet.autoSizeColumn(columnCount);
            createCell(row, columnCount++, healthForm.getShift().getDate().toString(), styleRow);
            sheet.autoSizeColumn(columnCount);
            createCell(row, columnCount++, healthForm.getShift().getTime(), styleRow);
            sheet.autoSizeColumn(columnCount);
            createCell(row, columnCount++, healthForm.getShift().getPlace(), styleRow);
            sheet.autoSizeColumn(columnCount);
            createCell(row, columnCount++, healthForm.getShift().getDoctor().getName(), styleRow);
            sheet.autoSizeColumn(columnCount);
            createCell(row, columnCount++, healthForm.getReason(), styleRow);
            sheet.autoSizeColumn(columnCount);
            createCell(row, columnCount++, healthForm.getStatus(), styleRow);
            sheet.autoSizeColumn(columnCount);
            createCell(row, columnCount++, healthForm.getAcceptedNumber(), styleRow);
            sheet.autoSizeColumn(columnCount);
        }


        String fileName = saveWorkbookToFile(workbook);



    }

    @Override
    public void createCell(Row row, int columnCount, Object value, CellStyle style) {
        Cell cell = row.createCell(columnCount);
        if (value instanceof Long) {
            cell.setCellValue((Long) value);
        } else if (value instanceof Integer) {
            cell.setCellValue((Integer) value);
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof LocalDateTime) {
            cell.setCellValue((LocalDateTime) value);
        } else {
            cell.setCellValue((String) value);
        }
    }

    private String saveWorkbookToFile(Workbook workbook) {
        // Tạo ByteArrayOutputStream để lưu dữ liệu workbook
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            workbook.write(baos);
            workbook.close();

            // Tạo ByteArrayInputStream từ ByteArrayOutputStream
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(baos.toByteArray())) {
                // Tạo tên file duy nhất
                String filename = "health-form.xlsx"; // hoặc tên khác nếu cần
                String uniqueFilename = UUID.randomUUID().toString() + "_" + filename;

                // Tạo đối tượng Path cho thư mục uploads
                Path uploadDir = Paths.get("uploads");

                // Kiểm tra và tạo thư mục uploads nếu chưa tồn tại
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                }

                // Tạo đường dẫn đích để lưu file
                Path destination = Paths.get(uploadDir.toString(), uniqueFilename);

                // Sao chép nội dung workbook vào file đích
                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);

                // Trả về tên file duy nhất
                return uniqueFilename;
            }
        } catch (IOException e) {
            System.err.println("Lỗi khi lưu file: " + e.getMessage());
            throw new RuntimeException("Lỗi khi lưu file", e);
        }
    }
}
