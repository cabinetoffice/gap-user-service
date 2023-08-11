package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.dto.DepartmentDto;
import gov.cabinetofice.gapuserservice.dto.RoleDto;
import gov.cabinetofice.gapuserservice.dto.SuperAdminDashboardPageDto;
import gov.cabinetofice.gapuserservice.dto.UserDto;
import gov.cabinetofice.gapuserservice.exceptions.ForbiddenException;
import gov.cabinetofice.gapuserservice.service.DepartmentService;
import gov.cabinetofice.gapuserservice.service.RoleService;
import gov.cabinetofice.gapuserservice.service.user.OneLoginUserService;
import gov.cabinetofice.gapuserservice.web.controlleradvice.Error;
import gov.cabinetofice.gapuserservice.web.controlleradvice.ErrorResponseBody;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@RequiredArgsConstructor
@Controller
@ConditionalOnProperty(value = "feature.onelogin.enabled", havingValue = "true")
public class SuperAdminController {
    private final DepartmentService departmentService;
    private final RoleService roleService;
    private final OneLoginUserService oneLoginUserService;

    @GetMapping("/super-admin-dashboard") ResponseEntity<?> superAdminDashboard(
            final HttpServletRequest httpRequest,
            final Pageable pagination,
            @RequestParam(value = "departments", required = false) String departments,
            @RequestParam(value = "roles", required = false) String roles,
            @RequestParam(value = "searchTerm", required = false) String searchTerm,
            @RequestParam(value = "clearAllFilters", required = false) boolean clearAllFilters
    ){
        if (!roleService.isSuperAdmin(httpRequest)) {
            throw new ForbiddenException();
        }

        if(searchTerm != null){
            if(searchTerm.length() > 255){
                Error errorResponse = Error.builder().errorMessage("Search term must be less than 255 characters").fieldName("searchTerm").build();
                ErrorResponseBody errorResponseBody = ErrorResponseBody.builder().responseAccepted(false).message("Search term must be less than 255 characters").errors(List.of(errorResponse)).build();
                return new ResponseEntity(errorResponseBody, HttpStatus.BAD_REQUEST);
             }
        }

        List<DepartmentDto> allDepartments = departmentService.getAllDepartments();
        List<RoleDto> allRoles = roleService.getAllRoles();
        List<UserDto> users = oneLoginUserService.getPaginatedUsers(pagination, searchTerm);
        long userCount = oneLoginUserService.getUserCount();

        if (clearAllFilters || (departments.isBlank() && roles.isBlank())){
            return ResponseEntity.ok(SuperAdminDashboardPageDto.builder()
                    .departments(allDepartments)
                    .roles(allRoles)
                    .users(users)
                    .userCount(searchTerm.isBlank() ? userCount : 30)
                    .previousFilterData(clearAllFilters ? null : List.of(List.of(), List.of(), List.of(searchTerm)) )
                    .build());
        }

        List<String> departmentsList = Arrays.stream(departments.split(",")).toList();
        List<String> rolesList = Arrays.stream(roles.split(",")).toList();

        if(departments == "") {
            departmentsList = allDepartments.stream().map(DepartmentDto::getId).collect(Collectors.toList());

        }
        if(roles == "") {
            rolesList = allRoles.stream().map(RoleDto::getId).collect(Collectors.toList());
        }

        List<String> finalDepartments = departmentsList;
        List<String> finalRoles = rolesList;
        List<UserDto> filteredUsers = users.stream()
                .filter(user -> finalDepartments.contains(String.valueOf(user.getDepartment().getId())))
                .filter(user -> user.getRoles().stream().anyMatch(role -> finalRoles.contains(String.valueOf(role.getId()))))
                .collect(Collectors.toList());


        return ResponseEntity.ok(SuperAdminDashboardPageDto.builder()
                .departments(allDepartments)
                .roles(allRoles)
                .users(filteredUsers)
                .userCount(filteredUsers.size())
                .previousFilterData(List.of(Arrays.stream(departments.split(",")).toList(),  Arrays.stream(roles.split(",")).toList(), List.of(searchTerm)))
                .build());
    }
}