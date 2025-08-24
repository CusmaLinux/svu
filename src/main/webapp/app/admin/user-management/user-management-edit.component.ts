import { type Ref, defineComponent, inject, ref, computed, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { useVuelidate } from '@vuelidate/core';
import { email, maxLength, minLength, required, sameAs, requiredIf, helpers } from '@vuelidate/validators';
import { useRoute, useRouter } from 'vue-router';
import UserManagementService from './user-management.service';
import { type IUser, User } from '@/shared/model/user.model';
import { useAlertService } from '@/shared/alert/alert.service';
import languages from '@/shared/config/languages';

const loginValidator = (value: string) => {
  if (!value) {
    return true;
  }
  return /^[a-zA-Z0-9!$&*+=?^_`{|}~.-]+@[a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*$|^[_.@A-Za-z0-9-]+$/.test(value);
};

export default defineComponent({
  compatConfig: { MODE: 3 },
  name: 'JhiUserManagementEdit',
  setup() {
    const route = useRoute();
    const router = useRouter();
    const { t } = useI18n();
    const alertService = inject('alertService', () => useAlertService(), true);
    const userManagementService = inject('userManagementService', () => new UserManagementService(), true);

    const userAccount: Ref<IUser> = ref(new User());
    const isSaving = ref(false);
    const authorities: Ref<string[]> = ref([]);

    const password: Ref<string> = ref('');
    const confirmPassword: Ref<string> = ref('');

    const languageOptions = computed(() => [
      { value: null, text: '-- Seleccione un lenguaje --', disabled: true },
      ...Object.keys(languages()).map(key => ({
        value: key,
        text: languages()[key].name,
      })),
    ]);

    const rules = computed(() => ({
      userAccount: {
        login: {
          required,
          maxLength: maxLength(254),
          pattern: loginValidator,
        },
        firstName: {
          maxLength: maxLength(50),
        },
        lastName: {
          maxLength: maxLength(50),
        },
        email: {
          required,
          email,
          minLength: minLength(5),
          maxLength: maxLength(50),
        },
      },
      password: {
        required: helpers.withMessage(
          t('global.messages.validate.newpassword.required'),
          requiredIf(() => !userAccount.value.id),
        ),
        minLength: helpers.withMessage(t('global.messages.validate.newpassword.minlength'), minLength(4)),
        maxLength: helpers.withMessage(t('global.messages.validate.newpassword.maxlength'), maxLength(50)),
      },
      confirmPassword: {
        required: helpers.withMessage(
          t('global.messages.validate.confirmpassword.required'),
          requiredIf(() => !!password.value),
        ),
        sameAs: helpers.withMessage(t('global.messages.error.dontmatch'), sameAs(password)),
      },
    }));

    const v$ = useVuelidate(rules, { userAccount, password, confirmPassword });

    const previousState = () => router.go(-1);

    const returnToList = () => {
      previousState();
    };

    const getToastMessageFromHeader = (res: any): string => {
      const param = decodeURIComponent(res.headers['x-ventanillaunicaapp-params'].replace(/\+/g, ' '));
      return t(res.headers['x-ventanillaunicaapp-alert'], { param }).toString();
    };

    const save = async () => {
      isSaving.value = true;
      try {
        if (password.value) {
          userAccount.value.password = password.value;
        }

        let response;
        if (userAccount.value.id) {
          response = await userManagementService.update(userAccount.value);
          alertService.showInfo(getToastMessageFromHeader(response));
        } else {
          response = await userManagementService.create(userAccount.value);
          alertService.showSuccess(getToastMessageFromHeader(response));
        }
        returnToList();
      } catch (error: any) {
        alertService.showHttpError(error.response);
      } finally {
        isSaving.value = false;
      }
    };

    onMounted(async () => {
      try {
        const response = await userManagementService.retrieveAuthorities();
        authorities.value = response.data;

        const userId = route.params?.userId;
        if (userId) {
          const userResponse = await userManagementService.get(userId as string);
          userAccount.value = userResponse.data;
        }
      } catch (error: any) {
        console.error('Failed to load initial data:', error);
        alertService.showHttpError(error.response);
      }
    });

    return {
      userAccount,
      password,
      confirmPassword,
      isSaving,
      authorities,
      previousState,
      save,
      v$,
      languages: languages(),
      languageOptions,
      t,
    };
  },
});
