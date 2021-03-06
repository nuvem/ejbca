/*************************************************************************
 *                                                                       *
 *  EJBCA Community: The OpenSource Certificate Authority                *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/

package org.ejbca.ui.cli.roles;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.authorization.user.AccessMatchType;
import org.cesecore.authorization.user.AccessUserAspectData;
import org.cesecore.authorization.user.matchvalues.AccessMatchValue;
import org.cesecore.authorization.user.matchvalues.AccessMatchValueReverseLookupRegistry;
import org.cesecore.authorization.user.matchvalues.X500PrincipalAccessMatchValue;
import org.cesecore.certificates.ca.CADoesntExistsException;
import org.cesecore.certificates.ca.CAInfo;
import org.cesecore.certificates.ca.CaSessionRemote;
import org.cesecore.roles.AdminGroupData;
import org.cesecore.roles.RoleNotFoundException;
import org.cesecore.roles.access.RoleAccessSessionRemote;
import org.cesecore.roles.management.RoleManagementSessionRemote;
import org.cesecore.util.EjbRemoteHelper;
import org.ejbca.ui.cli.infrastructure.command.CommandResult;
import org.ejbca.ui.cli.infrastructure.parameter.Parameter;
import org.ejbca.ui.cli.infrastructure.parameter.ParameterContainer;
import org.ejbca.ui.cli.infrastructure.parameter.enums.MandatoryMode;
import org.ejbca.ui.cli.infrastructure.parameter.enums.ParameterMode;
import org.ejbca.ui.cli.infrastructure.parameter.enums.StandaloneMode;

/**
 * Adds an admin
 * 
 * @version $Id$
 */
public class AddAdminCommand extends BaseRolesCommand {

    private static final Logger log = Logger.getLogger(AddAdminCommand.class);

    private static final String ROLE_NAME_KEY = "--role";
    private static final String CA_NAME_KEY = "--caname";
    private static final String MATCH_WITH_KEY = "--with";
    private static final String MATCH_TYPE_KEY = "--type";
    private static final String MATCH_VALUE_KEY = "--value";

    {
        registerParameter(new Parameter(ROLE_NAME_KEY, "Role Name", MandatoryMode.MANDATORY, StandaloneMode.ALLOW, ParameterMode.ARGUMENT,
                "Role to add admin to."));
        registerParameter(new Parameter(CA_NAME_KEY, "CA Name", MandatoryMode.MANDATORY, StandaloneMode.ALLOW, ParameterMode.ARGUMENT,
                "Name of issuing CA"));
        registerParameter(new Parameter(MATCH_WITH_KEY, "Value", MandatoryMode.MANDATORY, StandaloneMode.ALLOW, ParameterMode.ARGUMENT,
                "The MatchWith Value"));
        registerParameter(new Parameter(MATCH_TYPE_KEY, "Type", MandatoryMode.OPTIONAL, StandaloneMode.ALLOW, ParameterMode.ARGUMENT,
                "(Ignored) Match operator type. Kept to prevent legacy scripts from breaking. Currently implied by " + MATCH_WITH_KEY +" switch."));
        registerParameter(new Parameter(MATCH_VALUE_KEY, "Value", MandatoryMode.MANDATORY, StandaloneMode.ALLOW, ParameterMode.ARGUMENT,
                "The value to match against."));
    }

    @Override
    public String getMainCommand() {
        return "addadmin";
    }

