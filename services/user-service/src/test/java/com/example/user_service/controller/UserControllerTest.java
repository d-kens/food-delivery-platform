package com.example.user_service.controller;

import com.example.user_service.common.Role;
import com.example.user_service.common.UserStatus;
import com.example.user_service.config.SecurityConfig;
import com.example.user_service.dto.UpdateUserRequestDto;
import com.example.user_service.dto.UserResponseDto;
import com.example.user_service.exception.EmailAlreadyExistsException;
import com.example.user_service.exception.InvalidCurrentPasswordException;
import com.example.user_service.exception.OwnStatusChangeException;
import com.example.user_service.exception.PhoneNumberAlreadyExistsException;
import com.example.user_service.exception.UserNotFoundException;
import com.example.user_service.dto.PageResponseDto;
import com.example.user_service.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    private static final String USERS_URL = "/users";
    private static final UUID ADMIN_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void getUsers_returnsPage_forAdmin() throws Exception {
        var user = new UserResponseDto("id-1", "Jane", "Doe", "jane.doe@gmail.com", List.of("USER"));
        when(userService.getUsers(UserStatus.SUSPENDED, Role.RIDER, "jane", 1, 10))
                .thenReturn(new PageResponseDto<>(List.of(user), 1, 10, 11, 2));

        mockMvc.perform(get(USERS_URL)
                        .param("status", "SUSPENDED")
                        .param("role", "RIDER")
                        .param("search", "jane")
                        .param("page", "1")
                        .param("size", "10")
                        .with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userId").value("id-1"))
                .andExpect(jsonPath("$.content[0].email").value("jane.doe@gmail.com"))
                .andExpect(jsonPath("$.content[0].roles[0]").value("USER"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(11))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void getUsers_usesDefaults_whenNoParams() throws Exception {
        when(userService.getUsers(null, null, null, 0, 20))
                .thenReturn(new PageResponseDto<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get(USERS_URL).with(admin()))
                .andExpect(status().isOk());

        verify(userService).getUsers(isNull(), isNull(), isNull(), eq(0), eq(20));
    }

    @Test
    void getUsers_returnsForbidden_forNonAdmin() throws Exception {
        mockMvc.perform(get(USERS_URL).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());

        verify(userService, never()).getUsers(any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void getUsers_returnsUnauthorized_withoutToken() throws Exception {
        mockMvc.perform(get(USERS_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUsers_returnsBadRequest_whenSizeTooLarge() throws Exception {
        mockMvc.perform(get(USERS_URL).param("size", "101").with(admin()))
                .andExpect(status().isBadRequest());

        verify(userService, never()).getUsers(any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void getUsers_returnsBadRequest_whenPageNegative() throws Exception {
        mockMvc.perform(get(USERS_URL).param("page", "-1").with(admin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUsers_returnsBadRequest_whenStatusUnknown() throws Exception {
        mockMvc.perform(get(USERS_URL).param("status", "BANNED").with(admin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUserById_returnsUser_forAdmin() throws Exception {
        var id = UUID.randomUUID();
        when(userService.getUserById(id))
                .thenReturn(new UserResponseDto(id.toString(), "Jane", "Doe", "jane.doe@gmail.com", List.of("USER")));

        mockMvc.perform(get(USERS_URL + "/" + id).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(id.toString()))
                .andExpect(jsonPath("$.email").value("jane.doe@gmail.com"));
    }

    @Test
    void getUserById_returnsNotFound_whenMissing() throws Exception {
        var id = UUID.randomUUID();
        when(userService.getUserById(id)).thenThrow(new UserNotFoundException());

        mockMvc.perform(get(USERS_URL + "/" + id).with(admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("User Not Found"));
    }

    @Test
    void getUserById_returnsForbidden_forNonAdmin() throws Exception {
        mockMvc.perform(get(USERS_URL + "/" + UUID.randomUUID())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());

        verify(userService, never()).getUserById(any());
    }

    @Test
    void getUserById_returnsBadRequest_whenIdIsNotUuid() throws Exception {
        mockMvc.perform(get(USERS_URL + "/not-a-uuid").with(admin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void me_isAllowed_forNonAdmin() throws Exception {
        var id = UUID.randomUUID();
        when(userService.getCurrentUser(id))
                .thenReturn(new UserResponseDto(id.toString(), "Jane", "Doe", "jane.doe@gmail.com", List.of("USER")));

        mockMvc.perform(get(USERS_URL + "/me")
                        .with(jwt().jwt(j -> j.subject(id.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(id.toString()));
    }

    @Test
    void updateStatus_returnsNoContent_forAdmin() throws Exception {
        var id = UUID.randomUUID();

        mockMvc.perform(patch(USERS_URL + "/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"SUSPENDED\"}")
                        .with(admin()))
                .andExpect(status().isNoContent());

        verify(userService).updateUserStatus(ADMIN_ID, id, UserStatus.SUSPENDED);
    }

    @Test
    void updateStatus_returnsForbidden_forNonAdmin() throws Exception {
        mockMvc.perform(patch(USERS_URL + "/" + UUID.randomUUID() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"SUSPENDED\"}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());

        verify(userService, never()).updateUserStatus(any(), any(), any());
    }

    @Test
    void updateStatus_returnsBadRequest_whenStatusMissing() throws Exception {
        mockMvc.perform(patch(USERS_URL + "/" + UUID.randomUUID() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.status[0]").value("Status is required"));
    }

    @Test
    void updateStatus_returnsNotFound_whenUserMissing() throws Exception {
        var id = UUID.randomUUID();
        doThrow(new UserNotFoundException()).when(userService).updateUserStatus(ADMIN_ID, id, UserStatus.ACTIVE);

        mockMvc.perform(patch(USERS_URL + "/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"ACTIVE\"}")
                        .with(admin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateStatus_returnsConflict_whenAdminChangesOwnStatus() throws Exception {
        doThrow(new OwnStatusChangeException()).when(userService)
                .updateUserStatus(ADMIN_ID, ADMIN_ID, UserStatus.SUSPENDED);

        mockMvc.perform(patch(USERS_URL + "/" + ADMIN_ID + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"SUSPENDED\"}")
                        .with(admin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("You cannot change your own account status"));
    }

    @Test
    void updateMe_returnsUpdatedUser() throws Exception {
        var id = UUID.randomUUID();
        var request = new UpdateUserRequestDto("Janet", null, null, "+254707127309", null);
        when(userService.updateCurrentUser(id, request))
                .thenReturn(new UserResponseDto(id.toString(), "Janet", "Doe", "jane.doe@gmail.com", List.of("USER")));

        mockMvc.perform(patch(USERS_URL + "/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\": \"Janet\", \"phoneNumber\": \"+254707127309\"}")
                        .with(user(id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Janet"));
    }

    @Test
    void updateMe_returnsBadRequest_whenFieldsInvalid() throws Exception {
        mockMvc.perform(patch(USERS_URL + "/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\": \"  \", \"email\": \"not-an-email\", \"phoneNumber\": \"12ab\"}")
                        .with(user(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.firstName").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.phoneNumber").exists());

        verify(userService, never()).updateCurrentUser(any(), any());
    }

    @Test
    void updateMe_returnsBadRequest_whenCurrentPasswordWrong() throws Exception {
        when(userService.updateCurrentUser(any(), any()))
                .thenThrow(new InvalidCurrentPasswordException("Current password is incorrect"));

        mockMvc.perform(patch(USERS_URL + "/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"janet@gmail.com\", \"currentPassword\": \"wrong\"}")
                        .with(user(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Current password is incorrect"));
    }

    @Test
    void updateMe_returnsConflict_whenEmailTaken() throws Exception {
        when(userService.updateCurrentUser(any(), any()))
                .thenThrow(new EmailAlreadyExistsException("An account with this email already exists"));

        mockMvc.perform(patch(USERS_URL + "/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"taken@gmail.com\", \"currentPassword\": \"secret123\"}")
                        .with(user(UUID.randomUUID())))
                .andExpect(status().isConflict());
    }

    @Test
    void updateMe_returnsConflict_whenPhoneNumberTaken() throws Exception {
        when(userService.updateCurrentUser(any(), any()))
                .thenThrow(new PhoneNumberAlreadyExistsException("An account with this phone number already exists"));

        mockMvc.perform(patch(USERS_URL + "/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\": \"+254707127309\"}")
                        .with(user(UUID.randomUUID())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Phone number already exists"))
                .andExpect(jsonPath("$.detail").value("An account with this phone number already exists"));
    }

    @Test
    void updateMe_returnsUnauthorized_withoutToken() throws Exception {
        mockMvc.perform(patch(USERS_URL + "/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\": \"Janet\"}"))
                .andExpect(status().isUnauthorized());
    }

    private static RequestPostProcessor user(UUID id) {
        return jwt().jwt(j -> j.subject(id.toString())).authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private static RequestPostProcessor admin() {
        return jwt().jwt(j -> j.subject(ADMIN_ID.toString())).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
