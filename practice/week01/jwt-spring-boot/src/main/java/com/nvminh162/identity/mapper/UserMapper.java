package com.nvminh162.identity.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.nvminh162.identity.dto.request.UserCreationRequest;
import com.nvminh162.identity.dto.request.UserUpdateRequest;
import com.nvminh162.identity.dto.response.UserResponse;
import com.nvminh162.identity.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toUser(UserCreationRequest request);

    @Mapping(target = "roles", ignore = true)
    void updateUser(@MappingTarget User user, UserUpdateRequest request); // mapping request to user

    // @Mapping(target = "firstName", ignore = true) //false default
    //    @Mapping(source = "firstName", target = "lastName")
    UserResponse toUserResponse(User user);
}
