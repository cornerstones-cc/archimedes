package cc.cornerstones.biz.administration.usermanagement.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.administration.usermanagement.dto.AccountTypeDto;
import cc.cornerstones.biz.administration.usermanagement.dto.CreateAccountTypeDto;
import cc.cornerstones.biz.administration.usermanagement.dto.UpdateAccountTypeDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface AccountTypeService {
    AccountTypeDto createAccountType(
            CreateAccountTypeDto createAccountTypeDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void updateAccountType(
            Long uid,
            UpdateAccountTypeDto updateAccountTypeDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<String> listAllReferencesToAccountType(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void deleteAccountType(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    AccountTypeDto getAccountType(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<AccountTypeDto> listingQueryAccountTypes(
            Long uid,
            String name,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    Page<AccountTypeDto> pagingQueryAccountTypes(
            Long uid,
            String name,
            String description,
            List<Long> userUidListOfLastModifiedBy,
            List<String> lastModifiedTimestampAsStringList,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

}
