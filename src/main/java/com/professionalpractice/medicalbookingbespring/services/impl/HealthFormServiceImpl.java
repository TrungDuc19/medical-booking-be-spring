package com.professionalpractice.medicalbookingbespring.services.impl;

import com.professionalpractice.medicalbookingbespring.dtos.HealthFormDTO;
import com.professionalpractice.medicalbookingbespring.dtos.request.HealthFormRequest;
import com.professionalpractice.medicalbookingbespring.entities.HealthForm;
import com.professionalpractice.medicalbookingbespring.entities.Shift;
import com.professionalpractice.medicalbookingbespring.entities.User;
import com.professionalpractice.medicalbookingbespring.exceptions.NotFoundException;
import com.professionalpractice.medicalbookingbespring.repositories.HealthFormRepository;
import com.professionalpractice.medicalbookingbespring.repositories.ShiftRepository;
import com.professionalpractice.medicalbookingbespring.repositories.UserRepository;
import com.professionalpractice.medicalbookingbespring.services.HealthFormService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HealthFormServiceImpl implements HealthFormService {

    private final HealthFormRepository healthFormRepository;

    private final ModelMapper modelMapper;

    private final UserRepository userRepository;

    private final ShiftRepository shiftRepository;

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
            .shift(shift)
            .reason(healthFormRequest.getReason())
            .cccd(healthFormRequest.getCccd())
            .bhyt(healthFormRequest.getBhyt())
            .deniedReason(healthFormRequest.getDeniedReason())
            .build();
        HealthForm saveHealthForm = healthFormRepository.save(healthForm);
        return modelMapper.map(saveHealthForm,HealthFormDTO.class);
    }

    @Override
    public Page<HealthFormDTO> getHealthFormByUserId(Long userId, PageRequest pageRequest) {
        HealthForm healthForm = healthFormRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn khám"));
        Page<HealthForm> healthFormPage = healthFormRepository.queryHealthForm(userId, pageRequest);
        return healthFormPage.map(theHealthForm -> modelMapper.map(theHealthForm,HealthFormDTO.class));
    }

    @Override
    public Page<HealthFormDTO> getHealthForms(PageRequest pageRequest) {
        Page<HealthForm> healthFormPage = healthFormRepository.queryHealthForm(pageRequest);
        return healthFormPage.map(theHealthForm -> modelMapper.map(theHealthForm,HealthFormDTO.class));
    }

    @Override
    public HealthFormDTO updateHealthForm(Long healthFormId, HealthFormRequest healthFormRequest) {
        HealthForm healthForm = healthFormRepository.findById(healthFormId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn khám"));
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        Shift shift = shiftRepository.findById(healthFormRequest.getShift())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ca làm việc này này"));
        HealthForm newHealthForm = HealthForm.builder()
            .id(healthFormId)
            .user(user)
            .namePatient(healthFormRequest.getNamePatient())
            .email(healthFormRequest.getEmail())
            .phoneNumber(healthFormRequest.getPhoneNumber())
            .shift(shift)
            .reason(healthFormRequest.getReason())
            .cccd(healthFormRequest.getCccd())
            .bhyt(healthFormRequest.getBhyt())
            .deniedReason(healthFormRequest.getDeniedReason())
            .build();
        HealthForm saveHealthForm = healthFormRepository.save(newHealthForm);
        return modelMapper.map(saveHealthForm,HealthFormDTO.class);
    }

    @Override
    public void deleteHealthFormById(Long healthFormId) {
        HealthForm healthForm = healthFormRepository.findById(healthFormId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn khám"));
        healthFormRepository.deleteById(healthFormId);
    }
}