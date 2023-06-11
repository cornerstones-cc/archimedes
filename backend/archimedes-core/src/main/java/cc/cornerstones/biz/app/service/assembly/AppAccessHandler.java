package cc.cornerstones.biz.app.service.assembly;

import cc.cornerstones.almond.constants.InfrastructureConstants;
import cc.cornerstones.almond.exceptions.AbcAuthorizationException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.app.entity.AppDo;
import cc.cornerstones.biz.app.entity.AppMemberDo;
import cc.cornerstones.biz.app.persistence.AppMemberRepository;
import cc.cornerstones.biz.app.persistence.AppRepository;
import cc.cornerstones.biz.app.share.constants.AppMembershipEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.LinkedList;
import java.util.List;

@Component
public class AppAccessHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppAccessHandler.class);

    @Autowired
    private AppMemberRepository appMemberRepository;

    @Autowired
    private AppRepository appRepository;

    public void verifyAdminAuthorization(
            Long appUid,
            UserProfile operatingUserProfile) throws AbcAuthorizationException {
        AppDo appDo = this.appRepository.findByUid(appUid);
        if (appDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s:uid=%d", AppDo.RESOURCE_SYMBOL, appUid));
        }

        verifyAdminAuthorization(appDo, operatingUserProfile);
    }

    public void verifyAdminAuthorization(
            AppDo appDo,
            UserProfile operatingUserProfile) throws AbcAuthorizationException {
        //
        // Below users are allowed to admin the app
        // 1) root;
        // 2) the owner of the app;
        // 3) the members of the app who have owner membership;
        //
        boolean allowedOperation = false;
        if (InfrastructureConstants.ROOT_USER_UID.equals(operatingUserProfile.getUid())) {
            allowedOperation = true;
        } else if (operatingUserProfile.getUid().equals(appDo.getOwner())) {
            allowedOperation = true;
        } else {
            AppMemberDo appMemberDo = this.appMemberRepository.findByAppUidAndUserUid(appDo.getUid(),
                    operatingUserProfile.getUid());
            if (appMemberDo != null
                    && AppMembershipEnum.OWNER.equals(appMemberDo.getMembership())) {
                allowedOperation = true;
            }
        }
        if (!allowedOperation) {
            throw new AbcAuthorizationException("you are not allowed to perform this operation");
        }
    }

    public void verifyReadAuthorization(
            Long appUid,
            UserProfile operatingUserProfile) throws AbcAuthorizationException {
        AppDo appDo = this.appRepository.findByUid(appUid);
        if (appDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s:uid=%d", AppDo.RESOURCE_SYMBOL, appUid));
        }

        verifyReadAuthorization(appDo, operatingUserProfile);
    }

    public void verifyReadAuthorization(
            AppDo appDo,
            UserProfile operatingUserProfile) throws AbcAuthorizationException {
        //
        // Below users are allowed to read the app
        // 1) root;
        // 2) the owner of the app;
        // 3) the members of the app no matter whatever membership;
        //
        boolean allowedOperation = false;
        if (InfrastructureConstants.ROOT_USER_UID.equals(operatingUserProfile.getUid())) {
            allowedOperation = true;
        } else if (operatingUserProfile.getUid().equals(appDo.getOwner())) {
            allowedOperation = true;
        } else {
            AppMemberDo appMemberDo = this.appMemberRepository.findByAppUidAndUserUid(appDo.getUid(),
                    operatingUserProfile.getUid());
            if (appMemberDo != null) {
                allowedOperation = true;
            }
        }
        if (!allowedOperation) {
            throw new AbcAuthorizationException("you are not allowed to perform this operation");
        }
    }

    public void verifyWriteAuthorization(
            Long appUid,
            UserProfile operatingUserProfile) throws AbcAuthorizationException {
        AppDo appDo = this.appRepository.findByUid(appUid);
        if (appDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s:uid=%d", AppDo.RESOURCE_SYMBOL, appUid));
        }

        verifyWriteAuthorization(appDo, operatingUserProfile);
    }

    public void verifyWriteAuthorization(
            AppDo appDo,
            UserProfile operatingUserProfile) throws AbcAuthorizationException {
        //
        // Below users are allowed to write the app
        // 1) root;
        // 2) the owner of the app;
        // 3) the members of the app who have maintainer/owner membership;
        //
        boolean allowedOperation = false;
        if (InfrastructureConstants.ROOT_USER_UID.equals(operatingUserProfile.getUid())) {
            allowedOperation = true;
        } else if (operatingUserProfile.getUid().equals(appDo.getOwner())) {
            allowedOperation = true;
        } else {
            AppMemberDo appMemberDo = this.appMemberRepository.findByAppUidAndUserUid(appDo.getUid(),
                    operatingUserProfile.getUid());
            if (appMemberDo != null
                    && (AppMembershipEnum.MAINTAINER.equals(appMemberDo.getMembership())
                    || AppMembershipEnum.OWNER.equals(appMemberDo.getMembership()))) {
                allowedOperation = true;
            }
        }
        if (!allowedOperation) {
            throw new AbcAuthorizationException("you are not allowed to perform this operation");
        }
    }

    /**
     * Collect apps that are authorized to read by the operating user
     *
     * @param result
     * @param operatingUserProfile
     * @return true - with authorization restrictions, false - without authorization restrictions
     * @throws AbcAuthorizationException
     */
    public boolean collectAppsThatAreAuthorizedToRead(
            List<Long> result,
            UserProfile operatingUserProfile) throws AbcAuthorizationException {
        //
        // Find out which apps the user is allowed to read
        // 1) root;
        // 2) the owner of the app;
        // 3) the members of the app who have maintainer/owner membership;
        //

        if (InfrastructureConstants.ROOT_USER_UID.equals(operatingUserProfile.getUid())) {
            // root user
            // no limit
            return false;
        } else {
            if (result == null) {
                result = new LinkedList<>();
            }

            //
            // the owner of an app
            //
            List<Long> ownedAppUidList = this.appRepository.findByOwner(operatingUserProfile.getUid());
            if (!CollectionUtils.isEmpty(ownedAppUidList)) {
                result.addAll(ownedAppUidList);
            }

            //
            // the members of an app no matter whatever membership;
            //
            List<AppMemberDo> appMemberDoList = this.appMemberRepository.findByUserUid(
                    operatingUserProfile.getUid());
            if (!CollectionUtils.isEmpty(appMemberDoList)) {
                for (AppMemberDo appMemberDo : appMemberDoList) {
                    if (!result.contains(appMemberDo.getAppUid())) {
                        result.add(appMemberDo.getAppUid());
                    }
                }
            }

            if (CollectionUtils.isEmpty(result)) {
                throw new AbcAuthorizationException("you are not allowed to perform this operation");
            }

            return true;
        }
    }
}