    @Override
    public CommandResult execute(ParameterContainer parameters) {
        String roleName = parameters.get(ROLE_NAME_KEY);
        if (EjbRemoteHelper.INSTANCE.getRemoteSession(RoleAccessSessionRemote.class).findRole(roleName) == null) {
            getLogger().error("No such role \"" + roleName + "\".");
            return CommandResult.FUNCTIONAL_FAILURE;
        }
        String caName = parameters.get(CA_NAME_KEY);
        CAInfo caInfo;
        try {
            caInfo = EjbRemoteHelper.INSTANCE.getRemoteSession(CaSessionRemote.class).getCAInfo(getAuthenticationToken(), caName);
        } catch (CADoesntExistsException e) {
            getLogger().error("No such CA \"" + caName + "\".");
            return CommandResult.FUNCTIONAL_FAILURE;
        } catch (AuthorizationDeniedException e) {
            log.error("ERROR: CLI user not authorized to CA");
            return CommandResult.FUNCTIONAL_FAILURE;
        }
        if (caInfo == null) {
            getLogger().error("No such CA \"" + caName + "\".");
            return CommandResult.FUNCTIONAL_FAILURE;
        }
        int caid = caInfo.getCAId();
        AccessMatchValue matchWith = AccessMatchValueReverseLookupRegistry.INSTANCE.lookupMatchValueFromTokenTypeAndName(
                X500PrincipalAccessMatchValue.WITH_SERIALNUMBER.getTokenType(), parameters.get(MATCH_WITH_KEY));
        if (matchWith == null) {
            getLogger().error("No such thing to match with as \"" + parameters.get(MATCH_WITH_KEY) + "\".");
            return CommandResult.FUNCTIONAL_FAILURE;
        }
        final AccessMatchType matchType;
        if (matchWith.getAvailableAccessMatchTypes().isEmpty()) {
            matchType = AccessMatchType.TYPE_UNUSED;
        } else {
            // Just grab the first one, since we according to ECA-3164 only will have a single allowed match operator for each match key
            matchType = matchWith.getAvailableAccessMatchTypes().get(0);
        }
        final String matchTypeParam = parameters.get(MATCH_TYPE_KEY);
        if (StringUtils.isNotEmpty(matchTypeParam)) {
            log.info("Parameter " + MATCH_TYPE_KEY + " is ignored. " + MATCH_WITH_KEY + " value " + matchWith.name() + " implies " + matchType.name() + ".");
        }
        String matchValue = parameters.get(MATCH_VALUE_KEY);
        AccessUserAspectData accessUser = new AccessUserAspectData(roleName, caid, matchWith, matchType, matchValue);
        Collection<AccessUserAspectData> accessUsers = new ArrayList<AccessUserAspectData>();
        accessUsers.add(accessUser);
        try {
            EjbRemoteHelper.INSTANCE.getRemoteSession(RoleManagementSessionRemote.class).addSubjectsToRole(getAuthenticationToken(),
                    EjbRemoteHelper.INSTANCE.getRemoteSession(RoleAccessSessionRemote.class).findRole(roleName), accessUsers);
            return CommandResult.SUCCESS;
        } catch (RoleNotFoundException e) {
            throw new IllegalStateException("Previously found role suddenly not found.", e);
        } catch (AuthorizationDeniedException e) {
            log.error("ERROR: CLI user not authorized to edit role");
            return CommandResult.FUNCTIONAL_FAILURE;
        }

    }

    @Override
    public String getCommandDescription() {
        return "Adds an administrator";
    }

    @Override
    public String getFullHelpText() {
        StringBuilder sb = new StringBuilder();
        sb.append(getCommandDescription() + ".\n");
        Collection<AdminGroupData> roles = EjbRemoteHelper.INSTANCE.getRemoteSession(RoleManagementSessionRemote.class).getAllRolesAuthorizedToEdit(
                getAuthenticationToken());
        Collections.sort((List<AdminGroupData>) roles);
        String availableRoles = "";
        for (AdminGroupData role : roles) {
            availableRoles += (availableRoles.length() == 0 ? "" : ", ") + "\"" + role.getRoleName() + "\"";
        }
        sb.append("Available Roles: " + availableRoles + "\n");

        String availableCas = "";
        for (String caname : EjbRemoteHelper.INSTANCE.getRemoteSession(CaSessionRemote.class).getAuthorizedCaNames(getAuthenticationToken())) {
            availableCas += (availableCas.length() == 0 ? "" : ", ") + "\"" + caname + "\"";
        }
        sb.append("Available CAs: " + availableCas + "\n");
        String availableMatchers = "";
        for (AccessMatchValue currentMatchWith : X500PrincipalAccessMatchValue.values()) {
            if (!X500PrincipalAccessMatchValue.NONE.equals(currentMatchWith)) {
                availableMatchers += (availableMatchers.length() == 0 ? "" : ", ") + currentMatchWith;
            }
        }
        sb.append("Match with is one of: " + availableMatchers + "\n");
        return sb.toString();
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

}
