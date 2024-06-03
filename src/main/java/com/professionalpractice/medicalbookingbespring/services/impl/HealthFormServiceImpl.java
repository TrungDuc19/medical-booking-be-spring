package com.professionalpractice.medicalbookingbespring.services.impl;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.professionalpractice.medicalbookingbespring.entities.Doctor;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletOutputStream;
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
    public void exportHealthForm(HttpServletResponse response) throws IOException{
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Phiếu khám bệnh");

        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        CellStyle contentStyle = workbook.createCellStyle();
        Font contentFont = workbook.createFont();
        contentStyle.setFont(contentFont);

        createCell(headerRow, 0, "STT", headerStyle);
        createCell(headerRow, 1, "Họ và tên", headerStyle);
        createCell(headerRow, 2, "Tên bệnh nhân", headerStyle);
        createCell(headerRow, 3, "Email", headerStyle);
        createCell(headerRow, 4, "Sđt", headerStyle);
        createCell(headerRow, 5, "Địa chỉ", headerStyle);
        createCell(headerRow, 6, "Ngày khám", headerStyle);
        createCell(headerRow, 7, "Giờ khám", headerStyle);
        createCell(headerRow, 8, "Vị trí khám", headerStyle);
        createCell(headerRow, 9, "Bác sĩ", headerStyle);
        createCell(headerRow, 10, "Lý do", headerStyle);
        createCell(headerRow, 11, "Tình trạng", headerStyle);
        createCell(headerRow, 12, "Thứ tự khám", headerStyle);

        List<HealthForm> healthForms = healthFormRepository.findAll();

        int rowCount = 1;
        for (HealthForm healthForm : healthForms) {
            Row row = sheet.createRow(rowCount++);
            int columnCount = 0;

            createCell(row, columnCount++, healthForm.getId(), contentStyle);
            createCell(row, columnCount++, healthForm.getUser().getFullName(), contentStyle);
            createCell(row, columnCount++, healthForm.getNamePatient(), contentStyle);
            createCell(row, columnCount++, healthForm.getEmail(), contentStyle);
            createCell(row, columnCount++, healthForm.getPhoneNumber(), contentStyle);
            createCell(row, columnCount++, healthForm.getAddress(), contentStyle);
            createCell(row, columnCount++, healthForm.getShift().getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), contentStyle); // Định dạng ngày
            createCell(row, columnCount++, healthForm.getShift().getTime(), contentStyle);
            createCell(row, columnCount++, healthForm.getShift().getPlace(), contentStyle);
            createCell(row, columnCount++, healthForm.getShift().getDoctor().getName(), contentStyle);
            createCell(row, columnCount++, healthForm.getReason(), contentStyle);
            String statusText = "";
            switch (healthForm.getStatus()) {
                case 0:
                    statusText = "Chưa phê duyệt";
                    break;
                case 1:
                    statusText = "Đã phê duyệt";
                    break;
                case 2:
                    statusText = "Đã từ chối";
                    break;
            }
            createCell(row, columnCount++, statusText, contentStyle);
            createCell(row, columnCount++, healthForm.getAcceptedNumber(), contentStyle);
        }
        ServletOutputStream outputStream = response.getOutputStream();
        workbook.write(outputStream);
        workbook.close();
        outputStream.close();
    }

    private void createCell(Row row, int columnCount, Object value, CellStyle style) {
        Cell cell = row.createCell(columnCount);
        if (value instanceof Long) {
            cell.setCellValue((Long) value);
        } else if (value instanceof Integer) {
            cell.setCellValue((Integer) value);
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof LocalDateTime) {
            cell.setCellValue(((LocalDate) value).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        } else {
            cell.setCellValue((String) value);
        }
        if (style != null) {
            cell.setCellStyle(style);
        }
    }
}
