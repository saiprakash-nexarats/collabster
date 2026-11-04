package com.collabsphere_first.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @Size(max = 50, message = "First name cannot exceed 50 characters.")
    private String firstName;

    @Size(max = 50, message = "Last name cannot exceed 50 characters.")
    private String lastName;

    private String displayName;

    private String username;

    private String termsVersion;

    private String privacyPolicyVersion;

    @NotBlank(message = "Email is required.")
    @Email(message = "Invalid email format.")
    @Size(max = 100)
    private String email;

    @NotBlank(message = "Password is required.")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters.")
    private String password;

    private String confirmPassword;

    public void normalize() {
        if ((firstName == null || firstName.isBlank()) && displayName != null && !displayName.isBlank()) {
            String[] parts = displayName.trim().split("\\s+", 2);
            this.firstName = parts[0];
            this.lastName = parts.length > 1 ? parts[1] : "Member";
        }
        if (this.firstName == null || this.firstName.isBlank()) {
            this.firstName = "Member";
        }
        if (this.lastName == null || this.lastName.isBlank()) {
            this.lastName = "";
        }
        if (this.confirmPassword == null || this.confirmPassword.isBlank()) {
            this.confirmPassword = this.password;
        }
    }
}