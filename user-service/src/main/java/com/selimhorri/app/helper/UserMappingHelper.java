package com.selimhorri.app.helper;

import java.util.Optional;

import com.selimhorri.app.domain.Credential;
import com.selimhorri.app.domain.User;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.dto.UserDto;

public interface UserMappingHelper {
	
	public static UserDto map(final User user) {
		return UserDto.builder()
				.userId(user.getUserId())
				.firstName(user.getFirstName())
				.lastName(user.getLastName())
				.imageUrl(user.getImageUrl())
				.email(user.getEmail())
				.phone(user.getPhone())
				.credentialDto(Optional.ofNullable(user.getCredential())
						.map(credential -> CredentialDto.builder()
								.credentialId(credential.getCredentialId())
								.username(credential.getUsername())
								.password(credential.getPassword())
								.roleBasedAuthority(credential.getRoleBasedAuthority())
								.isEnabled(credential.getIsEnabled())
								.isAccountNonExpired(credential.getIsAccountNonExpired())
								.isAccountNonLocked(credential.getIsAccountNonLocked())
								.isCredentialsNonExpired(credential.getIsCredentialsNonExpired())
								.build())
						.orElse(null))
				.build();
	}
	
	public static User map(final UserDto userDto) {
		User user = User.builder()
				.userId(userDto.getUserId())
				.firstName(userDto.getFirstName())
				.lastName(userDto.getLastName())
				.imageUrl(userDto.getImageUrl())
				.email(userDto.getEmail())
				.phone(userDto.getPhone())
				.build();
		
		Optional.ofNullable(userDto.getCredentialDto())
				.ifPresent(credentialDto -> {
					Credential credential = Credential.builder()
							.credentialId(credentialDto.getCredentialId())
							.username(credentialDto.getUsername())
							.password(credentialDto.getPassword())
							.roleBasedAuthority(credentialDto.getRoleBasedAuthority())
							.isEnabled(credentialDto.getIsEnabled())
							.isAccountNonExpired(credentialDto.getIsAccountNonExpired())
							.isAccountNonLocked(credentialDto.getIsAccountNonLocked())
							.isCredentialsNonExpired(credentialDto.getIsCredentialsNonExpired())
							.user(user)
							.build();
					user.setCredential(credential);
				});
		
		return user;
	}
	
	
	
}






