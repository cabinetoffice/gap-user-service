package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.dto.DepartmentDto;
import gov.cabinetofice.gapuserservice.exceptions.ForbiddenException;
import gov.cabinetofice.gapuserservice.service.DepartmentService;
import gov.cabinetofice.gapuserservice.service.RoleService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PatchMapping("/department")
        public ResponseEntity<String> updateDepartment(final HttpServletRequest httpRequest,
                                                       @RequestBody final Object details) {
            if (!roleService.isSuperAdmin(httpRequest)) {
                throw new ForbiddenException();
            }

            departmentService.updateDepartment(1);

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
                                                       @RequestBody final DepartmentDto department) {
            if (!roleService.isSuperAdmin(httpRequest)) {
                throw new ForbiddenException();
            }

            departmentService.createDepartment(department.getName(), department.getGgisID());
            return ResponseEntity.ok("Department created");
        }

    }
