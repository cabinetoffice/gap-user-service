package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.dto.DepartmentDto;
import gov.cabinetofice.gapuserservice.dto.DepartmentReqDto;
import gov.cabinetofice.gapuserservice.exceptions.DepartmentNotFoundException;
import gov.cabinetofice.gapuserservice.exceptions.ForbiddenException;
import gov.cabinetofice.gapuserservice.mappers.DepartmentMapper;
import gov.cabinetofice.gapuserservice.model.Department;
import gov.cabinetofice.gapuserservice.service.DepartmentService;
import gov.cabinetofice.gapuserservice.service.RoleService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@RestController
public class DepartmentController {
    private final RoleService roleService;
    private final DepartmentService departmentService;

    private final DepartmentMapper mapper;

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

        if(department.isEmpty()){
            throw new DepartmentNotFoundException("Department not found");
        }
        return ResponseEntity.ok(mapper.departmentToDepartmentDto(department.get()));
    }

    @PatchMapping("/department/{id}")
        public ResponseEntity<String> updateDepartment(final HttpServletRequest httpRequest,
                                                     @Validated @RequestBody final DepartmentReqDto body,
                                                       @PathVariable int id) throws DepartmentNotFoundException, ForbiddenException {
            if (!roleService.isSuperAdmin(httpRequest)) {
                throw new ForbiddenException();
            }
            Optional<Department> department = departmentService.getDepartmentById(id);

            if(department.isEmpty()){
                throw new DepartmentNotFoundException("Could not update department: Department not found");
            }
            departmentService.updateDepartment(department.get(), body.getDepartmentName(), body.getGgisId());
            return ResponseEntity.ok("Department updated");
        }

    @DeleteMapping("/department/{id}")
    public ResponseEntity<String> deleteDepartment(final HttpServletRequest httpRequest,
                                                   @PathVariable final int id) {
        if (!roleService.isSuperAdmin(httpRequest)) {
            throw new ForbiddenException();
        }

        departmentService.deleteDepartment(id);

        return ResponseEntity.ok("Department deleted");
    }

    @PostMapping("/department")
    public ResponseEntity<String> createDepartment(final HttpServletRequest httpRequest,
                                                   @Validated @RequestBody final DepartmentReqDto department) {
        if (!roleService.isSuperAdmin(httpRequest)) {
            throw new ForbiddenException();
        }

        departmentService.createDepartment(department.getDepartmentName(), department.getGgisId());
        return ResponseEntity.ok("Department created");
    }
    }
