package com.professionalpractice.medicalbookingbespring.controllers;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.professionalpractice.medicalbookingbespring.config.RestApiV1;
import com.professionalpractice.medicalbookingbespring.dtos.HealthFormDTO;
import com.professionalpractice.medicalbookingbespring.dtos.request.HealthFormRequest;
import com.professionalpractice.medicalbookingbespring.dtos.response.PaginationResponse;
import com.professionalpractice.medicalbookingbespring.services.CloudinaryService;
import com.professionalpractice.medicalbookingbespring.services.EmailService;
import com.professionalpractice.medicalbookingbespring.services.HealthFormService;
import com.professionalpractice.medicalbookingbespring.utils.CustomResponse;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestApiV1
public class HealthFormController {

    private final HealthFormService healthFormService;

    private final CloudinaryService cloudinaryService;

    private final EmailService emailService;

    @PostMapping("/health-forms")
    public ResponseEntity<?> createHealthForms(
            @ModelAttribute HealthFormRequest healthFormRequest,
            @RequestParam(value = "cccd") MultipartFile cccdFile,
            @RequestParam(value = "bhyt", required = false) MultipartFile bhytFile) {

        String ccdUrl = cloudinaryService.uploadImage(cccdFile);
        healthFormRequest.setCccdUrl(ccdUrl);
        if (bhytFile != null) {
            String bhytUrl = cloudinaryService.uploadImage(bhytFile);
            healthFormRequest.setBhytUrl(bhytUrl);
        }

        HealthFormDTO healthForm = healthFormService.createHealthForm(healthFormRequest);
        return CustomResponse.success(HttpStatus.CREATED, "Tạo đơn khám thành công", healthForm);
    }

    @GetMapping("/health-forms")
    public ResponseEntity<?> getHealthForms(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) Integer status) {
        PageRequest pageRequest = PageRequest.of(
                page - 1, limit,
                Sort.by("id").ascending());
        Page<HealthFormDTO> healthFormPage = healthFormService.getHealthFormsByStatus(status, pageRequest);

        long totalPages = healthFormPage.getTotalElements();
        List<HealthFormDTO> healthForms = healthFormPage.getContent();

        return CustomResponse.success(new PaginationResponse(page, limit, totalPages, healthForms));
    }

    @GetMapping("/health-forms/history")
    public ResponseEntity<?> getHealthFormsByUserId(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) Integer status) {
        PageRequest pageRequest = PageRequest.of(
                page - 1, limit,
                Sort.by("id").ascending());

        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        Page<HealthFormDTO> healthFormPage = healthFormService.getHistory(userEmail, status, pageRequest);

        long totalPages = healthFormPage.getTotalElements();
        List<HealthFormDTO> healthForms = healthFormPage.getContent();

        return CustomResponse.success(new PaginationResponse(page, limit, totalPages, healthForms));
    }

    @PutMapping("/health-forms/{healthFormId}")
    public ResponseEntity<?> updateHealthForm(@PathVariable Long healthFormId,
            @RequestBody HealthFormRequest healthFormRequest) {
        HealthFormDTO healthForm = healthFormService.updateHealthForm(healthFormId, healthFormRequest);
        return CustomResponse.success("Cập nhật thành công");
    }

    @DeleteMapping("/health-forms/{healthFormId}")
    public ResponseEntity<?> deleteHealthForm(@PathVariable Long healthFormId) {
        healthFormService.deleteHealthFormById(healthFormId);
        return CustomResponse.success("Xóa thành công");
    }

    @PatchMapping("/health-forms/{healthFormId}")
    public ResponseEntity<?> aproveHealthForm(@PathVariable Long healthFormId,
            @RequestBody HealthFormRequest healthFormRequest) {
        HealthFormDTO healthForm = healthFormService.updateHealthForm(healthFormId, healthFormRequest);
        return CustomResponse.success("Cập nhật trạng thái thành công");
    }

    @PatchMapping("/health-forms/status/{healthFormId}")
    public ResponseEntity<?> updateStatusOfHealthForm(@PathVariable Long healthFormId,
            @RequestBody HealthFormRequest healthFormRequest) {
        HealthFormDTO healthForm = healthFormService.updateStatusOfHealthForm(healthFormId, healthFormRequest);

        switch (healthForm.getStatus()) {
            case 1:
                emailService.sendHealthFormConfirmation(healthForm);
                break;
            case 2:
                emailService.sendHealthFormRejection(healthForm);
                break;
            default:
                break;
        }

        return CustomResponse.success("Cập nhật trạng thái thành công");
    }

    @GetMapping("/health-forms/export/excel")
    public ResponseEntity<?> exportHealthForm(HttpServletResponse response) {
        try {
            healthFormService.exportHealthForm(response);
            return ResponseEntity.ok("Export Successful");
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Export Failed");
        }

//        response.setContentType("application/octet-stream");
//        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
//        String currentDateTime = dateFormatter.format(new Date());
//
//        String headerKey = "Content-Disposition";
//        String headerValue = "attachment; filename=healthforms_" + currentDateTime + ".xlsx";
//        response.setHeader(headerKey, headerValue);
//
//        this.exportExcelService.exportHealthForm(response);
//        return ResponseEntity.ok("Export Successful");
    }

}
