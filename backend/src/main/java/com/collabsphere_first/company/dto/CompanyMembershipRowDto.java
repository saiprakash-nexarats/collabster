package com.collabsphere_first.company.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyMembershipRowDto {
    private CompanyDto company;
    private String role;
    private String membershipState;
}
