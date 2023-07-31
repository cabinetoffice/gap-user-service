package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.dto.DepartmentDto;
import gov.cabinetofice.gapuserservice.dto.UpdateDepartmentReqDto;
import gov.cabinetofice.gapuserservice.exceptions.DepartmentNotFoundException;
import gov.cabinetofice.gapuserservice.exceptions.ForbiddenException;
import gov.cabinetofice.gapuserservice.mappers.DepartmentMapper;
import gov.cabinetofice.gapuserservice.model.Department;
import gov.cabinetofice.gapuserservice.service.DepartmentService;
import gov.cabinetofice.gapuserservice.service.RoleService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.mapstruct.factory.Mappers;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@RestController
public class DepartmentController {
    private final RoleService roleService;
    private final DepartmentService departmentService;

    @GetMapping("/department")
        public ResponseEntity<List<DepartmentDto>> getAll(final HttpServletRequest httpRequest) {
            if (!roleService.isSuperAdmin(httpRequest)) {
                throw new ForbiddenException();
            }
            return ResponseEntity.ok(departmentService.getAllDepartments());
        }

    @GetMapping("/department/{id}")
    public ResponseEntity<DepartmentDto> getById(@PathVariable int id, final HttpServletRequest httpRequest) {
        if (!roleService.isSuperAdmin(httpRequest)) {
            throw new ForbiddenException();
        }
        Optional<Department> department = departmentService.getDepartmentById(id);
        if(department == null){
            throw new DepartmentNotFoundException("Department not found");
        }
        DepartmentMapper INSTANCE = Mappers.getMapper(DepartmentMapper.class);
        return ResponseEntity.ok(INSTANCE.departmentToDepartmentDto(department.get()));
    }

    @PatchMapping("/department/{id}")
        public ResponseEntity<String> updateDepartment(final HttpServletRequest httpRequest,
                                                       @RequestBody final UpdateDepartmentReqDto body,
                                                       @PathVariable int id) throws DepartmentNotFoundException, ForbiddenException {
            if (!roleService.isSuperAdmin(httpRequest)) {
                throw new ForbiddenException();
            }
            Optional<Department> department = departmentService.getDepartmentById(id);

            if(body.getDepartmentName() == null || body.getGgisId() == null){
                throw new RuntimeException("Department name or ggis id is null");
            }

            if(department == null){
                throw new DepartmentNotFoundException("Department not found");
            }
            departmentService.updateDepartment(department.get(), body.getDepartmentName(), body.getGgisId());
            return ResponseEntity.ok("Department updated");
        }
    }
