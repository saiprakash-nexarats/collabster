package com.collabsphere_first.account.service;

public interface AccountLifecycleService {

    void deactivateAccount(Long userId);

    void deleteAccount(Long userId);

    void suspendAccount(Long adminId, Long targetUserId, String reason);

    void reactivateAccount(Long adminId, Long targetUserId);
}
