package com.nvminh162.identity.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.nvminh162.identity.dto.request.RoleRequest;
import com.nvminh162.identity.dto.response.RoleResponse;
import com.nvminh162.identity.entity.Role;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    @Mapping(target = "permissions", ignore = true)
    Role toRole(RoleRequest request);

    RoleResponse toRoleResponse(Role role);
}
