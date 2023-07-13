package gov.cabinetofice.gapuserservice.service.user;

import gov.cabinetofice.gapuserservice.dto.UserDto;
import gov.cabinetofice.gapuserservice.mappers.UserMapper;
import gov.cabinetofice.gapuserservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class OneLoginUserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    public List<UserDto> getPaginatedUsers(Pageable pageable) {
        return userRepository.findAll(pageable).stream()
                .map(userMapper::userToUserDto)
                .collect(Collectors.toList());
    }

    public long getUserCount() {
        return userRepository.count();
    }
}