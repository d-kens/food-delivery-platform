package com.example.user_service.service.mapper;

import com.example.user_service.common.Role;
import com.example.user_service.dto.UserResponseDto;
import com.example.user_service.dto.RegistrationRequestDto;
import com.example.user_service.dto.RegistrationResponseDto;
import com.example.user_service.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "phoneNumber", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toUserEntity(RegistrationRequestDto request);

    RegistrationResponseDto toRegistrationResponse(User user);

    @Mapping(target = "userId", source = "id")
    UserResponseDto toUserResponse(User user);

    default List<String> toRoleNames(Set<Role> roles) {
        return roles.stream().map(Role::name).sorted().toList();
    }
}
