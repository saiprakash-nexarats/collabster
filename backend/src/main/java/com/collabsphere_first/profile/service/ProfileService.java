package com.collabsphere_first.profile.service;

import com.collabsphere_first.profile.dto.request.CreateProfileRequest;
import com.collabsphere_first.profile.dto.response.ProfileResponse;

public interface ProfileService {

    ProfileResponse createProfile(CreateProfileRequest request);

    ProfileResponse getCurrentUserProfile();

    ProfileResponse updateProfile(CreateProfileRequest request);

}
