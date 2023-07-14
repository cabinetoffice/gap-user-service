package gov.cabinetofice.gapuserservice.service.user;

import gov.cabinetofice.gapuserservice.dto.UserDto;
import gov.cabinetofice.gapuserservice.exceptions.DepartmentNotFoundException;
import gov.cabinetofice.gapuserservice.exceptions.UserNotFoundException;
import gov.cabinetofice.gapuserservice.mappers.UserMapper;
import gov.cabinetofice.gapuserservice.model.Department;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.repository.DepartmentRepository;
import gov.cabinetofice.gapuserservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class OneLoginUserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final UserMapper userMapper;
    public List<UserDto> getPaginatedUsers(Pageable pageable) {
        return userRepository.findAll(pageable).stream()
                .map(userMapper::userToUserDto)
                .collect(Collectors.toList());
    }

    public User getUserById(int id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("user with id: " + id + "not found"));
    }

    public long getUserCount() {
        return userRepository.count();
    }

    public User updateDepartment(Integer id, Integer departmentId) {
        Optional<User> optionalUser = userRepository.findById(id);

        if (optionalUser.isEmpty()) {
            throw new UserNotFoundException("User not found");
        }

        Optional<Department> optionalDepartment = departmentRepository.findById(departmentId);
        if (optionalDepartment.isEmpty()) {
            throw new DepartmentNotFoundException("Department not found");
        }

        User user = optionalUser.get();
        Department department = optionalDepartment.get();

        user.setDepartment(department);
        return userRepository.save(user);
    }

}