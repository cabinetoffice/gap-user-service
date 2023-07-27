package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.dto.DepartmentDto;
import gov.cabinetofice.gapuserservice.exceptions.ForbiddenException;
import gov.cabinetofice.gapuserservice.model.Department;
import gov.cabinetofice.gapuserservice.service.DepartmentService;
import gov.cabinetofice.gapuserservice.service.RoleService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;

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
    public ResponseEntity<Department> getById(@PathVariable int id, final HttpServletRequest httpRequest) {
        if (!roleService.isSuperAdmin(httpRequest)) {
            throw new ForbiddenException();
        }
        return ResponseEntity.ok(departmentService.getDepartmentById(id));
    }

    @PatchMapping("/department/{id}")
        public ResponseEntity<String> updateDepartment(final HttpServletRequest httpRequest,
                                                       @RequestBody final LinkedHashMap<String, String> body,
                                                       @PathVariable int id) {
            if (!roleService.isSuperAdmin(httpRequest)) {
                throw new ForbiddenException();
            }
            departmentService.updateDepartment(id, body.get("departmentName"), body.get("ggisId"));
            return ResponseEntity.ok("Department updated");
        }
    }
