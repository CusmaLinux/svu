import { useAccountStore } from '@/shared/config/store/account-store';

type Action = 'view' | 'create' | 'edit' | 'delete' | 'assign' | 'respond' | 'resolve' | 'close' | 'assign_due_date';
type Subject = 'pqrs' | 'responses' | 'attached_files' | 'users' | 'dashboard' | 'notifications' | 'offices' | 'reports' | 'admin';

const permissions = {
  admin: {
    all: {
      all: true,
    },
  },
  front_desk: {
    pqrs: ['view', 'create', 'edit'],
    responses: ['view', 'create', 'edit'],
    notifications: ['view', 'create', 'edit'],
  },
  functionary: {
    pqrs: ['view', 'respond', 'resolve'],
    responses: ['view', 'create'],
    notifications: ['view', 'create', 'edit'],
  },
  user: {
    pqrs: ['view'],
    responses: ['view'],
    notifications: ['view'],
  },
  anonymous: {},
};

export function usePermissions() {
  const accountStore = useAccountStore();

  /**
   * Checks if the current user has a specific permission.
   * @param action The action to perform (e.g., 'edit')
   * @param subject The entity to perform the action on (e.g., 'pqrs')
   * @returns boolean
   */
  function can(action: Action, subject: Subject): boolean {
    const role = accountStore.userRole;

    if (permissions.admin.all.all) {
      if (role === 'admin') return true;
    }

    const rolePermissions = permissions[role];
    if (!rolePermissions) {
      return false;
    }

    const subjectPermissions = rolePermissions[subject as keyof typeof rolePermissions];
    if (!subjectPermissions) {
      return false;
    }

    return subjectPermissions.includes(action);
  }

  return { can };
}
