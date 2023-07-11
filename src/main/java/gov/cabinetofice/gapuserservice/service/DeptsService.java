package gov.cabinetofice.gapuserservice.service;


import gov.cabinetofice.gapuserservice.dto.DeptDto;
import gov.cabinetofice.gapuserservice.mappers.DeptMapper;
import gov.cabinetofice.gapuserservice.repository.DeptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeptsService {
    private final DeptRepository deptRepository;
    private final DeptMapper deptMapper;
    public List<DeptDto> getAllDepts() {
        return deptRepository.findAll().stream()
                .map(deptMapper::deptToDeptDto)
                .collect(Collectors.toList());
    }
}
