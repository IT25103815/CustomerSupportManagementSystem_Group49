package com.group49.support.util;
import com.group49.support.model.User;
/** Simple Factory, matching the lecture's factory-class example. */
public final class AccessPolicyFactory {
    public static AccessPolicy forUser(User user) {
        if(user==null || !"ACTIVE".equals(user.status())) return new CustomerAccessPolicy();
        return switch(user.role()) {
            case "CUSTOMER_SUPPORT_MANAGER" -> new ManagerAccessPolicy();
            case "IT_SUPPORT_COORDINATOR" -> new AccountAdminAccessPolicy();
            case "OPERATIONS_EXECUTIVE", "QUALITY_ASSURANCE_SUPERVISOR" -> new ReportingAccessPolicy();
            case "CUSTOMER_RELATIONS_OFFICER" -> new HelpEditorAccessPolicy();
            default -> new CustomerAccessPolicy();
        };
    }
}
final class CustomerAccessPolicy implements AccessPolicy {
    public boolean canManageUsers(){return false;} public boolean canReport(){return false;} public boolean canEditHelp(){return false;}
}
final class ManagerAccessPolicy implements AccessPolicy {
    public boolean canManageUsers(){return true;} public boolean canReport(){return true;} public boolean canEditHelp(){return true;}
}
final class AccountAdminAccessPolicy implements AccessPolicy {
    public boolean canManageUsers(){return true;} public boolean canReport(){return false;} public boolean canEditHelp(){return false;}
}
final class ReportingAccessPolicy implements AccessPolicy {
    public boolean canManageUsers(){return false;} public boolean canReport(){return true;} public boolean canEditHelp(){return false;}
}
final class HelpEditorAccessPolicy implements AccessPolicy {
    public boolean canManageUsers(){return false;} public boolean canReport(){return false;} public boolean canEditHelp(){return true;}
}
