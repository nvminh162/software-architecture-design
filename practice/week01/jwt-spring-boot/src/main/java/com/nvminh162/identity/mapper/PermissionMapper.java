package com.nvminh162.identity.mapper;

import org.mapstruct.Mapper;

import com.nvminh162.identity.dto.request.PermissionRequest;
import com.nvminh162.identity.dto.response.PermissionResponse;
import com.nvminh162.identity.entity.Permission;

@Mapper(componentModel = "spring")
public interface PermissionMapper {
    Permission toPermission(PermissionRequest request);

    PermissionResponse toPermissionResponse(Permission permission);
}
